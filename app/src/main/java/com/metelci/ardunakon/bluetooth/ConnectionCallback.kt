package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogType

/**
 * Callback interface for connection managers to report events to the central AppBluetoothManager.
 */
interface ConnectionCallback {
    fun onStateChanged(state: ConnectionState)
    fun onDataReceived(data: ByteArray)
    fun onError(message: String, type: LogType)
    fun onPacketStats(sent: Long, dropped: Long, failed: Long)
    fun onRssiUpdated(rssi: Int)
}
