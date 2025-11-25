package com.metelci.ardunakon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.model.defaultButtonConfigs
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.ui.components.JoystickControl
import kotlinx.coroutines.delay

@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager
) {
    // State
    var showDeviceList by remember { mutableStateOf<Int?>(null) }
    var showProfileSelector by remember { mutableStateOf(false) }
    var showDebugConsole by remember { mutableStateOf(false) }
    
    val scannedDevices by bluetoothManager.scannedDevices.collectAsState()
    val connectionStates by bluetoothManager.connectionStates.collectAsState()
    val rssiValues by bluetoothManager.rssiValues.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val incomingData by bluetoothManager.incomingData.collectAsState()
    val telemetry by bluetoothManager.telemetry.collectAsState()

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

    // Joystick State
    var leftJoystick by remember { mutableStateOf(Pair(0f, 0f)) }
    var rightJoystick by remember { mutableStateOf(Pair(0f, 0f)) }
    
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

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F3F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            Button(
                onClick = {
                    val stopPacket = ProtocolManager.formatEStopData()
                    bluetoothManager.sendDataToAll(stopPacket)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(72.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("STOP", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }

            // Slot 2
            StatusCard(
                label = "Dev 2",
                state = connectionStates[1],
                rssi = rssiValues[1],
                onClick = { showDeviceList = 1 }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profile & Debug
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showProfileSelector = true }) {
                Text("Profile: ${currentProfile.name}", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = { showDebugConsole = true }) {
                Icon(Icons.Default.Info, "Debug Console")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Joysticks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Stick (Movement)
            Box(contentAlignment = Alignment.Center) {
                JoystickControl(
                    onMove = { x, y -> leftJoystick = Pair(x, y) },
                    size = 200.dp,
                    sensitivity = currentProfile.sensitivity
                )
                Text("Move", modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp))
            }

            // Right Stick (Throttle)
            Box(contentAlignment = Alignment.Center) {
                JoystickControl(
                    onMove = { x, y -> rightJoystick = Pair(x, y) },
                    size = 200.dp,
                    sensitivity = currentProfile.sensitivity
                )
                Text(
                    if (currentProfile.isThrottleUnidirectional) "Throttle (0-100%)" else "Throttle (+/-)", 
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Aux Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            currentProfile.buttonConfigs.forEach { config ->
                AuxButton(config, bluetoothManager)
            }
        }
    }

    // Dialogs
    if (showDebugConsole) {
        com.metelci.ardunakon.ui.components.DebugConsoleDialog(debugLogs, telemetry) { showDebugConsole = false }
    }

    if (showDeviceList != null) {
        // ... (Device List Dialog - keep as is) ...
        AlertDialog(
            onDismissRequest = { showDeviceList = null },
            title = { Text("Select Device for Slot ${showDeviceList!! + 1}") },
            text = {
                Column {
                    Button(onClick = { bluetoothManager.startScan() }) { Text("Scan") }
                    Spacer(modifier = Modifier.height(8.dp))
                    scannedDevices.forEach { device ->
                        TextButton(onClick = {
                            bluetoothManager.connectToDevice(device, showDeviceList!!)
                            showDeviceList = null
                        }) {
                            Text("${device.name} (${device.address})")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDeviceList = null }) { Text("Cancel") } }
        )
    }
    
    // Profile Editor State
    var showProfileEditor by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<com.metelci.ardunakon.data.Profile?>(null) }

    if (showProfileSelector) {
        AlertDialog(
            onDismissRequest = { showProfileSelector = false },
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Edit, "Edit")
                                }
                                
                                IconButton(onClick = {
                                    if (profiles.size > 1) {
                                        profiles.removeAt(index)
                                        profileManager.saveProfiles(profiles)
                                        if (currentProfileIndex >= profiles.size) currentProfileIndex = 0
                                    }
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, "Delete", tint = Color(0xFFFF7675))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            profileToEdit = null
                            showProfileEditor = true
                            showProfileSelector = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create New Profile")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProfileSelector = false }) { Text("Close") } }
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
                profileManager.saveProfiles(profiles)
                showProfileEditor = false
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
    
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ${state.name.take(4)}",
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
fun AuxButton(config: com.metelci.ardunakon.model.ButtonConfig, manager: AppBluetoothManager) {
    Button(
        onClick = {
            val data = ProtocolManager.formatButtonData(config.id, true)
            manager.sendDataToAll(data)
        },
        colors = ButtonDefaults.buttonColors(containerColor = config.getColor()),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(width = 100.dp, height = 60.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp, 2.dp)
    ) {
        Text(config.label, color = Color(0xFF2D3436))
    }
}
