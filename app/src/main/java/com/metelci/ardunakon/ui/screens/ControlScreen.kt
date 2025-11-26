package com.metelci.ardunakon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.data.AuxAssignment
import com.metelci.ardunakon.model.ButtonConfig
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.ui.components.JoystickControl
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AssignedAux(val config: ButtonConfig, val slot: Int, val servoId: Int, val role: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager,
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {}
) {
    // State
    var showDeviceList by remember { mutableStateOf<Int?>(null) }
    var showProfileSelector by remember { mutableStateOf(false) }
    var showDebugConsole by remember { mutableStateOf(false) }

    val scannedDevices by bluetoothManager.scannedDevices.collectAsState()
    val connectionStates by bluetoothManager.connectionStates.collectAsState()
    val rssiValues by bluetoothManager.rssiValues.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val telemetry by bluetoothManager.telemetry.collectAsState()
    val activeAuxButtons = remember { mutableStateListOf<AssignedAux>() }
    var showAuxAssignDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val pastelBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFCE4EC),
                Color(0xFFE3F2FD),
                Color(0xFFE8F5E9)
            )
        )
    }

    // Profile State
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileManager = remember(context) { com.metelci.ardunakon.data.ProfileManager(context) }
    // Use mutableStateListOf to allow UI updates
    val profiles = remember { mutableStateListOf<com.metelci.ardunakon.data.Profile>().apply { addAll(profileManager.loadProfiles()) } }
    
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
    LaunchedEffect(currentProfile.id) {
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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
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
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
        // Global Bluetooth status & manual reconnect
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val anyConnected = connectionStates.any { it == ConnectionState.CONNECTED }
            val statusColor = if (anyConnected) {
                Color(0xFF00C853)
            } else {
                if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFFB0BEC5)
            }
            val statusText = if (anyConnected) "Bluetooth Connected" else "Bluetooth Disconnected"

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = statusText,
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (anyConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                )
            }

            IconButton(
                onClick = {
                    val reconnected = bluetoothManager.reconnectSavedDevices()
                    if (!reconnected) {
                        // If nothing to reconnect, open device picker for Slot 1
                        showDeviceList = 0
                    }
                },
                modifier = Modifier
                    .shadow(2.dp, CircleShape)
                    .background(pastelBrush, CircleShape)
                    .border(1.dp, Color(0xFFB0BEC5), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = "Connect / Reconnect",
                    tint = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436)
                )
            }
        }

        // Top Bar: Status & E-Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot 1
            StatusCard(
                label = "Dev 1",
                state = connectionStates[0],
                rssi = rssiValues[0],
                onClick = { showDeviceList = 0 }
            )

            // E-STOP BUTTON
            val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()
            
            Button(
                onClick = {
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .shadow(6.dp, CircleShape)
                    .background(
                        brush = if (isEStopActive) {
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF66BB6A), Color(0xFF43A047))
                            )
                        },
                        shape = CircleShape
                    )
                    .border(1.dp, if (isEStopActive) Color(0xFFB71C1C) else Color(0xFF1B5E20), CircleShape),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text(
                    if (isEStopActive) "RESET" else "STOP", 
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            // Slot 2
            StatusCard(
                label = "Dev 2",
                state = connectionStates[1],
                rssi = rssiValues[1],
                onClick = { showDeviceList = 1 }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Profile & Debug
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { showProfileSelector = true },
                colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(pastelBrush, RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFF2D3436), RoundedCornerShape(12.dp))
            ) {
                Text("Profile", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2D3436))
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { showAuxAssignDialog = true },
                colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(pastelBrush, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
            ) {
                Text("Aux", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2D3436))
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { showDebugConsole = true },
                colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(pastelBrush, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
        ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Open Debug Console",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Debug", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2D3436))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Joysticks with Aux Buttons on sides
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Aux Buttons Column
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                activeAuxButtons.filter { it.slot == 0 }.forEach { assigned ->
                    AuxButton(assigned, bluetoothManager)
                }
            }

            // Center: Joysticks
            Row(
                modifier = Modifier.weight(0.6f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Stick (Movement)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Move",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    JoystickControl(
                        onMoved = { state ->
                            leftJoystick = Pair(
                                state.x * currentProfile.sensitivity,
                                state.y * currentProfile.sensitivity
                            )
                        },
                        size = 200.dp
                    )
                }

                // Right Stick (Throttle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    JoystickControl(
                        onMoved = { state ->
                            rightJoystick = Pair(
                                state.x * currentProfile.sensitivity,
                                state.y * currentProfile.sensitivity
                            )
                        },
                        size = 200.dp
                    )
                    Text(
                        if (currentProfile.isThrottleUnidirectional) "Throttle\n(0-100%)" else "Throttle\n(+/-)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Right Aux Buttons Column
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                activeAuxButtons.filter { it.slot == 1 }.forEach { assigned ->
                    AuxButton(assigned, bluetoothManager)
                }
            }
        }
        }

        // Theme Toggle Button (Bottom Right)
        FloatingActionButton(
            onClick = onThemeToggle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color.White,
            contentColor = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFF2D3436)
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle Theme",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Dialogs
    if (showDebugConsole) {
        com.metelci.ardunakon.ui.components.DebugConsoleDialog(
            logs = debugLogs,
            telemetry = telemetry,
            onDismiss = { showDebugConsole = false }
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
                    Button(
                        onClick = {
                            isScanning = true
                            bluetoothManager.startScan()
                            // Auto-stop scanning indication after 5 seconds
                            coroutineScope.launch {
                                delay(5000)
                                isScanning = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .background(pastelBrush, RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFF2D3436), RoundedCornerShape(12.dp)),
                        enabled = !isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = "Scan",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isScanning) "Scanning..." else "Scan for Devices",
                            color = Color(0xFF2D3436),
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
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .background(pastelBrush, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                ) { Text("Close", color = Color(0xFF2D3436)) }
            }
        )
    }
    
    // Profile Editor State
    var showProfileEditor by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<com.metelci.ardunakon.data.Profile?>(null) }

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
                                        Text("Slot ${slot + 1} • Servo $servoText${if (roleText.isNotBlank()) " • $roleText" else ""}",
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
                                            labelText = new
                                            val servoId = servoText.toIntOrNull() ?: config.id
                                            val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                            tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
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
                                            commandText = new.filter { it.isDigit() }
                                            val servoId = servoText.toIntOrNull() ?: config.id
                                            val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                            tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
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
                                            TextButton(
                                                onClick = {
                                                    slot = s
                                                    val servoId = servoText.toIntOrNull() ?: config.id
                                                    val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                                    tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
                                                },
                                                colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                                                modifier = Modifier
                                                    .shadow(1.dp, RoundedCornerShape(10.dp))
                                                    .background(pastelBrush, RoundedCornerShape(10.dp))
                                                    .border(1.dp, if (slot == s) Color(0xFF2D3436) else Color(0xFFB0BEC5), RoundedCornerShape(10.dp))
                                            ) { Text("Slot ${s + 1}", color = Color(0xFF2D3436)) }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = servoText,
                                        onValueChange = { new ->
                                            servoText = new.filter { it.isDigit() }
                                            val servoId = servoText.toIntOrNull() ?: config.id
                                            val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                            tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
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
                                            roleText = new
                                            val servoId = servoText.toIntOrNull() ?: config.id
                                            val updatedConfig = config.copy(label = labelText, id = commandText.toIntOrNull() ?: config.id)
                                            tempAssignments[config.id] = AssignedAux(updatedConfig, slot, servoId, roleText)
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
                        profileManager.saveProfiles(profiles)
                        showAuxAssignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .background(pastelBrush, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                ) { Text("Done", color = Color(0xFF2D3436)) }
            },
            dismissButton = {
                Button(
                    onClick = {
                        tempAssignments.clear()
                        activeAuxButtons.clear()
                        val updatedProfile = currentProfile.copy(auxAssignments = emptyList())
                        profiles[currentProfileIndex] = updatedProfile
                        profileManager.saveProfiles(profiles)
                        showAuxAssignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .background(pastelBrush, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                ) { Text("Clear All", color = Color(0xFF2D3436)) }
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
                                TextButton(
                                    onClick = {
                                        currentProfileIndex = index
                                    },
                                    colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .shadow(2.dp, RoundedCornerShape(10.dp))
                                        .background(pastelBrush, RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(10.dp))
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
                                        profiles.removeAt(index)
                                        profileManager.saveProfiles(profiles)
                                        if (currentProfileIndex >= profiles.size) currentProfileIndex = 0
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF7675))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            profileToEdit = null
                            showProfileEditor = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .background(pastelBrush, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                    ) {
                        Text("Create New Profile")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProfileSelector = false },
                    colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .background(pastelBrush, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                ) { Text("Close") }
            }
        )
    }

    if (showProfileEditor) {
        com.metelci.ardunakon.ui.components.ProfileEditorDialog(
            profile = profileToEdit,
            onDismiss = { },
            onSave = { newProfile ->
                val index = profiles.indexOfFirst { it.id == newProfile.id }
                if (index != -1) {
                    profiles[index] = newProfile
                } else {
                    profiles.add(newProfile)
                }
                profileManager.saveProfiles(profiles)
            }
        )
    }
}


