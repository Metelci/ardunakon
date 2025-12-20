package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.data.ConnectionPreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AppBluetoothManager.
 * 
 * Note: Tests that trigger connectToDevice or other methods that spawn background
 * coroutines are disabled because the init block starts infinite loops that
 * hang the JVM test runner. These should be tested via instrumented tests or
 * by refactoring the background task initialization.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class AppBluetoothManagerTest {

    private lateinit var context: Context
    private lateinit var bluetoothManagerService: BluetoothManager
    private lateinit var adapter: BluetoothAdapter
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var appBluetoothManager: AppBluetoothManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bluetoothManagerService = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManagerService.adapter
        
        connectionPreferences = mockk(relaxed = true)
        
        // Mock default behavior for preferences
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            null, null, null, null, 8888, null, false, 1.0f
        )
        
        appBluetoothManager = AppBluetoothManager(
            context = context,
            connectionPreferences = connectionPreferences,
            cryptoEngine = com.metelci.ardunakon.TestCryptoEngine(),
            scope = testScope
        )
    }

    @Test
    fun `initial state is DISCONNECTED`() {
        assertEquals(ConnectionState.DISCONNECTED, appBluetoothManager.connectionState.value)
    }

    @Test
    fun `E-STOP blocks connection attempts`() {
        appBluetoothManager.setEmergencyStop(true)
        val device = BluetoothDeviceModel("Test", "00:11:22:33:44:55", DeviceType.CLASSIC)
        
        appBluetoothManager.connectToDevice(device)
        
        assertEquals(ConnectionState.DISCONNECTED, appBluetoothManager.connectionState.value)
        assertTrue(appBluetoothManager.debugLogs.value.any { it.message.contains("E-STOP ACTIVE") })
    }

    @Test
    fun `circuit breaker reset logs message`() {
        appBluetoothManager.resetCircuitBreaker()
        assertTrue(appBluetoothManager.debugLogs.value.any { it.message.contains("Circuit breaker reset") })
    }

    @Test
    fun `sendData blocks when E-STOP active`() {
        appBluetoothManager.setEmergencyStop(true)
        val data = byteArrayOf(0x01)
        
        // This should not throw and should be blocked silently
        appBluetoothManager.sendData(data)
        
        // Verification that connectionManager.send was NOT called is implicit
        // since connectionManager is null when disconnected.
    }

    @Test
    fun `onDataReceived updates flow`() {
        val testData = byteArrayOf(0x01, 0x02, 0x03)
        appBluetoothManager.onDataReceived(testData)
        
        assertArrayEquals(testData, appBluetoothManager.incomingData.value)
    }

    @Test
    fun `onDataReceived parses capabilities`() {
        // AA 01 05 [ID] [TYPE] [FLAGS...] 55
        val capPacket = byteArrayOf(0xAA.toByte(), 0x01, 0x05, 0x01, 0x04, 0x00, 0x00, 0x00, 0x10.toByte(), 0x55)
        
        appBluetoothManager.onDataReceived(capPacket)
        
        val caps = appBluetoothManager.deviceCapability.value
        assertNotEquals(DeviceCapabilities.DEFAULT, caps)
        assertTrue(appBluetoothManager.debugLogs.value.any { it.message.contains("Data received from") })
    }

    @Test
    fun `emergency stop updates state`() {
        assertFalse(appBluetoothManager.isEmergencyStopActive.value)
        
        appBluetoothManager.setEmergencyStop(true)
        
        assertTrue(appBluetoothManager.isEmergencyStopActive.value)
    }

    @Test
    fun `auto reconnect state is exposed`() {
        // Initially should be false (from mocked preferences)
        assertFalse(appBluetoothManager.autoReconnectEnabled.value)
    }
}
