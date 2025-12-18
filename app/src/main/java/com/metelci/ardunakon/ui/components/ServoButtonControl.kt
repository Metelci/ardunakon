package com.metelci.ardunakon.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServoButtonControl(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp,
    servoX: Float = 0f,
    servoY: Float = 0f,
    servoZ: Float = 0f,
    onMove: (x: Float, y: Float, z: Float) -> Unit,
    onLog: ((String) -> Unit)? = null
) {
    val view = LocalView.current
    var lastMoveTime by remember { mutableStateOf(0L) }
    val debounceDelay = 100L
    val step = 0.1f

    val aInteractionSource = remember { MutableInteractionSource() }
    val zInteractionSource = remember { MutableInteractionSource() }
    val isAPressed by aInteractionSource.collectIsPressedAsState()
    val isZPressed by zInteractionSource.collectIsPressedAsState()

    val targetZ = when {
        isAPressed && !isZPressed -> -1f
        isZPressed && !isAPressed -> 1f
        else -> 0f
    }

    LaunchedEffect(targetZ) {
        if (targetZ != servoZ) {
            onLog?.invoke(
                when (targetZ) {
                    -1f -> "Servo Z: MIN (A)"
                    1f -> "Servo Z: MAX (Z)"
                    else -> "Servo Z: CENTER"
                }
            )
            if (targetZ != 0f) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            onMove(servoX, servoY, targetZ)
        }
    }

    fun canMove(currentTime: Long): Boolean = currentTime - lastMoveTime >= debounceDelay

    fun commitMove(currentTime: Long) {
        lastMoveTime = currentTime
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row: A, W, Z
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move third servo negative" },
                interactionSource = aInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoZ == -1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    "A",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (canMove(currentTime)) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val newY = (servoY + step).coerceIn(-1f, 1f)
                        val angle = ((newY + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo Y: ${angle} deg (W)")
                        onMove(servoX, newY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo forward" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoY == 1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    "W",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {},
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move third servo positive" },
                interactionSource = zInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoZ == 1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    "Z",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Row: L, B, R
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (canMove(currentTime)) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val newX = (servoX - step).coerceIn(-1f, 1f)
                        val angle = ((newX + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo X: ${angle} deg (L)")
                        onMove(newX, servoY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo left" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoX == -1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    "L",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (canMove(currentTime)) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val newY = (servoY - step).coerceIn(-1f, 1f)
                        val angle = ((newY + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo Y: ${angle} deg (B)")
                        onMove(servoX, newY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo backward" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoY == -1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    "B",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (canMove(currentTime)) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val newX = (servoX + step).coerceIn(-1f, 1f)
                        val angle = ((newX + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo X: ${angle} deg (R)")
                        onMove(newX, servoY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo right" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoX == 1f) Color(0xFF00C853) else Color(0xFF2D3436),
                    contentColor = Color(0xFF00FF00)
                ),
                contentPadding = PaddingValues(4.dp)
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