@Composable
fun StatusCard(label: String, state: ConnectionState, rssi: Int, onClick: () -> Unit) {
    val color = when(state) {
        ConnectionState.CONNECTED -> Color(0xFFA5D6A7)
        ConnectionState.CONNECTING -> Color(0xFFFFE082)
        else -> Color(0xFFEF9A9A)
    }
    val stateText = when(state) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.RECONNECTING -> "Reconnecting..."
        ConnectionState.ERROR -> "Error"
        ConnectionState.DISCONNECTED -> "Disconnected"
    }
    
    Card(
        modifier = Modifier
            .widthIn(min = 150.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: $stateText",
                color = Color(0xFF2D3436),
                style = MaterialTheme.typography.labelMedium
            )
            if (state == ConnectionState.CONNECTED) {
                Spacer(modifier = Modifier.width(8.dp))
                val bars = when {
                    rssi > -60 -> 4
                    rssi > -70 -> 3
                    rssi > -80 -> 2
                    else -> 1
                }
                Text("l".repeat(bars), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2D3436))
            }
        }
    }
}

@Composable
fun AuxButton(assigned: AssignedAux, manager: AppBluetoothManager) {
    Button(
        onClick = {
            val data = ProtocolManager.formatButtonData(assigned.servoId, true)
            manager.sendDataToSlot(data, assigned.slot)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 110.dp, height = 64.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFCE4EC),
                        Color(0xFFE3F2FD),
                        Color(0xFFE8F5E9)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(16.dp)),
        elevation = ButtonDefaults.buttonElevation(4.dp, 2.dp)
    ) {
        val roleSuffix = if (assigned.role.isNotBlank()) " • ${assigned.role}" else ""
        Text("${assigned.config.label} (S${assigned.slot + 1}→${assigned.servoId})$roleSuffix", color = Color(0xFF2D3436))
    }
}
