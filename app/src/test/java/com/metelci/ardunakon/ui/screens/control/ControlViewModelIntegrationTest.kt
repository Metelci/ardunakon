package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.HapticPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.protocol.CustomCommandRegistry
import com.metelci.ardunakon.wifi.WifiManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Extended integration tests for ControlViewModel.
 * 
 * Focuses on public API behavior and state management flows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ControlViewModelIntegrationTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var hapticPreferences: HapticPreferences
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var customCommandRegistry: CustomCommandRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        connectionPreferences = mockk(relaxed = true)
        hapticPreferences = mockk(relaxed = true)
        onboardingManager = mockk(relaxed = true)
        customCommandRegistry = mockk(relaxed = true)

        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            btType = null,
            wifiIp = null,
            wifiPort = 8888,
            wifiPsk = null,
            autoReconnectWifi = false,
            joystickSensitivity = 1.0f
        )
        
        coEvery { hapticPreferences.isHapticEnabled() } returns true
        every { customCommandRegistry.getAvailableCommandIds() } returns listOf(0x20, 0x21, 0x22)
    }

    private fun createViewModel(
        bluetoothManager: AppBluetoothManager = createMockBluetoothManager(),
        wifiManager: WifiManager = createMockWifiManager()
    ): ControlViewModel {
        return ControlViewModel(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            connectionPreferences = connectionPreferences,
            onboardingManager = onboardingManager,
            customCommandRegistry = customCommandRegistry,
            hapticPreferences = hapticPreferences
        ).also { it.setForegroundActive(false) }
    }

    private fun createMockBluetoothManager(): AppBluetoothManager {
        val manager = mockk<AppBluetoothManager>(relaxed = true)
        every { manager.connectionState } returns MutableStateFlow(ConnectionState.DISCONNECTED)
        every { manager.isEmergencyStopActive } returns MutableStateFlow(false)
        every { manager.autoReconnectEnabled } returns MutableStateFlow(false)
        every { manager.debugLogs } returns MutableStateFlow(emptyList())
        every { manager.incomingData } returns MutableStateFlow(null)
        every { manager.rssiValue } returns MutableStateFlow(0)
        every { manager.health } returns MutableStateFlow(com.metelci.ardunakon.bluetooth.ConnectionHealth())
        every { manager.telemetry } returns MutableStateFlow(null)
        every { manager.rttHistory } returns MutableStateFlow(emptyList())
        every { manager.scannedDevices } returns MutableStateFlow(emptyList())
        return manager
    }

    private fun createMockWifiManager(): WifiManager {
        val manager = mockk<WifiManager>(relaxed = true)
        every { manager.connectionState } returns MutableStateFlow(com.metelci.ardunakon.wifi.WifiConnectionState.DISCONNECTED)
        every { manager.isScanning } returns MutableStateFlow(false)
        every { manager.rssi } returns MutableStateFlow(0)
        every { manager.encryptionError } returns MutableStateFlow(null)
        every { manager.rttHistory } returns MutableStateFlow(emptyList())
        every { manager.telemetry } returns MutableStateFlow(null)
        every { manager.scannedDevices } returns MutableStateFlow(emptyList())
        every { manager.autoReconnectEnabled } returns MutableStateFlow(false)
        return manager
    }

    // ==================== Joystick Control Flow ====================

    @Test
    fun `updateJoystick updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateJoystick(0.5f, -0.3f)
        
        val (x, y) = viewModel.leftJoystick
        assertEquals(0.5f, x, 0.001f)
        assertEquals(-0.3f, y, 0.001f)
    }

    @Test
    fun `joystick starts at center position`() = runTest {
        val viewModel = createViewModel()
        
        val (x, y) = viewModel.leftJoystick
        assertEquals(0f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    // ==================== Servo Control Flow ====================

    @Test
    fun `updateServo updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateServo(0.25f, 0.5f, 0.75f)
        
        assertEquals(0.25f, viewModel.servoX, 0.001f)
        assertEquals(0.5f, viewModel.servoY, 0.001f)
        assertEquals(0.75f, viewModel.servoZ, 0.001f)
    }

    @Test
    fun `servo starts at center position`() = runTest {
        val viewModel = createViewModel()
        
        assertEquals(0f, viewModel.servoX, 0.001f)
        assertEquals(0f, viewModel.servoY, 0.001f)
        assertEquals(0f, viewModel.servoZ, 0.001f)
    }

    // ==================== E-STOP Flow ====================

    @Test
    fun `toggleEStop calls manager`() = runTest {
        val bluetoothManager = createMockBluetoothManager()
        val viewModel = createViewModel(bluetoothManager = bluetoothManager)
        val mockView = mockk<View>(relaxed = true)
        
        viewModel.toggleEStop(mockView)
        
        verify { bluetoothManager.setEmergencyStop(any()) }
    }

    // ==================== Connection Mode Flow ====================

    @Test
    fun `initial connection mode is BLUETOOTH`() = runTest {
        val viewModel = createViewModel()
        
        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
    }

    @Test
    fun `toggleConnectionMode switches to WiFi`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.toggleConnectionMode()
        
        assertEquals(ConnectionMode.WIFI, viewModel.connectionMode)
    }

    @Test
    fun `toggleConnectionMode switches back to Bluetooth`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.toggleConnectionMode()
        viewModel.toggleConnectionMode()
        
        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
    }

    @Test
    fun `switchToWifi sets WiFi mode`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.switchToWifi()
        
        assertEquals(ConnectionMode.WIFI, viewModel.connectionMode)
    }

    @Test
    fun `switchToBluetooth sets Bluetooth mode`() = runTest {
        val viewModel = createViewModel()
        viewModel.switchToWifi()
        
        viewModel.switchToBluetooth()
        
        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
    }

    // ==================== Joystick Sensitivity Flow ====================

    @Test
    fun `updateJoystickSensitivity updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateJoystickSensitivity(0.75f)
        
        assertEquals(0.75f, viewModel.joystickSensitivity, 0.001f)
    }

    @Test
    fun `initial joystick sensitivity is 1_0`() = runTest {
        val viewModel = createViewModel()
        
        assertEquals(1.0f, viewModel.joystickSensitivity, 0.001f)
    }

    // ==================== Haptic Feedback Flow ====================

    @Test
    fun `updateHapticEnabled updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateHapticEnabled(false)
        
        assertFalse(viewModel.isHapticEnabled)
    }

    @Test
    fun `haptic is enabled by default`() = runTest {
        val viewModel = createViewModel()
        
        assertTrue(viewModel.isHapticEnabled)
    }

    // ==================== Reflection Setting Flow ====================

    @Test
    fun `updateAllowReflection updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateAllowReflection(true)
        
        assertTrue(viewModel.allowReflection)
    }

    @Test
    fun `reflection is disabled by default`() = runTest {
        val viewModel = createViewModel()
        
        assertFalse(viewModel.allowReflection)
    }

    // ==================== User Messages Flow ====================

    @Test
    fun `showMessage emits message event`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.showMessage("Test message")
        
        val message = viewModel.userMessage.first()
        assertEquals("Test message", message)
    }

    // ==================== Tutorial Reset Flow ====================

    @Test
    fun `resetTutorial calls onboarding manager`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.resetTutorial()
        
        verify { onboardingManager.resetOnboarding() }
    }

    // ==================== WiFi Auto-Reconnect Flow ====================

    @Test
    fun `toggleWifiAutoReconnect calls manager`() = runTest {
        val wifiManager = createMockWifiManager()
        val viewModel = createViewModel(wifiManager = wifiManager)
        
        viewModel.toggleWifiAutoReconnect(true)
        
        verify { wifiManager.setAutoReconnectEnabled(true) }
    }

    // ==================== Encryption Flow ====================

    @Test
    fun `updateRequireEncryption updates state`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.updateRequireEncryption(true)
        
        assertTrue(viewModel.requireEncryption)
    }

    @Test
    fun `encryption is not required by default`() = runTest {
        val viewModel = createViewModel()
        
        assertFalse(viewModel.requireEncryption)
    }

    // ==================== Custom Commands Flow ====================

    @Test
    fun `getAvailableCommandIds returns valid IDs`() = runTest {
        val viewModel = createViewModel()
        
        val ids = viewModel.getAvailableCommandIds()
        
        assertTrue(ids.isNotEmpty())
    }

    // ==================== Foreground State Flow ====================

    @Test
    fun `setForegroundActive calls bluetooth manager`() = runTest {
        val bluetoothManager = createMockBluetoothManager()
        val viewModel = createViewModel(bluetoothManager = bluetoothManager)
        
        viewModel.setForegroundActive(false)
        
        verify { bluetoothManager.setForegroundMode(false) }
    }

    // ==================== Servo Command Parsing ====================

    @Test
    fun `handleServoCommand W moves forward`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.handleServoCommand("W")
        
        assertEquals(1f, viewModel.servoY, 0.001f)
    }

    @Test
    fun `handleServoCommand B moves backward`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.handleServoCommand("B")
        
        assertEquals(-1f, viewModel.servoY, 0.001f)
    }

    @Test
    fun `handleServoCommand L moves left`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.handleServoCommand("L")
        
        assertEquals(-1f, viewModel.servoX, 0.001f)
    }

    @Test
    fun `handleServoCommand R moves right`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.handleServoCommand("R")
        
        assertEquals(1f, viewModel.servoX, 0.001f)
    }

    @Test
    fun `handleServoCommand handles unknown command`() = runTest {
        val viewModel = createViewModel()
        
        // Should not crash, just forward to connection
        viewModel.handleServoCommand("UNKNOWN")
    }

    // ==================== Reconnect Flow ====================

    @Test
    fun `reconnectBluetoothDevice calls manager`() = runTest {
        val bluetoothManager = createMockBluetoothManager()
        every { bluetoothManager.reconnectSavedDevice() } returns true
        
        val viewModel = createViewModel(bluetoothManager = bluetoothManager)
        
        val result = viewModel.reconnectBluetoothDevice()
        
        verify { bluetoothManager.reconnectSavedDevice() }
        assertTrue(result)
    }

    // ==================== Dialog State Tests ====================

    @Test
    fun `showDeviceList state toggles`() = runTest {
        val viewModel = createViewModel()
        
        assertFalse(viewModel.showDeviceList)
        
        viewModel.showDeviceList = true
        assertTrue(viewModel.showDeviceList)
    }

    @Test
    fun `showDebugConsole state toggles`() = runTest {
        val viewModel = createViewModel()
        
        assertFalse(viewModel.showDebugConsole)
        
        viewModel.showDebugConsole = true
        assertTrue(viewModel.showDebugConsole)
    }

    @Test
    fun `showWifiConfig state toggles`() = runTest {
        val viewModel = createViewModel()
        
        assertFalse(viewModel.showWifiConfig)
        
        viewModel.showWifiConfig = true
        assertTrue(viewModel.showWifiConfig)
    }
}
