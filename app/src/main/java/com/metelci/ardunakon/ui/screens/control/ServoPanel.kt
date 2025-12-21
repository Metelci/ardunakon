package com.metelci.ardunakon.ui.screens.control

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.ui.components.ServoButtonControl

/**
 * Wrapper component for ServoButtonControl.
 * Provides consistent layout and styling for servo control panels.
 *
 * @param servoX Current X position (-1 to 1)
 * @param servoY Current Y position (-1 to 1)
 * @param onServoMove Callback when servo position changes
 * @param onLog Callback to log servo actions
 * @param buttonSize Size of each button
 * @param modifier Optional modifier for the container Box
 * @param contentAlignment Alignment within the container
 */
@Suppress("FunctionName")
@Composable
fun ServoPanel(
    servoX: Float,
    servoY: Float,
    servoZ: Float,
    onServoMove: (x: Float, y: Float, z: Float) -> Unit,
    onLog: (String) -> Unit = {},
    buttonSize: Dp = 56.dp,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        ServoButtonControl(
            servoX = servoX,
            servoY = servoY,
            servoZ = servoZ,
            onMove = onServoMove,
            buttonSize = buttonSize,
            onLog = onLog
        )
    }
}
