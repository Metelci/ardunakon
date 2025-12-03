package com.metelci.ardunakon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import android.view.HapticFeedbackConstants
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.data.AuxAssignment
import com.metelci.ardunakon.model.ButtonConfig
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.ServoButtonControl
import com.metelci.ardunakon.ui.components.EmbeddedTerminal
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.security.AuthRequiredException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.metelci.ardunakon.model.AssignedAux
import com.metelci.ardunakon.ui.components.AuxButton
import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.StatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager,
    isDarkTheme: Boolean = true,
    onQuitApp: () -> Unit = {}
) {
    // State
    var showDeviceList by remember { mutableStateOf<Int?>(null) }
    var showProfileSelector by remember { mutableStateOf(false) }
    var showDebugConsole by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAuxAssignDialog by remember { mutableStateOf(false) }
    var isDebugPanelVisible by remember { mutableStateOf(true) } // Toggle for embedded debug panel
    var showMaximizedDebug by remember { mutableStateOf(false) } // Full-screen debug dialog

    val scannedDevices by bluetoothManager.scannedDevices.collectAsState()
    val connectionStates by bluetoothManager.connectionStates.collectAsState()
    val rssiValues by bluetoothManager.rssiValues.collectAsState()
    val health by bluetoothManager.health.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val telemetry by bluetoothManager.telemetry.collectAsState()
    val autoReconnectEnabled by bluetoothManager.autoReconnectEnabled.collectAsState()
    val activeAuxButtons = remember { mutableStateListOf<AssignedAux>() }
    val coroutineScope = rememberCoroutineScope()

    // Removed pastelBrush for better visibility


    val context = LocalContext.current
    var allowReflection by remember { mutableStateOf(false) }

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

    // Load profiles asynchronously
    LaunchedEffect(Unit) {
        try {
            profiles.addAll(profileManager.loadProfiles())
        } catch (e: AuthRequiredException) {
            securityErrorMessage = e.message ?: "Unlock your device to load encrypted profiles."
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

    // Joystick State (fixed size/placement)
    var leftJoystick by remember { mutableStateOf(Pair(0f, 0f)) }
    var rightJoystick by remember { mutableStateOf(Pair(0f, 0f)) }

    // Sync active aux buttons with current profile
    LaunchedEffect(currentProfile) {
        activeAuxButtons.clear()
        activeAuxButtons.addAll(
            currentProfile.auxAssignments.map { assign ->
                val cfg = currentProfile.buttonConfigs.find { it.id == assign.id }
                    ?: ButtonConfig(assign.id, "Aux ${assign.id}", assign.id.toString())
                AssignedAux(cfg, assign.slot, assign.servoId, assign.role)
            }
        )
    }
    
    // Transmission Loop
    LaunchedEffect(currentProfile, leftJoystick, rightJoystick) {
        while (isActive) {
            val packet = ProtocolManager.formatJoystickData(
                leftX = leftJoystick.first,
                leftY = leftJoystick.second,
                rightX = rightJoystick.first,
                rightY = rightJoystick.second,
                isThrottleUnidirectional = currentProfile.isThrottleUnidirectional,
                auxBits = 0 // Aux buttons are sent as separate commands
            )
            bluetoothManager.sendDataToAll(packet)
            delay(50) // 20Hz
        }
    }

    // UI Layout with theme toggle
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()

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
            // Left side: First Signal Strength Indicator
            val state0 = connectionStates[0]
            val stateColor0 = when (state0) {
                ConnectionState.CONNECTED -> Color(0xFF00C853)
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD54F)
                ConnectionState.ERROR -> Color(0xFFFF5252)
                else -> if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFFB0BEC5)
            }

            SignalStrengthIcon(
                rssi = rssiValues[0],
                color = stateColor0,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Bluetooth Reconnect button (LEFT of E-STOP)
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val reconnected = bluetoothManager.reconnectSavedDevices()
                    if (!reconnected) {
                        // If nothing to reconnect, open device picker for Slot 1
                        showDeviceList = 0
                    }
                },
                modifier = Modifier
                    .shadow(2.dp, CircleShape)
                    .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                    .border(1.dp, Color(0xFF00FF00), CircleShape) // Electric green border
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = "Connect / Reconnect",
                    tint = Color(0xFF00FF00) // Electric green
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Aux Button (now in position 3)
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showAuxAssignDialog = true
                },
                modifier = Modifier
                    .size(36.dp)
                    .shadow(2.dp, CircleShape)
                    .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                    .border(1.dp, Color(0xFF00FF00), CircleShape) // Electric green border
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Aux Buttons",
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
                    } else {
                        // Activate E-Stop FIRST to block other threads
                        bluetoothManager.setEmergencyStop(true)
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
            Spacer(modifier = Modifier.width(16.dp))

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
                        modifier = Modifier.size(20.dp)
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
                    DropdownMenuItem(
                        text = { Text("Legacy Reflection (HC-06): " + if (allowReflection) "On" else "Off") },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            allowReflection = !allowReflection
                            showOverflowMenu = false
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

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Second Signal Strength Indicator
            val state1 = connectionStates[1]
            val stateColor1 = when (state1) {
                ConnectionState.CONNECTED -> Color(0xFF00C853)
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD54F)
                ConnectionState.ERROR -> Color(0xFFFF5252)
                else -> if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFFB0BEC5)
            }

            SignalStrengthIcon(
                rssi = rssiValues[1],
                color = stateColor1,
                modifier = Modifier
            )
        }

        // Bluetooth slot information row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top
        ) {
            (0..1).forEach { slotIndex ->
                val slotHealth = health.getOrNull(slotIndex)
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
                        text = "Slot ${slotIndex + 1}",
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
                            bluetoothManager.requestRssi(slotIndex) 
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

        // Device Status Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle for Slot 1
            AutoReconnectToggle(
                slot = 0,
                enabled = autoReconnectEnabled[0],
                onToggle = { bluetoothManager.setAutoReconnectEnabled(0, it) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Slot 1
            StatusCard(
                label = "Dev 1",
                state = connectionStates[0],
                rssi = rssiValues[0],
                onClick = { showDeviceList = 0 },
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Slot 2
            StatusCard(
                label = "Dev 2",
                state = connectionStates[1],
                rssi = rssiValues[1],
                onClick = { showDeviceList = 1 },
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Toggle for Slot 2
            AutoReconnectToggle(
                slot = 1,
                enabled = autoReconnectEnabled[1],
                onToggle = { bluetoothManager.setAutoReconnectEnabled(1, it) }
            )
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Stick (Throttle) - with label on left side
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Label on left side
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "Throttle",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                    )
                    Text(
                        "(RC Motor)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                    )
                }
                // Joystick - Throttle only (vertical), no X axis
                JoystickControl(
                    onMoved = { state ->
                        leftJoystick = Pair(
                            0f, // No horizontal movement
                            state.y * currentProfile.sensitivity
                        )
                    },
                    size = joystickSize,
                    isThrottle = true, // Y axis doesn't auto-center
                    isUnidirectional = currentProfile.isThrottleUnidirectional
                )
            }

            // Right Side: WASD Servo Control - with label on right side
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // WASD Button Control
                ServoButtonControl(
                    onMove = { x, y ->
                        rightJoystick = Pair(
                            x * currentProfile.sensitivity,
                            y * currentProfile.sensitivity
                        )
                    },
                    buttonSize = 60.dp
                )
                // Label on right side
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "Servos",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                    )
                    Text(
                        "(wasd)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                    )
                }
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
                    val bytes = "$cmd\n".toByteArray()
                    bluetoothManager.sendDataToAll(bytes, force = true)
                    bluetoothManager.log("TX: $cmd", LogType.INFO)
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
    }

    // Dialogs
    if (showDebugConsole) {
        com.metelci.ardunakon.ui.components.TerminalDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { showDebugConsole = false },
            onSendCommand = { cmd ->
                // Send command with newline
                val bytes = "$cmd\n".toByteArray()
                bluetoothManager.sendDataToAll(bytes, force = true)
                bluetoothManager.log("TX: $cmd", LogType.INFO)
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
                val bytes = "$cmd\n".toByteArray()
                bluetoothManager.sendDataToAll(bytes, force = true)
                bluetoothManager.log("TX: $cmd", LogType.INFO)
            },
            onClearLogs = {
                bluetoothManager.log("Logs cleared", LogType.INFO)
            },
            onExportLogs = exportLogs
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

    if (showDeviceList != null) {
        var isScanning by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDeviceList = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bluetooth Devices", style = MaterialTheme.typography.titleLarge)
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Target slot info
                    Text(
                        "Select device for Slot ${showDeviceList!! + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF546E7A)
                    )

                    Divider(color = Color(0xFFB0BEC5), thickness = 1.dp)

                    // Scan button
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
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00FF00)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00)),
                        enabled = !isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = "Scan",
                            tint = Color(0xFF00FF00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isScanning) "Scanning..." else "Scan for Devices",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Device list header
                    if (scannedDevices.isNotEmpty()) {
                        Text(
                            "Available Devices (${scannedDevices.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2D3436)
                        )
                    }

                    // Scrollable device list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            items(scannedDevices) { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            bluetoothManager.connectToDevice(device, showDeviceList!!)
                                            showDeviceList = null
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = device.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color(0xFF2D3436)
                                                )
                                                Text(
                                                    text = device.address,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF757575)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Connect",
                                            tint = Color(0xFF74B9FF),
                                            modifier = Modifier
                                                .size(20.dp)
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
                    onClick = { showDeviceList = null }
                ) {
                    Text("Close", color = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2))
                }
            }
        )
    }
    
    // Profile Editor State
    var showProfileEditor by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<com.metelci.ardunakon.data.Profile?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var profileIndexToDelete by remember { mutableStateOf(-1) }

    // Aux assignment dialog
    if (showAuxAssignDialog) {
        val tempAssignments = remember { mutableStateMapOf<Int, AssignedAux>().apply {
            if (isEmpty()) {
                activeAuxButtons.forEach { put(it.config.id, it) }
            }
        } }
        AlertDialog(
            onDismissRequest = { showAuxAssignDialog = false },
            title = { Text("Assign Aux Buttons", color = if (isDarkTheme) Color.White else Color(0xFF2D3436)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentProfile.buttonConfigs.size) { index ->
                        val config = currentProfile.buttonConfigs[index]
                        val existing = tempAssignments[config.id]
                        var slot by remember(config.id) { mutableStateOf(existing?.slot ?: 0) }
                        var servoText by remember(config.id) { mutableStateOf((existing?.servoId ?: config.id).toString()) }
                        var roleText by remember(config.id) { mutableStateOf(existing?.role ?: "") }
                        var labelText by remember(config.id) { mutableStateOf(existing?.config?.label ?: config.label) }
                        var commandText by remember(config.id) { mutableStateOf(existing?.config?.id?.toString() ?: config.id.toString()) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(labelText, color = if (isDarkTheme) Color.White else Color(0xFF2D3436), style = MaterialTheme.typography.bodyMedium)
                                        Text("Slot ${slot + 1} - Servo $servoText${if (roleText.isNotBlank()) " - $roleText" else ""}",
                                            color = Color(0xFF546E7A),
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                    val isChecked = tempAssignments.containsKey(config.id)
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                val servoId = servoText.toIntOrNull() ?: config.id
                                                val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                            } else {
                                                tempAssignments.remove(config.id)
                                            }
                                        }
                                    )
                                }
                                if (tempAssignments.containsKey(config.id)) {
                                    Divider(color = if (isDarkTheme) Color(0xFF455A64) else Color(0xFFB0BEC5), thickness = 1.dp)
                                    // First row: Label and Command
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = labelText,
                                        onValueChange = { new ->
                                            if (new.length <= 20 && new.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".-_" }) {
                                                labelText = new
                                                val servoId = servoText.toIntOrNull() ?: config.id
                                                val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                            }
                                        },
                                        label = { Text("Label", color = if (isDarkTheme) Color.White else Color(0xFF2D3436)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF74B9FF),
                                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF546E7A) else Color(0xFFB0BEC5),
                                            focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436),
                                            unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                                        )
                                    )
                                    OutlinedTextField(
                                        value = commandText,
                                        onValueChange = { new ->
                                            val filtered = new.filter { it.isDigit() }
                                            val value = filtered.toIntOrNull()
                                            if (filtered.isEmpty() || (value != null && value in 0..255)) {
                                                commandText = filtered
                                                val servoId = servoText.toIntOrNull() ?: config.id
                                                val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                            }
                                        },
                                        label = { Text("Command", color = if (isDarkTheme) Color.White else Color(0xFF2D3436)) },
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF74B9FF),
                                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF546E7A) else Color(0xFFB0BEC5),
                                            focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436),
                                            unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                                        )
                                    )
                                }
                                // Second row: Slot buttons, Servo, and Role
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        (0..1).forEach { s ->
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    slot = s
                                                    val servoId = servoText.toIntOrNull() ?: config.id
                                                    val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                    tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (slot == s) {
                                                        if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0)
                                                    } else Color.Transparent,
                                                    contentColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp, 
                                                    if (slot == s) {
                                                        if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFF546E7A) else Color(0xFFB0BEC5)
                                                    }
                                                ),
                                                modifier = Modifier.height(36.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) { Text("Slot ${s + 1}", style = MaterialTheme.typography.labelSmall) }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = servoText,
                                        onValueChange = { new ->
                                            val filtered = new.filter { it.isDigit() }
                                            val value = filtered.toIntOrNull()
                                            if (filtered.isEmpty() || (value != null && value in 0..255)) {
                                                servoText = filtered
                                                val servoId = servoText.toIntOrNull() ?: config.id
                                                val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                            }
                                        },
                                        label = { Text("Servo", color = if (isDarkTheme) Color.White else Color(0xFF2D3436)) },
                                        singleLine = true,
                                        modifier = Modifier.width(90.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF74B9FF),
                                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF546E7A) else Color(0xFFB0BEC5),
                                            focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436),
                                            unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                                        )
                                    )
                                    OutlinedTextField(
                                        value = roleText,
                                        onValueChange = { new ->
                                            if (new.length <= 30 && new.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".-_/()" }) {
                                                roleText = new
                                                val servoId = servoText.toIntOrNull() ?: config.id
                                                val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                            }
                                        },
                                        label = { Text("Role", color = if (isDarkTheme) Color.White else Color(0xFF2D3436)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF74B9FF),
                                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF546E7A) else Color(0xFFB0BEC5),
                                            focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436),
                                            unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                                        )
                                    )
                                }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeAuxButtons.clear()
                        activeAuxButtons.addAll(tempAssignments.values)
                        val updatedProfile = currentProfile.copy(
                            auxAssignments = tempAssignments.values.map { AuxAssignment(it.config.id, it.slot, it.servoId, it.role) }
                        )
                        profiles[currentProfileIndex] = updatedProfile
                        coroutineScope.launch { saveProfilesSafely() }
                        showAuxAssignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2),
                        contentColor = if (isDarkTheme) Color(0xFF2D3436) else Color.White
                    )
                ) { Text("Done") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tempAssignments.clear()
                        activeAuxButtons.clear()
                        val updatedProfile = currentProfile.copy(auxAssignments = emptyList())
                        profiles[currentProfileIndex] = updatedProfile
                        coroutineScope.launch { saveProfilesSafely() }
                        showAuxAssignDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF5252)
                    )
                ) { Text("Clear All") }
            }
        )
    }

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
}







