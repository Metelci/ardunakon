package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.CombinedConnectionState
import com.metelci.ardunakon.bluetooth.ConnectionHealth
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.HapticPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.data.OnboardingPreferences
import com.metelci.ardunakon.protocol.CustomCommandRegistry
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiDevice
import com.metelci.ardunakon.wifi.WifiManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
class ControlLayoutsTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var hapticPreferences: HapticPreferences

    private data class LayoutHandles(
        val viewModel: ControlViewModel,
        val bluetoothManager: AppBluetoothManager,
        val wifiManager: WifiManager
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

    private fun createHandles(): LayoutHandles {
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
        every { bluetoothManager.isScanning } returns MutableStateFlow(false)
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
        every { wifiManager.isScanning } returns MutableStateFlow(false)
        every { wifiManager.connectedDeviceInfo } returns MutableStateFlow(null)
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
        ).also { it.setForegroundActive(false) }

        return LayoutHandles(viewModel = viewModel, bluetoothManager = bluetoothManager, wifiManager = wifiManager)
    }

    @Test
    @Config(sdk = [34], qualifiers = "port")
    fun portraitLayout_shows_terminal_and_hides_device_card() {
        val handles = createHandles()
        val orientationConfig = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
        }

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PortraitControlLayout(
                viewModel = handles.viewModel,
                bluetoothManager = handles.bluetoothManager,
                connectionState = ConnectionState.DISCONNECTED,
                wifiState = WifiConnectionState.DISCONNECTED,
                rssiValue = 0,
                wifiRssi = 0,
                wifiRtt = 0L,
                rttHistory = emptyList(),
                wifiRttHistory = emptyList(),
                health = ConnectionHealth(),
                debugLogs = emptyList(),
                telemetry = null,
                autoReconnectEnabled = false,
                isEStopActive = false,
                isWifiEncrypted = false,
                connectedDeviceInfo = null,
                safeDrawingPadding = PaddingValues(),
                orientationConfig = orientationConfig,
                view = View(context),
                context = context,
                onQuitApp = {},
                exportLogs = {}
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Terminal").assertExists()
        composeTestRule.onNodeWithText("Device").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [34], qualifiers = "land")
    fun landscapeLayout_shows_device_card_and_terminal() {
        val handles = createHandles()
        val orientationConfig = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            LandscapeControlLayout(
                viewModel = handles.viewModel,
                bluetoothManager = handles.bluetoothManager,
                connectionState = ConnectionState.DISCONNECTED,
                wifiState = WifiConnectionState.DISCONNECTED,
                rssiValue = 0,
                wifiRssi = 0,
                wifiRtt = 0L,
                rttHistory = emptyList(),
                wifiRttHistory = emptyList(),
                health = ConnectionHealth(),
                debugLogs = emptyList(),
                telemetry = null,
                autoReconnectEnabled = false,
                isEStopActive = false,
                isWifiEncrypted = false,
                connectedDeviceInfo = null,
                safeDrawingPadding = PaddingValues(),
                orientationConfig = orientationConfig,
                view = View(context),
                context = context,
                onQuitApp = {},
                exportLogs = {}
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Device", substring = true).assertExists()
        composeTestRule.onNodeWithText("Terminal", substring = true).assertExists()
    }
}
