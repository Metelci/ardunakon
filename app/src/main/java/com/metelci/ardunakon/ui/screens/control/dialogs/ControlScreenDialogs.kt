package com.metelci.ardunakon.ui.screens.control.dialogs

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.ota.BleOtaTransport
import com.metelci.ardunakon.ota.OtaManager
import com.metelci.ardunakon.ota.WifiOtaTransport
import com.metelci.ardunakon.ui.components.OtaDialog
import com.metelci.ardunakon.ui.components.TelemetryGraphDialog
import com.metelci.ardunakon.ui.components.TerminalDialog
import com.metelci.ardunakon.ui.components.WifiConfigDialog
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.wifi.WifiManager

/**
 * Container for all dialogs used in ControlScreen.
 * Centralizes dialog rendering to reduce main screen complexity.
 */
@Composable
fun ControlScreenDialogs(
    viewModel: ControlViewModel,
    bluetoothManager: AppBluetoothManager,
    wifiManager: WifiManager,
    isDarkTheme: Boolean,
    view: View,
    onExportLogs: () -> Unit
) {
    val context = LocalContext.current
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val telemetry by bluetoothManager.telemetry.collectAsState()
    val scannedDevices by bluetoothManager.scannedDevices.collectAsState()
    val wifiScannedDevices by wifiManager.scannedDevices.collectAsState()
    
    // OTA components
    val otaManager = remember { OtaManager(context) }
    val wifiOtaTransport = remember { WifiOtaTransport(context) }
    val bleOtaTransport = remember { BleOtaTransport(context, bluetoothManager) }

    // Debug Console Dialog
    if (viewModel.showDebugConsole) {
        TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { viewModel.showDebugConsole = false },
            onSendCommand = { cmd -> viewModel.handleServoCommand(cmd) },
            onClearLogs = { bluetoothManager.log("Logs cleared", LogType.INFO) },
            onExportLogs = onExportLogs
        )
    }

    // Maximized Debug Dialog
    if (viewModel.showMaximizedDebug) {
        TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { viewModel.showMaximizedDebug = false },
            onSendCommand = { cmd -> viewModel.handleServoCommand(cmd) },
            onClearLogs = { bluetoothManager.log("Logs cleared", LogType.INFO) },
            onExportLogs = onExportLogs
        )
    }

    // Telemetry Graph Dialog
    if (viewModel.showTelemetryGraph) {
        TelemetryGraphDialog(
            telemetryHistoryManager = bluetoothManager.telemetryHistoryManager,
            onDismiss = { viewModel.showTelemetryGraph = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Help Dialog
    if (viewModel.showHelpDialog) {
        com.metelci.ardunakon.ui.components.HelpDialog(
            onDismiss = { viewModel.showHelpDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // About Dialog
    if (viewModel.showAboutDialog) {
        com.metelci.ardunakon.ui.components.AboutDialog(
            onDismiss = { viewModel.showAboutDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Crash Log Dialog
    if (viewModel.showCrashLog) {
        val crashLog = com.metelci.ardunakon.crash.CrashHandler.getCrashLog(context)
        CrashLogDialog(
            crashLog = crashLog,
            isDarkTheme = isDarkTheme,
            view = view,
            onShare = { /* Share handled internally */ },
            onClear = { viewModel.showCrashLog = false },
            onDismiss = { viewModel.showCrashLog = false }
        )
    }

    // Device List Dialog
    if (viewModel.showDeviceList) {
        DeviceListDialog(
            scannedDevices = scannedDevices,
            isDarkTheme = isDarkTheme,
            onScan = { bluetoothManager.startScan() },
            onDeviceSelected = { device ->
                bluetoothManager.connectToDevice(device)
                viewModel.showDeviceList = false
            },
            onDismiss = { viewModel.showDeviceList = false },
            view = view
        )
    }

    // Security Error Dialog
    viewModel.securityErrorMessage?.let { msg ->
        SecurityErrorDialog(
            message = msg,
            onOpenSettings = {
                viewModel.securityErrorMessage = null
                try {
                    context.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                } catch (_: Exception) {}
            },
            onDismiss = { viewModel.securityErrorMessage = null }
        )
    }

    // OTA Dialog
    if (viewModel.showOtaDialog) {
        OtaDialog(
            otaManager = otaManager,
            bleTransport = bleOtaTransport,
            wifiTransport = wifiOtaTransport,
            onDismiss = { viewModel.showOtaDialog = false }
        )
    }

    // WiFi Config Dialog
    if (viewModel.showWifiConfig) {
        val prefs = context.getSharedPreferences("ArdunakonPrefs", Context.MODE_PRIVATE)
        val savedIp = remember { prefs.getString("last_wifi_ip", "192.168.4.1") ?: "192.168.4.1" }
        val savedPort = remember { prefs.getInt("last_wifi_port", 8888) }

        WifiConfigDialog(
            initialIp = savedIp,
            initialPort = savedPort,
            scannedDevices = wifiScannedDevices,
            onScan = { wifiManager.startDiscovery() },
            onDismiss = { viewModel.showWifiConfig = false },
            onSave = { ip, port ->
                viewModel.showWifiConfig = false
                prefs.edit().putString("last_wifi_ip", ip).putInt("last_wifi_port", port).apply()
                wifiManager.connect(ip, port)
            }
        )
    }
}
