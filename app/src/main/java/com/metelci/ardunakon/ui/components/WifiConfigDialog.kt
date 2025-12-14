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
@Composable
fun WifiConfigDialog(
    initialIp: String,
    initialPort: Int,
    scannedDevices: List<com.metelci.ardunakon.wifi.WifiDevice>,
    isEncrypted: Boolean = false,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var ipAddress by remember(initialIp) { mutableStateOf(initialIp) }
    var port by remember(initialPort) { mutableStateOf(initialPort.toString()) }
    var isScanning by remember { mutableStateOf(false) }

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

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { char -> char.isDigit() } },
                        label = { Text("Port", color = Color(0xFFB0BEC5), fontSize = 11.sp) },
                        modifier = Modifier.weight(0.8f),
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

                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF00FF00),
                            strokeWidth = 2.dp
                        )
                    } else {
                        OutlinedButton(
                            onClick = {
                                isScanning = true
                                onScan()
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    isScanning = false
                                }, 5000)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .semantics { contentDescription = "Scan for WiFi devices" }
                        ) {
                            Text("Scan", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Device List - Compact
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp, max = 80.dp)
                        .background(Color(0xFF232338), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    if (scannedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isScanning) "Scanning..." else "No devices",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Show up to 2 devices for compactness
                        scannedDevices.take(2).forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        ipAddress = device.ip
                                        port = device.port.toString()
                                    }
                                    .semantics {
                                        contentDescription = "Select ${device.name} at ${device.ip}"
                                    }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        device.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(device.ip, color = Color(0xFFB0BEC5), fontSize = 10.sp)
                                }
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF00),
                                    modifier = Modifier.size(14.dp)
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.semantics { contentDescription = "Cancel and close dialog" }
                    ) {
                        Text("Cancel", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val portInt = port.toIntOrNull() ?: 8888
                            onSave(ipAddress, portInt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.semantics { contentDescription = "Connect to WiFi device" }
                    ) {
                        Text("Connect", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
