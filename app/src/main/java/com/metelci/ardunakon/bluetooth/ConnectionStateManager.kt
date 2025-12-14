package com.metelci.ardunakon.bluetooth

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Bluetooth connection state transitions with debouncing and haptic feedback.
 * Centralizes state management and health tracking for both Classic and BLE connections.
 */
class ConnectionStateManager(private val context: Context, private val config: BluetoothConfig = BluetoothConfig) {
    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _rssi = MutableStateFlow(0)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()

    private val _health = MutableStateFlow(ConnectionHealth())
    val health: StateFlow<ConnectionHealth> = _health.asStateFlow()

    // RTT History for latency sparkline
    private val _rttHistory = MutableStateFlow<List<Long>>(emptyList())
    val rttHistory: StateFlow<List<Long>> = _rttHistory.asStateFlow()

    private var lastStateChangeAt = 0L

    /**
     * Updates connection state with debouncing for noisy transitions.
     * Provides haptic feedback on state changes.
     */
    fun updateState(newState: ConnectionState) {
        val now = System.currentTimeMillis()

        // Debounce noisy DISCONNECTED/ERROR flips to avoid UI spam
        val isNoisyState = newState == ConnectionState.DISCONNECTED || newState == ConnectionState.ERROR
        if (isNoisyState && (now - lastStateChangeAt) < 800) {
            return
        }

        lastStateChangeAt = now
        _state.value = newState

        // Haptic feedback
        when (newState) {
            ConnectionState.CONNECTED -> vibrate(BluetoothConfig.VIBRATION_CONNECTED_MS)
            ConnectionState.DISCONNECTED -> vibrate(BluetoothConfig.VIBRATION_DISCONNECTED_MS)
            ConnectionState.ERROR -> vibrate(BluetoothConfig.VIBRATION_ERROR_MS)
            else -> {}
        }
    }

    /**
     * Updates the current RSSI value.
     */
    fun updateRssi(rssi: Int) {
        _rssi.value = rssi
    }

    /**
     * Updates connection health metrics.
     */
    fun updateHealth(seq: Int, packetAt: Long, failures: Int, lastRttMs: Long = 0L) {
        _health.value = ConnectionHealth(
            lastPacketAt = packetAt,
            rssiFailureCount = failures,
            lastHeartbeatSeq = seq,
            lastHeartbeatAt = System.currentTimeMillis(),
            lastRttMs = lastRttMs
        )
    }

    /**
     * Adds an RTT sample to the history for sparkline visualization.
     */
    fun addRttToHistory(rtt: Long) {
        val current = _rttHistory.value.toMutableList()
        current.add(rtt)
        while (current.size > BluetoothConfig.MAX_RTT_HISTORY) {
            current.removeAt(0)
        }
        _rttHistory.value = current
    }

    /**
     * Resets all state on disconnect.
     */
    fun reset() {
        _state.value = ConnectionState.DISCONNECTED
        _rssi.value = 0
        _health.value = ConnectionHealth()
    }

    private fun vibrate(durationMs: Long) {
        try {
            val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(effect)
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            // Ignore vibration errors (no permission, no vibrator, etc.)
        }
    }
}
