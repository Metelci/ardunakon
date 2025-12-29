package com.metelci.ardunakon.ui.screens

import android.content.Context
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.CombinedConnectionState
import com.metelci.ardunakon.bluetooth.ConnectionHealth
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.HapticPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.data.OnboardingPreferences
import com.metelci.ardunakon.protocol.CustomCommandRegistry
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiDevice
import com.metelci.ardunakon.wifi.WifiManager
import io.mockk.*
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ControlScreenTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var hapticPreferences: HapticPreferences

    private data class ControlScreenHandles(
        val viewModel: ControlViewModel,
        val combinedState: MutableStateFlow<CombinedConnectionState>
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "connection_prefs.json").delete()
        context.getSharedPreferences("haptic_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        connectionPreferences = ConnectionPreferences(context, TestCryptoEngine())
        onboardingManager = OnboardingManager(OnboardingPreferences(context))
        hapticPreferences = HapticPreferences(context)
    }

    private fun createViewModel(): ControlScreenHandles {
        val bluetoothManager = mockk<AppBluetoothManager>(relaxed = true)
        val wifiManager = mockk<WifiManager>(relaxed = true)

        val combinedState = MutableStateFlow(
            CombinedConnectionState(
                connectionState = ConnectionState.DISCONNECTED,
                rssi = 0,
                health = ConnectionHealth(),
                telemetry = null,
                rttHistory = emptyList(),
                autoReconnectEnabled = false,
                isEmergencyStopActive = false
            )
        )

        every { bluetoothManager.combinedState } returns combinedState
        every { bluetoothManager.debugLogs } returns MutableStateFlow(emptyList())
        every { bluetoothManager.telemetry } returns MutableStateFlow(null)
        every { bluetoothManager.connectedDeviceInfo } returns MutableStateFlow(null)
        every { bluetoothManager.scannedDevices } returns MutableStateFlow(emptyList())
        every { bluetoothManager.telemetryHistoryManager } returns TelemetryHistoryManager()
        every { bluetoothManager.connectionState } returns MutableStateFlow(ConnectionState.DISCONNECTED)
        every { bluetoothManager.isEmergencyStopActive } returns MutableStateFlow(false)
        every { bluetoothManager.allowReflectionFallback } returns false
        every { bluetoothManager.allowReflectionFallback = any() } just runs
        every { bluetoothManager.setForegroundMode(any()) } just runs
        every { bluetoothManager.setAutoReconnectEnabled(any()) } just runs
        every { bluetoothManager.reconnectSavedDevice() } returns false
        every { bluetoothManager.log(any(), any()) } just runs
        every { bluetoothManager.sendDataToAll(any(), any()) } just runs
        every { bluetoothManager.sendDataToAll(any()) } just runs

        every { wifiManager.connectionState } returns MutableStateFlow(WifiConnectionState.DISCONNECTED)
        every { wifiManager.rssi } returns MutableStateFlow(0)
        every { wifiManager.rtt } returns MutableStateFlow(0L)
        every { wifiManager.rttHistory } returns MutableStateFlow(emptyList())
        every { wifiManager.telemetry } returns MutableStateFlow(null)
        every { wifiManager.autoReconnectEnabled } returns MutableStateFlow(false)
        every { wifiManager.encryptionError } returns MutableStateFlow(null)
        every { wifiManager.isEncrypted } returns MutableStateFlow(false)
        every { wifiManager.scannedDevices } returns MutableStateFlow<List<WifiDevice>>(emptyList())
        every { wifiManager.setRequireEncryption(any()) } just runs
        every { wifiManager.clearEncryptionError() } just runs
        every { wifiManager.sendData(any()) } just runs
        every { wifiManager.disconnect() } just runs
        every { wifiManager.setAutoReconnectEnabled(any()) } just runs

        val customCommandRegistry = mockk<CustomCommandRegistry>(relaxed = true)
        every { customCommandRegistry.commands } returns MutableStateFlow(emptyList())

        val raspManager = mockk<com.metelci.ardunakon.security.RASPManager>(relaxed = true)
        every { raspManager.securityViolations } returns MutableStateFlow(emptyList())
        every { raspManager.isSecurityCompromised } returns MutableStateFlow(false)

        val viewModel = ControlViewModel(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            connectionPreferences = connectionPreferences,
            onboardingManager = onboardingManager,
            customCommandRegistry = customCommandRegistry,
            hapticPreferences = hapticPreferences,
            raspManager = raspManager
        )

        return ControlScreenHandles(viewModel = viewModel, combinedState = combinedState)
    }

    @Test
    @Config(sdk = [34], qualifiers = "port")
    fun controlScreen_portrait_hides_device_status_card() {
        val handles = createViewModel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ControlScreen(viewModel = handles.viewModel)
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        // In portrait, StatusCard is not shown in PortraitControlLayout? Let's verify.
        // Actually, it should be in the header or somewhere.
        // Looking at PortraitControlLayout, let's see where StatusCard is.
        composeTestRule.onNodeWithText("Terminal", substring = true).assertExists()
        composeTestRule.onNodeWithText("Device", substring = true).assertDoesNotExist()
    }

    @Test
    @Config(sdk = [34], qualifiers = "land")
    fun controlScreen_landscape_shows_device_status_card() {
        val handles = createViewModel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ControlScreen(viewModel = handles.viewModel)
        }
        composeTestRule.mainClock.advanceTimeBy(2000)

        composeTestRule.onNodeWithText("Device", substring = true).assertExists()
        composeTestRule.onNodeWithText("Terminal", substring = true).assertExists()
    }

    @Test
    fun controlScreen_shows_snackbar_from_user_message() {
        val handles = createViewModel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ControlScreen(viewModel = handles.viewModel)
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        handles.viewModel.showMessage("Status updated")

        // Snackbar needs time to appear
        composeTestRule.mainClock.advanceTimeBy(2000)

        composeTestRule.onNodeWithText("Status updated", substring = true).assertExists()
    }

    @Test
    fun controlScreen_shows_packet_loss_warning_from_telemetry() {
        val handles = createViewModel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ControlScreen(viewModel = handles.viewModel)
        }
        // Need to set it before advancing clock or wait for recomposition
        handles.combinedState.value = handles.combinedState.value.copy(
            telemetry = Telemetry(
                batteryVoltage = 12.0f,
                status = "OK",
                packetsSent = 100,
                packetsDropped = 6,
                packetsFailed = 0
            )
        )
        composeTestRule.mainClock.advanceTimeBy(2000)

        composeTestRule.onNodeWithText("Packet Loss Detected", substring = true).assertExists()
    }
}
