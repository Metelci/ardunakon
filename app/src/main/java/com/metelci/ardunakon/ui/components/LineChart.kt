package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.telemetry.TelemetryDataPoint

data class LineChartSeries(
    val label: String,
    val data: List<TelemetryDataPoint>,
    val color: Color
)

@Composable
fun LineChart(
    series: List<LineChartSeries>,
    modifier: Modifier = Modifier,
    yAxisLabel: String = "",
    yAxisMin: Float? = null,
    yAxisMax: Float? = null,
    showGrid: Boolean = true,
    isDarkTheme: Boolean = true
) {
    Column(modifier = modifier) {
        // Y-axis label
        if (yAxisLabel.isNotEmpty()) {
            Text(
                text = yAxisLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(if (isDarkTheme) Color(0xFF1E1E1E) else Color.White)
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height

            // Compute global min/max across all series
            val allValues = series.flatMap { it.data.map { point -> point.value } }
            if (allValues.isEmpty()) {
                // Draw empty state message
                return@Canvas
            }

            val minValue = yAxisMin ?: allValues.minOrNull() ?: 0f
            val maxValue = yAxisMax ?: allValues.maxOrNull() ?: 1f
            val valueRange = (maxValue - minValue).coerceAtLeast(0.001f)

            // Find time range
            val allTimestamps = series.flatMap { it.data.map { point -> point.timestamp } }
            val minTime = allTimestamps.minOrNull() ?: 0L
            val maxTime = allTimestamps.maxOrNull() ?: 0L
            val timeRange = (maxTime - minTime).coerceAtLeast(1L)

            // Draw grid
            if (showGrid) {
                drawGrid(width, height, isDarkTheme)
            }

            // Draw each series
            series.forEach { seriesData ->
                if (seriesData.data.size >= 2) {
                    drawLineSeries(
                        data = seriesData.data,
                        color = seriesData.color,
                        width = width,
                        height = height,
                        minValue = minValue,
                        maxValue = maxValue,
                        minTime = minTime,
                        timeRange = timeRange
                    )
                }
            }

            // Draw axes
            drawAxes(width, height, isDarkTheme)
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            series.forEach { seriesData ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(seriesData.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${seriesData.label} (${seriesData.data.size} pts)",
                        fontSize = 10.sp,
                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Black
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawLineSeries(
    data: List<TelemetryDataPoint>,
    color: Color,
    width: Float,
    height: Float,
    minValue: Float,
    maxValue: Float,
    minTime: Long,
    timeRange: Long
) {
    val path = Path()
    val valueRange = (maxValue - minValue).coerceAtLeast(0.001f)

    data.forEachIndexed { index, point ->
        val x = ((point.timestamp - minTime).toFloat() / timeRange) * width
        val y = height - (((point.value - minValue) / valueRange) * height)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

private fun DrawScope.drawGrid(width: Float, height: Float, isDarkTheme: Boolean) {
    val gridColor = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0)
    val gridLines = 5

    // Horizontal grid lines
    for (i in 0..gridLines) {
        val y = (height / gridLines) * i
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
        )
    }
}

private fun DrawScope.drawAxes(width: Float, height: Float, isDarkTheme: Boolean) {
    val axisColor = if (isDarkTheme) Color(0xFF90CAF9) else Color.Black

    // X-axis
    drawLine(
        color = axisColor,
        start = Offset(0f, height),
        end = Offset(width, height),
        strokeWidth = 2.dp.toPx()
    )

    // Y-axis
    drawLine(
        color = axisColor,
        start = Offset(0f, 0f),
        end = Offset(0f, height),
        strokeWidth = 2.dp.toPx()
    )
}
