package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.ui.utils.hapticTap

@Suppress("FunctionName")
@Composable
fun StatusCard(
    label: String,
    state: ConnectionState,
    @Suppress("UNUSED_PARAMETER") rssi: Int,
    hasCrashLog: Boolean = false,
    onClick: () -> Unit,
    onCrashLogClick: () -> Unit = {}
) {
    val view = LocalView.current

    val stateText = when (state) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.RECONNECTING -> "Reconnecting..."
        ConnectionState.ERROR -> "Error"
        ConnectionState.DISCONNECTED -> "Disconnected"
    }

    // Soft filled background for connected, pale for disconnected/error
    val containerColor = when (state) {
        ConnectionState.CONNECTED -> Color(0xFFFFFF00).copy(alpha = 0.2f) // Electric Yellow shadow
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(
            0xFFFF9800
        ).copy(alpha = 0.2f) // Orange shadow
        else -> Color.Transparent // Pale (no fill)
    }

    val contentColor = when (state) {
        ConnectionState.CONNECTED -> Color(0xFFFFFF00) // Electric Yellow text
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800) // Orange text
        ConnectionState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.6f) // Pale red
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E) // Pale gray
    }

    val borderColor = when (state) {
        ConnectionState.CONNECTED -> Color(0xFFFFFF00) // Electric Yellow border
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFF9800) // Orange border
        ConnectionState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.6f) // Pale red border
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E).copy(alpha = 0.5f) // Very pale gray border
    }

    androidx.compose.material3.Surface(
        color = containerColor,
        contentColor = contentColor,
        // Fully rounded like Button
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(48.dp)
            .semantics { contentDescription = "$label status: $stateText" }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = if (hasCrashLog) 4.dp else 12.dp)
        ) {
            // Main status text - clickable
            Row(
                modifier = Modifier
                    .clickable {
                        view.hapticTap()
                        onClick()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: $stateText",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (hasCrashLog) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.IconButton(
                    onClick = {
                        view.hapticTap()
                        onCrashLogClick()
                    },
                    // 48dp minimum touch target for accessibility
                    modifier = Modifier.size(48.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                        contentDescription = "View Crash Log",
                        // Orange warning color
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
