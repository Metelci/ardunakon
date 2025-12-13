package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignalStrengthIcon(
    rssi: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isWifi: Boolean = false,
    showLabels: Boolean = true
) {
    // RSSI Levels (in dBm)
    val bars = when {
        rssi == 0 -> 0
        rssi > -50 -> 4
        rssi > -65 -> 3
        rssi > -80 -> 2
        rssi > -95 -> 1
        else -> 0
    }

    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            if (isWifi) {
                // Draw WiFi Arcs
                val centerX = size.width / 2
                val bottomY = size.height * 0.9f
                val maxRadius = size.width * 0.9f

                // Dot
                if (bars >= 1) {
                    drawCircle(color = color, radius = size.width * 0.1f, center = Offset(centerX, bottomY))
                } else {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = size.width * 0.1f,
                        center = Offset(centerX, bottomY)
                    )
                }

                // Arcs
                val strokeWidth = 2.dp.toPx()
                val startAngle = 225f
                val sweepAngle = 90f

                // Arcs for levels 2, 3, 4
                for (i in 2..4) {
                    val radius = (maxRadius / 3) * (i - 1)
                    val isActive = i <= bars
                    val arcColor = if (isActive) color else color.copy(alpha = 0.3f)

                    drawArc(
                        color = arcColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, bottomY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            } else {
                // Draw Cellular Bars (Existing Logic)
                val barWidth = size.width / 5
                val maxBarHeight = size.height
                val spacing = size.width / 10

                for (i in 1..4) {
                    val height = maxBarHeight * (i * 0.25f)
                    val x = (i - 1) * (barWidth + spacing)
                    val y = maxBarHeight - height

                    val isActive = i <= bars
                    val barColor = if (isActive) color else color.copy(alpha = 0.3f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }

        if (showLabels) {
            Text(
                text = if (rssi == 0) "N/A" else "$rssi dBm",
                fontSize = 10.sp,
                color = color,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
