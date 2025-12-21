@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import android.view.HapticFeedbackConstants
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
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager

enum class GraphTab {
    BATTERY,
    RSSI,
    RTT,
    PACKET_LOSS,
    QUALITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun TelemetryGraphDialog(telemetryHistoryManager: TelemetryHistoryManager, onDismiss: () -> Unit) {
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436))
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
                        color = Color.White
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
                                tint = Color(0xFFB0BEC5),
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
                                tint = Color(0xFFB0BEC5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color(0xFF455A64),
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == GraphTab.BATTERY,
                        onClick = { selectedTab = GraphTab.BATTERY },
                        text = {
                            Text("Battery", color = Color.White, fontSize = 10.sp)
                        }
                    )
                    Tab(
                        selected = selectedTab == GraphTab.RSSI,
                        onClick = { selectedTab = GraphTab.RSSI },
                        text = { Text("RSSI", color = Color.White, fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == GraphTab.RTT,
                        onClick = { selectedTab = GraphTab.RTT },
                        text = {
                            Text("Latency", color = Color.White, fontSize = 10.sp)
                        }
                    )
                    Tab(
                        selected = selectedTab == GraphTab.PACKET_LOSS,
                        onClick = { selectedTab = GraphTab.PACKET_LOSS },
                        text = {
                            Text("Loss", color = Color.White, fontSize = 10.sp)
                        }
                    )
                    Tab(
                        selected = selectedTab == GraphTab.QUALITY,
                        onClick = { selectedTab = GraphTab.QUALITY },
                        text = {
                            Text("Quality", color = Color.White, fontSize = 10.sp)
                        }
                    )
                }

                val unitsLabel = when (selectedTab) {
                    GraphTab.BATTERY -> "Units: Volts (V)"
                    GraphTab.RSSI -> "Units: Signal strength (dBm)"
                    GraphTab.RTT -> "Units: Latency (ms)"
                    GraphTab.PACKET_LOSS -> "Units: Packet loss (%)"
                    GraphTab.QUALITY -> "Units: Connection quality (%)"
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    unitsLabel,
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(6.dp))

                // Chart Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
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
                                        color = Color(0xFFB0BEC5),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "Battery Voltage (V)",
                                    yAxisMin = 0f,
                                    yAxisMax = 30f
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
                                        color = Color(0xFFB0BEC5),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "RSSI (dBm)",
                                    yAxisMin = -100f,
                                    yAxisMax = 0f
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
                                        color = Color(0xFFB0BEC5),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "Round-Trip Time (ms)",
                                    yAxisMin = 0f
                                )
                            }
                        }
                        GraphTab.PACKET_LOSS -> {
                            PacketLossHeatmap(
                                data = telemetryHistoryManager.getPacketLossHistory(
                                    900_000L
                                ).map { it.lossPercent / 100f },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        GraphTab.QUALITY -> {
                            val series = listOf(
                                LineChartSeries(
                                    label = "Connection Quality",
                                    data = telemetryHistoryManager.getConnectionQualityHistory(120_000L),
                                    color = Color(0xFF00E5FF)
                                )
                            )
                            if (series.all { it.data.isEmpty() }) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No quality data available",
                                        color = Color(0xFFB0BEC5),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LineChart(
                                    series = series,
                                    yAxisLabel = "Connection Quality (%)",
                                    yAxisMin = 0f,
                                    yAxisMax = 100f
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
