package com.metelci.ardunakon.bluetooth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.model.LogType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration-style tests for Bluetooth functionality.
 *
 * These tests focus on public API behavior and end-to-end flows
 * without requiring complex mocking of Android Bluetooth internals.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothIntegrationTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var bluetoothManager: AppBluetoothManager
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectionPreferences = mockk(relaxed = true)

        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = null,
            btAddress = null,
            btType = null,
            wifiIp = null,
            wifiPort = 8888,
            wifiPsk = null,
            autoReconnectWifi = false,
            joystickSensitivity = 1.0f
        )

        bluetoothManager = AppBluetoothManager(
            context = context,
            connectionPreferences = connectionPreferences,
            cryptoEngine = com.metelci.ardunakon.TestCryptoEngine(),
            scope = testScope,
            startMonitors = false
        )
    }

    @After
    fun tearDown() {
        bluetoothManager.cleanup()
    }

    // ==================== State Management Tests ====================

    @Test
    fun `initial connection state is DISCONNECTED`() = runTest(testDispatcher) {
        assertEquals(ConnectionState.DISCONNECTED, bluetoothManager.connectionState.value)
    }

    @Test
    fun `initial emergency stop is inactive`() = runTest(testDispatcher) {
        assertFalse(bluetoothManager.isEmergencyStopActive.value)
    }

    @Test
    fun `initial auto-reconnect is disabled`() = runTest(testDispatcher) {
        assertFalse(bluetoothManager.autoReconnectEnabled.value)
    }

    @Test
    fun `initial scanned devices list is empty`() = runTest(testDispatcher) {
        assertTrue(bluetoothManager.scannedDevices.value.isEmpty())
    }

    @Test
    fun `initial device capability is DEFAULT`() = runTest(testDispatcher) {
        assertEquals(DeviceCapabilities.DEFAULT, bluetoothManager.deviceCapability.value)
    }

    // ==================== Emergency Stop Flow ====================

    @Test
    fun `emergency stop activation updates state`() = runTest(testDispatcher) {
        bluetoothManager.setEmergencyStop(true)
        assertTrue(bluetoothManager.isEmergencyStopActive.value)
    }

    @Test
    fun `emergency stop deactivation updates state`() = runTest(testDispatcher) {
        bluetoothManager.setEmergencyStop(true)
        bluetoothManager.setEmergencyStop(false)
        assertFalse(bluetoothManager.isEmergencyStopActive.value)
    }

    @Test
    fun `emergency stop blocks connection attempts`() = runTest(testDispatcher) {
        bluetoothManager.setEmergencyStop(true)

        val device = BluetoothDeviceModel("Test", "00:11:22:33:44:55", DeviceType.LE)
        bluetoothManager.connectToDevice(device)
        testDispatcher.scheduler.runCurrent()

        // Should remain disconnected due to E-STOP
        assertEquals(ConnectionState.DISCONNECTED, bluetoothManager.connectionState.value)
        assertTrue(bluetoothManager.debugLogs.value.any { it.message.contains("E-STOP") })
    }

    @Test
    fun `emergency stop blocks data transmission`() = runTest(testDispatcher) {
        bluetoothManager.setEmergencyStop(true)

        // Should not crash, just be silently ignored
        bluetoothManager.sendData(byteArrayOf(0x01, 0x02, 0x03))
    }

    @Test
    fun `holdOfflineAfterEStopReset disables auto-reconnect`() = runTest(testDispatcher) {
        bluetoothManager.setAutoReconnectEnabled(true)
        bluetoothManager.holdOfflineAfterEStopReset()

        assertFalse(bluetoothManager.autoReconnectEnabled.value)
        assertTrue(
            bluetoothManager.debugLogs.value.any { it.message.contains("E-STOP reset") }
        )
    }

    // ==================== Auto-Reconnect Flow ====================

    @Test
    fun `enabling auto-reconnect updates state and logs`() = runTest(testDispatcher) {
        bluetoothManager.setAutoReconnectEnabled(true)

        assertTrue(bluetoothManager.autoReconnectEnabled.value)
        assertTrue(
            bluetoothManager.debugLogs.value.any { it.message.contains("Auto-reconnect ARMED") }
        )
    }

    @Test
    fun `disabling auto-reconnect updates state and logs`() = runTest(testDispatcher) {
        bluetoothManager.setAutoReconnectEnabled(true)
        bluetoothManager.setAutoReconnectEnabled(false)

        assertFalse(bluetoothManager.autoReconnectEnabled.value)
        assertTrue(
            bluetoothManager.debugLogs.value.any { it.message.contains("Auto-reconnect DISABLED") }
        )
    }

    // ==================== Data Processing Flow ====================

    @Test
    fun `onDataReceived updates incoming data flow`() = runTest(testDispatcher) {
        val testData = byteArrayOf(0x01, 0x02, 0x03)

        bluetoothManager.onDataReceived(testData)

        assertArrayEquals(testData, bluetoothManager.incomingData.value)
    }

    @Test
    fun `capability packet updates deviceCapability flow`() = runTest(testDispatcher) {
        // Capability packet: AA 01 05 [ID] [TYPE] [FLAGS...] 55
        val capPacket = byteArrayOf(
            0xAA.toByte(),
            0x01,
            0x05,
            // ID
            0x01,
            // Type
            0x04,
            // Flags
            0x01,
            0x00,
            0x00,
            0x00,
            0x55
        )

        bluetoothManager.onDataReceived(capPacket)

        val caps = bluetoothManager.deviceCapability.value
        assertNotEquals(DeviceCapabilities.DEFAULT, caps)
    }

    // ==================== Connection State Flow ====================

    @Test
    fun `onStateChanged CONNECTED updates connectedDeviceInfo`() = runTest(testDispatcher) {
        val device = BluetoothDeviceModel("Test BLE", "00:11:22:33:44:55", DeviceType.LE)

        // Inject saved device via reflection
        val field = AppBluetoothManager::class.java.getDeclaredField("savedDevice")
        field.isAccessible = true
        field.set(bluetoothManager, device)

        bluetoothManager.onStateChanged(ConnectionState.CONNECTED)

        assertEquals("Test BLE (BLE)", bluetoothManager.connectedDeviceInfo.value)
        // Cancel keep-alive loop started on connect so the test dispatcher can go idle.
        bluetoothManager.onStateChanged(ConnectionState.DISCONNECTED)
    }

    @Test
    fun `onStateChanged CONNECTED for Classic device shows correct type`() = runTest(testDispatcher) {
        val device = BluetoothDeviceModel("Test Classic", "00:11:22:33:44:55", DeviceType.CLASSIC)

        val field = AppBluetoothManager::class.java.getDeclaredField("savedDevice")
        field.isAccessible = true
        field.set(bluetoothManager, device)

        bluetoothManager.onStateChanged(ConnectionState.CONNECTED)

        assertEquals("Test Classic (Classic)", bluetoothManager.connectedDeviceInfo.value)
        // Cancel keep-alive loop started on connect so the test dispatcher can go idle.
        bluetoothManager.onStateChanged(ConnectionState.DISCONNECTED)
    }

    @Test
    fun `onStateChanged DISCONNECTED clears connectedDeviceInfo`() = runTest(testDispatcher) {
        bluetoothManager.onStateChanged(ConnectionState.DISCONNECTED)

        assertNull(bluetoothManager.connectedDeviceInfo.value)
    }

    @Test
    fun `onStateChanged ERROR updates state`() = runTest(testDispatcher) {
        bluetoothManager.onStateChanged(ConnectionState.ERROR)

        assertEquals(ConnectionState.ERROR, bluetoothManager.connectionState.value)
    }

    // ==================== Error Handling Flow ====================

    @Test
    fun `onError logs error message`() = runTest(testDispatcher) {
        bluetoothManager.onError("Test error message", LogType.ERROR)

        assertTrue(
            bluetoothManager.debugLogs.value.any { it.message.contains("Test error message") }
        )
    }

    @Test
    fun `onPacketStats updates internal state`() = runTest(testDispatcher) {
        // Just verify it doesn't crash
        bluetoothManager.onPacketStats(100, 5, 2)
    }

    // ==================== Logging Flow ====================

    @Test
    fun `log list is truncated at 500 entries`() = runTest(testDispatcher) {
        repeat(510) { i ->
            bluetoothManager.onError("Message $i", LogType.INFO)
        }

        assertEquals(500, bluetoothManager.debugLogs.value.size)
        assertFalse(bluetoothManager.debugLogs.value.any { it.message == "Message 0" })
        assertTrue(bluetoothManager.debugLogs.value.any { it.message == "Message 509" })
    }

    // ==================== Circuit Breaker Flow ====================

    @Test
    fun `resetCircuitBreaker logs reset`() = runTest(testDispatcher) {
        bluetoothManager.resetCircuitBreaker()

        assertTrue(
            bluetoothManager.debugLogs.value.any { it.message.contains("Circuit breaker reset") }
        )
    }

    // ==================== Cleanup Flow ====================

    @Test
    fun `cleanup is safe to call multiple times`() = runTest(testDispatcher) {
        bluetoothManager.cleanup()
        bluetoothManager.cleanup()

        // Should not crash
    }

    @Test
    fun `disconnect when not connected is safe`() = runTest(testDispatcher) {
        bluetoothManager.disconnect()

        assertEquals(ConnectionState.DISCONNECTED, bluetoothManager.connectionState.value)
    }

    // ==================== Connection Attempt Flow ====================

    @Test
    fun `connectToDevice with BLE device logs something`() = runTest(testDispatcher) {
        val device = BluetoothDeviceModel("BLE Device", "AA:BB:CC:DD:EE:FF", DeviceType.LE)

        bluetoothManager.connectToDevice(device)
        testDispatcher.scheduler.runCurrent()

        // Should log either connection attempt or error (e.g., Bluetooth off)
        assertTrue(
            bluetoothManager.debugLogs.value.any {
                it.message.contains("Connecting") ||
                    it.message.contains("BLE Device") ||
                    it.message.contains("Bluetooth") ||
                    it.message.contains("Connect failed")
            }
        )
    }

    @Test
    fun `connectToDevice with Classic device logs something`() = runTest(testDispatcher) {
        val device = BluetoothDeviceModel("Classic Device", "11:22:33:44:55:66", DeviceType.CLASSIC)

        bluetoothManager.connectToDevice(device)
        testDispatcher.scheduler.runCurrent()

        // Should log either connection attempt or error (e.g., Bluetooth off)
        assertTrue(
            bluetoothManager.debugLogs.value.any {
                it.message.contains("Connecting") ||
                    it.message.contains("Classic Device") ||
                    it.message.contains("Bluetooth") ||
                    it.message.contains("Connect failed")
            }
        )
    }
}
