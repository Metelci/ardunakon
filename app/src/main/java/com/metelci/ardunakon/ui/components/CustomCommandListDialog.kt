@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.ui.utils.hapticTap

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metelci.ardunakon.model.CustomCommand

/**
 * Dialog showing list of all configured custom commands.
 */
@Suppress("FunctionName")
@Composable
fun CustomCommandListDialog(
    commands: List<CustomCommand>,
    view: View,
    onAddCommand: () -> Unit,
    onEditCommand: (CustomCommand) -> Unit,
    onDeleteCommand: (String) -> Unit,
    onSendCommand: (CustomCommand) -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color(0xFF00FF00),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Custom Commands",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${commands.size}/16 commands configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF455A64))
                Spacer(modifier = Modifier.height(16.dp))

                // Content
                if (commands.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No custom commands yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Text(
                                "Tap + to create your first command",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commands, key = { it.id }) { command ->
                            CustomCommandItem(
                                command = command,
                                view = view,
                                onEdit = { onEditCommand(command) },
                                onDelete = { showDeleteConfirmation = command.id },
                                onSend = { onSendCommand(command) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add button
                Button(
                    onClick = {
                        view.hapticTap()
                        onAddCommand()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = commands.size < 16,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Command")
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { commandId ->
        val command = commands.find { it.id == commandId }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Command?") },
            text = { Text("Are you sure you want to delete \"${command?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCommand(commandId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2A2A3E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun CustomCommandItem(
    command: CustomCommand,
    view: View,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xFF2A2A3E),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon with color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(command.colorHex), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName(command.iconName),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        command.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        "ID: 0x${"%02X".format(command.commandId)} • ${if (command.isToggle) "Toggle" else "Momentary"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Send button
                IconButton(
                    onClick = {
                        view.hapticTap()
                        onSend()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Edit button
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
