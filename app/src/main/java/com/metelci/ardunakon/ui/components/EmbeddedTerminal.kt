package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedTerminal(
    logs: List<LogEntry>,
    telemetry: AppBluetoothManager.Telemetry?,
    onSendCommand: (String) -> Unit,
    onClearLogs: () -> Unit,
    onMaximize: () -> Unit,
    onMinimize: () -> Unit,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
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
                Text(
                    "Terminal",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Clear button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onClearLogs()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFB0BEC5), modifier = Modifier.size(16.dp))
                    }
                    // Maximize button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onMaximize()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, "Maximize", tint = Color(0xFF90CAF9), modifier = Modifier.size(16.dp))
                    }
                    // Minimize/Hide button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
                    items(logs) { log ->
                        val color = when (log.type) {
                            LogType.INFO -> Color(0xFF90CAF9)
                            LogType.SUCCESS -> Color(0xFF00C853)
                            LogType.WARNING -> Color(0xFFFFD54F)
                            LogType.ERROR -> Color(0xFFFF7675)
                        }
                        Text(
                            text = "> ${log.message}",
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        )
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
                    modifier = Modifier.weight(1f).height(36.dp),
                    placeholder = { Text("Cmd...", fontSize = 10.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
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
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (inputText.isNotBlank()) {
                            onSendCommand(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Send, "Send", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
