package com.metelci.ardunakon.platform

import android.bluetooth.BluetoothAdapter

/**
 * Abstraction for Android system services.
 * Allows business logic to access services without direct Context dependency.
 */
interface SystemServices {
    /** Get the system BluetoothAdapter, or null if not available */
    fun getBluetoothAdapter(): BluetoothAdapter?
    
    /** Check if Bluetooth is currently enabled */
    fun isBluetoothEnabled(): Boolean
    
    /** Trigger haptic feedback for the specified duration */
    fun vibrate(durationMs: Long)
    
    /** Check if BLUETOOTH_CONNECT permission is granted (Android 12+) */
    fun hasBluetoothConnectPermission(): Boolean
    
    /** Check if BLUETOOTH_SCAN permission is granted (Android 12+) */
    fun hasBluetoothScanPermission(): Boolean
}
