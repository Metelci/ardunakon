package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * Common interface for Bluetooth connection managers (Classic and BLE).
 */
interface BluetoothConnectionManager {
    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun send(data: ByteArray)
    fun requestRssi()
    fun cleanup()

    // For auto-reconnect logic or stats
    fun getPacketStats(): NetworkStats
}

data class NetworkStats(
    val sent: Long,
    val dropped: Long,
    val failed: Long
)
