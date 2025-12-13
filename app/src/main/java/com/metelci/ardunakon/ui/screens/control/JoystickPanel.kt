package com.metelci.ardunakon.ui.screens.control

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.ui.components.JoystickControl

/**
 * Wrapper component for JoystickControl that handles latency calculation for connection quality ring.
 * Reduces duplication between portrait and landscape layouts.
 *
 * @param onMoved Callback with (x, y) values -1 to 1, already multiplied by sensitivity
 * @param size Size of the joystick
 * @param isThrottle Whether Y-axis should stay at last position (throttle mode)
 * @param bluetoothRttMs Bluetooth round-trip time in milliseconds (from health.lastRttMs)
 * @param wifiRttMs WiFi round-trip time in milliseconds
 * @param isWifiMode Whether currently in WiFi mode
 * @param sensitivity Profile sensitivity multiplier (default 1.0)
 * @param modifier Optional modifier for the container Box
 * @param contentAlignment Alignment of the joystick within the container
 */
@Composable
fun JoystickPanel(
    onMoved: (x: Float, y: Float) -> Unit,
    size: Dp = 180.dp,
    isThrottle: Boolean = false,
    bluetoothRttMs: Long? = null,
    wifiRttMs: Long = 0L,
    isWifiMode: Boolean = false,
    sensitivity: Float = 1.0f,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center
) {
    // Calculate current connection latency for quality ring
    val currentLatency: Long? = if (isWifiMode) {
        wifiRttMs.takeIf { it > 0 }
    } else {
        bluetoothRttMs?.takeIf { it > 0 }
    }

    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        JoystickControl(
            onMoved = { state ->
                onMoved(
                    state.x * sensitivity,
                    state.y * sensitivity
                )
            },
            size = size,
            isThrottle = isThrottle,
            connectionLatencyMs = currentLatency
        )
    }
}
