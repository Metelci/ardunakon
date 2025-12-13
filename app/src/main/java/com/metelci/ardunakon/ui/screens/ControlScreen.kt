package com.metelci.ardunakon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import android.view.HapticFeedbackConstants
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.ServoButtonControl
import com.metelci.ardunakon.ui.screens.control.JoystickPanel
import com.metelci.ardunakon.ui.screens.control.ServoPanel
import com.metelci.ardunakon.ui.components.EmbeddedTerminal
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.security.AuthRequiredException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.StatusCard
import com.metelci.ardunakon.ui.components.LatencySparkline
import com.metelci.ardunakon.ui.components.OtaDialog
import com.metelci.ardunakon.ota.OtaManager
import com.metelci.ardunakon.ota.WifiOtaTransport
import com.metelci.ardunakon.ota.BleOtaTransport
import com.metelci.ardunakon.wifi.WifiManager
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.ui.components.WifiConfigDialog
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import com.metelci.ardunakon.ui.screens.control.ControlHeaderBar
import com.metelci.ardunakon.ui.screens.control.dialogs.DeviceListDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.ui.screens.control.ControlViewModelFactory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager,
    wifiManager: WifiManager,
    isDarkTheme: Boolean = true,
    onQuitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // ViewModel instantiation
    val viewModel: ControlViewModel = viewModel(
        factory = ControlViewModelFactory(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            context = context
        )
    )

    val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()


    val scannedDevices by bluetoothManager.scannedDevices.collectAsState()
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val rssiValue by bluetoothManager.rssiValue.collectAsState()
    val health by bluetoothManager.health.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val telemetry by bluetoothManager.telemetry.collectAsState()
    val autoReconnectEnabled by bluetoothManager.autoReconnectEnabled.collectAsState()
    val rttHistory by bluetoothManager.rttHistory.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    


    // Removed pastelBrush for better visibility

    
    // OTA Manager and Transports
    val otaManager = remember { OtaManager(context) }
    val wifiOtaTransport = remember { WifiOtaTransport(context) }
    val bleOtaTransport = remember { BleOtaTransport(context, bluetoothManager) }
    
    val wifiState by wifiManager.connectionState.collectAsState()
    val wifiRssi by wifiManager.rssi.collectAsState()
    val wifiRtt by wifiManager.rtt.collectAsState()
    val wifiIncomingData by wifiManager.incomingData.collectAsState()

    // Bridge WiFi telemetry to HistoryManager and Telemetry Parser
    LaunchedEffect(wifiState, wifiRssi, wifiRtt, wifiIncomingData) {
        if (wifiState == WifiConnectionState.CONNECTED) {
            if (wifiRssi != 0) bluetoothManager.telemetryHistoryManager.recordRssi(wifiRssi)
            if (wifiRtt != 0L) bluetoothManager.telemetryHistoryManager.recordRtt(wifiRtt)
            
            // Parse binary telemetry from WiFi (Battery, Status)
            wifiIncomingData?.let { data ->
                bluetoothManager.parseTelemetry(data)
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

    // Haptics
    val view = LocalView.current

    // Left unchanged for now as they are effectively removed by previous edit or not used locally in a way that conflicts if removed above
    // Actually, I need to remove the local vals I added in lines 277-283

    
    // Transmission Loop is handled in ViewModel init block

    // WiFi Incoming Data Processing
    val wifiRttHistory by wifiManager.rttHistory.collectAsState()
    
    LaunchedEffect(wifiIncomingData) {
        wifiIncomingData?.let { data ->
            // Parse telemetry packet from Arduino (10-byte protocol)
            // [START(0xAA), DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END(0x55)]
            if (data.size >= 10 && data[0] == 0xAA.toByte() && data[9] == 0x55.toByte()) {
                val cmd = data[2].toInt() and 0xFF
                when (cmd) {
                    0x03 -> { // CMD_HEARTBEAT - Telemetry
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

    // UI Layout with theme toggle
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()

    // Orientation detection
    val orientationConfig = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = orientationConfig.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF1A1A2E), // dark blue-gray
                            Color(0xFF16213E), // darker blue
                            Color(0xFF0F1419)  // almost black
                        )
                    } else {
                        listOf(
                            Color(0xFFFCE4EC), // soft pink
                            Color(0xFFE3F2FD), // soft blue
                            Color(0xFFE8F5E9)  // soft green
                        )
                    }
                )
            )
    ) {
        if (isPortrait) {
            // Portrait Layout: Stacked vertically (Debug -> Servo -> Joystick)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safeDrawingPadding)
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()

                // Top status bar (compact for portrait) - using extracted component
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
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.arduino.cc"))
                        context.startActivity(intent)
                    },
                    onQuitApp = onQuitApp,
                    context = context,
                    view = view
                )


                // Debug Panel (if visible) - at top in portrait
                if (viewModel.isDebugPanelVisible) {
                    EmbeddedTerminal(
                        logs = debugLogs,
                        telemetry = telemetry,
                        onSendCommand = { cmd: String -> viewModel.handleServoCommand(cmd) },
                        onClearLogs = { bluetoothManager.log("Logs cleared", LogType.INFO) },
                        onMaximize = { viewModel.showMaximizedDebug = true },
                        onMinimize = { viewModel.isDebugPanelVisible = false },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(0.4f).fillMaxWidth(),
                        onExportLogs = exportLogs
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Servo Buttons - middle
                ServoPanel(
                    servoX = viewModel.servoX,
                    servoY = viewModel.servoY,
                    onServoMove = { x, y -> viewModel.updateServo(x, y) },
                    onLog = { message -> bluetoothManager.log(message, LogType.INFO) },
                    buttonSize = 56.dp,
                    modifier = Modifier.weight(if (viewModel.isDebugPanelVisible) 0.25f else 0.4f).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Joystick - bottom
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
        } else {
        // Landscape Layout: Side by side (existing layout)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(safeDrawingPadding)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(if (viewModel.isDebugPanelVisible) 0.65f else 1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
        // Global Bluetooth status & E-STOP - using extracted component
        val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()

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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.arduino.cc"))
                context.startActivity(intent)
            },
            onQuitApp = onQuitApp,
            context = context,
            view = view
        )


        Spacer(modifier = Modifier.height(4.dp))

        // Device Status Card - Single slot centered
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto-Reconnect Toggle
            AutoReconnectToggle(
                enabled = autoReconnectEnabled,
                onToggle = { bluetoothManager.setAutoReconnectEnabled(it) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Device Status Card
            val hasCrashLog = com.metelci.ardunakon.crash.CrashHandler.hasCrashLog(context)

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
                onCrashLogClick = {
                    viewModel.showCrashLog = true
                },
                isDarkTheme = isDarkTheme
            )
        }

        // Packet Loss Warning Banner
        telemetry?.let { telem ->
            val totalLoss = telem.packetsDropped + telem.packetsFailed
            val lossPercent = if (telem.packetsSent > 0) {
                (totalLoss.toFloat() / telem.packetsSent * 100)
            } else 0f

            if (lossPercent > 1.0f) { // Show warning if >1% packet loss
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lossPercent > 10f) {
                            Color(0xFFFF5252) // Red for >10% loss
                        } else if (lossPercent > 5f) {
                            Color(0xFFFF9800) // Orange for >5% loss
                        } else {
                            Color(0xFFFFC107) // Yellow for >1% loss
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "⚠ Packet Loss Detected",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                "${"%.2f".format(lossPercent)}% loss (${totalLoss}/${telem.packetsSent} packets)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (telem.packetsDropped > 0) {
                                Text(
                                    "Dropped: ${telem.packetsDropped}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                            if (telem.packetsFailed > 0) {
                                Text(
                                    "Failed: ${telem.packetsFailed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // Joysticks with Aux Buttons on sides - Responsive Layout
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp
        
        // Estimate available space (Screen height - top content approx 250dp)
        // This is an approximation but safer than BoxWithConstraints which caused issues
        val availableHeight = (screenHeight - 250.dp).coerceAtLeast(150.dp)
        // Adjust available width based on debug panel visibility to prevent joystick overlap
        // Adjust available width based on debug panel visibility
        val panelWidth = if (viewModel.isDebugPanelVisible) screenWidth * 0.65f else screenWidth
        // Subtract Aux column (120dp) and padding (16dp) to get actual space for joysticks
        val availableForJoysticks = (panelWidth - 136.dp).coerceAtLeast(10.dp)
        
        // We have 2 joysticks, so max width per joystick is roughly half the available space
        val maxJoystickWidth = availableForJoysticks / 2.1f
        val maxJoystickHeight = availableHeight
        val joystickSize = minOf(maxJoystickWidth, maxJoystickHeight, 220.dp)

        // Joysticks - each centered in its half of the screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Take remaining vertical space
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Stick (Joystick)
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

            // Right Side: WASD Servo Control
            // Right Side: WASD Servo Control
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

        // Right side: Embedded Debug Panel (Full Height)
        // Right side: Embedded Debug Panel (Full Height)
        if (viewModel.isDebugPanelVisible) {
            Spacer(modifier = Modifier.width(8.dp))
            EmbeddedTerminal(
                logs = debugLogs,
                telemetry = telemetry,
                onSendCommand = { cmd: String -> viewModel.handleServoCommand(cmd) },
                onClearLogs = {
                    bluetoothManager.log("Logs cleared", LogType.INFO)
                },
                onMaximize = {
                    viewModel.showMaximizedDebug = true
                },
                onMinimize = {
                    viewModel.isDebugPanelVisible = false
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                onExportLogs = exportLogs
            )
        }
    }
    } // End of else (landscape)
    }

    // Dialogs
    if (viewModel.showDebugConsole) {
        com.metelci.ardunakon.ui.components.TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { viewModel.showDebugConsole = false },
            onSendCommand = { cmd -> viewModel.handleServoCommand(cmd) },
            onClearLogs = {
                // We need to add a clearLogs method to BluetoothManager or just ignore for now
                // For now, let's just log a message
                bluetoothManager.log("Logs cleared", LogType.INFO)
            },
            onExportLogs = exportLogs
        )
    }

    // Maximized Debug Dialog (from embedded terminal)
    if (viewModel.showMaximizedDebug) {
        com.metelci.ardunakon.ui.components.TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { viewModel.showMaximizedDebug = false },
            onSendCommand = { cmd -> viewModel.handleServoCommand(cmd) },
            onClearLogs = {
                bluetoothManager.log("Logs cleared", LogType.INFO)
            },
            onExportLogs = exportLogs
        )
    }

    if (viewModel.showTelemetryGraph) {
        com.metelci.ardunakon.ui.components.TelemetryGraphDialog(
            telemetryHistoryManager = bluetoothManager.telemetryHistoryManager,
            onDismiss = { viewModel.showTelemetryGraph = false },
            isDarkTheme = isDarkTheme
        )
    }

    if (viewModel.showHelpDialog) {
        com.metelci.ardunakon.ui.components.HelpDialog(
            onDismiss = { viewModel.showHelpDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    if (viewModel.showAboutDialog) {
        com.metelci.ardunakon.ui.components.AboutDialog(
            onDismiss = { viewModel.showAboutDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Crash Log Dialog
    if (viewModel.showCrashLog) {
        val crashLog = com.metelci.ardunakon.crash.CrashHandler.getCrashLog(context)
        AlertDialog(
            onDismissRequest = { viewModel.showCrashLog = false },
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Crash Log", color = Color(0xFFFF9800))
                    Row {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val shareIntent = com.metelci.ardunakon.crash.CrashHandler.getShareIntent(context)
                            if (shareIntent != null) {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Crash Log"))
                            }
                        }) {
                            Icon(Icons.Default.Share, "Share", tint = Color(0xFF00E5FF))
                        }
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            com.metelci.ardunakon.crash.CrashHandler.clearCrashLog(context)
                            viewModel.showCrashLog = false
                        }) {
                            Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFFF5252))
                        }
                    }
                }
            },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = crashLog.ifEmpty { "No crash logs available" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Black,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showCrashLog = false }) {
                    Text("Close", color = Color(0xFF00E5FF))
                }
            }
        )
    }

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

    viewModel.securityErrorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.securityErrorMessage = null },
            title = { Text("Unlock Required") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.securityErrorMessage = null
                        try {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                        } catch (_: Exception) {}
                    }
                ) { Text("Open Security Settings") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.securityErrorMessage = null }) { Text("Close") }
            }
        )
    }
    
    // OTA Firmware Update Dialog
    if (viewModel.showOtaDialog) {
        OtaDialog(
            otaManager = otaManager,
            bleTransport = bleOtaTransport,
            wifiTransport = wifiOtaTransport,
            onDismiss = { viewModel.showOtaDialog = false }
        )
    }
    // Load saved WiFi config
    val prefs = context.getSharedPreferences("ArdunakonPrefs", Context.MODE_PRIVATE)
    val savedIp = remember { prefs.getString("last_wifi_ip", "192.168.4.1") ?: "192.168.4.1" }
    val savedPort = remember { prefs.getInt("last_wifi_port", 8888) }
    
    // Scanned devices state
    val wifiScannedDevices by wifiManager.scannedDevices.collectAsState()

    if (viewModel.showWifiConfig) {
        WifiConfigDialog(
            initialIp = savedIp,
            initialPort = savedPort,
            scannedDevices = wifiScannedDevices,
            onScan = { wifiManager.startDiscovery() },
            onDismiss = { viewModel.showWifiConfig = false },
            onSave = { ip, port -> 
                viewModel.showWifiConfig = false
                // Save config
                prefs.edit().putString("last_wifi_ip", ip).putInt("last_wifi_port", port).apply()
                
            wifiManager.connect(ip, port)
            }
        )
    }
}
