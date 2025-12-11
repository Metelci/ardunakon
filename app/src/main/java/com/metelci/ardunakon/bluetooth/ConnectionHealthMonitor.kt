package com.metelci.ardunakon.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Connection Health Monitor - Tracks connection liveness and manages reconnection
 * 
 * Responsibilities:
 * - Heartbeat timeout detection
 * - Exponential backoff for reconnection
 * - Missed ACK threshold tracking
 * - Auto-reconnect scheduling
 */
class ConnectionHealthMonitor(
    private val scope: CoroutineScope,
    private val config: HealthConfig = HealthConfig(),
    private val callbacks: HealthCallbacks
) {
    /**
     * Configuration for health monitoring thresholds
     */
    data class HealthConfig(
        val heartbeatTimeoutClassicMs: Long = BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS,
        val heartbeatTimeoutBleMs: Long = BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS,
        val missedAckThresholdClassic: Int = BluetoothConfig.MISSED_ACK_THRESHOLD_CLASSIC,
        val missedAckThresholdBle: Int = BluetoothConfig.MISSED_ACK_THRESHOLD_BLE,
        val backoffBaseDelayMs: Long = BluetoothConfig.BACKOFF_BASE_DELAY_MS,
        val backoffMaxDelayMs: Long = BluetoothConfig.BACKOFF_MAX_DELAY_MS,
        val maxReconnectAttempts: Int = BluetoothConfig.MAX_RECONNECT_ATTEMPTS,
        val monitorIntervalMs: Long = BluetoothConfig.RECONNECT_MONITOR_INTERVAL_MS
    )

    /**
     * Callbacks for health events
     */
    interface HealthCallbacks {
        fun onHeartbeatTimeout(reason: String)
        fun onMissedAckThresholdReached()
        fun onReconnectAttempt(attempt: Int, delayMs: Long)
        fun onCircuitBreakerTripped()
        fun getConnectionState(): ConnectionState
        fun getConnectionType(): DeviceType?
        fun isAutoReconnectEnabled(): Boolean
        fun hasSavedDevice(): Boolean
    }

    // State tracking
    private var monitorJob: Job? = null
    private var lastPacketReceivedAt: Long = 0L
    private var lastHeartbeatSentAt: Long = 0L
    private var missedHeartbeatAcks: Int = 0
    private var reconnectAttempts: Int = 0
    private var nextReconnectAt: Long = 0L

    /**
     * Start the health monitor loop
     */
    fun start() {
        stop() // Cancel any existing monitor
        
        monitorJob = scope.launch {
            while (isActive) {
                delay(config.monitorIntervalMs)
                checkHealth()
            }
        }
    }

    /**
     * Stop the health monitor
     */
    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Record that a packet was received (heartbeat ACK, telemetry, etc.)
     * Resets missed ACK counter and updates last packet time
     */
    fun recordInboundPacket() {
        lastPacketReceivedAt = System.currentTimeMillis()
        missedHeartbeatAcks = 0
        resetReconnectBackoff()
    }

    /**
     * Record that a heartbeat was sent
     * Increments missed ACK counter until next inbound packet
     */
    fun recordHeartbeatSent() {
        lastHeartbeatSentAt = System.currentTimeMillis()
        missedHeartbeatAcks++
    }

    /**
     * Reset reconnect backoff state (e.g., after successful connection)
     */
    fun resetReconnectBackoff() {
        reconnectAttempts = 0
        nextReconnectAt = 0L
    }

    /**
     * Increment reconnect attempts and calculate next allowed time
     * @return delay in milliseconds until next reconnect is allowed
     */
    fun scheduleNextReconnect(): Long {
        reconnectAttempts++
        val delay = calculateBackoffDelay(reconnectAttempts)
        nextReconnectAt = System.currentTimeMillis() + delay
        return delay
    }

    /**
     * Check if reconnect is currently allowed
     */
    fun canReconnectNow(): Boolean {
        return System.currentTimeMillis() >= nextReconnectAt
    }

    /**
     * Check if circuit breaker has tripped (too many failed attempts)
     */
    fun isCircuitBreakerTripped(): Boolean {
        return reconnectAttempts >= config.maxReconnectAttempts
    }

    /**
     * Get current reconnect attempt count
     */
    fun getReconnectAttempts(): Int = reconnectAttempts

    /**
     * Get last recorded RTT (round-trip time) in milliseconds
     */
    fun getLastRttMs(): Long {
        return if (lastHeartbeatSentAt > 0 && lastPacketReceivedAt > lastHeartbeatSentAt) {
            lastPacketReceivedAt - lastHeartbeatSentAt
        } else {
            0L
        }
    }

    private fun checkHealth() {
        val state = callbacks.getConnectionState()
        val connectionType = callbacks.getConnectionType()
        
        when (state) {
            ConnectionState.CONNECTED -> checkConnectedHealth(connectionType)
            ConnectionState.DISCONNECTED -> checkDisconnectedHealth()
            else -> { /* No monitoring needed for CONNECTING, ERROR states */ }
        }
    }

    private fun checkConnectedHealth(connectionType: DeviceType?) {
        val now = System.currentTimeMillis()
        val heartbeatTimeout = when (connectionType) {
            DeviceType.CLASSIC -> config.heartbeatTimeoutClassicMs
            DeviceType.LE -> config.heartbeatTimeoutBleMs
            else -> config.heartbeatTimeoutClassicMs
        }
        val missedAckThreshold = when (connectionType) {
            DeviceType.CLASSIC -> config.missedAckThresholdClassic
            DeviceType.LE -> config.missedAckThresholdBle
            else -> config.missedAckThresholdClassic
        }

        // Check for heartbeat timeout
        if (lastPacketReceivedAt > 0 && (now - lastPacketReceivedAt) > heartbeatTimeout) {
            callbacks.onHeartbeatTimeout("No packet received for ${heartbeatTimeout}ms")
            return
        }

        // Check for missed ACK threshold
        if (missedHeartbeatAcks >= missedAckThreshold) {
            callbacks.onMissedAckThresholdReached()
            missedHeartbeatAcks = 0
        }
    }

    private fun checkDisconnectedHealth() {
        if (!callbacks.isAutoReconnectEnabled()) return
        if (!callbacks.hasSavedDevice()) return
        if (!canReconnectNow()) return

        if (isCircuitBreakerTripped()) {
            callbacks.onCircuitBreakerTripped()
            return
        }

        val delay = scheduleNextReconnect()
        callbacks.onReconnectAttempt(reconnectAttempts, delay)
    }

    /**
     * Calculate exponential backoff delay
     * Formula: baseDelay * 2^min(attempts, 3) clamped to maxDelay
     */
    fun calculateBackoffDelay(attempts: Int): Long {
        val multiplier = 1 shl attempts.coerceAtMost(3) // 1, 2, 4, 8
        val delay = config.backoffBaseDelayMs * multiplier
        return delay.coerceAtMost(config.backoffMaxDelayMs)
    }
}
