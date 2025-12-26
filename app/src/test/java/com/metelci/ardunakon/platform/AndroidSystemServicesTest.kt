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
import org.robolectric.annotation.Config
import android.os.Build.VERSION_CODES

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
    fun bluetoothEnabled_is_false_when_adapter_missing() {
        val bluetoothManager = mockk<BluetoothManager>()
        every { bluetoothManager.adapter } returns null

        val context = mockk<Context>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager

        val services = AndroidSystemServices(context, fakePlatform(sdk = 34))
        assertFalse(services.isBluetoothEnabled())
        assertTrue(services.getBluetoothAdapter() == null)
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

    @Test
    fun vibrate_uses_vibrator_manager_on_android12_plus() {
        val context = mockk<Context>(relaxed = true)
        val vibratorManager = mockk<android.os.VibratorManager>(relaxed = true)
        val vibrator = mockk<android.os.Vibrator>(relaxed = true)

        every { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) } returns vibratorManager
        every { vibratorManager.defaultVibrator } returns vibrator

        val services = AndroidSystemServices(context, fakePlatform(sdk = 31))
        services.vibrate(100)

        io.mockk.verify { vibrator.vibrate(any<android.os.VibrationEffect>()) }
    }

    @Test
    fun vibrate_uses_vibrator_on_pre_android12() {
        val context = mockk<Context>(relaxed = true)
        val vibrator = mockk<android.os.Vibrator>(relaxed = true)

        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator

        // SDK 26+ (Oreo) uses VibrationEffect
        val services = AndroidSystemServices(context, fakePlatform(sdk = 26))
        services.vibrate(100)
        io.mockk.verify { vibrator.vibrate(any<android.os.VibrationEffect>()) }
    }

    @Test
    @Config(sdk = [VERSION_CODES.N_MR1])
    fun vibrate_legacy_api_on_pre_oreo() {
        val context = mockk<Context>(relaxed = true)
        val vibrator = mockk<android.os.Vibrator>(relaxed = true)
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator

        // SDK < 26 uses legacy vibrate(Long)
        val services = AndroidSystemServices(context, fakePlatform(sdk = 25))
        services.vibrate(100)
        @Suppress("DEPRECATION")
        io.mockk.verify { vibrator.vibrate(100L) }
    }

    @Test
    @Config(sdk = [VERSION_CODES.O])
    fun vibrate_uses_vibration_effect_on_oreo() {
        val context = mockk<Context>(relaxed = true)
        val vibrator = mockk<android.os.Vibrator>(relaxed = true)
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator

        val services = AndroidSystemServices(context, fakePlatform(sdk = 26))
        services.vibrate(100)
        io.mockk.verify { vibrator.vibrate(any<android.os.VibrationEffect>()) }
    }

    @Test
    fun vibrate_handles_exceptions_gracefully() {
        val context = mockk<Context>()
        every { context.getSystemService(any()) } throws RuntimeException("Vibration failed")

        val services = AndroidSystemServices(context, fakePlatform(sdk = 31))
        // Should not throw
        services.vibrate(100)
    }

    @Test
    fun vibrate_handles_exceptions_pre_android12_gracefully() {
        val context = mockk<Context>()
        every { context.getSystemService(any()) } throws RuntimeException("Vibration failed")

        val services = AndroidSystemServices(context, fakePlatform(sdk = 30))
        // Should not throw
        services.vibrate(100)
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
