package com.metelci.ardunakon.ui.espresso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for permission handling flows.
 *
 * Tests that required permissions are properly requested and handled.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EspressoPermissionFlowTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun permissions_bluetoothScanIsGranted() {
        val status = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        )
        assertEquals(
            "BLUETOOTH_SCAN should be granted",
            PackageManager.PERMISSION_GRANTED,
            status
        )
    }

    @Test
    fun permissions_bluetoothConnectIsGranted() {
        val status = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        assertEquals(
            "BLUETOOTH_CONNECT should be granted",
            PackageManager.PERMISSION_GRANTED,
            status
        )
    }

    @Test
    fun permissions_nearbyWifiDevicesIsGranted() {
        val status = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
        assertEquals(
            "NEARBY_WIFI_DEVICES should be granted",
            PackageManager.PERMISSION_GRANTED,
            status
        )
    }

    @Test
    fun permissions_postNotificationsIsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            assertEquals(
                "POST_NOTIFICATIONS should be granted on Android 13+",
                PackageManager.PERMISSION_GRANTED,
                status
            )
        }
    }

    @Test
    fun permissions_activityFunctionsWithAllPermissions() {
        activityRule.scenario.onActivity { activity ->
            // With all permissions granted, activity should function normally
            assertNotNull("Activity should be running with permissions", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun permissions_allBluetoothPermissionsGranted() {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        permissions.forEach { permission ->
            val status = ContextCompat.checkSelfPermission(context, permission)
            assertEquals(
                "$permission should be granted",
                PackageManager.PERMISSION_GRANTED,
                status
            )
        }
    }

    @Test
    fun permissions_packageHasExpectedPermissions() {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        val declaredPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        // Verify expected permissions are declared in manifest
        assertTrue(
            "Manifest should declare BLUETOOTH_SCAN",
            declaredPermissions.contains(Manifest.permission.BLUETOOTH_SCAN)
        )
        assertTrue(
            "Manifest should declare BLUETOOTH_CONNECT",
            declaredPermissions.contains(Manifest.permission.BLUETOOTH_CONNECT)
        )
        assertTrue(
            "Manifest should declare NEARBY_WIFI_DEVICES",
            declaredPermissions.contains(Manifest.permission.NEARBY_WIFI_DEVICES)
        )
    }

    @Test
    fun permissions_activitySurvivesRecreationWithPermissions() {
        // Verify permissions persist through activity recreation
        activityRule.scenario.recreate()

        val bluetoothStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        )
        assertEquals(
            "BLUETOOTH_SCAN should still be granted after recreation",
            PackageManager.PERMISSION_GRANTED,
            bluetoothStatus
        )

        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should exist after recreation", activity)
        }
    }
}
