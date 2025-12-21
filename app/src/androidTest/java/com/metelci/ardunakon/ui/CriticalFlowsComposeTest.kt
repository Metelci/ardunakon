package com.metelci.ardunakon.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.DeviceType
import com.metelci.ardunakon.ui.components.WifiConfigDialog
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import com.metelci.ardunakon.ui.screens.control.ConnectionModeSelector
import com.metelci.ardunakon.ui.screens.control.ConnectionStatusWidget
import com.metelci.ardunakon.ui.screens.control.ControlHeaderBar
import com.metelci.ardunakon.ui.screens.control.dialogs.DeviceListDialog
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalFlowsComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun connectionModeSelector_switches_between_bluetooth_and_wifi() {
        composeRule.setContent {
            var mode by remember { mutableStateOf(ConnectionMode.BLUETOOTH) }
            MaterialTheme {
                Column {
                    ConnectionModeSelector(
                        selectedMode = mode,
                        onModeSelected = { mode = it },
                        view = LocalView.current
                    )
                    Text("mode=$mode")
                }
            }
        }

        composeRule.onNodeWithText("mode=BLUETOOTH").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("WiFi").performClick()
        composeRule.onNodeWithText("mode=WIFI").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Bluetooth").performClick()
        composeRule.onNodeWithText("mode=BLUETOOTH").assertIsDisplayed()
    }

    @Test
    fun connectionStatusWidget_bluetooth_shows_scan_and_reconnect_actions() {
        composeRule.setContent {
            var lastAction by remember { mutableStateOf("none") }
            MaterialTheme {
                Column {
                    ConnectionStatusWidget(
                        connectionMode = ConnectionMode.BLUETOOTH,
                        btState = ConnectionState.DISCONNECTED,
                        wifiState = WifiConnectionState.DISCONNECTED,
                        rssi = -60,
                        rttHistory = listOf(17L),
                        onReconnect = { lastAction = "reconnect" },
                        onConfigure = { lastAction = "configure" },
                        onScanDevices = { lastAction = "scan" },
                        view = LocalView.current,
                        modifier = Modifier.semantics { contentDescription = "Connection Status" }
                    )
                    Text("action=$lastAction")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Connection Status").performClick()
        composeRule.onNodeWithText("Scan for Devices").assertIsDisplayed()
        composeRule.onNodeWithText("Reconnect Device").assertIsDisplayed()
        composeRule.onNodeWithText("Configure WiFi").assertDoesNotExist()

        composeRule.onNodeWithText("Scan for Devices").performClick()
        composeRule.onNodeWithText("action=scan").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Connection Status").performClick()
        composeRule.onNodeWithText("Reconnect Device").performClick()
        composeRule.onNodeWithText("action=reconnect").assertIsDisplayed()
    }

    @Test
    fun connectionStatusWidget_wifi_shows_configure_action() {
        composeRule.setContent {
            var lastAction by remember { mutableStateOf("none") }
            MaterialTheme {
                Column {
                    ConnectionStatusWidget(
                        connectionMode = ConnectionMode.WIFI,
                        btState = ConnectionState.DISCONNECTED,
                        wifiState = WifiConnectionState.DISCONNECTED,
                        rssi = -45,
                        rttHistory = listOf(23L),
                        onReconnect = { lastAction = "reconnect" },
                        onConfigure = { lastAction = "configure" },
                        onScanDevices = { lastAction = "scan" },
                        view = LocalView.current,
                        modifier = Modifier.semantics { contentDescription = "Connection Status" }
                    )
                    Text("action=$lastAction")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Connection Status").performClick()
        composeRule.onNodeWithText("Configure WiFi").assertIsDisplayed()
        composeRule.onNodeWithText("Scan for Devices").assertDoesNotExist()
        composeRule.onNodeWithText("Reconnect Device").assertDoesNotExist()

        composeRule.onNodeWithText("Configure WiFi").performClick()
        composeRule.onNodeWithText("action=configure").assertIsDisplayed()
    }

    @Test
    fun header_overflowMenu_triggers_help_dialog() {
        composeRule.setContent {
            val context = LocalContext.current
            val view = LocalView.current
            var helpOpened by remember { mutableStateOf(false) }

            MaterialTheme {
                Column {
                    ControlHeaderBar(
                        connectionMode = ConnectionMode.BLUETOOTH,
                        bluetoothConnectionState = ConnectionState.DISCONNECTED,
                        wifiConnectionState = WifiConnectionState.DISCONNECTED,
                        rssiValue = -60,
                        wifiRssi = -45,
                        rttHistory = listOf(10L),
                        wifiRttHistory = listOf(10L),
                        isEStopActive = false,
                        autoReconnectEnabled = true,
                        onToggleAutoReconnect = {},
                        isWifiEncrypted = false,
                        onScanDevices = {},
                        onReconnectDevice = {},
                        onSwitchToWifi = {},
                        onSwitchToBluetooth = {},
                        onConfigureWifi = {},
                        onTelemetryGraph = {},
                        onToggleEStop = {},
                        onShowSettings = {},
                        onShowHelp = { helpOpened = true },
                        onShowAbout = {},
                        onShowCrashLog = {},
                        onShowOta = {},
                        onOpenArduinoCloud = {},
                        onQuitApp = {},
                        context = context,
                        view = view
                    )
                    Text("helpOpened=$helpOpened")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.onNodeWithText("Help").performClick()
        composeRule.onNodeWithText("helpOpened=true").assertIsDisplayed()
    }

    @Test
    fun header_emergencyStop_toggles_semantics_and_label() {
        composeRule.setContent {
            val context = LocalContext.current
            val view = LocalView.current
            var isEStopActive by remember { mutableStateOf(false) }

            MaterialTheme {
                Column {
                    ControlHeaderBar(
                        connectionMode = ConnectionMode.BLUETOOTH,
                        bluetoothConnectionState = ConnectionState.DISCONNECTED,
                        wifiConnectionState = WifiConnectionState.DISCONNECTED,
                        rssiValue = -60,
                        wifiRssi = -45,
                        rttHistory = listOf(10L),
                        wifiRttHistory = listOf(10L),
                        isEStopActive = isEStopActive,
                        autoReconnectEnabled = true,
                        onToggleAutoReconnect = {},
                        isWifiEncrypted = false,
                        onScanDevices = {},
                        onReconnectDevice = {},
                        onSwitchToWifi = {},
                        onSwitchToBluetooth = {},
                        onConfigureWifi = {},
                        onTelemetryGraph = {},
                        onToggleEStop = { isEStopActive = !isEStopActive },
                        onShowSettings = {},
                        onShowHelp = {},
                        onShowAbout = {},
                        onShowCrashLog = {},
                        onShowOta = {},
                        onOpenArduinoCloud = {},
                        onQuitApp = {},
                        context = context,
                        view = view
                    )
                }
            }
        }

        composeRule.onNodeWithText("STOP").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Emergency stop. Tap to immediately stop all motors.").performClick()
        composeRule.onNodeWithText("RESET").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Emergency stop active. Tap to release and resume control."
        ).assertIsDisplayed()
    }

    @Test
    fun connectionStatusWidget_wifi_encrypted_shows_lock_icon() {
        composeRule.setContent {
            var lastAction by remember { mutableStateOf("none") }
            MaterialTheme {
                Column {
                    ConnectionStatusWidget(
                        connectionMode = ConnectionMode.WIFI,
                        btState = ConnectionState.DISCONNECTED,
                        wifiState = WifiConnectionState.CONNECTED,
                        rssi = -45,
                        rttHistory = listOf(12L),
                        isEncrypted = true,
                        onReconnect = { lastAction = "reconnect" },
                        onConfigure = { lastAction = "configure" },
                        onScanDevices = { lastAction = "scan" },
                        view = LocalView.current,
                        modifier = Modifier.semantics { contentDescription = "Connection Status" }
                    )
                    Text("action=$lastAction")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Encrypted connection").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Connection Status").performClick()
        composeRule.onNodeWithText("Configure WiFi").performClick()
        composeRule.onNodeWithText("action=configure").assertIsDisplayed()
    }

    @Test
    fun controlHeaderBar_mode_switch_invokes_callbacks() {
        composeRule.setContent {
            val context = LocalContext.current
            val view = LocalView.current
            var switches by remember { mutableStateOf(0) }

            MaterialTheme {
                Column {
                    ControlHeaderBar(
                        connectionMode = ConnectionMode.BLUETOOTH,
                        bluetoothConnectionState = ConnectionState.DISCONNECTED,
                        wifiConnectionState = WifiConnectionState.DISCONNECTED,
                        rssiValue = -60,
                        wifiRssi = -45,
                        rttHistory = listOf(10L),
                        wifiRttHistory = listOf(12L),
                        isEStopActive = false,
                        autoReconnectEnabled = false,
                        onToggleAutoReconnect = {},
                        isWifiEncrypted = false,
                        onScanDevices = {},
                        onReconnectDevice = {},
                        onSwitchToWifi = { switches++ },
                        onSwitchToBluetooth = { switches++ },
                        onConfigureWifi = {},
                        onTelemetryGraph = {},
                        onToggleEStop = {},
                        onShowSettings = {},
                        onShowHelp = {},
                        onShowAbout = {},
                        onShowCrashLog = {},
                        onShowOta = {},
                        onOpenArduinoCloud = {},
                        onQuitApp = {},
                        context = context,
                        view = view
                    )
                    Text("switches=$switches")
                }
            }
        }

        composeRule.onNodeWithText("switches=0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("WiFi").performClick()
        composeRule.onNodeWithText("switches=1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Bluetooth").performClick()
        composeRule.onNodeWithText("switches=2").assertIsDisplayed()
    }

    @Test
    fun deviceListDialog_scan_and_connect_device() {
        val device = BluetoothDeviceModel(
            name = "HC-05 (00:11:22:33:44:55)",
            address = "00:11:22:33:44:55",
            type = DeviceType.CLASSIC,
            rssi = -50
        )

        composeRule.setContent {
            val view = LocalView.current
            var scanClicks by remember { mutableStateOf(0) }
            var selectedDevice: BluetoothDeviceModel? by remember { mutableStateOf(null) }

            MaterialTheme {
                Column {
                    DeviceListDialog(
                        scannedDevices = listOf(device),
                        onScan = { scanClicks++ },
                        onDeviceSelected = { selectedDevice = it },
                        onDismiss = {},
                        view = view
                    )
                    Text("scanClicks=$scanClicks")
                    Text("selected=${selectedDevice?.address ?: "none"}")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Scan").performClick()
        composeRule.onNodeWithText("scanClicks=1").assertIsDisplayed()

        composeRule.onNodeWithText("HC-05").performClick()
        composeRule.onNodeWithText("selected=00:11:22:33:44:55").assertIsDisplayed()
    }

    @Test
    fun wifiConfigDialog_select_device_prefills_fields_and_connects() {
        val device = WifiDevice(name = "Arduino R4 WiFi", ip = "192.168.4.1", port = 8888, trusted = true)

        composeRule.setContent {
            var saved by remember { mutableStateOf<Pair<String, Int>?>(null) }
            MaterialTheme {
                Column {
                    WifiConfigDialog(
                        initialIp = "",
                        initialPort = 8888,
                        scannedDevices = listOf(device),
                        isEncrypted = true,
                        onScan = {},
                        onDismiss = {},
                        onSave = { ip, port -> saved = ip to port }
                    )
                    Text("saved=${saved?.first ?: "none"}:${saved?.second ?: -1}")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Select Arduino R4 WiFi at 192.168.4.1").performClick()
        composeRule.onNodeWithText("192.168.4.1").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Connect to WiFi device").performClick()
        composeRule.onNodeWithText("saved=192.168.4.1:8888").assertIsDisplayed()
    }
}
