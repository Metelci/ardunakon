package com.metelci.ardunakon.baselineprofile

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.metelci.ardunakon"
private const val ONBOARDING_PREFS = "onboarding_prefs"
private const val ONBOARDING_VERSION = 1

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(packageName = PACKAGE_NAME) {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        prepareAppState(targetContext, device)

        device.pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)
        device.waitForIdle()

        device.findObject(By.desc("Settings"))?.click()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private fun prepareAppState(context: Context, device: UiDevice) {
        grantRuntimePermissions(device)
        enableBluetooth(device)
        setOnboardingCompleted(context)
    }

    private fun setOnboardingCompleted(context: Context) {
        val prefs = context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("completed", true)
            .putBoolean("skipped", true)
            .putBoolean("in_progress", false)
            .putInt("version", ONBOARDING_VERSION)
            .putInt("current_step", 0)
            .apply()
    }

    private fun grantRuntimePermissions(device: UiDevice) {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.POST_NOTIFICATIONS
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            else -> listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissions.forEach { permission ->
            tryShellCommand(device, "pm grant $PACKAGE_NAME $permission")
        }
    }

    private fun enableBluetooth(device: UiDevice) {
        tryShellCommand(device, "svc bluetooth enable")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tryShellCommand(device, "cmd bluetooth_manager enable")
        }
    }

    private fun tryShellCommand(device: UiDevice, command: String) {
        runCatching { device.executeShellCommand(command) }
    }
}
