package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class JoystickState(
    val x: Float, // -1.0 to 1.0
    val y: Float  // -1.0 to 1.0
)

@Composable
fun JoystickControl(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    dotSize: Dp = 50.dp,
    backgroundColor: Color = Color.DarkGray,
    stickColor: Color = Color.Cyan,
    isThrottle: Boolean = false, // If true, Y axis doesn't auto-center
    isUnidirectional: Boolean = false, // If true, Y axis maps 0.0 (bottom) to 1.0 (top)
    onMoved: (JoystickState) -> Unit
) {
    val density = LocalDensity.current
    val center = remember(size, density) { 
        with(density) { Offset(size.toPx() / 2, size.toPx() / 2) } 
    }
    val radius = remember(size, dotSize, density) { 
        with(density) { (size.toPx() / 2) - (dotSize.toPx() / 2) } 
    }

    var knobPosition by remember { mutableStateOf(center) }

    // Initialize position for unidirectional throttle if needed
    LaunchedEffect(isUnidirectional, radius) {
        if (isUnidirectional && knobPosition == center) {
            knobPosition = Offset(center.x, center.y + radius)
        }
    }

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier
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

                        if (distance < radius * 0.05f) { // 5% Deadzone
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
                        val rawY = -(knobPosition.y - center.y) / radius // -1 (bottom) to 1 (top)
                        
                        val finalY = if (isUnidirectional) {
                            ((rawY + 1) / 2).coerceIn(0f, 1f)
                        } else {
                            rawY.coerceIn(-1f, 1f)
                        }

                        onMoved(JoystickState(normalizedX.coerceIn(-1f, 1f), finalY))
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
                        onMoved(JoystickState(0f, 0f))
                    } else {
                        // Only auto-center X axis, keep Y
                        knobPosition = knobPosition.copy(x = center.x)
                        
                        // Recalculate output on release
                        val rawY = -(knobPosition.y - center.y) / radius
                        val finalY = if (isUnidirectional) {
                            ((rawY + 1) / 2).coerceIn(0f, 1f)
                        } else {
                            rawY.coerceIn(-1f, 1f)
                        }
                        onMoved(JoystickState(0f, finalY))
                    }
                }
            }
        ) {
            // Draw Base
            drawCircle(
                color = backgroundColor,
                radius = size.toPx() / 2,
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
