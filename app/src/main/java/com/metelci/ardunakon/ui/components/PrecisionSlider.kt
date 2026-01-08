package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.ui.utils.hapticTap
import kotlin.math.roundToInt

/**
 * Precision vertical slider for fine-grained servo control.
 * Provides millimeter-level adjustment capability next to the joystick.
 *
 * @param value Current value (-1f to 1f)
 * @param onValueChange Callback when value changes
 * @param label Label to display above the slider (e.g., "Z" or "AUX")
 * @param height Total height of the slider track
 * @param width Width of the slider
 * @param thumbSize Size of the draggable thumb
 * @param modifier Optional modifier
 */
@Suppress("FunctionName")
@Composable
fun PrecisionSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String = "Z",
    height: Dp = 160.dp,
    width: Dp = 48.dp,
    thumbSize: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    // Convert height to pixels for calculations
    val trackHeightPx = with(density) { height.toPx() }
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val usableTrackPx = trackHeightPx - thumbSizePx

    // Calculate thumb offset from value (-1 to 1 -> bottom to top)
    // Value 1 = top, Value -1 = bottom
    var dragOffset by remember(value) {
        mutableFloatStateOf((1f - value) / 2f * usableTrackPx)
    }

    // Track colors based on value
    val trackGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00C853).copy(alpha = 0.6f),  // Top - max
            Color(0xFF455A64).copy(alpha = 0.4f),  // Middle - center
            Color(0xFFFF5252).copy(alpha = 0.6f)   // Bottom - min
        )
    )

    val thumbColor = when {
        value > 0.5f -> Color(0xFF00E676)   // Green for high values
        value < -0.5f -> Color(0xFFFF5252)  // Red for low values
        else -> Color(0xFFFFD600)           // Yellow for center range
    }

    // Calculate display angle (0-180°)
    val displayAngle = ((value + 1f) / 2f * 180f).toInt()

    Column(
        modifier = modifier
            .width(width)
            .semantics { contentDescription = "$label precision slider at $displayAngle degrees" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Label
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FF00)
        )

        // Angle display
        Text(
            text = "${displayAngle}°",
            fontSize = 10.sp,
            color = Color.Gray
        )

        // Slider track with thumb
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(width / 2))
                .background(trackGradient)
                .border(1.dp, Color(0xFF00FF00).copy(alpha = 0.5f), RoundedCornerShape(width / 2))
        ) {
            // Center line indicator
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(width - 8.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Draggable thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                    .padding(horizontal = 4.dp)
                    .size(width - 8.dp, thumbSize)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            val newOffset = (dragOffset + delta).coerceIn(0f, usableTrackPx)
                            dragOffset = newOffset

                            // Convert offset to value (inverted: top = 1, bottom = -1)
                            val newValue = 1f - (newOffset / usableTrackPx * 2f)

                            // Haptic feedback at center and edges
                            if ((newValue > -0.05f && newValue < 0.05f && (value <= -0.05f || value >= 0.05f)) ||
                                (newValue >= 0.95f && value < 0.95f) ||
                                (newValue <= -0.95f && value > -0.95f)
                            ) {
                                view.hapticTap()
                            }

                            onValueChange(newValue.coerceIn(-1f, 1f))
                        },
                        onDragStopped = {
                            view.hapticTap()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Thumb grip lines
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(thumbSize * 0.5f)
                                .height(2.dp)
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // Min/Max labels
        Text(
            text = "MIN",
            fontSize = 8.sp,
            color = Color(0xFFFF5252).copy(alpha = 0.7f)
        )
    }
}
