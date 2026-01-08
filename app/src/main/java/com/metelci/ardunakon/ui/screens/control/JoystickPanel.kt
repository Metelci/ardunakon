package com.metelci.ardunakon.ui.screens.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.PrecisionSlider

/**
 * Wrapper component for JoystickControl that handles latency calculation for connection quality ring.
 * Reduces duplication between portrait and landscape layouts.
 *
 * @param onMoved Callback with (x, y) values -1 to 1
 * @param size Size of the joystick
 * @param isThrottle Whether Y-axis should stay at last position (throttle mode)
 * @param bluetoothRttMs Bluetooth round-trip time in milliseconds (from health.lastRttMs)
 * @param wifiRttMs WiFi round-trip time in milliseconds
 * @param isWifiMode Whether currently in WiFi mode
 * @param showPrecisionSlider Whether to show the precision Z-axis slider
 * @param precisionValue Current precision slider value (-1 to 1)
 * @param onPrecisionChange Callback when precision slider changes
 * @param modifier Optional modifier for the container Box
 * @param contentAlignment Alignment of the joystick within the container
 */
@Suppress("FunctionName")
@Composable
fun JoystickPanel(
    onMoved: (x: Float, y: Float) -> Unit,
    size: Dp = 180.dp,
    isThrottle: Boolean = false,
    bluetoothRttMs: Long? = null,
    wifiRttMs: Long = 0L,
    isWifiMode: Boolean = false,
    showPrecisionSlider: Boolean = false,
    precisionValue: Float = 0f,
    onPrecisionChange: (Float) -> Unit = {},
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            JoystickControl(
                onMoved = { state ->
                    onMoved(state.x, state.y)
                },
                size = size,
                isThrottle = isThrottle,
                connectionLatencyMs = currentLatency
            )

            // Precision slider for Z-axis (servo fine control)
            if (showPrecisionSlider) {
                Spacer(modifier = Modifier.width(12.dp))
                PrecisionSlider(
                    value = precisionValue,
                    onValueChange = onPrecisionChange,
                    label = "Z",
                    height = size * 0.85f,
                    width = 44.dp,
                    thumbSize = 28.dp
                )
            }
        }
    }
}
