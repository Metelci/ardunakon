package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.model.InterfaceElement

/**
 * Key for identifying elements that can be highlighted during the interface tour.
 */
object HighlightTags {
    const val ESTOP = "highlight_estop"
    const val CONNECTION_STATUS = "highlight_connection_status"
    const val JOYSTICK = "highlight_joystick"
    const val SERVO_CONTROLS = "highlight_servo_controls"
    const val CONNECTION_MODE = "highlight_connection_mode"

    fun tagFor(element: InterfaceElement): String = when (element) {
        InterfaceElement.ESTOP -> ESTOP
        InterfaceElement.CONNECTION_STATUS -> CONNECTION_STATUS
        InterfaceElement.LEFT_JOYSTICK -> JOYSTICK
        InterfaceElement.SERVO_CONTROLS -> SERVO_CONTROLS
        InterfaceElement.CONNECTION_MODE -> CONNECTION_MODE
    }
}

/**
 * State holder for element bounds during interface tour.
 * Used to track positions of highlightable elements.
 */
class HighlightState {
    var elementBounds by mutableStateOf<Map<String, Rect>>(emptyMap())
        private set

    fun updateBounds(tag: String, bounds: Rect) {
        elementBounds = elementBounds + (tag to bounds)
    }

    fun getBounds(element: InterfaceElement): Rect? {
        return elementBounds[HighlightTags.tagFor(element)]
    }
}

/**
 * Remember a HighlightState that can be passed to child composables.
 */
@Composable
fun rememberHighlightState(): HighlightState {
    return remember { HighlightState() }
}

/**
 * Modifier extension to register an element for highlighting.
 */
fun Modifier.highlightable(state: HighlightState, tag: String): Modifier =
    this.onGloballyPositioned { layoutCoordinates ->
        val bounds = layoutCoordinates.boundsInRoot()
        state.updateBounds(tag, bounds)
    }

/**
 * Overlay that highlights a specific element with a cutout effect.
 * Draws a semi-transparent dark overlay with a transparent hole over the target element.
 */
@Suppress("FunctionName")
@Composable
fun HighlightOverlay(
    targetRect: androidx.compose.ui.geometry.Rect?,
    text: String,
    // "top", "bottom", "left", "right"
    position: String = "bottom",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrimColor: Color = Color.Black.copy(alpha = 0.8f)
    val cornerRadius: Float = 16f

    // Pulse animation for the highlight border
    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val borderScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_scale"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // Required for BlendMode.Clear to work
            .graphicsLayer(alpha = 0.99f)
    ) {
        // Draw the scrim
        drawRect(color = scrimColor)

        targetRect?.let { bounds ->
            // Add some padding around the element
            val padding = 8f
            val highlightRect = Rect(
                offset = Offset(bounds.left - padding, bounds.top - padding),
                size = Size(bounds.width + 2 * padding, bounds.height + 2 * padding)
            )

            // Clear a rounded rectangle hole for the highlighted element
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(highlightRect.left, highlightRect.top),
                size = Size(highlightRect.width, highlightRect.height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                blendMode = BlendMode.Clear
            )

            // Draw a pulsing border around the highlighted element
            val scaledPadding = padding * borderScale
            val borderRect = Rect(
                offset = Offset(bounds.left - scaledPadding, bounds.top - scaledPadding),
                size = Size(bounds.width + 2 * scaledPadding, bounds.height + 2 * scaledPadding)
            )

            drawRoundRect(
                color = Color(0xFF00C853).copy(alpha = pulseAlpha),
                topLeft = Offset(borderRect.left, borderRect.top),
                size = Size(borderRect.width, borderRect.height),
                cornerRadius = CornerRadius(cornerRadius + 4, cornerRadius + 4),
                style = Stroke(width = 4f)
            )
        }
    }
}

/**
 * Tutorial card that appears during interface tour, showing info about the highlighted element.
 * Positioned at the bottom of the screen.
 */
@Suppress("FunctionName")
@Composable
fun TourInfoCard(
    element: InterfaceElement,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    showBackButton: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFF00C853),
            trackColor = Color.White.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Title with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getElementEmoji(element),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = element.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = element.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        // Back button (conditional)
                        if (showBackButton) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Back")
                            }
                        }

                        // Next button
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00C853)
                            )
                        ) {
                            Text("Next →")
                        }
                    }
                }
            }
        }
    }
}

private fun getElementEmoji(element: InterfaceElement): String = when (element) {
    InterfaceElement.ESTOP -> "🔴"
    InterfaceElement.CONNECTION_STATUS -> "📶"
    InterfaceElement.LEFT_JOYSTICK -> "🎮"
    InterfaceElement.SERVO_CONTROLS -> "🤖"
    InterfaceElement.CONNECTION_MODE -> "🔄"
}
