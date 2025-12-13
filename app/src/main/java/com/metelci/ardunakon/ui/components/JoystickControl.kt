package com.metelci.ardunakon.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class JoystickState(
    val x: Float, // -1.0 to 1.0
    val y: Float // -1.0 to 1.0
)

/**
 * Enhanced joystick control with haptic feedback and connection quality visualization.
 *
 * @param modifier Modifier for the composable
 * @param size Total size of the joystick area
 * @param dotSize Size of the movable knob
 * @param backgroundColor Color of the joystick base
 * @param stickColor Color of the movable knob
 * @param isThrottle If true, Y axis doesn't auto-center on release
 * @param enableHaptics Enable haptic feedback on deadzone entry and edge hit
 * @param connectionLatencyMs Current connection latency in milliseconds for quality ring (null = hide ring)
 * @param onMoved Callback when joystick position changes
 */
@Composable
fun JoystickControl(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    dotSize: Dp = 50.dp,
    backgroundColor: Color = Color.DarkGray,
    stickColor: Color = Color.Cyan,
    isThrottle: Boolean = false,
    enableHaptics: Boolean = true,
    connectionLatencyMs: Long? = null,
    onMoved: (JoystickState) -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val center = remember(size, density) {
        with(density) { Offset(size.toPx() / 2, size.toPx() / 2) }
    }
    val radius = remember(size, dotSize, density) {
        with(density) { (size.toPx() / 2) - (dotSize.toPx() / 2) }
    }

    // Initialize position at center
    val initialPosition = remember(center) { center }
    var knobPosition by remember(initialPosition) { mutableStateOf(initialPosition) }

    // Haptic feedback state
    var wasInDeadzone by remember { mutableStateOf(true) }
    var wasAtEdge by remember { mutableStateOf(false) }

    // Calculate connection quality ring color
    val qualityRingColor = connectionLatencyMs?.let { latency ->
        when {
            latency < 50 -> Color(0xFF00E676) // Green - Excellent
            latency < 100 -> Color(0xFFFFD54F) // Yellow - Good
            latency < 200 -> Color(0xFFFF9800) // Orange - Fair
            else -> Color(0xFFFF5252) // Red - Poor
        }
    }

    // Notify initial position on composition/mode change
    LaunchedEffect(isThrottle) {
        val normalizedX = (knobPosition.x - center.x) / radius
        val normalizedY = -(knobPosition.y - center.y) / radius
        onMoved(
            JoystickState(
                normalizedX.coerceIn(-1f, 1f),
                normalizedY.coerceIn(-1f, 1f)
            )
        )
    }

    Box(modifier = modifier.size(size)) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downId = down.id

                        fun updatePosition(pointerPosition: Offset) {
                            val newPos = pointerPosition
                            val distance = sqrt(
                                (newPos.x - center.x).pow(2) + (newPos.y - center.y).pow(2)
                            )

                            val inDeadzone = distance < radius * 0.05f
                            val atEdge = distance >= radius * 0.95f

                            // Haptic feedback on state transitions
                            if (enableHaptics) {
                                // Feedback when entering deadzone (centering)
                                if (inDeadzone && !wasInDeadzone) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                // Feedback when hitting the edge
                                if (atEdge && !wasAtEdge) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                            }

                            wasInDeadzone = inDeadzone
                            wasAtEdge = atEdge

                            if (inDeadzone) {
                                knobPosition = center
                            } else if (distance <= radius) {
                                knobPosition = newPos
                            } else {
                                val angle = atan2(newPos.y - center.y, newPos.x - center.x)
                                knobPosition = Offset(
                                    center.x + cos(angle) * radius,
                                    center.y + sin(angle) * radius
                                )
                            }

                            // Normalize output
                            val normalizedX = (knobPosition.x - center.x) / radius
                            val normalizedY = -(knobPosition.y - center.y) / radius // -1 (bottom) to 1 (top)

                            onMoved(JoystickState(normalizedX.coerceIn(-1f, 1f), normalizedY.coerceIn(-1f, 1f)))
                        }

                        // Handle initial touch
                        updatePosition(down.position)
                        down.consume()

                        // Handle drag
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == downId }
                            if (change != null && change.pressed) {
                                updatePosition(change.position)
                                change.consume()
                            } else {
                                break // Finger lifted
                            }
                        } while (true)

                        // On Release
                        if (!isThrottle) {
                            knobPosition = center
                            wasInDeadzone = true
                            wasAtEdge = false
                            onMoved(JoystickState(0f, 0f))
                        } else {
                            // Only auto-center X axis, keep Y
                            knobPosition = knobPosition.copy(x = center.x)
                            wasInDeadzone = false
                            wasAtEdge = false

                            // Recalculate output on release
                            val normalizedY = (-(knobPosition.y - center.y) / radius).coerceIn(-1f, 1f)
                            onMoved(JoystickState(0f, normalizedY))
                        }
                    }
                }
        ) {
            val sizePx = size.toPx()
            val ringWidth = 4.dp.toPx()

            // Draw Connection Quality Ring (outer ring) if latency provided
            qualityRingColor?.let { ringColor ->
                drawCircle(
                    color = ringColor.copy(alpha = 0.7f),
                    radius = sizePx / 2 - ringWidth / 2,
                    center = center,
                    style = Stroke(width = ringWidth)
                )
            }

            // Draw Base (main background)
            drawCircle(
                color = backgroundColor,
                radius = sizePx / 2 - (if (qualityRingColor != null) ringWidth + 2.dp.toPx() else 0f),
                center = center
            )

            // Draw Stick/Knob
            drawCircle(
                color = stickColor,
                radius = dotSize.toPx() / 2,
                center = knobPosition
            )
        }
    }
}
