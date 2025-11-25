package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugConsoleDialog(
    logs: List<LogEntry>,
    telemetry: com.metelci.ardunakon.bluetooth.AppBluetoothManager.Telemetry?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "System Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                // Telemetry Section
                if (telemetry != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E272E), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Battery", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Text("${telemetry.batteryVoltage}V", color = Color(0xFFA5D6A7), style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Status", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Text(
                                telemetry.status, 
                                color = if (telemetry.status == "Safe Mode") Color(0xFFEF9A9A) else Color(0xFF74B9FF),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Real-time connection events",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E272E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        reverseLayout = true // Show newest at bottom (or top if we reverse list)
                        // Actually, standard consoles scroll down. Let's keep newest at bottom.
                        // But we passed logs.reversed() before.
                        // Let's just show list as is (newest last) and auto-scroll or reverse order.
                        // Usually newest at top is easier on mobile.
                    ) {
                        items(logs.reversed()) { log ->
                            LogItem(log)
                            Divider(color = Color(0xFF2D3436), thickness = 0.5.dp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF74B9FF))
                ) {
                    Text("Close", color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val icon = when(log.type) {
        LogType.SUCCESS -> Icons.Default.CheckCircle
        LogType.ERROR -> Icons.Default.Close
        LogType.WARNING -> Icons.Default.Warning
        LogType.INFO -> Icons.Default.Info
    }
    
    val color = when(log.type) {
        LogType.SUCCESS -> Color(0xFFA5D6A7) // Green
        LogType.ERROR -> Color(0xFFEF9A9A) // Red
        LogType.WARNING -> Color(0xFFFFE082) // Amber
        LogType.INFO -> Color(0xFF81ECEC) // Cyan
    }
    
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = log.message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = timeFormat.format(Date(log.timestamp)),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
