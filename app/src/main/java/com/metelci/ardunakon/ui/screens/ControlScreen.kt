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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager,
    wifiManager: WifiManager,
    isDarkTheme: Boolean = true,
    onQuitApp: () -> Unit = {}
) {
    // State
    var showDeviceList by rememberSaveable { mutableStateOf(false) }
    var showProfileSelector by rememberSaveable { mutableStateOf(false) }
    var showDebugConsole by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showTelemetryGraph by rememberSaveable { mutableStateOf(false) }
    var isDebugPanelVisible by rememberSaveable { mutableStateOf(true) } // Toggle for embedded debug panel
    var showMaximizedDebug by rememberSaveable { mutableStateOf(false) } // Full-screen debug dialog
    var showOtaDialog by rememberSaveable { mutableStateOf(false) } // OTA Firmware Update dialog
    var showWifiConfig by rememberSaveable { mutableStateOf(false) }
    var showCrashLog by rememberSaveable { mutableStateOf(false) } // Crash log viewer
    var connectionMode by rememberSaveable { mutableStateOf(ConnectionMode.BLUETOOTH) }
    var showHeaderMenu by remember { mutableStateOf(false) }

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


    val context = LocalContext.current
    var allowReflection by remember { mutableStateOf(false) }
    
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
    val exportLogs: () -> Unit = {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "ardunakon_logs_$timestamp.txt"
            val file = File(context.cacheDir, fileName)

            // Format logs with timestamps
            val logContent = buildString {
                appendLine("Ardunakon Debug Logs")
                appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("=".repeat(50))
                appendLine()

                if (debugLogs.isEmpty()) {
                    appendLine("No logs available")
                } else {
                    debugLogs.forEach { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))
                        val typeStr = log.type.name.padEnd(8)
                        appendLine("[$timeStr] $typeStr: ${log.message}")
                    }
                }

                appendLine()
                appendLine("=".repeat(50))
                val currentTelemetry = telemetry
                if (currentTelemetry != null) {
                    appendLine("Telemetry:")
                    appendLine("  Battery Voltage: ${currentTelemetry.batteryVoltage}V")
                    appendLine("  Status: ${currentTelemetry.status}")
                    appendLine("  Packets Sent: ${currentTelemetry.packetsSent}")
                    appendLine("  Packets Dropped: ${currentTelemetry.packetsDropped}")
                    appendLine("  Packets Failed: ${currentTelemetry.packetsFailed}")
                    val totalLoss = currentTelemetry.packetsDropped + currentTelemetry.packetsFailed
                    val lossPercent = if (currentTelemetry.packetsSent > 0) {
                        (totalLoss.toFloat() / currentTelemetry.packetsSent * 100)
                    } else 0f
                    appendLine("  Packet Loss: ${"%.2f".format(lossPercent)}%")
                }
            }

            file.writeText(logContent)

            // Create share intent
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ardunakon Debug Logs")
                putExtra(Intent.EXTRA_TEXT, "Debug logs from Ardunakon app")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
        } catch (e: Exception) {
            bluetoothManager.log("Export failed: ${e.message}", LogType.ERROR)
        }
    }

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

    // Profile State
    val profileManager = remember(context) { com.metelci.ardunakon.data.ProfileManager(context) }
    // Use mutableStateListOf to allow UI updates
    val profiles = remember { mutableStateListOf<com.metelci.ardunakon.data.Profile>() }
    var securityErrorMessage by remember { mutableStateOf<String?>(null) }
    val saveProfilesSafely: suspend () -> Unit = {
        try {
            profileManager.saveProfiles(profiles)
        } catch (e: AuthRequiredException) {
            securityErrorMessage = e.message ?: "Unlock your device to save encrypted profiles."
        }
    }

    // Load profiles asynchronously with comprehensive error handling
    LaunchedEffect(Unit) {
        try {
            profiles.addAll(profileManager.loadProfiles())
        } catch (e: AuthRequiredException) {
            securityErrorMessage = e.message ?: "Unlock your device to load encrypted profiles."
            profiles.addAll(profileManager.createDefaultProfiles())
        } catch (e: java.io.IOException) {
            // Handle file I/O errors (corrupted file, permission issues, etc.)
            bluetoothManager.log("Failed to load profiles: ${e.message}", com.metelci.ardunakon.model.LogType.ERROR)
            profiles.addAll(profileManager.createDefaultProfiles())
        } catch (e: Exception) {
            // Catch any unexpected errors to prevent crashes
            bluetoothManager.log("Unexpected error loading profiles: ${e.message}", LogType.ERROR)
            profiles.addAll(profileManager.createDefaultProfiles())
        }
    }
    LaunchedEffect(allowReflection) {
        bluetoothManager.allowReflectionFallback = allowReflection
    }
    
    var currentProfileIndex by remember { mutableStateOf(0) }
    
    // Derived directly from the state list to ensure updates trigger recomposition
    val currentProfile = if (profiles.isNotEmpty() && currentProfileIndex in profiles.indices) {
        profiles[currentProfileIndex]
    } else {
        // Fallback if list is empty or index invalid
        if (profiles.isEmpty()) {
            val defaults = profileManager.createDefaultProfiles()
            profiles.addAll(defaults)
            defaults[0]
        } else {
            profiles[0]
        }
    }

    // Joystick State (motor control)
    var leftJoystick by remember { mutableStateOf(Pair(0f, 0f)) }
    
    // Servo Control State (W, A, L, R buttons) - Persistent positions
    var servoX by remember { mutableStateOf(0f) } // Horizontal: A=-1, L=+1, CENTER=0
    var servoY by remember { mutableStateOf(0f) } // Vertical: W=+1, R=-1, CENTER=0
    
    // Transmission Loop - Combine joystick and servo inputs
    LaunchedEffect(currentProfile, leftJoystick, servoX, servoY, connectionMode) {

        while (isActive) {
            // Check E-STOP state - block all transmissions if active
            val isEStopActive = bluetoothManager.isEmergencyStopActive.value
            if (isEStopActive) {
                delay(50) // Still maintain loop timing
                continue  // Skip sending any packets
            }

            // Joystick controls motors (left stick)
            val leftX = leftJoystick.first // Joystick throttle X axis
            val leftY = leftJoystick.second // Joystick throttle Y axis

            // Servo positions are persistent, set by W/A/L/R buttons
            val rightX = servoX  // Set directly from button state
            val rightY = servoY  // Set directly from button state

            val packet = ProtocolManager.formatJoystickData(
                leftX = leftX,
                leftY = leftY,
                rightX = rightX,
                rightY = rightY,
                auxBits = 0 // Aux buttons are sent as separate commands
            )

            if (connectionMode == ConnectionMode.WIFI) {
                wifiManager.sendData(packet)
            } else {
                bluetoothManager.sendDataToAll(packet)
            }
            delay(50) // 20Hz
        }
    }

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

                // Top status bar (compact for portrait) - all header buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (connectionMode == ConnectionMode.BLUETOOTH) {
                        // Signal indicator with sparkline below
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val stateColor = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF00FF00)
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD54F)
                                ConnectionState.ERROR -> Color(0xFFFF5252)
                                else -> Color.Gray
                            }
                            SignalStrengthIcon(rssi = rssiValue, color = stateColor, modifier = Modifier)

                            // Latency Sparkline below RSSI (width matches RSSI)
                            LatencySparkline(
                                rttValues = rttHistory,
                                modifier = Modifier.width(40.dp).height(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
    
                        // Bluetooth Reconnect button
                        // Bluetooth Reconnect button
                        Box {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    showHeaderMenu = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                                    .border(1.dp, Color(0xFF00FF00), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bluetooth,
                                    contentDescription = "Connect / Reconnect",
                                    tint = Color(0xFF00FF00),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reconnect Device") },
                                    onClick = {
                                        showHeaderMenu = false
                                        val reconnected = bluetoothManager.reconnectSavedDevice()
                                        if (!reconnected) showDeviceList = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Switch to WiFi") },
                                    onClick = {
                                        showHeaderMenu = false
                                        connectionMode = ConnectionMode.WIFI
                                    }
                                )
                            }
                        }
                    } else {
                        // WiFi Mode - Dual Slot Layout (like Bluetooth)
                        // Slot 1: Active WiFi connection
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val state = wifiState
                            val stateColor = when (state) {
                                WifiConnectionState.CONNECTED -> Color(0xFF00FF00) // Green glow
                                WifiConnectionState.CONNECTING -> Color(0xFFFFD54F) 
                                WifiConnectionState.ERROR -> Color(0xFFFF5252)
                                else -> Color.Gray
                            }
                            SignalStrengthIcon(
                                rssi = wifiRssi, 
                                color = stateColor, 
                                modifier = Modifier,
                                isWifi = true,
                                showLabels = false
                            )
                            LatencySparkline(
                                rttValues = wifiRttHistory,
                                modifier = Modifier.width(40.dp).height(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // WiFi Config Button (Replaces BT Button)
                        Box {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    showHeaderMenu = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                                    .border(1.dp, Color(0xFF00C853), CircleShape) 
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "WiFi Configuration",
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Configure WiFi") },
                                    onClick = {
                                        showHeaderMenu = false
                                        showWifiConfig = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Switch to Bluetooth") },
                                    onClick = {
                                        showHeaderMenu = false
                                        connectionMode = ConnectionMode.BLUETOOTH
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // Telemetry Graph Button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showTelemetryGraph = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(2.dp, CircleShape)
                            .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                            .border(1.dp, Color(0xFF00FF00), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Telemetry Graphs",
                            tint = Color(0xFF00FF00),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // E-STOP Button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            if (isEStopActive) {
                                bluetoothManager.setEmergencyStop(false)
                                bluetoothManager.holdOfflineAfterEStopReset()
                            } else {
                                bluetoothManager.setEmergencyStop(true)
                                bluetoothManager.disconnectAllForEStop()
                                val stopPacket = ProtocolManager.formatEStopData()
                                bluetoothManager.sendDataToAll(stopPacket, force = true)
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(2.dp, CircleShape)
                            .background(
                                if (isEStopActive) Color(0xFFFF5252) else if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0),
                                CircleShape
                            )
                    ) {
                        Text(
                            if (isEStopActive) "GO" else "STOP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isEStopActive) Color.White else Color(0xFFFF5252)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // Debug Panel Toggle Button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isDebugPanelVisible = !isDebugPanelVisible
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(2.dp, CircleShape)
                            .background(
                                if (isDebugPanelVisible) Color(0xFF43A047) else Color(0xFF455A64),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFF00FF00), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Toggle Debug",
                            tint = if (isDebugPanelVisible) Color.White else Color(0xFF00FF00),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // Menu Button with dropdown
                    Box {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showOverflowMenu = !showOverflowMenu
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .shadow(2.dp, CircleShape)
                                .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                                .border(1.dp, Color(0xFF00FF00), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = "Menu",
                                tint = Color(0xFF00FF00),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Help") },
                                leadingIcon = { Icon(Icons.Default.Help, null) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    showHelpDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                leadingIcon = { Icon(Icons.Outlined.Info, null) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    showAboutDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            if (com.metelci.ardunakon.crash.CrashHandler.hasCrashLog(context)) {
                                DropdownMenuItem(
                                    text = { Text("View Crash Log", color = Color(0xFFFF9800)) },
                                    leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        showCrashLog = true
                                        showOverflowMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Legacy Reflection (HC-06): " + if (allowReflection) "On" else "Off") },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    allowReflection = !allowReflection
                                    showOverflowMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Switch to ${if (connectionMode == ConnectionMode.BLUETOOTH) "WiFi" else "Bluetooth"}") },
                                onClick = {
                                    showOverflowMenu = false
                                    connectionMode = if (connectionMode == ConnectionMode.BLUETOOTH) ConnectionMode.WIFI else ConnectionMode.BLUETOOTH
                                     view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (connectionMode == ConnectionMode.BLUETOOTH) Icons.Default.Wifi else Icons.Default.Bluetooth,
                                        contentDescription = "Switch Mode",
                                        tint = Color.Gray
                                    )
                                }
                            )
                            Divider(color = if (isDarkTheme) Color(0xFF455A64) else Color(0xFFB0BEC5), thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text("Quit App", color = Color(0xFFFF5252)) },
                                leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF5252)) },
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    showOverflowMenu = false
                                    onQuitApp()
                                }
                            )
                        }
                    }
                }

                // Debug Panel (if visible) - at top in portrait
                if (isDebugPanelVisible) {
                    EmbeddedTerminal(
                        logs = debugLogs,
                        telemetry = telemetry,
                        onSendCommand = { cmd: String ->
                            // Parse servo commands (W/A/L/R/B)
                            when (cmd.uppercase().trim()) {
                                "W" -> {
                                    servoY = if (servoY == 1f) 0f else 1f
                                    bluetoothManager.log("Servo: ${if (servoY == 1f) "FORWARD" else "CENTER"} (W)", LogType.INFO)
                                }
                                "B" -> {
                                    servoY = if (servoY == -1f) 0f else -1f
                                    bluetoothManager.log("Servo: ${if (servoY == -1f) "BACKWARD" else "CENTER"} (B)", LogType.INFO)
                                }
                                "L", "A" -> {
                                    servoX = if (servoX == -1f) 0f else -1f
                                    bluetoothManager.log("Servo: ${if (servoX == -1f) "LEFT" else "CENTER"} (L)", LogType.INFO)
                                }
                                "R" -> {
                                    servoX = if (servoX == 1f) 0f else 1f
                                    bluetoothManager.log("Servo: ${if (servoX == 1f) "RIGHT" else "CENTER"} (R)", LogType.INFO)
                                }
                                else -> {
                                    // Send raw command to Connection (WiFi or Bluetooth)
                                    val bytes = "$cmd\n".toByteArray()
                                    if (connectionMode == ConnectionMode.WIFI) {
                                         wifiManager.sendData(bytes)
                                         bluetoothManager.log("TX (WiFi): $cmd", LogType.INFO)
                                    } else {
                                         bluetoothManager.sendDataToAll(bytes, force = true)
                                         bluetoothManager.log("TX (BT): $cmd", LogType.INFO)
                                    }
                                }
                            }
                        },
                        onClearLogs = { bluetoothManager.log("Logs cleared", LogType.INFO) },
                        onMaximize = { showMaximizedDebug = true },
                        onMinimize = { isDebugPanelVisible = false },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(0.4f).fillMaxWidth(),
                        onExportLogs = exportLogs
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Servo Buttons - middle
                Box(
                    modifier = Modifier.weight(if (isDebugPanelVisible) 0.25f else 0.4f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ServoButtonControl(
                        servoX = servoX,
                        servoY = servoY,
                        onMove = { x, y -> servoX = x; servoY = y },
                        buttonSize = 56.dp,
                        onLog = { message -> bluetoothManager.log(message, LogType.INFO) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Joystick - bottom
                Box(
                    modifier = Modifier.weight(if (isDebugPanelVisible) 0.35f else 0.6f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val portraitJoystickSize = minOf(orientationConfig.screenWidthDp.dp * 0.5f, 180.dp)
                    JoystickControl(
                        onMoved = { state ->
                            leftJoystick = Pair(
                                state.x * currentProfile.sensitivity,
                                state.y * currentProfile.sensitivity
                            )
                        },
                        size = portraitJoystickSize,
                        isThrottle = false
                    )
                }
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
                    .weight(if (isDebugPanelVisible) 0.65f else 1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
        // Global Bluetooth status & E-STOP - Centered Layout
        val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (connectionMode == ConnectionMode.BLUETOOTH) {
                // Signal Strength Indicator with sparkline below
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val stateColor = when (connectionState) {
                        ConnectionState.CONNECTED -> Color(0xFF00FF00)
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD54F)
                        ConnectionState.ERROR -> Color(0xFFFF5252)
                        else -> Color.Gray
                    }

                    SignalStrengthIcon(
                        rssi = rssiValue,
                        color = stateColor,
                        modifier = Modifier
                    )

                    // Latency Sparkline below RSSI (width matches RSSI)
                    LatencySparkline(
                        rttValues = rttHistory,
                        modifier = Modifier.width(40.dp).height(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Bluetooth Reconnect button (LEFT of E-STOP)
                Box {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showHeaderMenu = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                            .border(1.dp, Color(0xFF00FF00), CircleShape) // Electric green border
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = "Connect / Reconnect",
                            tint = Color(0xFF00FF00), // Electric green
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showHeaderMenu,
                        onDismissRequest = { showHeaderMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reconnect Device") },
                            onClick = {
                                showHeaderMenu = false
                                val reconnected = bluetoothManager.reconnectSavedDevice()
                                if (!reconnected) showDeviceList = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Switch to WiFi") },
                            onClick = {
                                showHeaderMenu = false
                                connectionMode = ConnectionMode.WIFI
                            }
                        )
                    }
                }
            } else {
                // WiFi Landscape Header - Dual Slot Layout
                // Slot 1: Active WiFi connection
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val state = wifiState
                    val stateColor = when (state) {
                        WifiConnectionState.CONNECTED -> Color(0xFF00FF00) // Green glow
                        WifiConnectionState.CONNECTING -> Color(0xFFFFD54F)
                        WifiConnectionState.ERROR -> Color(0xFFFF5252)
                        else -> Color.Gray
                    }
                    SignalStrengthIcon(
                        rssi = wifiRssi,
                        color = stateColor,
                        modifier = Modifier,
                        isWifi = true,
                        showLabels = false
                    )
                    LatencySparkline(
                        rttValues = wifiRttHistory,
                        modifier = Modifier.width(40.dp).height(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                // WiFi Connect/Config button (LEFT of E-STOP)
                Box {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showHeaderMenu = true
                        },
                        modifier = Modifier
                            .size(36.dp) // Maintain exact size 36dp
                            .shadow(2.dp, CircleShape)
                            .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                            .border(1.dp, Color(0xFF00C853), CircleShape) // Electric green border for consistency
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "WiFi Configuration",
                            tint = Color(0xFF00C853), 
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showHeaderMenu,
                        onDismissRequest = { showHeaderMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Configure WiFi") },
                            onClick = {
                                showHeaderMenu = false
                                showWifiConfig = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Switch to Bluetooth") },
                            onClick = {
                                showHeaderMenu = false
                                connectionMode = ConnectionMode.BLUETOOTH
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Telemetry Graph Button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showTelemetryGraph = true
                },
                modifier = Modifier
                    .size(36.dp)
                    .shadow(2.dp, CircleShape)
                    .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                    .border(1.dp, Color(0xFF00FF00), CircleShape) // Electric green border
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Telemetry Graphs",
                    tint = Color(0xFF00FF00), // Electric green
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // E-STOP BUTTON (now in position 4)
            IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (isEStopActive) {
                            // Reset E-Stop
                            bluetoothManager.setEmergencyStop(false)
                            bluetoothManager.holdOfflineAfterEStopReset()
                        } else {
                            // Activate E-Stop FIRST to block other threads
                            bluetoothManager.setEmergencyStop(true)
                            // Immediately drop all links and clear slot visuals
                            bluetoothManager.disconnectAllForEStop()
                        // Then Force Send STOP packet
                        val stopPacket = ProtocolManager.formatEStopData()
                        bluetoothManager.sendDataToAll(stopPacket, force = true)
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .shadow(2.dp, CircleShape)
                    .background(
                        if (isEStopActive) {
                            Color(0xFFFF5252) // Active red
                        } else {
                            if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0)
                        },
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isEStopActive) {
                            Color(0xFFD32F2F) // Darker red border
                        } else {
                            if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF455A64)
                        },
                        CircleShape
                    )
            ) {
                Text(
                    if (isEStopActive) "RESET" else "STOP",
                    color = if (isEStopActive) Color.White else {
                        if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Debug Panel Toggle Button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    isDebugPanelVisible = !isDebugPanelVisible
                },
                modifier = Modifier
                    .size(36.dp)
                    .shadow(2.dp, CircleShape)
                    .background(
                        if (isDebugPanelVisible) Color(0xFF43A047) else Color(0xFF455A64),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        Color(0xFF00FF00), // Electric green border
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Toggle Debug",
                    tint = if (isDebugPanelVisible) Color.White else Color(0xFF00FF00), // Electric green
                    modifier = Modifier.size(18.dp)
                )
            }

            // Clear and consistent spacing between debug and menu icons
            Spacer(modifier = Modifier.width(12.dp))

            // Menu Button with Box wrapper for proper DropdownMenu positioning
            Box {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showOverflowMenu = !showOverflowMenu
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(2.dp, CircleShape)
                        .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                        .border(1.dp, Color(0xFF00FF00), CircleShape) // Electric green border
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Menu",
                        tint = Color(0xFF00FF00), // Electric green
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Help") },
                        leadingIcon = { Icon(Icons.Default.Help, null) },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showHelpDialog = true
                            showOverflowMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        leadingIcon = { Icon(Icons.Outlined.Info, null) },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showAboutDialog = true
                            showOverflowMenu = false
                        }
                    )
                    if (com.metelci.ardunakon.crash.CrashHandler.hasCrashLog(context)) {
                        DropdownMenuItem(
                            text = { Text("View Crash Log", color = Color(0xFFFF9800)) },
                            leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showCrashLog = true
                                showOverflowMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Legacy Reflection (HC-06): " + if (allowReflection) "On" else "Off") },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            allowReflection = !allowReflection
                            showOverflowMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open Arduino Cloud") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF00FF00)) },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showOverflowMenu = false
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.arduino.cc"))
                            context.startActivity(intent)
                        }
                    )

                    
                    DropdownMenuItem(
                        text = { Text("Switch to ${if (connectionMode == ConnectionMode.BLUETOOTH) "WiFi" else "Bluetooth"}") },
                        onClick = {
                            showOverflowMenu = false
                            connectionMode = if (connectionMode == ConnectionMode.BLUETOOTH) ConnectionMode.WIFI else ConnectionMode.BLUETOOTH
                             view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (connectionMode == ConnectionMode.BLUETOOTH) Icons.Default.Wifi else Icons.Default.Bluetooth,
                                contentDescription = "Switch Mode",
                                tint = Color.Gray
                            )
                        }
                    )
                    Divider(color = if (isDarkTheme) Color(0xFF455A64) else Color(0xFFB0BEC5), thickness = 1.dp)
                    DropdownMenuItem(
                        text = { Text("Quit App", color = Color(0xFFFF5252)) },
                        leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF5252)) },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showOverflowMenu = false
                            onQuitApp()
                        }
                    )
                }
            }
        }

        if (connectionMode == ConnectionMode.BLUETOOTH) {
            // Bluetooth device information row - Single slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                val slotHealth = health
                val lastPacketAgo = slotHealth?.lastPacketAt?.takeIf { it > 0 }?.let {
                    val delta = System.currentTimeMillis() - it
                    "${(delta / 1000).coerceAtLeast(0)}s"
                } ?: "n/a"
                val rtt = slotHealth?.lastRttMs?.takeIf { it > 0 }?.let { "${it}ms" } ?: "n/a"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                    )
                    Text(
                        text = "pkt $lastPacketAgo | rssi ${slotHealth?.rssiFailureCount ?: 0} | rtt $rtt",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF2D3436)
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            bluetoothManager.requestRssi()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD500F9)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD500F9)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Refresh", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

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

            val currentConnectionState = if (connectionMode == ConnectionMode.WIFI) {
                when (wifiState) {
                    WifiConnectionState.CONNECTED -> ConnectionState.CONNECTED
                    WifiConnectionState.CONNECTING -> ConnectionState.CONNECTING
                    WifiConnectionState.ERROR -> ConnectionState.ERROR
                    else -> ConnectionState.DISCONNECTED
                }
            } else {
                connectionState
            }

            val currentRssi = if (connectionMode == ConnectionMode.WIFI) wifiRssi else rssiValue

            StatusCard(
                label = "Device",
                state = currentConnectionState,
                rssi = currentRssi,
                hasCrashLog = hasCrashLog,
                onClick = {
                    if (connectionMode == ConnectionMode.WIFI) {
                        showWifiConfig = true
                    } else {
                        showDeviceList = true
                    }
                },
                onCrashLogClick = {
                    showCrashLog = true
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
        val panelWidth = if (isDebugPanelVisible) screenWidth * 0.65f else screenWidth
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
            // Left Stick (Throttle)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Joystick - Throttle only (vertical), no X axis
                JoystickControl(
                    onMoved = { state ->
                        leftJoystick = Pair(
                            state.x * currentProfile.sensitivity, // X axis (Steering)
                            state.y * currentProfile.sensitivity  // Y axis (Throttle)
                        )
                    },
                    size = joystickSize,
                    isThrottle = false // Enable 2-axis control
                )
            }

            // Right Side: WASD Servo Control with keyboard integration
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // WASD Button Control with persistent servo state
                ServoButtonControl(
                    servoX = servoX,
                    servoY = servoY,
                    onMove = { x, y ->
                        // Directly update persistent servo positions
                        servoX = x
                        servoY = y
                    },
                    buttonSize = 60.dp,
                    onLog = { message ->
                        bluetoothManager.log(message, LogType.INFO)
                    }
                )
            }
        }
        }

        // Right side: Embedded Debug Panel (Full Height)
        if (isDebugPanelVisible) {
            Spacer(modifier = Modifier.width(8.dp))
            EmbeddedTerminal(
                logs = debugLogs,
                telemetry = telemetry,
                onSendCommand = { cmd: String ->
                    // Parse servo commands (W/A/L/R/B)
                    when (cmd.uppercase().trim()) {
                        "W" -> {
                            servoY = if (servoY == 1f) 0f else 1f
                            bluetoothManager.log("Servo: ${if (servoY == 1f) "FORWARD" else "CENTER"} (W)", LogType.INFO)
                        }
                        "B" -> {
                            servoY = if (servoY == -1f) 0f else -1f
                            bluetoothManager.log("Servo: ${if (servoY == -1f) "BACKWARD" else "CENTER"} (B)", LogType.INFO)
                        }
                        "L", "A" -> {
                            servoX = if (servoX == -1f) 0f else -1f
                            bluetoothManager.log("Servo: ${if (servoX == -1f) "LEFT" else "CENTER"} (L)", LogType.INFO)
                        }
                        "R" -> {
                            servoX = if (servoX == 1f) 0f else 1f
                            bluetoothManager.log("Servo: ${if (servoX == 1f) "RIGHT" else "CENTER"} (R)", LogType.INFO)
                        }
                        else -> {
                            val bytes = "$cmd\n".toByteArray()
                            bluetoothManager.sendDataToAll(bytes, force = true)
                            bluetoothManager.log("TX: $cmd", LogType.INFO)
                        }
                    }
                },
                onClearLogs = {
                    bluetoothManager.log("Logs cleared", LogType.INFO)
                },
                onMaximize = {
                    showMaximizedDebug = true
                },
                onMinimize = {
                    isDebugPanelVisible = false
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
    if (showDebugConsole) {
        com.metelci.ardunakon.ui.components.TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { showDebugConsole = false },
            onSendCommand = { cmd ->
                // Parse servo commands (W/A/L/R/B)
                when (cmd.uppercase().trim()) {
                    "W" -> {
                        servoY = if (servoY == 1f) 0f else 1f
                        bluetoothManager.log("Servo: ${if (servoY == 1f) "FORWARD" else "CENTER"} (W)", LogType.INFO)
                    }
                    "B" -> {
                        servoY = if (servoY == -1f) 0f else -1f
                        bluetoothManager.log("Servo: ${if (servoY == -1f) "BACKWARD" else "CENTER"} (B)", LogType.INFO)
                    }
                    "L", "A" -> {
                        servoX = if (servoX == -1f) 0f else -1f
                        bluetoothManager.log("Servo: ${if (servoX == -1f) "LEFT" else "CENTER"} (L)", LogType.INFO)
                    }
                    "R" -> {
                        servoX = if (servoX == 1f) 0f else 1f
                        bluetoothManager.log("Servo: ${if (servoX == 1f) "RIGHT" else "CENTER"} (R)", LogType.INFO)
                    }
                    else -> {
                        val bytes = "$cmd\n".toByteArray()
                        bluetoothManager.sendDataToAll(bytes, force = true)
                        bluetoothManager.log("TX: $cmd", LogType.INFO)
                    }
                }
            },
            onClearLogs = {
                // We need to add a clearLogs method to BluetoothManager or just ignore for now
                // For now, let's just log a message
                bluetoothManager.log("Logs cleared", LogType.INFO)
            },
            onExportLogs = exportLogs
        )
    }

    // Maximized Debug Dialog (from embedded terminal)
    if (showMaximizedDebug) {
        com.metelci.ardunakon.ui.components.TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { showMaximizedDebug = false },
            onSendCommand = { cmd ->
                // Parse servo commands (W/A/L/R/B)
                when (cmd.uppercase().trim()) {
                    "W" -> {
                        servoY = if (servoY == 1f) 0f else 1f
                        bluetoothManager.log("Servo: ${if (servoY == 1f) "FORWARD" else "CENTER"} (W)", LogType.INFO)
                    }
                    "B" -> {
                        servoY = if (servoY == -1f) 0f else -1f
                        bluetoothManager.log("Servo: ${if (servoY == -1f) "BACKWARD" else "CENTER"} (B)", LogType.INFO)
                    }
                    "L", "A" -> {
                        servoX = if (servoX == -1f) 0f else -1f
                        bluetoothManager.log("Servo: ${if (servoX == -1f) "LEFT" else "CENTER"} (L)", LogType.INFO)
                    }
                    "R" -> {
                        servoX = if (servoX == 1f) 0f else 1f
                        bluetoothManager.log("Servo: ${if (servoX == 1f) "RIGHT" else "CENTER"} (R)", LogType.INFO)
                    }
                    else -> {
                        val bytes = "$cmd\n".toByteArray()
                        bluetoothManager.sendDataToAll(bytes, force = true)
                        bluetoothManager.log("TX: $cmd", LogType.INFO)
                    }
                }
            },
            onClearLogs = {
                bluetoothManager.log("Logs cleared", LogType.INFO)
            },
            onExportLogs = exportLogs
        )
    }

    if (showTelemetryGraph) {
        com.metelci.ardunakon.ui.components.TelemetryGraphDialog(
            telemetryHistoryManager = bluetoothManager.telemetryHistoryManager,
            onDismiss = { showTelemetryGraph = false },
            isDarkTheme = isDarkTheme
        )
    }

    if (showHelpDialog) {
        com.metelci.ardunakon.ui.components.HelpDialog(
            onDismiss = { showHelpDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    if (showAboutDialog) {
        com.metelci.ardunakon.ui.components.AboutDialog(
            onDismiss = { showAboutDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Crash Log Dialog
    if (showCrashLog) {
        val crashLog = com.metelci.ardunakon.crash.CrashHandler.getCrashLog(context)
        AlertDialog(
            onDismissRequest = { showCrashLog = false },
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
                            showCrashLog = false
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
                TextButton(onClick = { showCrashLog = false }) {
                    Text("Close", color = Color(0xFF00E5FF))
                }
            }
        )
    }

    if (showDeviceList) {
        var isScanning by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDeviceList = false },
            modifier = Modifier.fillMaxWidth(0.95f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bluetooth Devices", style = MaterialTheme.typography.titleMedium)
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF74B9FF)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Compact target slot info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Device",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF546E7A)
                        )
                        // Compact scan button
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                isScanning = true
                                bluetoothManager.startScan()
                                // Auto-stop scanning indication after 5 seconds
                                coroutineScope.launch {
                                    delay(5000)
                                    isScanning = false
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00FF00)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00)),
                            enabled = !isScanning,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = "Scan",
                                tint = Color(0xFF00FF00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isScanning) "Scanning..." else "Scan",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Divider(color = Color(0xFFB0BEC5), thickness = 1.dp)

                    // Compact device list header
                    if (scannedDevices.isNotEmpty()) {
                        Text(
                            "Found ${scannedDevices.size} device${if (scannedDevices.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2D3436)
                        )
                    }

                    // Scrollable device list - EXPANDED
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (scannedDevices.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFFB0BEC5)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No devices found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF757575)
                                    )
                                    Text(
                                        "Tap 'Scan for Devices' to search",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        } else {
                            items(scannedDevices, key = { it.address }) { device ->
                                // Strip MAC address in parentheses from display name (e.g., "HC-06 (AA:BB:CC:DD:EE:FF)")
                                val displayName = device.name.replace(Regex("\\s*\\([0-9A-Fa-f:]{11,}\\)$"), "")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 10.dp)
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            bluetoothManager.connectToDevice(device)
                                            showDeviceList = false
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bluetooth,
                                                contentDescription = null,
                                                tint = Color(0xFF2196F3),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = displayName.ifBlank { device.name },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF2D3436)
                                                )
                                                Text(
                                                    text = device.address,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF757575)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Connect",
                                            tint = Color(0xFF74B9FF),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .rotate(180f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeviceList = false },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 12.dp, minWidth = 0.dp)
                        .height(12.dp)
                        .padding(vertical = 0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        "Close",
                        color = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }
    
    // Profile Editor State
    var showProfileEditor by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<com.metelci.ardunakon.data.Profile?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var profileIndexToDelete by remember { mutableStateOf(-1) }

    if (showProfileSelector) {
        AlertDialog(
            onDismissRequest = { showProfileSelector = false },
            title = { Text("Select Profile") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profiles.forEachIndexed { index, profile ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == currentProfileIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        currentProfileIndex = index
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, 
                                        if (isDarkTheme) Color(0xFF546E7A) else Color(0xFFB0BEC5)
                                    )
                                ) {
                                    Text(profile.name, style = MaterialTheme.typography.bodyLarge)
                                }
                                
                                IconButton(onClick = {
                                    profileToEdit = profile
                                    showProfileEditor = true
                                }) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                                
                                IconButton(onClick = {
                                    if (profiles.size > 1) {
                                        profileIndexToDelete = index
                                        showDeleteConfirmation = true
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF7675))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            profileToEdit = null
                            showProfileEditor = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                        )
                    ) {
                        Text("Create New Profile")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProfileSelector = false }
                ) { Text("Close") }
            }
        )
    }

    if (showProfileEditor) {
        com.metelci.ardunakon.ui.components.ProfileEditorDialog(
            profile = profileToEdit,
            onDismiss = { showProfileEditor = false },
            onSave = { newProfile ->
                val index = profiles.indexOfFirst { it.id == newProfile.id }
                if (index != -1) {
                    profiles[index] = newProfile
                } else {
                    profiles.add(newProfile)
                }
                coroutineScope.launch {
                    saveProfilesSafely()
                }
                showProfileEditor = false
            }
        )
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Profile") },
            text = {
                Text("Are you sure you want to delete \"${profiles.getOrNull(profileIndexToDelete)?.name ?: "this profile"}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profileIndexToDelete in profiles.indices) {
                            val wasSelected = profileIndexToDelete == currentProfileIndex
                            val isBeforeSelected = profileIndexToDelete < currentProfileIndex

                            profiles.removeAt(profileIndexToDelete)
                            coroutineScope.launch { saveProfilesSafely() }

                            if (isBeforeSelected) {
                                currentProfileIndex--
                            } else if (wasSelected) {
                                currentProfileIndex = 0
                            }
                            if (currentProfileIndex >= profiles.size) currentProfileIndex = 0
                        }
                        showDeleteConfirmation = false
                        profileIndexToDelete = -1
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF7675))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    profileIndexToDelete = -1
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    securityErrorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { securityErrorMessage = null },
            title = { Text("Unlock Required") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        securityErrorMessage = null
                        try {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                        } catch (_: Exception) {}
                    }
                ) { Text("Open Security Settings") }
            },
            dismissButton = {
                TextButton(onClick = { securityErrorMessage = null }) { Text("Close") }
            }
        )
    }
    
    // OTA Firmware Update Dialog
    if (showOtaDialog) {
        OtaDialog(
            otaManager = otaManager,
            bleTransport = bleOtaTransport,
            wifiTransport = wifiOtaTransport,
            onDismiss = { showOtaDialog = false }
        )
    }
    // Load saved WiFi config
    val prefs = context.getSharedPreferences("ArdunakonPrefs", Context.MODE_PRIVATE)
    val savedIp = remember { prefs.getString("last_wifi_ip", "192.168.4.1") ?: "192.168.4.1" }
    val savedPort = remember { prefs.getInt("last_wifi_port", 8888) }
    
    // Scanned devices state
    val wifiScannedDevices by wifiManager.scannedDevices.collectAsState()

    if (showWifiConfig) {
        WifiConfigDialog(
            initialIp = savedIp,
            initialPort = savedPort,
            scannedDevices = wifiScannedDevices,
            onScan = { wifiManager.startDiscovery() },
            onDismiss = { showWifiConfig = false },
            onSave = { ip, port -> 
                showWifiConfig = false
                // Save config
                prefs.edit().putString("last_wifi_ip", ip).putInt("last_wifi_port", port).apply()
                
                wifiManager.connect(ip, port)
            }
        )
    }
}

enum class ConnectionMode {
    BLUETOOTH, WIFI
}












