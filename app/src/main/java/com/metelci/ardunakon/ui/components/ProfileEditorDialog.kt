package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.metelci.ardunakon.data.Profile
import com.metelci.ardunakon.model.ButtonConfig
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import java.util.UUID

@Composable
fun ProfileEditorDialog(
    profile: Profile?,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit
) {
    val view = LocalView.current
    var name by remember { mutableStateOf(profile?.name ?: "New Profile") }
    var sensitivity by remember { mutableStateOf(profile?.sensitivity ?: 1.0f) }
    var isUnidirectional by remember { mutableStateOf(profile?.isThrottleUnidirectional ?: false) }
    var buttonConfigs by remember { 
        mutableStateOf(profile?.buttonConfigs ?: com.metelci.ardunakon.model.defaultButtonConfigs) 
    }
    val pastelBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFCE4EC),
                Color(0xFFE3F2FD),
                Color(0xFFE8F5E9)
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (profile == null) "Create Profile" else "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF74B9FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Sensitivity
                Text("Joystick Sensitivity: ${(sensitivity * 100).toInt()}%", color = Color(0xFFB2BEC3))
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.1f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF74B9FF),
                        activeTrackColor = Color(0xFF74B9FF)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Throttle Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Unidirectional Throttle (0-100%)",
                        color = Color(0xFFB2BEC3),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isUnidirectional,
                        onCheckedChange = { isUnidirectional = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF74B9FF),
                            checkedTrackColor = Color(0xFF2D3436)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Text("Button Configuration", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(buttonConfigs) { index, btn ->
                        ButtonConfigRow(btn) { newBtn ->
                            val newList = buttonConfigs.toMutableList()
                            newList[index] = newBtn
                            buttonConfigs = newList
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .background(pastelBrush, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                    ) {
                        Text("Cancel", color = Color(0xFF2D3436))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val newProfile = Profile(
                                id = profile?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                buttonConfigs = buttonConfigs,
                                isThrottleUnidirectional = isUnidirectional,
                                sensitivity = sensitivity
                            )
                            onSave(newProfile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .background(pastelBrush, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                    ) {
                        Text("Save", color = Color(0xFF2D3436))
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonConfigRow(config: ButtonConfig, onUpdate: (ButtonConfig) -> Unit) {
    var label by remember(config.label) { mutableStateOf(config.label) }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF353B48), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Color Indicator (Click to cycle colors - simplified for now)
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(config.colorHex), RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedTextField(
            value = label,
            onValueChange = { 
                label = it
                onUpdate(config.copy(label = it))
            },
            label = { Text("Label", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}
