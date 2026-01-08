package com.metelci.ardunakon.platform

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of SystemServices.
 * Provides access to Bluetooth, vibration, and permission services.
 */
@Singleton
class AndroidSystemServices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val platformInfo: PlatformInfo
) : SystemServices {

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    override fun getBluetoothAdapter(): BluetoothAdapter? {
        return bluetoothManager?.adapter
    }

    override fun isBluetoothEnabled(): Boolean {
        return getBluetoothAdapter()?.isEnabled == true
    }

    override fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
                return
            }

            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Vibration not critical, silently fail
        }
    }

    override fun hasBluetoothConnectPermission(): Boolean {
        return if (platformInfo.isAtLeastAndroid12()) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 12: implied from manifest
        }
    }

    override fun hasBluetoothScanPermission(): Boolean {
        return if (platformInfo.isAtLeastAndroid12()) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 12: implied from manifest
        }
    }
}
