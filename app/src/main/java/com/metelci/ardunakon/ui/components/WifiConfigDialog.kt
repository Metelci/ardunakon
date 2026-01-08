package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun WifiConfigDialog(
    initialIp: String,
    initialPort: Int,
    scannedDevices: List<com.metelci.ardunakon.wifi.WifiDevice>,
    isEncrypted: Boolean = false,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var ipAddress by remember(initialIp) { mutableStateOf(initialIp) }
    var port by remember(initialPort) { mutableStateOf(initialPort.toString()) }

    val ipError by remember {
        derivedStateOf { validateIpv4Address(ipAddress) }
    }
    val portError by remember {
        derivedStateOf { validatePort(port) }
    }
    val canConnect = ipError == null && portError == null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with encryption status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WiFi Configuration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Encryption status icon
                    Icon(
                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isEncrypted) "Connection encrypted" else "Connection not encrypted",
                        tint = if (isEncrypted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier
                            .size(18.dp)
                            .semantics {
                                contentDescription = if (isEncrypted) {
                                    "Secure connection. Data is encrypted."
                                } else {
                                    "Insecure connection. Data is not encrypted."
                                }
                            }
                    )
                }

                // Encryption status text
                Text(
                    text = if (isEncrypted) "🔒 Encrypted" else "⚠️ Not Encrypted",
                    fontSize = 11.sp,
                    color = if (isEncrypted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // IP Address and Port in same row for landscape
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("IP Address", color = Color(0xFFB0BEC5), fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        isError = ipError != null,
                        supportingText = ipError?.let { msg ->
                            {
                                Text(msg, color = Color(0xFFFF5252), fontSize = 10.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF00),
                            unfocusedBorderColor = Color(0xFF455A64),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { char -> char.isDigit() } },
                        label = { Text("Port", color = Color(0xFFB0BEC5), fontSize = 11.sp) },
                        modifier = Modifier.weight(0.8f),
                        isError = portError != null,
                        supportingText = portError?.let { msg ->
                            {
                                Text(msg, color = Color(0xFFFF5252), fontSize = 10.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF00),
                            unfocusedBorderColor = Color(0xFF455A64),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scanning Section - more compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Devices",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB0BEC5)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF00FF00),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        OutlinedButton(
                            onClick = { if (isScanning) onStopScan() else onStartScan() },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .semantics {
                                    contentDescription = if (isScanning) {
                                        "Stop scanning for WiFi devices"
                                    } else {
                                        "Scan for WiFi devices"
                                    }
                                }
                        ) {
                            Text(if (isScanning) "Stop" else "Scan", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Device List - Scrollable and larger
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 200.dp)
                        .background(Color(0xFF232338), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (scannedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isScanning) "Scanning..." else "No devices",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Show all devices with scrolling
                        scannedDevices.forEach { device ->
                            val selectedPort = port.toIntOrNull()
                            val isSelected =
                                device.ip == ipAddress && (selectedPort == null || device.port == selectedPort)
                            val isLastConnected = device.ip == initialIp && device.port == initialPort

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .background(
                                        when {
                                            isSelected -> Color(0x332196F3)
                                            isLastConnected -> Color(0x1A00FF00)
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        ipAddress = device.ip
                                        port = device.port.toString()
                                    }
                                    .semantics {
                                        contentDescription =
                                            buildString {
                                                append("Select ${device.name} at ${device.ip}:${device.port}")
                                                if (isLastConnected) append(". Last connected")
                                                if (isSelected) append(". Selected")
                                            }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            device.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (isLastConnected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Last connected",
                                                color = Color(0xFF4CAF50),
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .background(Color(0x332E7D32), RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(device.ip, color = Color(0xFFB0BEC5), fontSize = 10.sp)
                                }
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "WiFi device available",
                                    tint = Color(0xFF00FF00),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Buttons Row - Compact with accessibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier =
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .semantics { contentDescription = "Cancel and close dialog" },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Cancel", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val ip = ipAddress.trim()
                            val portInt = port.trim().toInt()
                            onSave(ip, portInt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        enabled = canConnect,
                        modifier =
                        Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .semantics { contentDescription = "Connect to WiFi device" },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Connect", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun validateIpv4Address(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return "IP address required"

    val parts = trimmed.split('.')
    if (parts.size != 4) return "Enter a valid IPv4 address"
    if (parts.any { it.isEmpty() }) return "Enter a valid IPv4 address"

    for (part in parts) {
        val value = part.toIntOrNull() ?: return "Enter a valid IPv4 address"
        if (value !in 0..255) return "Enter a valid IPv4 address"
    }
    return null
}

private fun validatePort(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return "Port required"
    val value = trimmed.toIntOrNull() ?: return "Enter a valid port"
    return if (value in 1..65535) null else "Port must be 1–65535"
}
