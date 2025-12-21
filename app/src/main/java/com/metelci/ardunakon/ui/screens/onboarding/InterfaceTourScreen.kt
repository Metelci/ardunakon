package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.model.InterfaceElement

/**
 * Phase 2: Interface tour screen.
 * Guides users through the main UI elements with highlighted overlays.
 */
@Suppress("FunctionName")
@Composable
fun InterfaceTourScreen(
    element: InterfaceElement,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress indicator at top
            OnboardingProgress(progress = progress)

            Spacer(modifier = Modifier.weight(1f))

            // Element highlight visualization
            ElementHighlight(element = element)

            Spacer(modifier = Modifier.weight(1f))

            // Tutorial card at bottom
            TutorialCard(
                title = element.displayName,
                description = element.description,
                icon = getElementIcon(element),
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip,
                showBackButton = element != InterfaceElement.tourOrder().first()
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ElementHighlight(element: InterfaceElement, modifier: Modifier = Modifier) {
    // Pulse animation for the highlight
    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Simulated UI element representation
        Surface(
            modifier = Modifier
                .scale(scale)
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00C853).copy(alpha = borderAlpha),
                            Color(0xFF00E676).copy(alpha = borderAlpha * 0.7f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = getElementIcon(element),
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = element.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
fun TutorialCard(
    title: String,
    description: String,
    icon: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    showBackButton: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
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
                    text = icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = description,
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }

                    // Next button
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853)
                        )
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
fun OnboardingProgress(progress: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 32.dp),
            color = Color(0xFF00C853),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(progress * 100).toInt()}% complete",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getElementIcon(element: InterfaceElement): String {
    return when (element) {
        InterfaceElement.ESTOP -> "🔴"
        InterfaceElement.CONNECTION_STATUS -> "📶"
        InterfaceElement.LEFT_JOYSTICK -> "🎮"
        InterfaceElement.SERVO_CONTROLS -> "🤖"
        InterfaceElement.CONNECTION_MODE -> "🔄"
    }
}
