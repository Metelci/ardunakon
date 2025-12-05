package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants

@Composable
fun ServoButtonControl(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 70.dp,
    onMove: (x: Float, y: Float) -> Unit,
    onLog: ((String) -> Unit)? = null
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var lastMoveTime by remember { mutableStateOf(0L) }
    val debounceDelay = 100L

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row: Z (Up)
        Button(
            onClick = {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMoveTime >= debounceDelay) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onLog?.invoke("Servo: UP (Z)")
                    onMove(0f, 1f) // Y = 1 (up)
                    lastMoveTime = currentTime

                    coroutineScope.launch {
                        delay(150)
                        onMove(0f, 0f)
                    }
                }
            },
            modifier = Modifier.size(buttonSize),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2D3436),
                contentColor = Color(0xFF00FF00)
            ),
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Up",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Z",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Middle Row: A (Left), S (Down), D (Right)
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
                        onLog?.invoke("Servo: LEFT (L)")
                        onMove(-1f, 0f) // X = -1 (left)
                        lastMoveTime = currentTime

                        coroutineScope.launch {
                            delay(150)
                            onMove(0f, 0f)
                        }
                    }
                },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Left",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "L",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // C (Down)
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMoveTime >= debounceDelay) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onLog?.invoke("Servo: DOWN (C)")
                        onMove(0f, -1f) // Y = -1 (down)
                        lastMoveTime = currentTime

                        coroutineScope.launch {
                            delay(150)
                            onMove(0f, 0f)
                        }
                    }
                },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Down",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "C",
                        fontSize = 12.sp,
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
                        onLog?.invoke("Servo: RIGHT (R)")
                        onMove(1f, 0f) // X = 1 (right)
                        lastMoveTime = currentTime

                        coroutineScope.launch {
                            delay(150)
                            onMove(0f, 0f)
                        }
                    }
                },
                modifier = Modifier.size(buttonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Right",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "R",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
