package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.data.OnboardingPreferences
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.wifi.WifiManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ControlViewModelTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var onboardingManager: OnboardingManager
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val timeoutMs = 1_500L

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        connectionPreferences = ConnectionPreferences(context, TestCryptoEngine())
        onboardingManager = OnboardingManager(OnboardingPreferences(context))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun waitUntil(condition: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) {
                delay(10)
            }
        }
    }

    private fun createViewModel(
        bluetoothManager: AppBluetoothManager = mockk(relaxed = true),
        wifiManager: WifiManager = mockk(relaxed = true)
    ): ControlViewModel {
        every { bluetoothManager.debugLogs } returns MutableStateFlow<List<LogEntry>>(emptyList())
        every { bluetoothManager.telemetry } returns MutableStateFlow(null)
        every { bluetoothManager.connectionState } returns MutableStateFlow(com.metelci.ardunakon.bluetooth.ConnectionState.DISCONNECTED)
        every { bluetoothManager.isEmergencyStopActive } returns MutableStateFlow(false)
        every { bluetoothManager.sendDataToAll(any(), any()) } just runs
        every { bluetoothManager.log(any(), any()) } just runs
        every { bluetoothManager.setForegroundMode(any()) } just runs
        every { bluetoothManager.allowReflectionFallback } returns false
        every { bluetoothManager.allowReflectionFallback = any() } just runs

        every { wifiManager.autoReconnectEnabled } returns MutableStateFlow(false)
        every { wifiManager.connectionState } returns MutableStateFlow(com.metelci.ardunakon.wifi.WifiConnectionState.DISCONNECTED)
        every { wifiManager.encryptionError } returns MutableStateFlow(null)
        every { wifiManager.setRequireEncryption(any()) } just runs
        every { wifiManager.clearEncryptionError() } just runs
        every { wifiManager.sendData(any()) } just runs
        every { wifiManager.disconnect() } just runs
        every { wifiManager.setAutoReconnectEnabled(any()) } just runs

        return ControlViewModel(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            connectionPreferences = connectionPreferences,
            onboardingManager = onboardingManager
        ).also { it.setForegroundActive(false) }
    }

    @Test
    fun switchToWifi_disconnects_bluetooth_when_currently_bluetooth_and_persists_mode() = runBlocking {
        val bluetoothManager = mockk<AppBluetoothManager>(relaxed = true)
        val wifiManager = mockk<WifiManager>(relaxed = true)
        val viewModel = createViewModel(bluetoothManager, wifiManager)

        viewModel.connectionMode = ConnectionMode.BLUETOOTH
        viewModel.switchToWifi()
        waitUntil { viewModel.connectionMode == ConnectionMode.WIFI }

        verify(exactly = 1) { bluetoothManager.disconnect() }
        assertEquals(ConnectionMode.WIFI, viewModel.connectionMode)
        assertEquals("WIFI", connectionPreferences.loadLastConnection().type)
    }

    @Test
    fun switchToBluetooth_disconnects_wifi_when_currently_wifi_and_persists_mode() = runBlocking {
        val bluetoothManager = mockk<AppBluetoothManager>(relaxed = true)
        val wifiManager = mockk<WifiManager>(relaxed = true)
        val viewModel = createViewModel(bluetoothManager, wifiManager)

        viewModel.connectionMode = ConnectionMode.WIFI
        viewModel.switchToBluetooth()
        waitUntil { viewModel.connectionMode == ConnectionMode.BLUETOOTH }

        verify(exactly = 1) { wifiManager.disconnect() }
        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
        assertEquals("BLUETOOTH", connectionPreferences.loadLastConnection().type)
    }

    @Test
    fun toggleConnectionMode_switches_between_modes() = runBlocking {
        val bluetoothManager = mockk<AppBluetoothManager>(relaxed = true)
        val wifiManager = mockk<WifiManager>(relaxed = true)
        val viewModel = createViewModel(bluetoothManager, wifiManager)

        viewModel.connectionMode = ConnectionMode.BLUETOOTH
        viewModel.toggleConnectionMode()
        waitUntil { viewModel.connectionMode == ConnectionMode.WIFI }
        assertEquals(ConnectionMode.WIFI, viewModel.connectionMode)

        viewModel.toggleConnectionMode()
        waitUntil { viewModel.connectionMode == ConnectionMode.BLUETOOTH }
        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
    }

    @Test
    fun toggleWifiAutoReconnect_delegates_to_manager() = runBlocking {
        val bluetoothManager = mockk<AppBluetoothManager>(relaxed = true)
        val wifiManager = mockk<WifiManager>(relaxed = true)
        val viewModel = createViewModel(bluetoothManager, wifiManager)

        viewModel.toggleWifiAutoReconnect(true)
        viewModel.toggleWifiAutoReconnect(false)

        verify(exactly = 1) { wifiManager.setAutoReconnectEnabled(true) }
        verify(exactly = 1) { wifiManager.setAutoReconnectEnabled(false) }
    }

    @Test
    fun updateJoystickSensitivity_updates_state_and_persists() = runBlocking {
        connectionPreferences.saveLastConnection(joystickSensitivity = 1.0f)
        val bluetoothManager = mockk<AppBluetoothManager>(relaxed = true)
        val wifiManager = mockk<WifiManager>(relaxed = true)
        val viewModel = createViewModel(bluetoothManager, wifiManager)
        waitUntil { viewModel.joystickSensitivity == 1.0f }

        viewModel.updateJoystickSensitivity(1.75f)
        waitUntil { viewModel.joystickSensitivity == 1.75f }
        waitUntil { connectionPreferences.loadLastConnection().joystickSensitivity == 1.75f }

        assertEquals(1.75f, viewModel.joystickSensitivity, 0.0001f)
        assertEquals(1.75f, connectionPreferences.loadLastConnection().joystickSensitivity, 0.0001f)
    }

    @Test
    fun resetTutorial_calls_manager_and_emits_user_message() = runBlocking {
        val bluetoothManager = mockk<AppBluetoothManager>()
        val wifiManager = mockk<WifiManager>()
        val onboardingManagerMock = mockk<OnboardingManager>(relaxed = true)

        every { bluetoothManager.debugLogs } returns MutableStateFlow<List<LogEntry>>(emptyList())
        every { bluetoothManager.telemetry } returns MutableStateFlow(null)
        every { bluetoothManager.connectionState } returns
            MutableStateFlow(com.metelci.ardunakon.bluetooth.ConnectionState.DISCONNECTED)
        // Prevent any accidental transmission from touching unstubbed state.
        every { bluetoothManager.isEmergencyStopActive } returns MutableStateFlow(true)
        every { bluetoothManager.sendDataToAll(any(), any()) } just runs
        every { bluetoothManager.log(any(), any()) } just runs
        every { bluetoothManager.setForegroundMode(any()) } just runs
        every { bluetoothManager.allowReflectionFallback } returns false
        every { bluetoothManager.allowReflectionFallback = any() } just runs

        every { wifiManager.autoReconnectEnabled } returns MutableStateFlow(false)
        every { wifiManager.connectionState } returns MutableStateFlow(com.metelci.ardunakon.wifi.WifiConnectionState.DISCONNECTED)
        every { wifiManager.encryptionError } returns MutableStateFlow(null)
        every { wifiManager.setRequireEncryption(any()) } just runs
        every { wifiManager.clearEncryptionError() } just runs
        every { wifiManager.disconnect() } just runs

        val viewModel = ControlViewModel(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            connectionPreferences = connectionPreferences,
            onboardingManager = onboardingManagerMock
        ).also { it.setForegroundActive(false) }

        viewModel.resetTutorial()
        val message = viewModel.userMessage.first()

        verify(exactly = 1) { onboardingManagerMock.resetOnboarding() }
        verify { bluetoothManager.log(match { it.contains("Tutorial reset") }, LogType.INFO) }
        assertEquals("Tutorial reset! Restart app to see it.", message)
    }
}
