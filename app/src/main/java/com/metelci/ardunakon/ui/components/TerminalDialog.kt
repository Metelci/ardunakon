package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalDialog(
    logs: List<LogEntry>,
    telemetry: AppBluetoothManager.Telemetry?,
    onDismiss: () -> Unit,
    onSendCommand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    val view = LocalView.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when logs update
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(min = 400.dp, max = 600.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Terminal", style = MaterialTheme.typography.titleLarge)
                    if (telemetry != null) {
                        Text(
                            "Bat: ${telemetry.batteryVoltage}V | ${telemetry.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.batteryVoltage < 11.0f) Color(0xFFFF7675) else Color(0xFF00C853)
                        )
                    }
                }
                Row {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClearLogs()
                    }) {
                        Icon(Icons.Default.Delete, "Clear Logs", tint = Color(0xFFB0BEC5))
                    }
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Log Output Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...", fontSize = 14.sp) },
                        singleLine = true,
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
                            unfocusedBorderColor = Color(0xFFB0BEC5)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        },
        confirmButton = {} // Handled by custom layout
    )
}
