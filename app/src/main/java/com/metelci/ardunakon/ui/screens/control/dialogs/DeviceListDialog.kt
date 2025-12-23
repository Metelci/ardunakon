@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.screens.control.dialogs

import com.metelci.ardunakon.ui.utils.hapticTap

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dialog for displaying and selecting Bluetooth devices.
 */
@Suppress("FunctionName")
@Composable
fun DeviceListDialog(
    scannedDevices: List<BluetoothDeviceModel>,
    onScan: () -> Unit,
    onDeviceSelected: (BluetoothDeviceModel) -> Unit,
    onDismiss: () -> Unit,
    view: View,
    modifier: Modifier = Modifier
) {
    var isScanning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f),
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
                    OutlinedButton(
                        onClick = {
                            view.hapticTap()
                            isScanning = true
                            onScan()
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

                // Scrollable device list
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
                                    contentDescription = "No Bluetooth devices found",
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
                            // Strip MAC address in parentheses from display name
                            val displayName = device.name.replace(Regex("\\s*\\([0-9A-Fa-f:]{11,}\\)$"), "")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 10.dp)
                                    .clickable {
                                        view.hapticTap()
                                        onDeviceSelected(device)
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
                                            contentDescription = "Bluetooth device",
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
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 12.dp, minWidth = 0.dp)
                    .height(12.dp)
                    .padding(vertical = 0.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    "Close",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    )
}
