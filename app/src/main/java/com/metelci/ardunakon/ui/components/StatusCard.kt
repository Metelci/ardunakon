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

    val stateText = when(state) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.RECONNECTING -> "Reconnecting..."
        ConnectionState.ERROR -> "Error"
        ConnectionState.DISCONNECTED -> "Disconnected"
    }

    // Soft filled background for connected, pale for disconnected/error
    val containerColor = when(state) {
        ConnectionState.CONNECTED -> Color(0xFF43A047).copy(alpha = 0.3f) // Soft green shadow
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFEB3B).copy(alpha = 0.2f) // Soft yellow
        else -> Color.Transparent // Pale (no fill)
    }

    val contentColor = when(state) {
        ConnectionState.CONNECTED -> Color(0xFF66BB6A) // Bright green text
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFFF00) // Electric yellow
        ConnectionState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.6f) // Pale red
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E) // Pale gray
    }

    val borderColor = when(state) {
        ConnectionState.CONNECTED -> Color(0xFF66BB6A) // Green border
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFFF00) // Yellow border
        ConnectionState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.6f) // Pale red border
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E).copy(alpha = 0.5f) // Very pale gray border
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
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, borderColor)
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
