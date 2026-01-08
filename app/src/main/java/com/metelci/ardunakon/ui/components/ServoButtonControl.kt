package com.metelci.ardunakon.ui.components

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
import com.metelci.ardunakon.ui.utils.hapticTap

@Suppress("FunctionName")
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

    // Interaction sources for all buttons
    val aInteractionSource = remember { MutableInteractionSource() }
    val zInteractionSource = remember { MutableInteractionSource() }
    val wInteractionSource = remember { MutableInteractionSource() }
    val bInteractionSource = remember { MutableInteractionSource() }
    val lInteractionSource = remember { MutableInteractionSource() }
    val rInteractionSource = remember { MutableInteractionSource() }

    // Collect pressed states
    val isAPressed by aInteractionSource.collectIsPressedAsState()
    val isZPressed by zInteractionSource.collectIsPressedAsState()
    val isWPressed by wInteractionSource.collectIsPressedAsState()
    val isBPressed by bInteractionSource.collectIsPressedAsState()
    val isLPressed by lInteractionSource.collectIsPressedAsState()
    val isRPressed by rInteractionSource.collectIsPressedAsState()

    // Elevation values for pressed/unpressed states
    val defaultElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 4.dp,
        pressedElevation = 8.dp,
        hoveredElevation = 6.dp
    )

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
                view.hapticTap()
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
                elevation = defaultElevation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isAPressed -> Color(0xFFFFD600)  // Yellow when pressed
                        servoZ == -1f -> Color(0xFF00C853)  // Green when active
                        else -> Color(0xFF2D3436)  // Default dark
                    },
                    contentColor = if (isAPressed) Color.Black else Color(0xFF00FF00)
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
                        view.hapticTap()
                        val newY = (servoY + step).coerceIn(-1f, 1f)
                        val angle = ((newY + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo Y: $angle deg (W)")
                        onMove(servoX, newY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo forward" },
                interactionSource = wInteractionSource,
                elevation = defaultElevation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoY == 1f || isWPressed) Color(0xFF00C853) else Color(0xFF2D3436),
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
                elevation = defaultElevation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isZPressed -> Color(0xFFFFD600)  // Yellow when pressed
                        servoZ == 1f -> Color(0xFF00C853)  // Green when active
                        else -> Color(0xFF2D3436)  // Default dark
                    },
                    contentColor = if (isZPressed) Color.Black else Color(0xFF00FF00)
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
                        view.hapticTap()
                        val newX = (servoX - step).coerceIn(-1f, 1f)
                        val angle = ((newX + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo X: $angle deg (L)")
                        onMove(newX, servoY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo left" },
                interactionSource = lInteractionSource,
                elevation = defaultElevation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoX == -1f || isLPressed) Color(0xFF00C853) else Color(0xFF2D3436),
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
                        view.hapticTap()
                        val newY = (servoY - step).coerceIn(-1f, 1f)
                        val angle = ((newY + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo Y: $angle deg (B)")
                        onMove(servoX, newY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo backward" },
                interactionSource = bInteractionSource,
                elevation = defaultElevation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoY == -1f || isBPressed) Color(0xFF00C853) else Color(0xFF2D3436),
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
                        view.hapticTap()
                        val newX = (servoX + step).coerceIn(-1f, 1f)
                        val angle = ((newX + 1f) / 2f * 180f).toInt()
                        onLog?.invoke("Servo X: $angle deg (R)")
                        onMove(newX, servoY, servoZ)
                        commitMove(currentTime)
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .semantics { contentDescription = "Move servo right" },
                interactionSource = rInteractionSource,
                elevation = defaultElevation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (servoX == 1f || isRPressed) Color(0xFF00C853) else Color(0xFF2D3436),
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
