package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Bluetooth connectivity operations.
 *
 * Defines the contract for Bluetooth device scanning, connection management,
 * data transmission, and state observation. Implementations can be mocked
 * for testing purposes.
 */
interface IBluetoothManager {

    // --- State Observations ---

    /** Stream of discovered Bluetooth devices. */
    val scannedDevices: StateFlow<List<BluetoothDeviceModel>>

    /** Current Bluetooth connection state. */
    val connectionState: StateFlow<ConnectionState>

    /** Latest RSSI reading in dBm. */
    val rssiValue: StateFlow<Int>

    /** Connection health metrics. */
    val health: StateFlow<ConnectionHealth>

    /** Latest telemetry sample, if any. */
    val telemetry: StateFlow<Telemetry?>

    /** Rolling RTT history. */
    val rttHistory: StateFlow<List<Long>>

    /** True when auto-reconnect is enabled. */
    val autoReconnectEnabled: StateFlow<Boolean>

    /** Emergency stop status. */
    val isEmergencyStopActive: StateFlow<Boolean>

    /** Connected device name and type for UI display. */
    val connectedDeviceInfo: StateFlow<String?>

    /** Debug log entries for UI display. */
    val debugLogs: StateFlow<List<com.metelci.ardunakon.model.LogEntry>>

    /** Telemetry history manager for charts and stats. */
    val telemetryHistoryManager: com.metelci.ardunakon.telemetry.TelemetryHistoryManager

    /** Consolidated state flow for efficient UI recomposition. */
    val combinedState: StateFlow<CombinedConnectionState>

    // --- Scanning Operations ---

    /** Starts Bluetooth device discovery. */
    fun startScan()

    /** Stops Bluetooth device discovery. */
    fun stopScan()

    // --- Connection Operations ---

    /**
     * Connects to a selected Bluetooth device.
     *
     * @param deviceModel Target device model.
     * @param isAutoReconnect True when invoked from auto-reconnect.
     */
    fun connectToDevice(deviceModel: BluetoothDeviceModel, isAutoReconnect: Boolean = false)

    /** Disconnects the active Bluetooth connection and stops auto-reconnect. */
    fun disconnect()

    /**
     * Attempts to reconnect to the last saved device.
     *
     * @return True when a reconnect attempt was initiated.
     */
    fun reconnectSavedDevice(): Boolean

    /**
     * Disconnects and enters E-STOP state.
     */
    fun disconnectAllForEStop()

    // --- Data Operations ---

    /**
     * Sends a data packet to the active connection.
     *
     * @param data Packet payload.
     * @param force True to bypass duplicate suppression or rate limits.
     */
    fun sendData(data: ByteArray, force: Boolean = false)

    /**
     * Sends data to the active connection (alias for [sendData]).
     */
    fun sendDataToAll(data: ByteArray, force: Boolean = false)

    // --- Control Operations ---

    /**
     * Enables or disables emergency stop mode.
     *
     * @param active True to enable E-STOP.
     */
    fun setEmergencyStop(active: Boolean)

    /**
     * Enables or disables auto-reconnect.
     *
     * @param enabled True to enable auto-reconnect.
     */
    fun setAutoReconnectEnabled(enabled: Boolean)

    /** Resets reconnect backoff state after persistent failures. */
    fun resetCircuitBreaker()

    /** Requests an RSSI update from the active connection. */
    fun requestRssi()

    /**
     * Holds offline after an E-STOP reset to prevent auto-reconnect churn.
     */
    fun holdOfflineAfterEStopReset()

    /**
     * Allows reflection-based fallback for legacy Bluetooth stacks.
     */
    var allowReflectionFallback: Boolean

    // --- Lifecycle ---

    /**
     * Set foreground/background mode for battery optimization.
     *
     * @param foreground True when app is in foreground.
     */
    fun setForegroundMode(foreground: Boolean)

    /** Releases resources and cancels background work. */
    fun cleanup()

    // --- Logging ---

    /**
     * Adds a log entry to the debug log stream.
     *
     * @param message Log message.
     * @param type Log severity type.
     */
    fun log(message: String, type: LogType = LogType.INFO)
}
