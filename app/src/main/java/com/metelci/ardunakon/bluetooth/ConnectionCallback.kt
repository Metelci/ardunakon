package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogType

/**
 * Callback interface for connection managers to report events to the central AppBluetoothManager.
 */
interface ConnectionCallback {
    /**
     * Reports connection state changes.
     *
     * @param state New connection state.
     */
    fun onStateChanged(state: ConnectionState)

    /**
     * Delivers raw data received from the connection.
     *
     * @param data Byte payload received.
     */
    fun onDataReceived(data: ByteArray)

    /**
     * Reports an error message with a severity type.
     *
     * @param message Error description.
     * @param type Log severity classification.
     */
    fun onError(message: String, type: LogType)

    /**
     * Updates packet statistics for monitoring and UI.
     *
     * @param sent Total packets sent.
     * @param dropped Packets dropped before send.
     * @param failed Packets that failed to send.
     */
    fun onPacketStats(sent: Long, dropped: Long, failed: Long)

    /**
     * Reports RSSI updates from the connection.
     *
     * @param rssi Signal strength in dBm.
     */
    fun onRssiUpdated(rssi: Int)
}
