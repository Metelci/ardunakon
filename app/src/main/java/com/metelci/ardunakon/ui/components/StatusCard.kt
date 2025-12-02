package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.ConnectionState

@Composable
fun StatusCard(label: String, state: ConnectionState, @Suppress("UNUSED_PARAMETER") rssi: Int, onClick: () -> Unit, @Suppress("UNUSED_PARAMETER") isDarkTheme: Boolean) {
    val view = LocalView.current
    // Electric yellow for all states
    val color = Color(0xFFFFFF00)
    val stateText = when(state) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.RECONNECTING -> "Reconnecting..."
        ConnectionState.ERROR -> "Error"
        ConnectionState.DISCONNECTED -> "Disconnected"
    }

    OutlinedButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = Modifier
            .height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        ),
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$label: $stateText",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
