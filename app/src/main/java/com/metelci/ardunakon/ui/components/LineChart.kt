package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.telemetry.TelemetryDataPoint
import kotlin.math.abs

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
    xAxisLabel: String = "Time",
    yAxisMin: Float? = null,
    yAxisMax: Float? = null,
    showGrid: Boolean = true,
    isDarkTheme: Boolean = true
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.dp, if (isDarkTheme) Color(0xFF455A64) else Color.LightGray)
                    .background(if (isDarkTheme) Color(0xFF1E1E1E) else Color.White)
                    .padding(start = 50.dp, top = 30.dp, end = 16.dp, bottom = 32.dp)
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

                // Draw Y-axis numeric labels (5 divisions)
                drawYLabels(
                    width = width,
                    height = height,
                    minValue = minValue,
                    maxValue = maxValue,
                    isDarkTheme = isDarkTheme
                )

                // Draw X-axis start/end time labels
                drawTimeLabels(
                    width = width,
                    height = height,
                    timeRangeMs = timeRange,
                    isDarkTheme = isDarkTheme
                )
            }

            // Overlay axis labels for clear unit visibility
            if (yAxisLabel.isNotEmpty()) {
                Text(
                    text = yAxisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 52.dp, top = 4.dp)
                )
            }
            if (xAxisLabel.isNotEmpty()) {
                Text(
                    text = xAxisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
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

private fun DrawScope.drawYLabels(
    width: Float,
    height: Float,
    minValue: Float,
    maxValue: Float,
    isDarkTheme: Boolean
) {
    val gridLines = 5
    val paint = android.graphics.Paint().apply {
        color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 12.sp.toPx()
        isAntiAlias = true
    }
    val valueRange = (maxValue - minValue).coerceAtLeast(0.001f)
    for (i in 0..gridLines) {
        val ratio = i / gridLines.toFloat()
        val value = maxValue - (valueRange * ratio)
        val y = (height / gridLines) * i
        val label = formatValue(value)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                label,
                4.dp.toPx(),
                y + 4.dp.toPx(), // Slight offset for readability
                paint
            )
        }
    }
}

private fun DrawScope.drawTimeLabels(
    width: Float,
    height: Float,
    timeRangeMs: Long,
    isDarkTheme: Boolean
) {
    val paint = android.graphics.Paint().apply {
        color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 12.sp.toPx()
        isAntiAlias = true
    }
    val startLabel = "0s"
    val endSeconds = (timeRangeMs / 1000f).coerceAtLeast(0.1f)
    val endLabel = "${formatValue(endSeconds)}s"

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            startLabel,
            4.dp.toPx(),
            height + 24.dp.toPx(),
            paint
        )
    }
    val endWidth = paint.measureText(endLabel)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            endLabel,
            width - endWidth - 4.dp.toPx(),
            height + 24.dp.toPx(),
            paint
        )
    }
}

private fun formatValue(value: Float): String {
    return if (abs(value) >= 100f) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
