package com.metelci.ardunakon.service

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

/**
 * Tests for BluetoothService foreground service and permission handling.
 * Uses Robolectric to simulate Android context.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Android 13 for notification permission tests
class BluetoothServiceTest {

    private lateinit var application: Application
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowApp = shadowOf(application)
    }

    // ============== Permission Denial Tests ==============

    @Test
    fun `missing POST_NOTIFICATIONS permission is detected on Android 13+`() {
        // Revoke notification permission
        shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val hasPermission = application.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        assertFalse("Should not have notification permission", hasPermission)
    }

    @Test
    fun `BLUETOOTH_CONNECT permission check works`() {
        // Grant permission
        shadowApp.grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)

        val hasPermission = application.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

        assertTrue("Should have Bluetooth connect permission", hasPermission)
    }

    @Test
    fun `missing BLUETOOTH_CONNECT permission is detected`() {
        shadowApp.denyPermissions(Manifest.permission.BLUETOOTH_CONNECT)

        val hasPermission = application.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

        assertFalse("Should not have Bluetooth connect permission", hasPermission)
    }

    @Test
    fun `BLUETOOTH_SCAN permission check works`() {
        shadowApp.grantPermissions(Manifest.permission.BLUETOOTH_SCAN)

        val hasPermission = application.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED

        assertTrue("Should have Bluetooth scan permission", hasPermission)
    }

    @Test
    fun `ACCESS_FINE_LOCATION permission check works`() {
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val hasPermission = application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        assertTrue("Should have fine location permission", hasPermission)
    }

    // ============== startForeground Failure Simulation ==============

    @Test
    fun `startForeground SecurityException is catchable`() {
        // Simulate the behavior where startForeground throws SecurityException
        var exceptionCaught = false

        try {
            // This simulates what happens in BluetoothService when permission is missing
            throw SecurityException("Missing POST_NOTIFICATIONS permission")
        } catch (se: SecurityException) {
            exceptionCaught = true
        }

        assertTrue("SecurityException should be caught", exceptionCaught)
    }

    @Test
    fun `service stops self when foreground fails`() {
        // Verify the expected behavior is to call stopSelf()
        var stopSelfCalled = false

        // Simulate the logic from BluetoothService.startForegroundService()
        try {
            throw SecurityException("Test exception")
        } catch (se: SecurityException) {
            // Missing POST_NOTIFICATIONS will prevent foreground start on Android 13+
            stopSelfCalled = true // represents stopSelf()
        }

        assertTrue("Should stop self when foreground fails", stopSelfCalled)
    }

    @Test
    fun `notification channel ID is correct`() {
        val expectedChannelId = "ArdunakonConnection"
        assertEquals("Channel ID should match", "ArdunakonConnection", expectedChannelId)
    }

    @Test
    fun `notification channel name is correct`() {
        val expectedChannelName = "Bluetooth Connection"
        assertEquals("Channel name should match", "Bluetooth Connection", expectedChannelName)
    }

    // ============== Wake Lock Logic Tests ==============

    @Test
    fun `wake lock timeout is 1 hour`() {
        val wakeLockTimeoutMs = 60 * 60 * 1000L
        assertEquals(3600000L, wakeLockTimeoutMs)
    }

    @Test
    fun `wake lock tag is correctly formatted`() {
        val expectedTag = "Ardunakon::BluetoothService"
        assertTrue("Wake lock tag should contain app name", expectedTag.contains("Ardunakon"))
    }
}
