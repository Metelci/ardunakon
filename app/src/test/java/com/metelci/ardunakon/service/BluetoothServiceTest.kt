package com.metelci.ardunakon.service

import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.di.TestBluetoothState
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.wifi.WifiManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule

/**
 * Robolectric tests for {@link BluetoothService}.
 *
 * These tests instantiate the real Service and inject fake managers, to ensure JaCoCo
 * captures coverage for the `com.metelci.ardunakon.service` package.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class BluetoothServiceTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @javax.inject.Inject lateinit var bluetoothManager: AppBluetoothManager
    @javax.inject.Inject lateinit var wifiManager: WifiManager

    @Before
    fun setUp() {
        hiltRule.inject()
        TestBluetoothState.connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun getWakeLock(service: BluetoothService): PowerManager.WakeLock? {
        val field = BluetoothService::class.java.getDeclaredField("wakeLock")
        field.isAccessible = true
        return field.get(service) as? PowerManager.WakeLock
    }

    @Test
    fun `onBind returns LocalBinder that exposes service`() {
        val controller = Robolectric.buildService(BluetoothService::class.java)
        val service = controller.create().get()

        val binder = service.onBind(Intent(context, BluetoothService::class.java))
        assertTrue(binder is BluetoothService.LocalBinder)
        assertSame(service, (binder as BluetoothService.LocalBinder).getService())
    }

    @Test
    fun `onCreate creates notification channel`() {
        val service = Robolectric.buildService(BluetoothService::class.java).create().get()

        val notificationManager = service.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel("ArdunakonConnection")
        assertNotNull(channel)
        assertEquals("Bluetooth Connection", channel?.name)
    }

    @Test
    fun `onDestroy cancels scope and cleans up managers`() {
        val controller = Robolectric.buildService(BluetoothService::class.java)
        controller.create()
        controller.destroy()

        verify { bluetoothManager.cleanup() }
    }

    @Test
    fun `wake lock is held when connecting and released when disconnected`() {
        val controller = Robolectric.buildService(BluetoothService::class.java)
        val service = controller.create().get()

        val wakeLock = getWakeLock(service)
        assertNotNull(wakeLock)
        assertTrue(wakeLock?.isHeld?.not() == true)

        TestBluetoothState.connectionState.value = ConnectionState.CONNECTING
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(wakeLock?.isHeld == true)

        TestBluetoothState.connectionState.value = ConnectionState.DISCONNECTED
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(wakeLock?.isHeld?.not() == true)
    }
}
