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
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.ota.BleOtaTransport
import com.metelci.ardunakon.ota.OtaManager
import com.metelci.ardunakon.ota.WifiOtaTransport
import com.metelci.ardunakon.ui.components.OtaDialog
import com.metelci.ardunakon.ui.components.SettingsDialog
import com.metelci.ardunakon.ui.components.TelemetryGraphDialog
import com.metelci.ardunakon.ui.components.TerminalDialog
import com.metelci.ardunakon.ui.components.WifiConfigDialog
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.wifi.WifiManager

/**
 * Container for all dialogs used in ControlScreen.
 * Centralizes dialog rendering to reduce main screen complexity.
 */
@Suppress("FunctionName")
@Composable
fun ControlScreenDialogs(
    viewModel: ControlViewModel,
    bluetoothManager: AppBluetoothManager,
    wifiManager: WifiManager,
    view: View,
    onExportLogs: () -> Unit,
    onTakeTutorial: (() -> Unit)? = null
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
            onDismiss = { viewModel.showTelemetryGraph = false }
        )
    }

    // Help Dialog
    if (viewModel.showHelpDialog) {
        com.metelci.ardunakon.ui.components.HelpDialog(
            onDismiss = { viewModel.showHelpDialog = false },
            onTakeTutorial = onTakeTutorial
        )
    }

    // About Dialog
    if (viewModel.showAboutDialog) {
        com.metelci.ardunakon.ui.components.AboutDialog(
            onDismiss = { viewModel.showAboutDialog = false }
        )
    }

    // Crash Log Dialog
    if (viewModel.showCrashLog) {
        val crashLog = com.metelci.ardunakon.crash.CrashHandler.getCrashLog(context)
        CrashLogDialog(
            crashLog = crashLog,
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

    // Encryption Error Dialog - blocking prompt for encryption failures
    viewModel.encryptionError?.let { error ->
        EncryptionErrorDialog(
            error = error,
            onRetry = { viewModel.retryWithEncryption() },
            onDisableEncryption = { viewModel.continueWithoutEncryption() },
            onDismiss = { viewModel.dismissEncryptionError() }
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

    // Settings Dialog
    if (viewModel.showSettingsDialog) {
        val customCommands by viewModel.customCommandRegistry.commands.collectAsState()
        SettingsDialog(
            onDismiss = { viewModel.showSettingsDialog = false },
            view = view,
            isDebugPanelVisible = viewModel.isDebugPanelVisible,
            onToggleDebugPanel = { viewModel.isDebugPanelVisible = !viewModel.isDebugPanelVisible },
            isHapticEnabled = viewModel.isHapticEnabled,
            onToggleHaptic = { viewModel.updateHapticEnabled(!viewModel.isHapticEnabled) },
            joystickSensitivity = viewModel.joystickSensitivity,
            onJoystickSensitivityChange = { viewModel.updateJoystickSensitivity(it) },
            allowReflection = viewModel.allowReflection,
            onToggleReflection = { viewModel.updateAllowReflection(!viewModel.allowReflection) },
            onShowOta = {
                viewModel.showSettingsDialog = false
                viewModel.showOtaDialog = true
            },
            customCommandCount = customCommands.size,
            onShowCustomCommands = {
                viewModel.showSettingsDialog = false
                viewModel.showCustomCommandsDialog = true
            },
            onResetTutorial = { viewModel.resetTutorial() }
        )
    }

    // Custom Commands List Dialog
    if (viewModel.showCustomCommandsDialog) {
        val customCommands by viewModel.customCommandRegistry.commands.collectAsState()
        com.metelci.ardunakon.ui.components.CustomCommandListDialog(
            commands = customCommands,
            view = view,
            onAddCommand = {
                viewModel.editingCommand = null
                viewModel.showCustomCommandEditor = true
            },
            onEditCommand = { cmd ->
                viewModel.editingCommand = cmd
                viewModel.showCustomCommandEditor = true
            },
            onDeleteCommand = { viewModel.deleteCustomCommand(it) },
            onSendCommand = { viewModel.sendCustomCommand(it) },
            onDismiss = { viewModel.showCustomCommandsDialog = false }
        )
    }

    // Custom Command Editor Dialog
    if (viewModel.showCustomCommandEditor) {
        com.metelci.ardunakon.ui.components.CustomCommandDialog(
            command = viewModel.editingCommand,
            availableCommandIds = viewModel.getAvailableCommandIds(),
            view = view,
            onSave = { viewModel.saveCustomCommand(it) },
            onDismiss = {
                viewModel.editingCommand = null
                viewModel.showCustomCommandEditor = false
            }
        )
    }

    // WiFi Config Dialog
    if (viewModel.showWifiConfig) {
        val prefs = context.getSharedPreferences("ArdunakonPrefs", Context.MODE_PRIVATE)
        val savedIp = remember { prefs.getString("last_wifi_ip", "192.168.4.1") ?: "192.168.4.1" }
        val savedPort = remember { prefs.getInt("last_wifi_port", 8888) }
        val isEncrypted by wifiManager.isEncrypted.collectAsState()

        WifiConfigDialog(
            initialIp = savedIp,
            initialPort = savedPort,
            scannedDevices = wifiScannedDevices,
            isEncrypted = isEncrypted,
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
