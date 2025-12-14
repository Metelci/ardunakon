package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.crash.CrashHandler
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.EmbeddedTerminal
import com.metelci.ardunakon.ui.components.PacketLossWarningCard
import com.metelci.ardunakon.ui.components.StatusCard
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import com.metelci.ardunakon.wifi.WifiConnectionState

/**
 * Landscape layout: Side by side (Controls left, Debug right)
 */
@Composable
fun LandscapeControlLayout(
    viewModel: ControlViewModel,
    bluetoothManager: AppBluetoothManager,
    connectionState: ConnectionState,
    wifiState: WifiConnectionState,
    rssiValue: Int,
    wifiRssi: Int,
    wifiRtt: Long,
    rttHistory: List<Long>,
    wifiRttHistory: List<Long>,
    health: com.metelci.ardunakon.bluetooth.ConnectionHealth?,
    debugLogs: List<com.metelci.ardunakon.model.LogEntry>,
    telemetry: Telemetry?,
    autoReconnectEnabled: Boolean,
    isEStopActive: Boolean,
    isWifiEncrypted: Boolean = false,
    isDarkTheme: Boolean,
    safeDrawingPadding: androidx.compose.foundation.layout.PaddingValues,
    orientationConfig: Configuration,
    view: android.view.View,
    context: Context,
    onQuitApp: () -> Unit,
    exportLogs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(safeDrawingPadding)
            .padding(8.dp)
    ) {
        // Left column: Controls
        Column(
            modifier = Modifier
                .weight(if (viewModel.isDebugPanelVisible) 0.65f else 1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            ControlHeaderBar(
                connectionMode = viewModel.connectionMode,
                bluetoothConnectionState = connectionState,
                wifiConnectionState = wifiState,
                rssiValue = rssiValue,
                wifiRssi = wifiRssi,
                rttHistory = rttHistory,
                wifiRttHistory = wifiRttHistory,
                isEStopActive = isEStopActive,
                autoReconnectEnabled = autoReconnectEnabled,
                onToggleAutoReconnect = { bluetoothManager.setAutoReconnectEnabled(it) },
                isWifiEncrypted = isWifiEncrypted,
                isDebugPanelVisible = viewModel.isDebugPanelVisible,

                isDarkTheme = isDarkTheme,
                allowReflection = viewModel.allowReflection,
                buttonSize = 36.dp,
                eStopSize = 72.dp,
                onScanDevices = { viewModel.showDeviceList = true },
                onReconnectDevice = {
                    val reconnected = bluetoothManager.reconnectSavedDevice()
                    if (!reconnected) viewModel.showDeviceList = true
                },
                onSwitchToWifi = { viewModel.connectionMode = ConnectionMode.WIFI },
                onSwitchToBluetooth = { viewModel.connectionMode = ConnectionMode.BLUETOOTH },
                onConfigureWifi = { viewModel.showWifiConfig = true },
                onTelemetryGraph = { viewModel.showTelemetryGraph = true },
                onToggleEStop = { viewModel.toggleEStop(view) },
                onToggleDebugPanel = { viewModel.isDebugPanelVisible = !viewModel.isDebugPanelVisible },
                onShowHelp = { viewModel.showHelpDialog = true },
                onShowAbout = { viewModel.showAboutDialog = true },
                onShowCrashLog = { viewModel.showCrashLog = true },
                onShowOta = { viewModel.showOtaDialog = true },
                onToggleReflection = { viewModel.allowReflection = !viewModel.allowReflection },
                onOpenArduinoCloud = {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://cloud.arduino.cc"))
                    context.startActivity(intent)
                },
                onQuitApp = onQuitApp,
                context = context,
                view = view
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Status row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentConnectionState = if (viewModel.connectionMode == ConnectionMode.WIFI) {

                    when (wifiState) {
                        WifiConnectionState.CONNECTED -> ConnectionState.CONNECTED
                        WifiConnectionState.CONNECTING -> ConnectionState.CONNECTING
                        WifiConnectionState.ERROR -> ConnectionState.ERROR
                        else -> ConnectionState.DISCONNECTED
                    }
                } else {
                    connectionState
                }

                val currentRssi = if (viewModel.connectionMode == ConnectionMode.WIFI) wifiRssi else rssiValue
                val hasCrashLog = CrashHandler.hasCrashLog(context)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusCard(
                        label = "Device",
                        state = currentConnectionState,
                        rssi = currentRssi,
                        hasCrashLog = hasCrashLog,
                        onClick = {
                            if (viewModel.connectionMode == ConnectionMode.WIFI) {
                                viewModel.showWifiConfig = true
                            } else {
                                viewModel.showDeviceList = true
                            }
                        },
                        onCrashLogClick = { viewModel.showCrashLog = true },
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            // Packet Loss Warning
            telemetry?.let { telem ->
                PacketLossWarningCard(
                    packetsSent = telem.packetsSent,
                    packetsDropped = telem.packetsDropped,
                    packetsFailed = telem.packetsFailed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Joysticks
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            val screenWidth = configuration.screenWidthDp.dp
            val availableHeight = (screenHeight - 250.dp).coerceAtLeast(150.dp)
            val panelWidth = if (viewModel.isDebugPanelVisible) screenWidth * 0.65f else screenWidth
            val availableForJoysticks = (panelWidth - 136.dp).coerceAtLeast(10.dp)
            val maxJoystickWidth = availableForJoysticks / 2.1f
            val joystickSize = minOf(maxJoystickWidth, availableHeight, 220.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JoystickPanel(
                    onMoved = { x, y -> viewModel.updateJoystick(x, y) },
                    size = joystickSize,
                    isThrottle = false,
                    bluetoothRttMs = health?.lastRttMs,
                    wifiRttMs = wifiRtt,
                    isWifiMode = viewModel.connectionMode == ConnectionMode.WIFI,
                    sensitivity = viewModel.currentProfile.sensitivity,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                ServoPanel(
                    servoX = viewModel.servoX,
                    servoY = viewModel.servoY,
                    servoZ = viewModel.servoZ,
                    onServoMove = { x, y, z -> viewModel.updateServo(x, y, z) },
                    onLog = { message -> bluetoothManager.log(message, LogType.INFO) },
                    buttonSize = 60.dp,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Right side: Debug Panel
        if (viewModel.isDebugPanelVisible) {
            Spacer(modifier = Modifier.width(8.dp))
            EmbeddedTerminal(
                logs = debugLogs,
                telemetry = telemetry,
                onSendCommand = { cmd -> viewModel.handleServoCommand(cmd) },
                onClearLogs = { bluetoothManager.log("Logs cleared", LogType.INFO) },
                onMaximize = { viewModel.showMaximizedDebug = true },
                onMinimize = { viewModel.isDebugPanelVisible = false },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                onExportLogs = exportLogs
            )
        }
    }
}
