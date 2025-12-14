package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.ui.components.EmbeddedTerminal
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.PacketLossWarningCard


/**
 * Portrait layout: Stacked vertically (Header → Debug → Servo → Joystick)
 */
@Composable
fun PortraitControlLayout(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(safeDrawingPadding)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header bar
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
            buttonSize = 32.dp,
            eStopSize = 56.dp,
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



        // Packet Loss Warning
        telemetry?.let { telem ->
            PacketLossWarningCard(
                packetsSent = telem.packetsSent,
                packetsDropped = telem.packetsDropped,
                packetsFailed = telem.packetsFailed
            )
        }


        // Debug Panel (if visible)
        if (viewModel.isDebugPanelVisible) {
            EmbeddedTerminal(
                logs = debugLogs,
                telemetry = telemetry,
                onSendCommand = { cmd -> viewModel.handleServoCommand(cmd) },
                onClearLogs = { bluetoothManager.log("Logs cleared", LogType.INFO) },
                onMaximize = { viewModel.showMaximizedDebug = true },
                onMinimize = { viewModel.isDebugPanelVisible = false },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(0.4f).fillMaxWidth(),
                onExportLogs = exportLogs
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Servo Buttons
        ServoPanel(
            servoX = viewModel.servoX,
            servoY = viewModel.servoY,
            onServoMove = { x, y -> viewModel.updateServo(x, y) },
            onLog = { message -> bluetoothManager.log(message, LogType.INFO) },
            buttonSize = 56.dp,
            modifier = Modifier.weight(if (viewModel.isDebugPanelVisible) 0.25f else 0.4f).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Joystick
        val portraitJoystickSize = minOf(orientationConfig.screenWidthDp.dp * 0.5f, 180.dp)
        JoystickPanel(
            onMoved = { x, y -> viewModel.updateJoystick(x, y) },
            size = portraitJoystickSize,
            isThrottle = false,
            bluetoothRttMs = health?.lastRttMs,
            wifiRttMs = wifiRtt,
            isWifiMode = viewModel.connectionMode == ConnectionMode.WIFI,
            sensitivity = viewModel.currentProfile.sensitivity,
            modifier = Modifier.weight(if (viewModel.isDebugPanelVisible) 0.35f else 0.6f).fillMaxWidth()
        )
    }
}
