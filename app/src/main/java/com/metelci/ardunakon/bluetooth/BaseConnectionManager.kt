package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.metelci.ardunakon.model.LogType
import java.util.concurrent.atomic.AtomicLong

/**
 * Abstract base class for Bluetooth connection managers (Classic and BLE).
 * 
 * Provides common functionality:
 * - Permission checking
 * - Bluetooth adapter validation
 * - Packet statistics tracking
 * - Common cleanup pattern
 */
abstract class BaseConnectionManager(
    protected val context: Context,
    protected val adapter: BluetoothAdapter?,
    protected val callback: ConnectionCallback
) : BluetoothConnectionManager {

    // Shared packet statistics
    protected val packetsSent = AtomicLong(0)
    protected val packetsDropped = AtomicLong(0)
    protected val packetsFailed = AtomicLong(0)

    /**
     * Check if BLUETOOTH_CONNECT permission is granted (required on Android 12+).
     */
    protected fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == 
                PackageManager.PERMISSION_GRANTED
        } else {
            true // Legacy: implied from manifest
        }
    }

    /**
     * Validate that the Bluetooth adapter is available and enabled.
     * Logs an error via callback if not ready.
     */
    protected fun isBluetoothReady(): Boolean {
        if (adapter == null) {
            callback.onError("Bluetooth adapter not available", LogType.ERROR)
            return false
        }
        if (!adapter.isEnabled) {
            callback.onError("Bluetooth is off", LogType.ERROR)
            return false
        }
        return true
    }

    /**
     * Reset packet statistics counters.
     */
    protected fun resetPacketStats() {
        packetsSent.set(0)
        packetsDropped.set(0)
        packetsFailed.set(0)
    }

    /**
     * Get current packet statistics.
     */
    override fun getPacketStats(): NetworkStats {
        return NetworkStats(packetsSent.get(), packetsDropped.get(), packetsFailed.get())
    }

    /**
     * Default cleanup calls disconnect.
     */
    override fun cleanup() {
        disconnect()
    }

    // Abstract methods for subclass implementation
    abstract override fun connect(device: BluetoothDevice)
    abstract override fun disconnect()
    abstract override fun send(data: ByteArray)
    abstract override fun requestRssi()
}
