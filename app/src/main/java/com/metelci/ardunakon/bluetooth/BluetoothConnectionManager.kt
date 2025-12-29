package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * Common interface for Bluetooth connection managers (Classic and BLE).
 */
interface BluetoothConnectionManager {
    /**
     * Initiates a connection to the provided Bluetooth device.
     *
     * @param device Target device to connect.
     */
    fun connect(device: BluetoothDevice)

    /**
     * Disconnects the current connection, if any.
     */
    fun disconnect()

    /**
     * Sends raw data over the active connection.
     *
     * @param data Payload to send.
     */
    fun send(data: ByteArray)

    /**
     * Requests an RSSI update from the connection.
     */
    fun requestRssi()

    /**
     * Releases resources and cancels active work.
     */
    fun cleanup()

    // For auto-reconnect logic or stats
    /**
     * Returns packet statistics for monitoring and UI.
     *
     * @return Current packet stats.
     */
    fun getPacketStats(): NetworkStats
}

/**
 * Packet statistics for a connection manager.
 *
 * @property sent Total packets sent.
 * @property dropped Packets dropped before send.
 * @property failed Packets that failed to send.
 */
data class NetworkStats(
    val sent: Long,
    val dropped: Long,
    val failed: Long
)
