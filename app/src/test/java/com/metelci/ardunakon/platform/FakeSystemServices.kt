package com.metelci.ardunakon.platform

import android.bluetooth.BluetoothAdapter
import io.mockk.mockk

/**
 * Fake implementation of SystemServices for unit testing.
 * Allows tests to control service behavior without Android dependencies.
 *
 * Note: Uses MockK to create a mock BluetoothAdapter since actual Android
 * classes cannot be instantiated in unit tests.
 */
class FakeSystemServices(
    private var bluetoothEnabled: Boolean = true,
    private var hasConnectPermission: Boolean = true,
    private var hasScanPermission: Boolean = true
) : SystemServices {

    private val mockAdapter: BluetoothAdapter = mockk(relaxed = true)

    var vibrationCount = 0
        private set

    var lastVibrationDuration: Long = 0
        private set

    override fun getBluetoothAdapter(): BluetoothAdapter? = mockAdapter

    override fun isBluetoothEnabled(): Boolean = bluetoothEnabled

    override fun vibrate(durationMs: Long) {
        vibrationCount++
        lastVibrationDuration = durationMs
    }

    override fun hasBluetoothConnectPermission(): Boolean = hasConnectPermission

    override fun hasBluetoothScanPermission(): Boolean = hasScanPermission

    fun setBluetoothEnabled(enabled: Boolean) {
        bluetoothEnabled = enabled
    }

    fun setPermissions(connect: Boolean, scan: Boolean) {
        hasConnectPermission = connect
        hasScanPermission = scan
    }

    fun reset() {
        vibrationCount = 0
        lastVibrationDuration = 0
    }
}
