package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration-style tests for Data/Preferences package.
 *
 * Tests preference storage, loading, and encryption flows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class DataIntegrationTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var autoReconnectPreferences: AutoReconnectPreferences
    private lateinit var deviceNameCache: DeviceNameCache

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val cryptoEngine = TestCryptoEngine()

        connectionPreferences = ConnectionPreferences(context, cryptoEngine)
        autoReconnectPreferences = AutoReconnectPreferences(context, cryptoEngine)
        deviceNameCache = DeviceNameCache(context, cryptoEngine)
    }

    // ==================== Connection Preferences Flow ====================

    @Test
    fun `saveLastConnection and loadLastConnection round-trip`() = runTest {
        connectionPreferences.saveLastConnection(
            type = "BLUETOOTH",
            btAddress = "AA:BB:CC:DD:EE:FF",
            btType = "LE"
        )

        val loaded = connectionPreferences.loadLastConnection()

        assertEquals("BLUETOOTH", loaded.type)
        assertEquals("AA:BB:CC:DD:EE:FF", loaded.btAddress)
        assertEquals("LE", loaded.btType)
    }

    @Test
    fun `saveLastConnection with WiFi settings rounds-trip`() = runTest {
        connectionPreferences.saveLastConnection(
            type = "WIFI",
            wifiIp = "192.168.1.100",
            wifiPort = 8080,
            wifiPsk = "secret123"
        )

        val loaded = connectionPreferences.loadLastConnection()

        assertEquals("WIFI", loaded.type)
        assertEquals("192.168.1.100", loaded.wifiIp)
        assertEquals(8080, loaded.wifiPort)
        assertEquals("secret123", loaded.wifiPsk)
    }

    @Test
    fun `loadLastConnection returns defaults when empty`() = runTest {
        // Clear any existing data
        context.getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        val loaded = connectionPreferences.loadLastConnection()

        assertNull(loaded.type)
        assertNull(loaded.btAddress)
    }

    @Test
    fun `saveLastConnection with joystick sensitivity`() = runTest {
        connectionPreferences.saveLastConnection(
            type = "BLUETOOTH",
            btAddress = "11:22:33:44:55:66",
            btType = "CLASSIC",
            joystickSensitivity = 0.75f
        )

        val loaded = connectionPreferences.loadLastConnection()

        assertEquals(0.75f, loaded.joystickSensitivity, 0.001f)
    }

    @Test
    fun `saveLastConnection with autoReconnect flag`() = runTest {
        connectionPreferences.saveLastConnection(
            type = "WIFI",
            wifiIp = "10.0.0.1",
            wifiPort = 9000,
            autoReconnectWifi = true
        )

        val loaded = connectionPreferences.loadLastConnection()

        assertTrue(loaded.autoReconnectWifi)
    }

    // ==================== Auto-Reconnect Preferences Flow ====================

    @Test
    fun `saveAutoReconnectState and loadAutoReconnectState round-trip`() = runTest {
        autoReconnectPreferences.saveAutoReconnectState(0, true)

        val loaded = autoReconnectPreferences.loadAutoReconnectState()

        assertTrue(loaded[0])
    }

    @Test
    fun `saveAutoReconnectState handles multiple slots`() = runTest {
        autoReconnectPreferences.saveAutoReconnectState(0, true)
        autoReconnectPreferences.saveAutoReconnectState(1, false)

        val loaded = autoReconnectPreferences.loadAutoReconnectState()

        assertTrue(loaded[0])
        assertFalse(loaded[1])
    }

    @Test
    fun `loadAutoReconnectState returns false when empty`() = runTest {
        // Clear any existing data
        context.getSharedPreferences("auto_reconnect_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        val loaded = autoReconnectPreferences.loadAutoReconnectState()

        // All slots should default to false
        loaded.forEach { assertFalse(it) }
    }

    // ==================== Device Name Cache Flow ====================

    @Test
    fun `saveName and getName round-trip`() = runTest {
        val address = "AA:BB:CC:DD:EE:FF"
        val name = "My Arduino"

        deviceNameCache.saveName(address, name, com.metelci.ardunakon.bluetooth.DeviceType.LE)

        val loaded = deviceNameCache.getName(address)

        assertEquals(name, loaded)
    }

    @Test
    fun `getName returns null for unknown address`() = runTest {
        val loaded = deviceNameCache.getName("00:00:00:00:00:00")

        assertNull(loaded)
    }

    @Test
    fun `saveName updates existing entry`() = runTest {
        val address = "11:22:33:44:55:66"

        deviceNameCache.saveName(address, "Old Name", com.metelci.ardunakon.bluetooth.DeviceType.CLASSIC)
        deviceNameCache.saveName(address, "New Name", com.metelci.ardunakon.bluetooth.DeviceType.CLASSIC)

        val loaded = deviceNameCache.getName(address)

        assertEquals("New Name", loaded)
    }

    @Test
    fun `multiple devices can be cached`() = runTest {
        deviceNameCache.saveName("AA:AA:AA:AA:AA:AA", "Device A", com.metelci.ardunakon.bluetooth.DeviceType.LE)
        deviceNameCache.saveName("BB:BB:BB:BB:BB:BB", "Device B", com.metelci.ardunakon.bluetooth.DeviceType.LE)
        deviceNameCache.saveName("CC:CC:CC:CC:CC:CC", "Device C", com.metelci.ardunakon.bluetooth.DeviceType.CLASSIC)

        assertEquals("Device A", deviceNameCache.getName("AA:AA:AA:AA:AA:AA"))
        assertEquals("Device B", deviceNameCache.getName("BB:BB:BB:BB:BB:BB"))
        assertEquals("Device C", deviceNameCache.getName("CC:CC:CC:CC:CC:CC"))
    }

    @Test
    fun `cleanOldEntries does not crash`() = runTest {
        // Add some entries
        deviceNameCache.saveName("DD:DD:DD:DD:DD:DD", "Old Device", com.metelci.ardunakon.bluetooth.DeviceType.LE)

        // Should not crash
        deviceNameCache.cleanOldEntries()
    }

    // ==================== Data Class Tests ====================

    @Test
    fun `LastConnection data class equality`() {
        val conn1 = ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = "AA:BB:CC:DD:EE:FF",
            btType = "LE",
            wifiIp = null,
            wifiPort = 8888,
            wifiPsk = null,
            autoReconnectWifi = false,
            joystickSensitivity = 1.0f
        )

        val conn2 = ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = "AA:BB:CC:DD:EE:FF",
            btType = "LE",
            wifiIp = null,
            wifiPort = 8888,
            wifiPsk = null,
            autoReconnectWifi = false,
            joystickSensitivity = 1.0f
        )

        assertEquals(conn1, conn2)
    }

    @Test
    fun `LastConnection data class copy`() {
        val original = ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = "AA:BB:CC:DD:EE:FF",
            btType = "LE",
            wifiIp = null,
            wifiPort = 8888,
            wifiPsk = null,
            autoReconnectWifi = false,
            joystickSensitivity = 1.0f
        )

        val modified = original.copy(type = "WIFI", wifiIp = "192.168.1.1")

        assertEquals("WIFI", modified.type)
        assertEquals("192.168.1.1", modified.wifiIp)
        // Original unchanged
        assertEquals("BLUETOOTH", original.type)
    }
}
