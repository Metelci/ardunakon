package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.protocol.ProtocolManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages automatic reconnection with exponential backoff and heartbeat monitoring.
 * Implements circuit breaker pattern to prevent infinite reconnect loops.
 */
class ReconnectPolicy(private val scope: CoroutineScope, private val config: BluetoothConfig = BluetoothConfig) {
    private var reconnectAttempts = 0
    private var nextReconnectAt = 0L
    private var heartbeatSeq = 0
    private var lastHeartbeatSentAt = 0L
    private var lastPacketAt = 0L
    private var lastRttMs = 0L
    private var missedHeartbeatAcks = 0

    private val _shouldReconnect = MutableStateFlow(false)
    val shouldReconnect: StateFlow<Boolean> = _shouldReconnect.asStateFlow()

    private var monitorJob: Job? = null
    private var heartbeatJob: Job? = null

    /**
     * Starts the reconnect monitor that periodically checks if reconnection is needed.
     */
    fun startMonitor(
        isEmergencyStop: () -> Boolean,
        getConnectionState: () -> ConnectionState,
        getSavedDevice: () -> BluetoothDeviceModel?,
        isAutoReconnectEnabled: () -> Boolean,
        onReconnect: (BluetoothDeviceModel) -> Unit,
        onCircuitBreakerTripped: () -> Unit,
        onLog: (String, com.metelci.ardunakon.model.LogType) -> Unit
    ) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (!isEmergencyStop()) {
                    val currentState = getConnectionState()

                    // Check if backoff period has elapsed
                    if (now >= nextReconnectAt) {
                        if (_shouldReconnect.value &&
                            (currentState == ConnectionState.DISCONNECTED || currentState == ConnectionState.ERROR) &&
                            getSavedDevice() != null &&
                            isAutoReconnectEnabled()
                        ) {
                            // Check circuit breaker
                            if (reconnectAttempts >= BluetoothConfig.MAX_RECONNECT_ATTEMPTS) {
                                onLog(
                                    "Circuit breaker: Too many failed attempts",
                                    com.metelci.ardunakon.model.LogType.ERROR
                                )
                                _shouldReconnect.value = false
                                onCircuitBreakerTripped()
                            } else {
                                val backoffDelay = calculateBackoffDelay(reconnectAttempts)
                                onLog(
                                    "Auto-reconnecting (attempt ${reconnectAttempts + 1}, backoff ${backoffDelay}ms)",
                                    com.metelci.ardunakon.model.LogType.WARNING
                                )

                                reconnectAttempts++
                                nextReconnectAt = now + backoffDelay

                                getSavedDevice()?.let { device ->
                                    onReconnect(device)
                                }
                            }
                        }
                    }
                }
                delay(1000) // Check every second
            }
        }
    }

    /**
     * Starts the heartbeat/keep-alive ping loop.
     */
    fun startHeartbeat(
        isConnected: () -> Boolean,
        getConnectionType: () -> DeviceType?,
        sendData: (ByteArray) -> Unit,
        onTimeout: (String) -> Unit,
        onHealthUpdate: (Int, Long, Long) -> Unit,
        onLog: (String, com.metelci.ardunakon.model.LogType) -> Unit
    ) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(BluetoothConfig.HEARTBEAT_INTERVAL_MS)
                if (isConnected()) {
                    heartbeatSeq = (heartbeatSeq + 1) and 0xFFFF
                    val heartbeat = ProtocolManager.formatHeartbeatData(heartbeatSeq)
                    sendData(heartbeat)
                    lastHeartbeatSentAt = System.currentTimeMillis()
                    missedHeartbeatAcks++
                    onHealthUpdate(heartbeatSeq, lastPacketAt, lastRttMs)

                    // Check for timeout
                    val sinceLastPacket = System.currentTimeMillis() - lastPacketAt
                    val isBle = getConnectionType() == DeviceType.LE
                    val timeoutMs = if (isBle) {
                        BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS
                    } else {
                        BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS
                    }
                    val ackThreshold = if (isBle) {
                        BluetoothConfig.MISSED_ACK_THRESHOLD_BLE
                    } else {
                        BluetoothConfig.MISSED_ACK_THRESHOLD_CLASSIC
                    }

                    if (missedHeartbeatAcks >= ackThreshold && lastPacketAt > 0 && sinceLastPacket > timeoutMs) {
                        onLog(
                            "Heartbeat timeout after ${sinceLastPacket}ms (missed $missedHeartbeatAcks acks)",
                            com.metelci.ardunakon.model.LogType.ERROR
                        )
                        missedHeartbeatAcks = 0
                        onTimeout("Heartbeat timeout")
                    }
                }
            }
        }
    }

    /**
     * Records an inbound packet, resetting missed ACK counter and calculating RTT.
     */
    fun recordInbound(): Long {
        val now = System.currentTimeMillis()
        lastPacketAt = now
        missedHeartbeatAcks = 0

        if (lastHeartbeatSentAt > 0 && now >= lastHeartbeatSentAt) {
            lastRttMs = now - lastHeartbeatSentAt
        }
        return lastRttMs
    }

    /**
     * Resets the circuit breaker, allowing reconnection attempts.
     */
    fun resetCircuitBreaker() {
        reconnectAttempts = 0
        nextReconnectAt = 0L
    }

    /**
     * Records a successful connection, resetting backoff.
     */
    fun recordSuccess() {
        reconnectAttempts = 0
        nextReconnectAt = 0L
        lastPacketAt = System.currentTimeMillis()
    }

    /**
     * Arms auto-reconnect for future use without immediately triggering.
     */
    fun arm() {
        _shouldReconnect.value = false // Will be set after first manual connect
        reconnectAttempts = 0
        nextReconnectAt = 0L
    }

    /**
     * Enables auto-reconnect.
     */
    fun enable() {
        _shouldReconnect.value = true
    }

    /**
     * Disables auto-reconnect.
     */
    fun disable() {
        _shouldReconnect.value = false
        reconnectAttempts = 0
        nextReconnectAt = 0L
    }

    /**
     * Stops all monitoring jobs.
     */
    fun stop() {
        monitorJob?.cancel()
        heartbeatJob?.cancel()
    }

    /**
     * Gets the last recorded RTT.
     */
    fun getLastRtt(): Long = lastRttMs

    /**
     * Gets the last packet timestamp.
     */
    fun getLastPacketAt(): Long = lastPacketAt

    /**
     * Gets current heartbeat sequence.
     */
    fun getHeartbeatSeq(): Int = heartbeatSeq

    private fun calculateBackoffDelay(attempts: Int): Long = BluetoothConfig.calculateBackoffDelay(attempts)
}
