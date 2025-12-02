package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.model.AssignedAux
import com.metelci.ardunakon.protocol.ProtocolManager

@Composable
fun AuxButton(assigned: AssignedAux, manager: AppBluetoothManager) {
    val view = androidx.compose.ui.platform.LocalView.current
    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 64.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2980B9), // Deep Blue
                        Color(0xFF6DD5FA)  // Cyan
                    )
                )
            )
            .clickable {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                val data = ProtocolManager.formatButtonData(assigned.servoId, true)
                manager.sendDataToSlot(data, assigned.slot)
            }
            .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val roleSuffix = if (assigned.role.isNotBlank()) " - ${assigned.role}" else ""
        Text(
            "${assigned.config.label} (S${assigned.slot + 1}/Servo ${assigned.servoId})$roleSuffix",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
