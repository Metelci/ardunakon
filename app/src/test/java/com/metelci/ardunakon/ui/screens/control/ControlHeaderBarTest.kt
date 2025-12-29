package com.metelci.ardunakon.ui.screens.control

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import com.metelci.ardunakon.wifi.WifiConnectionState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ControlHeaderBarTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun `E-STOP button shows STOP when inactive`() {
        composeTestRule.setContent {
            ControlHeaderBar(
                connectionMode = ConnectionMode.BLUETOOTH,
                bluetoothConnectionState = ConnectionState.DISCONNECTED,
                wifiConnectionState = WifiConnectionState.DISCONNECTED,
                rssiValue = 0,
                wifiRssi = 0,
                rttHistory = emptyList(),
                wifiRttHistory = emptyList(),
                isEStopActive = false,
                autoReconnectEnabled = false,
                onToggleAutoReconnect = {},
                onScanDevices = {},
                onReconnectDevice = {},
                onSwitchToWifi = {},
                onSwitchToBluetooth = {},
                onConfigureWifi = {},
                onTelemetryGraph = {},
                onToggleEStop = {},
                onShowSettings = {},
                onShowHelp = {},
                onShowAbout = {},
                onShowCrashLog = {},
                onShowPerformanceStats = {},
                onOpenArduinoCloud = {},
                onQuitApp = {},
                context = mockk(relaxed = true),
                view = mockk(relaxed = true)
            )
        }

        composeTestRule.onNodeWithText("STOP").assertExists()
    }

    @Test
    fun `E-STOP button shows RESET when active`() {
        composeTestRule.setContent {
            ControlHeaderBar(
                connectionMode = ConnectionMode.BLUETOOTH,
                bluetoothConnectionState = ConnectionState.CONNECTED,
                wifiConnectionState = WifiConnectionState.DISCONNECTED,
                rssiValue = -60,
                wifiRssi = 0,
                rttHistory = emptyList(),
                wifiRttHistory = emptyList(),
                isEStopActive = true,
                autoReconnectEnabled = false,
                onToggleAutoReconnect = {},
                onScanDevices = {},
                onReconnectDevice = {},
                onSwitchToWifi = {},
                onSwitchToBluetooth = {},
                onConfigureWifi = {},
                onTelemetryGraph = {},
                onToggleEStop = {},
                onShowSettings = {},
                onShowHelp = {},
                onShowAbout = {},
                onShowCrashLog = {},
                onShowPerformanceStats = {},
                onOpenArduinoCloud = {},
                onQuitApp = {},
                context = mockk(relaxed = true),
                view = mockk(relaxed = true)
            )
        }

        composeTestRule.onNodeWithText("RESET").assertExists()
    }

    @Test
    fun `connection mode selector shows bluetooth icon`() {
        composeTestRule.setContent {
            ControlHeaderBar(
                connectionMode = ConnectionMode.BLUETOOTH,
                bluetoothConnectionState = ConnectionState.DISCONNECTED,
                wifiConnectionState = WifiConnectionState.DISCONNECTED,
                rssiValue = 0,
                wifiRssi = 0,
                rttHistory = emptyList(),
                wifiRttHistory = emptyList(),
                isEStopActive = false,
                autoReconnectEnabled = false,
                onToggleAutoReconnect = {},
                onScanDevices = {},
                onReconnectDevice = {},
                onSwitchToWifi = {},
                onSwitchToBluetooth = {},
                onConfigureWifi = {},
                onTelemetryGraph = {},
                onToggleEStop = {},
                onShowSettings = {},
                onShowHelp = {},
                onShowAbout = {},
                onShowCrashLog = {},
                onShowPerformanceStats = {},
                onOpenArduinoCloud = {},
                onQuitApp = {},
                context = mockk(relaxed = true),
                view = mockk(relaxed = true)
            )
        }

        composeTestRule.onNodeWithContentDescription("Bluetooth").assertExists()
        composeTestRule.onNodeWithContentDescription("WiFi").assertExists()
    }
}
