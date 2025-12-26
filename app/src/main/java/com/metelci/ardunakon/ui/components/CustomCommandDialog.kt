@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.ui.utils.hapticTap

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metelci.ardunakon.model.CustomCommand

/**
 * Dialog for creating or editing a custom command.
 */
@Suppress("FunctionName")
@Composable
fun CustomCommandDialog(
    command: CustomCommand?,
    availableCommandIds: List<Byte>,
    view: View,
    onSave: (CustomCommand) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = command != null
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()
    val singleScrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Form state
    var name by remember { mutableStateOf(command?.name ?: "") }
    var selectedCommandId by remember {
        mutableStateOf(command?.commandId ?: availableCommandIds.firstOrNull() ?: 0x20.toByte())
    }
    var payloadHex by remember {
        mutableStateOf(command?.payload?.joinToString("") { "%02X".format(it) } ?: "0000000000")
    }
    var selectedColorHex by remember { mutableStateOf(command?.colorHex ?: 0xFF2196F3) }
    var isToggle by remember { mutableStateOf(command?.isToggle ?: false) }
    var selectedIconName by remember { mutableStateOf(command?.iconName ?: "Build") }
    var selectedShortcut by remember { mutableStateOf(command?.keyboardShortcut) }

    // Validation
    val isValid = name.isNotBlank() && payloadHex.length == 10

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
                .fillMaxWidth(if (isLandscape) 0.95f else 0.92f)
                .fillMaxHeight(if (isLandscape) 0.9f else 0.85f),
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
                            if (isEditMode) "Edit Command" else "New Command",
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

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF455A64))
                Spacer(modifier = Modifier.height(16.dp))

                // Content - Two-column layout for landscape
                if (isLandscape) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left column: Name, Command ID, Payload, Toggle
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(leftScrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Name field
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(20) },
                                label = { Text("Command Name") },
                                placeholder = { Text("e.g., Horn, Lights") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00C853),
                                    unfocusedBorderColor = Color(0xFF455A64),
                                    focusedLabelColor = Color(0xFF00C853),
                                    unfocusedLabelColor = Color.Gray
                                )
                            )

                            // Command ID selector
                            CommandIdSelector(
                                selectedId = selectedCommandId,
                                availableIds = if (isEditMode) {
                                    (availableCommandIds + command!!.commandId).distinct().sorted()
                                } else {
                                    availableCommandIds
                                },
                                onIdSelected = { selectedCommandId = it }
                            )

                            // Payload hex editor
                            OutlinedTextField(
                                value = payloadHex,
                                onValueChange = { newValue ->
                                    val filtered = newValue.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
                                    payloadHex = filtered.take(10)
                                },
                                label = { Text("Payload (5 bytes hex)") },
                                placeholder = { Text("0102030405") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                supportingText = {
                                    Text(
                                        "${payloadHex.length}/10 hex",
                                        color = if (payloadHex.length == 10) Color(0xFF00C853) else Color.Gray
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00C853),
                                    unfocusedBorderColor = Color(0xFF455A64),
                                    focusedLabelColor = Color(0xFF00C853),
                                    unfocusedLabelColor = Color.Gray
                                )
                            )

                            // Toggle vs Momentary
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2A2A3E), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Toggle Mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        if (isToggle) "On/off" else "Hold",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Switch(
                                    checked = isToggle,
                                    onCheckedChange = {
                                        view.hapticTap()
                                        isToggle = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00C853),
                                        checkedTrackColor = Color(0xFF00C853).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }

                        // Vertical divider
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = Color(0xFF455A64)
                        )

                        // Right column: Color, Icon, Shortcut
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rightScrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Color picker
                            ColorPicker(
                                selectedColor = selectedColorHex,
                                onColorSelected = { selectedColorHex = it }
                            )

                            // Icon picker
                            IconPicker(
                                selectedIconName = selectedIconName,
                                onIconSelected = { selectedIconName = it }
                            )

                            // Keyboard shortcut picker
                            ShortcutPicker(
                                selectedShortcut = selectedShortcut,
                                onShortcutSelected = { selectedShortcut = it }
                            )
                        }
                    }
                } else {
                    // Portrait: Single column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(singleScrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Name field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(20) },
                            label = { Text("Command Name") },
                            placeholder = { Text("e.g., Horn, Lights, Servo A") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00C853),
                                unfocusedBorderColor = Color(0xFF455A64),
                                focusedLabelColor = Color(0xFF00C853),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        // Command ID selector
                        CommandIdSelector(
                            selectedId = selectedCommandId,
                            availableIds = if (isEditMode) {
                                (availableCommandIds + command!!.commandId).distinct().sorted()
                            } else {
                                availableCommandIds
                            },
                            onIdSelected = { selectedCommandId = it }
                        )

                        // Payload hex editor
                        OutlinedTextField(
                            value = payloadHex,
                            onValueChange = { newValue ->
                                val filtered = newValue.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
                                payloadHex = filtered.take(10)
                            },
                            label = { Text("Payload (5 bytes hex)") },
                            placeholder = { Text("e.g., 0102030405") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            supportingText = {
                                Text(
                                    "${payloadHex.length}/10 hex chars (${payloadHex.length / 2} bytes)",
                                    color = if (payloadHex.length == 10) Color(0xFF00C853) else Color.Gray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00C853),
                                unfocusedBorderColor = Color(0xFF455A64),
                                focusedLabelColor = Color(0xFF00C853),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        // Toggle vs Momentary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A2A3E), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Toggle Mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    if (isToggle) "Press to toggle on/off" else "Press & hold to send",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isToggle,
                                onCheckedChange = {
                                    view.hapticTap()
                                    isToggle = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00C853),
                                    checkedTrackColor = Color(0xFF00C853).copy(alpha = 0.3f)
                                )
                            )
                        }

                        // Color picker
                        ColorPicker(
                            selectedColor = selectedColorHex,
                            onColorSelected = { selectedColorHex = it }
                        )

                        // Icon picker
                        IconPicker(
                            selectedIconName = selectedIconName,
                            onIconSelected = { selectedIconName = it }
                        )

                        // Keyboard shortcut picker
                        ShortcutPicker(
                            selectedShortcut = selectedShortcut,
                            onShortcutSelected = { selectedShortcut = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            view.hapticTap()
                            // Parse payload
                            val payload = payloadHex.padEnd(10, '0')
                                .chunked(2)
                                .map { it.toInt(16).toByte() }
                                .toByteArray()

                            val savedCommand = CustomCommand(
                                id = command?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                commandId = selectedCommandId,
                                payload = payload,
                                colorHex = selectedColorHex,
                                isToggle = isToggle,
                                iconName = selectedIconName,
                                keyboardShortcut = selectedShortcut
                            )
                            onSave(savedCommand)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853)
                        )
                    ) {
                        Text(if (isEditMode) "Save" else "Create")
                    }
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun CommandIdSelector(
    selectedId: Byte,
    availableIds: List<Byte>,
    onIdSelected: (Byte) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            "Command ID",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                color = Color(0xFF2A2A3E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "0x${"%02X".format(selectedId)}",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableIds.forEach { id ->
                    DropdownMenuItem(
                        text = { Text("0x${"%02X".format(id)}") },
                        onClick = {
                            onIdSelected(id)
                            expanded = false
                        },
                        leadingIcon = {
                            if (id == selectedId) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val colors = listOf(
        0xFF2196F3L, // Blue
        0xFF4CAF50L, // Green
        0xFFF44336L, // Red
        0xFFFF9800L, // Orange
        0xFF9C27B0L, // Purple
        0xFF00BCD4L, // Cyan
        0xFFFFEB3BL, // Yellow
        0xFF607D8BL, // Blue Grey
        0xFFE91E63L, // Pink
        0xFF795548L  // Brown
    )

    Column {
        Text(
            "Button Color",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(colorHex))
                        .then(
                            if (colorHex == selectedColor) {
                                Modifier.border(2.dp, Color.White, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onColorSelected(colorHex) }
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun IconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit
) {
    val icons = mapOf(
        "Build" to Icons.Default.Build,
        "Lightbulb" to Icons.Default.Lightbulb,
        "Notifications" to Icons.Default.Notifications,
        "PlayArrow" to Icons.Default.PlayArrow,
        "Stop" to Icons.Default.Stop,
        "VolumeUp" to Icons.Default.VolumeUp,
        "Bolt" to Icons.Default.Bolt,
        "Star" to Icons.Default.Star
    )

    Column {
        Text(
            "Button Icon",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icons.forEach { (name, icon) ->
                IconButton(
                    onClick = { onIconSelected(name) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (name == selectedIconName) Color(0xFF00C853).copy(alpha = 0.3f)
                            else Color(0xFF2A2A3E),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (name == selectedIconName) Color(0xFF00C853) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ShortcutPicker(
    selectedShortcut: Char?,
    onShortcutSelected: (Char?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableKeys = CustomCommand.AVAILABLE_SHORTCUT_KEYS

    Column {
        Text(
            "Keyboard Shortcut (Optional)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                color = Color(0xFF2A2A3E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = if (selectedShortcut != null) Color(0xFF00C853) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            selectedShortcut?.toString() ?: "None",
                            color = if (selectedShortcut != null) Color.White else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // None option
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onShortcutSelected(null)
                        expanded = false
                    },
                    leadingIcon = {
                        if (selectedShortcut == null) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00C853)
                            )
                        }
                    }
                )
                // Available keys
                availableKeys.forEach { key ->
                    DropdownMenuItem(
                        text = { Text(key.toString()) },
                        onClick = {
                            onShortcutSelected(key)
                            expanded = false
                        },
                        leadingIcon = {
                            if (key == selectedShortcut) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853)
                                )
                            }
                        }
                    )
                }
            }
        }
        Text(
            "Reserved: W, A, S, D, L, R, B, Z (servo controls)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Helper to get Material icon by name.
 */
fun getIconByName(name: String): ImageVector {
    return when (name) {
        "Build" -> Icons.Default.Build
        "Lightbulb" -> Icons.Default.Lightbulb
        "Notifications" -> Icons.Default.Notifications
        "PlayArrow" -> Icons.Default.PlayArrow
        "Stop" -> Icons.Default.Stop
        "VolumeUp" -> Icons.Default.VolumeUp
        "Bolt" -> Icons.Default.Bolt
        "Star" -> Icons.Default.Star
        else -> Icons.Default.Build
    }
}
