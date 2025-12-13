package com.metelci.ardunakon.bluetooth

/**
 * Common interface for both Classic Bluetooth and BLE connections.
 * Defines the contract for connection management and data transmission.
 */
interface BluetoothConnection {
    /**
     * Writes data to the connected device.
     */
    fun write(bytes: ByteArray)

    /**
     * Cancels the connection and cleans up resources.
     */
    fun cancel()

    /**
     * Requests RSSI update (optional, only supported by BLE).
     */
    fun requestRssi() {}
}
