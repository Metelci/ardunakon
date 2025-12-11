@file:Suppress("DEPRECATION")
package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.view.HapticFeedbackConstants
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager



enum class GraphTab {
    BATTERY, RSSI, RTT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryGraphDialog(
    telemetryHistoryManager: TelemetryHistoryManager,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val view = LocalView.current
    var selectedTab by remember { mutableStateOf(GraphTab.BATTERY) }

    // Force recomposition when history updates
    @Suppress("UNUSED_VARIABLE")
    val historyUpdated by telemetryHistoryManager.historyUpdated.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Telemetry Graphs",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                telemetryHistoryManager.clearAllHistory()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Clear History",
                                tint = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Close",
                                tint = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0)
                ) {
                    Tab(
                        selected = selectedTab == GraphTab.BATTERY,
                        onClick = { selectedTab = GraphTab.BATTERY },
                        text = { Text("Battery", color = if (isDarkTheme) Color.White else Color.Black, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == GraphTab.RSSI,
                        onClick = { selectedTab = GraphTab.RSSI },
                        text = { Text("RSSI", color = if (isDarkTheme) Color.White else Color.Black, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == GraphTab.RTT,
                        onClick = { selectedTab = GraphTab.RTT },
                        text = { Text("Latency", color = if (isDarkTheme) Color.White else Color.Black, fontSize = 13.sp) }
                    )
                }

                val unitsLabel = when (selectedTab) {
                    GraphTab.BATTERY -> "Units: Volts (V)"
                    GraphTab.RSSI -> "Units: Signal strength (dBm)"
                    GraphTab.RTT -> "Units: Latency (ms)"
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    unitsLabel,
                    color = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF37474F),
                    style = MaterialTheme.typography.labelSmall
                )


                Spacer(modifier = Modifier.height(8.dp))



                Spacer(modifier = Modifier.height(6.dp))

                // Chart Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(if (isDarkTheme) Color(0xFF1E1E1E) else Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    when (selectedTab) {
                        GraphTab.BATTERY -> {
                            val series = listOf(
                                LineChartSeries(
                                    label = "Device",
                                    data = telemetryHistoryManager.getBatteryHistory(120_000L),
                                    color = Color(0xFF00E5FF)
                                )
                            )
                            if (series.all { it.data.isEmpty() }) {
                                // Empty state
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No battery data available",
                                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "Battery Voltage (V)",
                                    yAxisMin = 0f,
                                    yAxisMax = 30f,
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                        GraphTab.RSSI -> {
                            val series = listOf(
                                LineChartSeries(
                                    label = "Device",
                                    data = telemetryHistoryManager.getRssiHistory(120_000L),
                                    color = Color(0xFF00E5FF)
                                )
                            )
                            if (series.all { it.data.isEmpty() }) {
                                // Empty state
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No RSSI data available",
                                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "RSSI (dBm)",
                                    yAxisMin = -100f,
                                    yAxisMax = 0f,
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                        GraphTab.RTT -> {
                            val series = listOf(
                                LineChartSeries(
                                    label = "Device",
                                    data = telemetryHistoryManager.getRttHistory(120_000L),
                                    color = Color(0xFF00E5FF)
                                )
                            )
                            if (series.all { it.data.isEmpty() }) {
                                // Empty state
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No RTT data available",
                                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "Round-Trip Time (ms)",
                                    yAxisMin = 0f,
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
