package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiConfigDialog(
    initialIp: String,
    initialPort: Int,
    scannedDevices: List<com.metelci.ardunakon.wifi.WifiDevice>,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(initialPort.toString()) }
    var isScanning by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),  // Reduced from 16.dp
            shape = RoundedCornerShape(12.dp),  // Slightly smaller
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)  // Reduced from 16.dp
                    .verticalScroll(rememberScrollState()),  // Make scrollable
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WiFi Configuration",
                    fontSize = 16.sp,  // Reduced from 18.sp
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))  // Reduced from 16.dp
                
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
                
                Spacer(modifier = Modifier.height(8.dp))  // Reduced from 16.dp

                // Scanning Section - more compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Devices",
                        style = MaterialTheme.typography.labelMedium,  // Smaller
                        color = Color(0xFFB0BEC5)
                    )
                    
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),  // Smaller
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),  // Compact
                            modifier = Modifier.height(32.dp)  // Fixed compact height
                        ) {
                            Text("Scan", fontSize = 12.sp)  // Shorter text
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))  // Reduced from 8.dp

                // Device List - Compact
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp, max = 80.dp)  // Reduced max height
                        .background(Color(0xFF232338), RoundedCornerShape(6.dp))
                        .padding(6.dp)  // Reduced padding
                ) {
                    if (scannedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(24.dp),  // Smaller
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isScanning) "Scanning..." else "No devices",
                                color = Color.Gray,
                                fontSize = 11.sp  // Smaller
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
                                    .padding(vertical = 2.dp),  // Reduced
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)  // Smaller
                                    Text(device.ip, color = Color(0xFFB0BEC5), fontSize = 10.sp)  // Smaller
                                }
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF00),
                                    modifier = Modifier.size(14.dp)  // Smaller
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))  // Reduced from 24.dp
                
                // Buttons Row - Compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Connect", fontSize = 13.sp)  // Shorter: "Connect" instead of "Save & Connect"
                    }
                }
            }
        }
    }
}
