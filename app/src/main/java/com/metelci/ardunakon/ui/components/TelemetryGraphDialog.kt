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

enum class TimeRange(val label: String, val milliseconds: Long?) {
    ONE_MIN("1 min", 60_000L),
    FIVE_MIN("5 min", 300_000L),
    TEN_MIN("10 min", 600_000L),
    ALL("All", null)
}

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
    var timeRange by remember { mutableStateOf(TimeRange.FIVE_MIN) }
    var showSlot1 by remember { mutableStateOf(true) }
    var showSlot2 by remember { mutableStateOf(true) }

    // Force recomposition when history updates
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
                .fillMaxHeight(),
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

                // Time Range Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeRange.values().forEach { range ->
                        FilterChip(
                            selected = timeRange == range,
                            onClick = { timeRange = range },
                            label = { Text(range.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF2196F3),
                                selectedLabelColor = Color.Black,
                                containerColor = if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0),
                                labelColor = if (isDarkTheme) Color.White else Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Slot Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showSlot1,
                            onCheckedChange = { showSlot1 = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF)),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Slot 1", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showSlot2,
                            onCheckedChange = { showSlot2 = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD500F9)),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Slot 2", color = Color(0xFFD500F9), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            val series = buildList {
                                if (showSlot1) {
                                    add(
                                        LineChartSeries(
                                            label = "Slot 1",
                                            data = telemetryHistoryManager.getBatteryHistory(0, timeRange.milliseconds),
                                            color = Color(0xFF00E5FF)
                                        )
                                    )
                                }
                                if (showSlot2) {
                                    add(
                                        LineChartSeries(
                                            label = "Slot 2",
                                            data = telemetryHistoryManager.getBatteryHistory(1, timeRange.milliseconds),
                                            color = Color(0xFFD500F9)
                                        )
                                    )
                                }
                            }
                            if (series.isEmpty() || series.all { it.data.isEmpty() }) {
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
                            val series = buildList {
                                if (showSlot1) {
                                    add(
                                        LineChartSeries(
                                            label = "Slot 1",
                                            data = telemetryHistoryManager.getRssiHistory(0, timeRange.milliseconds),
                                            color = Color(0xFF00E5FF)
                                        )
                                    )
                                }
                                if (showSlot2) {
                                    add(
                                        LineChartSeries(
                                            label = "Slot 2",
                                            data = telemetryHistoryManager.getRssiHistory(1, timeRange.milliseconds),
                                            color = Color(0xFFD500F9)
                                        )
                                    )
                                }
                            }
                            if (series.isEmpty() || series.all { it.data.isEmpty() }) {
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
                            val series = buildList {
                                if (showSlot1) {
                                    add(
                                        LineChartSeries(
                                            label = "Slot 1",
                                            data = telemetryHistoryManager.getRttHistory(0, timeRange.milliseconds),
                                            color = Color(0xFF00E5FF)
                                        )
                                    )
                                }
                                if (showSlot2) {
                                    add(
                                        LineChartSeries(
                                            label = "Slot 2",
                                            data = telemetryHistoryManager.getRttHistory(1, timeRange.milliseconds),
                                            color = Color(0xFFD500F9)
                                        )
                                    )
                                }
                            }
                            if (series.isEmpty() || series.all { it.data.isEmpty() }) {
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
