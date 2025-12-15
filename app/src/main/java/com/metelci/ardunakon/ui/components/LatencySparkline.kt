package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A mini sparkline graph showing real-time latency (RTT) values.
 *
 * @param rttValues List of RTT values in milliseconds (most recent last)
 * @param modifier Modifier for sizing/positioning
 * @param maxValues Maximum number of values to display (default 20)
 */
@Composable
fun LatencySparkline(
    rttValues: List<Long>,
    modifier: Modifier = Modifier.width(60.dp).height(16.dp),
    maxValues: Int = 20,
    fixedColor: Color? = null
) {
    // Take last N values
    val values = rttValues.takeLast(maxValues)

    // Determine color based on average latency
    val avgRtt = if (values.isNotEmpty()) values.average() else 0.0
    val dynamicColor = when {
        avgRtt < 50 -> Color(0xFF00C853) // Green - excellent
        avgRtt < 100 -> Color(0xFFFFD54F) // Yellow - acceptable
        else -> Color(0xFFFF5252) // Red - poor
    }
    
    val lineColor = fixedColor ?: dynamicColor

    Canvas(
        modifier = modifier
    ) {
        if (values.isEmpty()) {
            // Draw placeholder line when no data
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        // Calculate scale
        val maxRtt = values.maxOrNull()?.coerceAtLeast(100L) ?: 100L
        val minRtt = 0L
        val range = (maxRtt - minRtt).coerceAtLeast(1L)

        val stepX = size.width / (maxValues - 1).coerceAtLeast(1)

        // Build path
        val path = Path()
        values.forEachIndexed { index, rtt ->
            val x = index * stepX
            // Invert Y (0 at top in Canvas)
            val normalizedY = ((rtt - minRtt).toFloat() / range).coerceIn(0f, 1f)
            val y = size.height - (normalizedY * size.height * 0.8f) - (size.height * 0.1f)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw the sparkline
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw current value dot at the end
        if (values.isNotEmpty()) {
            val lastX = (values.size - 1) * stepX
            val lastRtt = values.last()
            val normalizedY = ((lastRtt - minRtt).toFloat() / range).coerceIn(0f, 1f)
            val lastY = size.height - (normalizedY * size.height * 0.8f) - (size.height * 0.1f)

            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}
