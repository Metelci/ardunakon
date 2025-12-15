package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.model.InterfaceElement

/**
 * Real Screen Tour - Shows the DemoControlLayout with highlighted elements
 * and tutorial information overlays.
 */
@Composable
fun RealScreenTour(
    currentElement: InterfaceElement,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val tourOrder = InterfaceElement.tourOrder()
    val currentIndex = tourOrder.indexOf(currentElement)
    val isFirst = currentIndex == 0
    val isLast = currentIndex == tourOrder.lastIndex

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Demo Control Layout showing all UI elements with highlights
        DemoControlLayout(
            highlightedElement = currentElement,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Semi-transparent overlay for tutorial focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
        )

        // Layer 3: Arrow pointing to highlighted element
        ElementArrowIndicator(
            element = currentElement,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 4: Tutorial content - Dynamic Positioning
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top: Progress bar (Always at top)
            Column(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00C853),
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Interface Tour - ${currentIndex + 1}/${tourOrder.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Navigation card: Position based on highlighted element to avoid overlap
            // If element is at the bottom (Joystick/Servo), move card to Top.
            // If element is at the top (Header), move card to Bottom.
            val isBottomElement = currentElement == InterfaceElement.LEFT_JOYSTICK || currentElement == InterfaceElement.SERVO_CONTROLS
            
            TutorialNavigationCard(
                element = currentElement,
                isFirst = isFirst,
                isLast = isLast,
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip,
                modifier = Modifier
                    .align(if (isBottomElement) Alignment.TopCenter else Alignment.BottomCenter)
                    .navigationBarsPadding() // Use safe area handling
                    .padding(
                        top = if (isBottomElement) 60.dp else 0.dp,
                        bottom = if (!isBottomElement) 80.dp else 0.dp // Aggressive padding to clear system nav
                    )
            )
        }
    }
}

@Composable
private fun ElementArrowIndicator(
    element: InterfaceElement,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    
    // Animate arrow movement
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_bob")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    Box(modifier = modifier) {
        when (element) {
            InterfaceElement.CONNECTION_MODE -> {
                // Points to connection switch (Top Left)
                // Center of switch is ~45dp from start. Arrow center is start+20dp. 
                // So start = 25dp.
                // Bottom of header is 56dp. Arrow tip should be close, e.g. 60dp.
                ArrowUp(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 25.dp, top = 60.dp + offset.dp)
                )
            }
            InterfaceElement.CONNECTION_STATUS -> {
                // Points to RSSI widget (Top Left, after switch)
                // Switch ends at ~90dp. Gap 8dp. Widget starts ~98dp. width 56dp. center ~126dp.
                // Arrow center (start+20) = 126. Start = 106.
                ArrowUp(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 106.dp, top = 60.dp + offset.dp)
                )
            }
            InterfaceElement.ESTOP -> {
                // Points to Center Stop Button
                // Position: Below header, pointing UP
                ArrowUp(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp + offset.dp)
                )
            }
            InterfaceElement.SERVO_CONTROLS -> {
                if (isPortrait) {
                    // Portrait: Middle-bottom (above joystick)
                    ArrowDown(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 80.dp + offset.dp)
                    )
                } else {
                    // Landscape: Right side column
                    // Point to the vertical center of the right half
                    ArrowDown(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 80.dp, bottom = offset.dp) // Pointing down at it? Or side?
                            // Let's use ArrowDown from top-right area
                            // Actually, if it's CenterEnd, we might want pointing LEFT?
                            // But keeping it simple with ArrowDown from slightly above.
                            // Align TopEnd, padding top ~100dp, end ~80dp.
                    )
                }
            }
            InterfaceElement.LEFT_JOYSTICK -> {
                if (isPortrait) {
                    // Portrait: Bottom Center
                    // Move up to be fully above the joystick area (approx 300dp from bottom)
                    ArrowDown(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 320.dp + offset.dp)
                    )
                } else {
                    // Landscape: Left side (centered in left half)
                    // Point from above. Center is ~50% height. Joystick size ~160dp.
                    // Need to be above the joystick.
                    ArrowDown(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = 120.dp, // Approx center of left half
                                bottom = 240.dp + offset.dp // Moved up significantly
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrowUp(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00C853)
) {
    Canvas(modifier = modifier.size(40.dp)) {
        val path = Path().apply {
            moveTo(size.width / 2, 0f)
            lineTo(size.width, 20f)
            lineTo(size.width / 2, 0f)
            lineTo(0f, 20f)
        }
        
        // Shaft
        drawLine(
            color = color,
            start = Offset(size.width / 2, size.height),
            end = Offset(size.width / 2, 0f),
            strokeWidth = 8f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Head
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

@Composable
private fun ArrowDown(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00C853)
) {
    Canvas(modifier = modifier.size(40.dp)) {
        val path = Path().apply {
            moveTo(size.width / 2, size.height)
            lineTo(size.width, size.height - 20f)
            lineTo(size.width / 2, size.height)
            lineTo(0f, size.height - 20f)
        }
        
        // Shaft
        drawLine(
            color = color,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 8f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Head
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

@Composable
private fun TutorialNavigationCard(
    element: InterfaceElement,
    isFirst: Boolean,
    isLast: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Title with emoji
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Additional tip
            Text(
                text = getElementTip(element),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip Tour", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row {
                    if (!isFirst) {
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

                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853)
                        )
                    ) {
                        Text(if (isLast) "Continue" else "Next")
                        if (!isLast) {
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
}

private fun getElementEmoji(element: InterfaceElement): String = when (element) {
    InterfaceElement.ESTOP -> "🔴"
    InterfaceElement.CONNECTION_STATUS -> "📶"
    InterfaceElement.LEFT_JOYSTICK -> "🎮"
    InterfaceElement.SERVO_CONTROLS -> "🤖"
    InterfaceElement.CONNECTION_MODE -> "🔄"
}

private fun getElementTip(element: InterfaceElement): String = when (element) {
    InterfaceElement.ESTOP -> "💡 Tip: This is your safety switch! Tap it anytime to immediately stop all motors. Tap again to reset and resume control."
    InterfaceElement.CONNECTION_STATUS -> "💡 Tip: Tap this widget to scan for devices or configure WiFi. The sparkline shows connection latency over time."
    InterfaceElement.LEFT_JOYSTICK -> "💡 Tip: The outer ring shows connection quality. Green = great, yellow = okay, red = poor latency."
    InterfaceElement.SERVO_CONTROLS -> "💡 Tip: Use these buttons to control attached servos (e.g. camera mount or robot arm). Press to toggle positions."
    InterfaceElement.CONNECTION_MODE -> "💡 Tip: Bluetooth (BLE) works for most Arduino boards. Use WiFi for Arduino R4 WiFi with better range."
}
