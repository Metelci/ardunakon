package com.metelci.ardunakon.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.EmbeddedTerminal
import com.metelci.ardunakon.ui.components.PacketLossWarningCard
import com.metelci.ardunakon.ui.components.StatusCard
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import com.metelci.ardunakon.ui.screens.control.ControlHeaderBar
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.ui.screens.control.ControlViewModelFactory
import com.metelci.ardunakon.ui.screens.control.JoystickPanel
import com.metelci.ardunakon.ui.screens.control.ServoPanel
import com.metelci.ardunakon.ui.screens.control.dialogs.ControlScreenDialogs
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager

/**
 * Main control screen for the Ardunakon application.
 * Refactored to delegate to layout-specific composables and dialog manager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager,
    wifiManager: WifiManager,
    isDarkTheme: Boolean = true,
    onQuitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    // ViewModel instantiation
    val viewModel: ControlViewModel = viewModel(
        factory = ControlViewModelFactory(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            context = context
        )
    )

    // State collections
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val rssiValue by bluetoothManager.rssiValue.collectAsState()
    val health by bluetoothManager.health.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val telemetry by bluetoothManager.telemetry.collectAsState()
    val autoReconnectEnabled by bluetoothManager.autoReconnectEnabled.collectAsState()
    val rttHistory by bluetoothManager.rttHistory.collectAsState()
    val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()
    
    val wifiState by wifiManager.connectionState.collectAsState()
    val wifiRssi by wifiManager.rssi.collectAsState()
    val wifiRtt by wifiManager.rtt.collectAsState()
    val wifiIncomingData by wifiManager.incomingData.collectAsState()
    val wifiRttHistory by wifiManager.rttHistory.collectAsState()

    // Bridge WiFi telemetry to HistoryManager
    LaunchedEffect(wifiState, wifiRssi, wifiRtt, wifiIncomingData) {
        if (wifiState == WifiConnectionState.CONNECTED) {
            if (wifiRssi != 0) bluetoothManager.telemetryHistoryManager.recordRssi(wifiRssi)
            if (wifiRtt != 0L) bluetoothManager.telemetryHistoryManager.recordRtt(wifiRtt)
            wifiIncomingData?.let { data -> bluetoothManager.parseTelemetry(data) }
        }
    }

    // WiFi incoming data processing
    LaunchedEffect(wifiIncomingData) {
        wifiIncomingData?.let { data ->
            if (data.size >= 10 && data[0] == 0xAA.toByte() && data[9] == 0x55.toByte()) {
                val cmd = data[2].toInt() and 0xFF
                when (cmd) {
                    0x03 -> { // CMD_HEARTBEAT
                        val batteryRaw = data[3].toInt() and 0xFF
                        val batteryVoltage = batteryRaw / 10.0f
                        val isEStop = (data[4].toInt() and 0xFF) == 1
                        bluetoothManager.log("WiFi RX: Battery=${batteryVoltage}V, E-Stop=$isEStop", LogType.INFO)
                    }
                    0x05 -> { // CMD_ANNOUNCE_CAPABILITIES
                        val caps = data[3].toInt() and 0xFF
                        val boardType = data[5].toInt() and 0xFF
                        bluetoothManager.log("WiFi RX: Capabilities=0x${caps.toString(16)}, Board=$boardType", LogType.INFO)
                    }
                }
            }
        }
    }

    // Export logs function
    val exportLogs: () -> Unit = { viewModel.exportLogs(context) }

    // Keep Screen On
    val currentActivity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) {
        currentActivity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            currentActivity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Layout
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val orientationConfig = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = orientationConfig.orientation == Configuration.ORIENTATION_PORTRAIT

    val backgroundBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F1419))
        } else {
            listOf(Color(0xFFFCE4EC), Color(0xFFE3F2FD), Color(0xFFE8F5E9))
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        if (isPortrait) {
            PortraitControlLayout(
                viewModel = viewModel,
                bluetoothManager = bluetoothManager,
                connectionState = connectionState,
                wifiState = wifiState,
                rssiValue = rssiValue,
                wifiRssi = wifiRssi,
                wifiRtt = wifiRtt,
                rttHistory = rttHistory,
                wifiRttHistory = wifiRttHistory,
                health = health,
                debugLogs = debugLogs,
                telemetry = telemetry,
                isEStopActive = isEStopActive,
                isDarkTheme = isDarkTheme,
                safeDrawingPadding = safeDrawingPadding,
                orientationConfig = orientationConfig,
                view = view,
                context = context,
                onQuitApp = onQuitApp,
                exportLogs = exportLogs
            )
        } else {
            LandscapeControlLayout(
                viewModel = viewModel,
                bluetoothManager = bluetoothManager,
                connectionState = connectionState,
                wifiState = wifiState,
                rssiValue = rssiValue,
                wifiRssi = wifiRssi,
                wifiRtt = wifiRtt,
                rttHistory = rttHistory,
                wifiRttHistory = wifiRttHistory,
                health = health,
                debugLogs = debugLogs,
                telemetry = telemetry,
                autoReconnectEnabled = autoReconnectEnabled,
                isEStopActive = isEStopActive,
                isDarkTheme = isDarkTheme,
                safeDrawingPadding = safeDrawingPadding,
                orientationConfig = orientationConfig,
                view = view,
                context = context,
                onQuitApp = onQuitApp,
                exportLogs = exportLogs
            )
        }
    }

    // Dialogs
    ControlScreenDialogs(
        viewModel = viewModel,
        bluetoothManager = bluetoothManager,
        wifiManager = wifiManager,
        isDarkTheme = isDarkTheme,
        view = view,
        onExportLogs = exportLogs
    )
}

/**
 * Portrait layout: Stacked vertically (Header → Debug → Servo → Joystick)
 */
@Composable
private fun PortraitControlLayout(
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
    telemetry: AppBluetoothManager.Telemetry?,
    isEStopActive: Boolean,
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

/**
 * Landscape layout: Side by side (Controls left, Debug right)
 */
@Composable
private fun LandscapeControlLayout(
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
    telemetry: AppBluetoothManager.Telemetry?,
    autoReconnectEnabled: Boolean,
    isEStopActive: Boolean,
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
            verticalArrangement = Arrangement.Top
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoReconnectToggle(
                    enabled = autoReconnectEnabled,
                    onToggle = { bluetoothManager.setAutoReconnectEnabled(it) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                val currentConnectionState = if (viewModel.connectionMode == ConnectionMode.WIFI) {
                    when (wifiState) {
                        WifiConnectionState.CONNECTED -> ConnectionState.CONNECTED
                        WifiConnectionState.CONNECTING -> ConnectionState.CONNECTING
                        WifiConnectionState.ERROR -> ConnectionState.ERROR
                        else -> ConnectionState.DISCONNECTED
                    }
                } else connectionState

                val currentRssi = if (viewModel.connectionMode == ConnectionMode.WIFI) wifiRssi else rssiValue
                val hasCrashLog = com.metelci.ardunakon.crash.CrashHandler.hasCrashLog(context)

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
                    onServoMove = { x, y -> viewModel.updateServo(x, y) },
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
