package com.metelci.ardunakon.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

@Composable
fun AutoReconnectToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val view = LocalView.current

    IconButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onToggle(!enabled)
        },
        modifier = Modifier
            .size(36.dp)
            .shadow(2.dp, CircleShape)
            .background(
                if (enabled) Color(0xFF43A047) else Color(0xFF455A64),
                CircleShape
            )
            .border(1.dp, Color(0xFF00FF00), CircleShape)
    ) {
        Icon(
            imageVector = if (enabled) Icons.Filled.Sync else Icons.Filled.SyncDisabled,
            contentDescription = "Auto-reconnect ${if (enabled) "enabled" else "disabled"}",
            tint = if (enabled) Color.White else Color(0xFFFF5252),
            modifier = Modifier.size(20.dp)
        )
    }
}
