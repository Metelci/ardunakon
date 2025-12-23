// HapticFeedbackConstants.KEYBOARD_TAP deprecated in API 33, using CONFIRM would require SDK checks.
// Suppression kept for backward compatibility with pre-API 33 devices.
@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.ui.utils.hapticTap

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.bluetooth.TroubleshootHints
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun EmbeddedTerminal(
    logs: List<LogEntry>,
    telemetry: Telemetry?,
    connectedDeviceInfo: String? = null,
    onSendCommand: (String) -> Unit,
    onClearLogs: () -> Unit,
    onMaximize: () -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
    onExportLogs: () -> Unit = {}
) {
    val view = LocalView.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedFilters by remember {
        mutableStateOf(
            setOf(LogType.INFO, LogType.SUCCESS, LogType.WARNING, LogType.ERROR)
        )
    }

    // Filtered logs
    val filteredLogs = remember(logs, searchQuery, selectedFilters) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isEmpty() || log.message.contains(searchQuery, ignoreCase = true)
            val matchesType = log.type in selectedFilters
            matchesSearch && matchesType
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty() && searchQuery.isEmpty()) { // Only auto-scroll if not searching to avoid jumping
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp).fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Terminal",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    if (connectedDeviceInfo != null) {
                        Text(
                            connectedDeviceInfo,
                            style = MaterialTheme.typography.labelSmall,
                            // Green for connected
                            color = Color(0xFF00C853)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Export button
                    IconButton(
                        onClick = {
                            view.hapticTap()
                            onExportLogs()
                            // Also clear logs via long press ideally, but let's keep simple
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Share, "Export", tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                    }
                    // Search toggle
                    IconButton(
                        onClick = {
                            view.hapticTap()
                            showSearch = !showSearch
                            if (!showSearch) searchQuery = ""
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            // Need Search Icon
                            imageVector = if (showSearch) {
                                Icons.Default.Close
                            } else {
                                Icons.Default.Search
                            },
                            contentDescription = "Search",
                            tint = if (showSearch) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Clear button
                    IconButton(
                        onClick = {
                            view.hapticTap()
                            onClearLogs()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFB0BEC5), modifier = Modifier.size(16.dp))
                    }
                    // Maximize button
                    IconButton(
                        onClick = {
                            view.hapticTap()
                            onMaximize()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            "Maximize",
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Minimize/Hide button
                    IconButton(
                        onClick = {
                            view.hapticTap()
                            onMinimize()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, "Hide", tint = Color(0xFFB0BEC5), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Telemetry info (compact)
            if (telemetry != null) {
                Text(
                    "Bat: ${telemetry.batteryVoltage}V",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (telemetry.batteryVoltage < 11.0f) Color(0xFFFF7675) else Color(0xFF00C853)
                )
            }

            // Search and Filter Controls
            if (showSearch) {
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = { Text("Search logs...", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear search", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF74B9FF),
                            unfocusedBorderColor = Color(0xFF555555),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFAAAAAA)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LogType.values().forEach { type ->
                            val isSelected = type in selectedFilters
                            val color = when (type) {
                                LogType.INFO -> Color(0xFF90CAF9)
                                LogType.SUCCESS -> Color(0xFF00C853)
                                LogType.WARNING -> Color(0xFFFFD54F)
                                LogType.ERROR -> Color(0xFFFF7675)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedFilters = if (isSelected) {
                                        if (selectedFilters.size > 1) selectedFilters - type else selectedFilters
                                    } else {
                                        selectedFilters + type
                                    }
                                },
                                label = { Text(type.name.first().toString(), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color,
                                    disabledLabelColor = Color.Gray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = color,
                                    borderColor = Color.Gray,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Log Output Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    itemsIndexed(
                        items = filteredLogs,
                        key = { index, log -> "${index}_${log.timestamp}_${log.message.hashCode()}" }
                    ) { _, log ->
                        val color = when (log.type) {
                            LogType.INFO -> Color(0xFF90CAF9)
                            LogType.SUCCESS -> Color(0xFF00C853)
                            LogType.WARNING -> Color(0xFFFFD54F)
                            LogType.ERROR -> Color(0xFFFF7675)
                        }

                        Column(
                            modifier = if (log.type == LogType.SUCCESS) {
                                Modifier.background(
                                    Color(0xFF00C853).copy(alpha = 0.15f),
                                    RoundedCornerShape(2.dp)
                                ).padding(horizontal = 2.dp)
                            } else {
                                Modifier
                            }
                        ) {
                            Text(
                                text = "> ${log.message}",
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )

                            // Show troubleshoot hint for ERROR logs
                            if (log.type == LogType.ERROR) {
                                val hint = TroubleshootHints.getHintForError(log.message)
                                if (hint != null) {
                                    Text(
                                        text = "  → ${hint.first}. ${hint.second}",
                                        // Electric yellow
                                        color = Color(0xFFFFFF00),
                                        fontFamily = FontFamily.Monospace,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Input Area (compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    placeholder = { Text("Cmd...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendCommand(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF74B9FF),
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        view.hapticTap()
                        if (inputText.isNotBlank()) {
                            onSendCommand(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Send, "Send", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
