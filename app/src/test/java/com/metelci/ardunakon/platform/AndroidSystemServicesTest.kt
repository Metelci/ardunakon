package com.metelci.ardunakon.platform

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidSystemServicesTest {

    @Test
    fun bluetoothEnabled_is_false_when_manager_missing() {
        val context = mockk<Context>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns null

        val services = AndroidSystemServices(context, fakePlatform(sdk = 34))
        assertFalse(services.isBluetoothEnabled())
        assertFalse(services.getBluetoothAdapter() != null)
    }

    @Test
    fun bluetoothEnabled_is_true_when_adapter_enabled() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.isEnabled } returns true

        val bluetoothManager = mockk<BluetoothManager>()
        every { bluetoothManager.adapter } returns adapter

        val context = mockk<Context>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager

        val services = AndroidSystemServices(context, fakePlatform(sdk = 34))
        assertTrue(services.isBluetoothEnabled())
        assertTrue(services.getBluetoothAdapter() === adapter)
    }

    @Test
    fun permission_checks_return_true_pre_android12() {
        val context = mockk<Context>(relaxed = true)
        val services = AndroidSystemServices(context, fakePlatform(sdk = 30))

        assertTrue(services.hasBluetoothConnectPermission())
        assertTrue(services.hasBluetoothScanPermission())
    }

    @Test
    fun permission_checks_use_context_on_android12_plus() {
        val context = mockk<Context>(relaxed = true)
        every {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } returns PackageManager.PERMISSION_DENIED
        every {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_GRANTED

        val services = AndroidSystemServices(context, fakePlatform(sdk = 31))

        assertFalse(services.hasBluetoothConnectPermission())
        assertTrue(services.hasBluetoothScanPermission())
    }

    private fun fakePlatform(sdk: Int): PlatformInfo {
        return object : PlatformInfo {
            override val sdkVersion: Int = sdk
            override val manufacturer: String = "unknown"
            override val model: String = "m"
            override val androidVersion: String = "v"
        }
    }
}
