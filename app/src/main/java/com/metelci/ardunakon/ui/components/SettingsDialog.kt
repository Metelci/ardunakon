@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metelci.ardunakon.ui.utils.hapticTap

/**
 * Settings dialog for app configuration.
 * Contains: Debug toggle, Joystick sensitivity, Legacy reflection, Custom commands, Reset tutorial.
 */
@Suppress("FunctionName")
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    view: View,

    // Debug panel
    isDebugPanelVisible: Boolean,
    onToggleDebugPanel: () -> Unit,

    // Haptic feedback
    isHapticEnabled: Boolean,
    onToggleHaptic: () -> Unit,

    // Joystick sensitivity
    joystickSensitivity: Float,
    onJoystickSensitivityChange: (Float) -> Unit,

    // Legacy reflection
    allowReflection: Boolean,
    onToggleReflection: () -> Unit,

    // Custom commands
    customCommandCount: Int = 0,
    onShowCustomCommands: () -> Unit = {},

    // Reset tutorial
    onResetTutorial: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()
    val singleScrollState = rememberScrollState()

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
                .fillMaxHeight(if (isLandscape) 0.85f else 0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2E)
            ),
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF00FF00),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Settings",
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

                // Content - Two columns in landscape, single column in portrait
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(leftScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Debug Window Toggle
                            SettingsSection(
                                icon = Icons.Default.BugReport,
                                title = "Debug Window",
                                subtitle = if (isDebugPanelVisible) "Visible" else "Hidden",
                                trailing = {
                                    Switch(
                                        checked = isDebugPanelVisible,
                                        onCheckedChange = {
                                            view.hapticTap()
                                            onToggleDebugPanel()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FF00),
                                            checkedTrackColor = Color(0xFF00FF00).copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            )

                            Divider(color = Color(0xFF333333))

                            // Custom Commands
                            SettingsSection(
                                icon = Icons.Default.Extension,
                                title = "Custom Commands",
                                subtitle = if (customCommandCount > 0) {
                                    "$customCommandCount configured"
                                } else {
                                    "Create custom device commands"
                                },
                                onClick = {
                                    view.hapticTap()
                                    onShowCustomCommands()
                                    onDismiss()
                                }
                            )

                            Divider(color = Color(0xFF333333))

                            // Haptic Feedback Toggle
                            SettingsSection(
                                icon = Icons.Default.Vibration,
                                title = "Haptic Feedback",
                                subtitle = if (isHapticEnabled) "Enabled" else "Disabled",
                                trailing = {
                                    Switch(
                                        checked = isHapticEnabled,
                                        onCheckedChange = {
                                            if (isHapticEnabled) {
                                                view.hapticTap()
                                            }
                                            onToggleHaptic()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFF9800),
                                            checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            )
                        }

                        // Vertical divider
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = Color(0xFF455A64)
                        )

                        // Right column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rightScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Joystick Sensitivity Slider (compact for landscape)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2A2A3E), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gamepad,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Sensitivity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "${"%.1f".format(joystickSensitivity)}x",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00C853)
                                    )
                                }
                                Slider(
                                    value = joystickSensitivity,
                                    onValueChange = onJoystickSensitivityChange,
                                    valueRange = 0.5f..2.0f,
                                    steps = 14,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00C853),
                                        activeTrackColor = Color(0xFF00C853),
                                        inactiveTrackColor = Color(0xFF455A64)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0.5x", fontSize = 9.sp, color = Color.Gray)
                                    Text("2.0x", fontSize = 9.sp, color = Color.Gray)
                                }
                            }

                            Divider(color = Color(0xFF333333))

                            // Legacy Reflection Toggle
                            SettingsSection(
                                icon = Icons.Default.Bluetooth,
                                title = "Legacy Reflection (HC-06)",
                                subtitle = if (allowReflection) "Enabled for Xiaomi/MIUI devices" else "Disabled",
                                trailing = {
                                    Switch(
                                        checked = allowReflection,
                                        onCheckedChange = {
                                            view.hapticTap()
                                            onToggleReflection()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFFD54F),
                                            checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            )

                            Divider(color = Color(0xFF333333))

                            // Reset Tutorial
                            SettingsSection(
                                icon = Icons.Default.Refresh,
                                title = "Reset Tutorial",
                                subtitle = "Show onboarding tutorial again",
                                onClick = {
                                    view.hapticTap()
                                    onResetTutorial()
                                    onDismiss()
                                }
                            )
                        }
                    }
                } else {
                    // Portrait mode - single column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(singleScrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Debug Window Toggle
                        SettingsSection(
                            icon = Icons.Default.BugReport,
                            title = "Debug Window",
                            subtitle = if (isDebugPanelVisible) "Visible" else "Hidden",
                            trailing = {
                                Switch(
                                    checked = isDebugPanelVisible,
                                    onCheckedChange = {
                                        view.hapticTap()
                                        onToggleDebugPanel()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00FF00),
                                        checkedTrackColor = Color(0xFF00FF00).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        )

                        Divider(color = Color(0xFF333333))

                        // Custom Commands (top priority for quick access)
                        SettingsSection(
                            icon = Icons.Default.Extension,
                            title = "Custom Commands",
                            subtitle = if (customCommandCount > 0) {
                                "$customCommandCount configured"
                            } else {
                                "Create custom device commands"
                            },
                            onClick = {
                                view.hapticTap()
                                onShowCustomCommands()
                                onDismiss()
                            }
                        )

                        Divider(color = Color(0xFF333333))

                        // Haptic Feedback Toggle
                        SettingsSection(
                            icon = Icons.Default.Vibration,
                            title = "Haptic Feedback",
                            subtitle = if (isHapticEnabled) "Enabled" else "Disabled",
                            trailing = {
                                Switch(
                                    checked = isHapticEnabled,
                                    onCheckedChange = {
                                        if (isHapticEnabled) {
                                            view.hapticTap()
                                        }
                                        onToggleHaptic()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFF9800),
                                        checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        )

                        Divider(color = Color(0xFF333333))

                        // Joystick Sensitivity Slider (compact)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A2A3E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Sensitivity",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "${"%.1f".format(joystickSensitivity)}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00C853)
                                )
                            }
                            Slider(
                                value = joystickSensitivity,
                                onValueChange = onJoystickSensitivityChange,
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00C853),
                                    activeTrackColor = Color(0xFF00C853),
                                    inactiveTrackColor = Color(0xFF455A64)
                                ),
                                modifier = Modifier.height(32.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0.5x", fontSize = 9.sp, color = Color.Gray)
                                Text("2.0x", fontSize = 9.sp, color = Color.Gray)
                            }
                        }

                        Divider(color = Color(0xFF333333))

                        // Legacy Reflection Toggle
                        SettingsSection(
                            icon = Icons.Default.Bluetooth,
                            title = "Legacy Reflection (HC-06)",
                            subtitle = if (allowReflection) "Enabled for Xiaomi/MIUI devices" else "Disabled",
                            trailing = {
                                Switch(
                                    checked = allowReflection,
                                    onCheckedChange = {
                                        view.hapticTap()
                                        onToggleReflection()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFD54F),
                                        checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        )

                        Divider(color = Color(0xFF333333))

                        // Reset Tutorial
                        SettingsSection(
                            icon = Icons.Default.Refresh,
                            title = "Reset Tutorial",
                            subtitle = "Show onboarding tutorial again",
                            onClick = {
                                view.hapticTap()
                                onResetTutorial()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
