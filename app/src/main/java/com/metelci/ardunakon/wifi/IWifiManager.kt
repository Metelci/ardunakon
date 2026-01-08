package com.metelci.ardunakon.wifi

import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.model.ConnectionError
import com.metelci.ardunakon.security.EncryptionException
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for WiFi connectivity operations.
 *
 * Defines the contract for WiFi device discovery, connection management,
 * data transmission, and state observation. Implementations can be mocked
 * for testing purposes.
 */
interface IWifiManager {

    // --- State Observations ---

    /** Current WiFi connection state. */
    val connectionState: StateFlow<WifiConnectionState>

    /** Latest connection error. */
    val lastError: StateFlow<ConnectionError?>

    /** Stream of discovered WiFi devices. */
    val scannedDevices: StateFlow<List<WifiDevice>>

    /** True when WiFi discovery/scanning is active. */
    val isScanning: StateFlow<Boolean>

    /** Connected device info for UI display (name/IP). */
    val connectedDeviceInfo: StateFlow<String?>

    /** True when auto-reconnect is enabled. */
    val autoReconnectEnabled: StateFlow<Boolean>

    /** Latest RSSI reading. */
    val rssi: StateFlow<Int>

    /** Latest RTT value in ms. */
    val rtt: StateFlow<Long>

    /** Rolling RTT history. */
    val rttHistory: StateFlow<List<Long>>

    /** Incoming raw data packets. */
    val incomingData: StateFlow<ByteArray?>

    /** Latest telemetry sample. */
    val telemetry: StateFlow<Telemetry?>

    /** True when connection is encrypted. */
    val isEncrypted: StateFlow<Boolean>

    /** Latest encryption error, if any. */
    val encryptionError: StateFlow<EncryptionException?>

    // --- Discovery Operations ---

    /** Starts WiFi device discovery via UDP broadcast. */
    fun startDiscovery()

    /** Stops WiFi device discovery. */
    fun stopDiscovery()

    // --- Connection Operations ---

    /**
     * Connects to a WiFi device.
     *
     * @param ip Target IP address.
     * @param port Target port number.
     */
    fun connect(ip: String, port: Int)

    /** Disconnects the active WiFi connection. */
    fun disconnect()

    // --- Data Operations ---

    /**
     * Sends data to the connected device.
     *
     * @param data Packet payload.
     */
    fun sendData(data: ByteArray)

    // --- Configuration ---

    /**
     * Enables or disables auto-reconnect.
     *
     * @param enabled True to enable auto-reconnect.
     */
    fun setAutoReconnectEnabled(enabled: Boolean)

    /**
     * Sets whether encryption is required.
     *
     * @param required True to require encryption.
     */
    fun setRequireEncryption(required: Boolean)

    /** Returns true if encryption is required. */
    fun isEncryptionRequired(): Boolean

    /** Clears any pending encryption error. */
    fun clearEncryptionError()

    // --- Lifecycle ---

    /** Releases resources and cancels background work. */
    fun cleanup()

    // --- Logging ---

    /**
     * Adds a log entry to the debug log stream.
     *
     * @param message Log message.
     * @param type Log severity type.
     */
    fun log(message: String, type: com.metelci.ardunakon.model.LogType = com.metelci.ardunakon.model.LogType.INFO)
}
