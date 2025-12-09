package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants

@Composable
fun ServoButtonControl(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp,
    servoX: Float = 0f,
    servoY: Float = 0f,
    onMove: (x: Float, y: Float) -> Unit,
    onLog: ((String) -> Unit)? = null
) {
    val view = LocalView.current
    var lastMoveTime by remember { mutableStateOf(0L) }
    val debounceDelay = 100L
    
    // All buttons use uniform sizing for consistent appearance

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row: W (Up/Forward)
        Button(
            onClick = {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMoveTime >= debounceDelay) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    // Toggle: if already at W position, return to center
                    val newY = if (servoY == 1f) {
                        onLog?.invoke("Servo: CENTER (released W)")
                        0f
                    } else {
                        onLog?.invoke("Servo: FORWARD (W)")
                        1f
                    }
                    onMove(servoX, newY)
                    lastMoveTime = currentTime
                }
            },
            modifier = Modifier.size(buttonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (servoY == 1f) Color(0xFF00C853) else Color(0xFF2D3436),
                contentColor = Color(0xFF00FF00)
            ),
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "W",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Middle Row: L (Left), B (Backward), R (Right)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // L (Left)
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMoveTime >= debounceDelay) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        // Toggle: if already at L position, return to center
                        val newX = if (servoX == -1f) {
                            onLog?.invoke("Servo: CENTER (released L)")
                            0f
                        } else {
                            onLog?.invoke("Servo: LEFT (L)")
                            -1f
                        }
                        onMove(newX, servoY)
                        lastMoveTime = currentTime
                    }
                },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoX == -1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "L",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // B (Backward/Reverse)
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMoveTime >= debounceDelay) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        // Toggle: if already at B position, return to center
                        val newY = if (servoY == -1f) {
                            onLog?.invoke("Servo: CENTER (released B)")
                            0f
                        } else {
                            onLog?.invoke("Servo: BACKWARD (B)")
                            -1f
                        }
                        onMove(servoX, newY)
                        lastMoveTime = currentTime
                    }
                },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoY == -1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "B",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // R (Right)
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMoveTime >= debounceDelay) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        // Toggle: if already at R position, return to center
                        val newX = if (servoX == 1f) {
                            onLog?.invoke("Servo: CENTER (released R)")
                            0f
                        } else {
                            onLog?.invoke("Servo: RIGHT (R)")
                            1f
                        }
                        onMove(newX, servoY)
                        lastMoveTime = currentTime
                    }
                },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoX == 1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "R",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
