package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
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
    var knobPosition by remember { mutableStateOf(Offset.Zero) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }

    // Initial setup for throttle stick (start at bottom if unidirectional)
    LaunchedEffect(center, isUnidirectional) {
        if (center != Offset.Zero && isUnidirectional && knobPosition == center) {
            knobPosition = Offset(center.x, center.y + radius) // Start at bottom
        }
    }

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (!isThrottle) {
                            // Auto-center both axes
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
                ) { change, dragAmount ->
                    change.consume()
                    
                    val newPos = knobPosition + dragAmount
                    val distance = sqrt(
                        (newPos.x - center.x).pow(2) + (newPos.y - center.y).pow(2)
                    )

                    if (distance <= radius) {
                        knobPosition = newPos
                    } else {
                        // Constrain to circle
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
                        // Map [-1, 1] to [0, 1]
                        ((rawY + 1) / 2).coerceIn(0f, 1f)
                    } else {
                        rawY.coerceIn(-1f, 1f)
                    }

                    onMoved(JoystickState(normalizedX.coerceIn(-1f, 1f), finalY))
                }
            }
        ) {
            // Initialize center and radius
            if (center == Offset.Zero) {
                center = Offset(size.toPx() / 2, size.toPx() / 2)
                radius = (size.toPx() / 2) - (dotSize.toPx() / 2)
                
                // Set initial position based on mode
                knobPosition = if (isUnidirectional) {
                    Offset(center.x, center.y + radius) // Bottom
                } else {
                    center
                }
            }

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
