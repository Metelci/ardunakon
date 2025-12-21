package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.telemetry.PacketLossDataPoint

/**
 * Packet Loss Heatmap - visualizes packet loss over time with color intensity.
 *
 * Grid cells represent time windows:
 * - Green: Good connection (0-2% loss)
 * - Yellow: Moderate issues (2-10% loss)
 * - Red: High loss (>10% loss)
 *
 * Intensity (alpha) represents severity within each category.
 */
@Suppress("FunctionName")
@Composable
fun PacketLossHeatmap(
    data: List<Float>,
    // 0f to 1f
    packetLossThreshold: Float = 0.5f,
    timeWindowMs: Long = 900_000L,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("Good (0-2%)", Color(0xFF4CAF50))
            LegendItem("Moderate (2-10%)", Color(0xFFFFC107))
            LegendItem("High (>10%)", Color(0xFFFF5252))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Heatmap grid
        if (data.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No packet loss data available",
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            HeatmapGrid(
                lossHistory = data.map {
                    val lossPercent = it * 100
                    val packetsSent = 100
                    val packetsDropped = (packetsSent * it).toInt()
                    val packetsFailed = 0
                    val packetsReceived = packetsSent - packetsDropped - packetsFailed
                    PacketLossDataPoint(
                        timestamp = System.currentTimeMillis(),
                        packetsSent = packetsSent,
                        packetsReceived = packetsReceived,
                        packetsDropped = packetsDropped,
                        packetsFailed = packetsFailed,
                        lossPercent = lossPercent
                    )
                },
                timeWindowMs = timeWindowMs,
                isDarkTheme = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFFB0BEC5)
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun HeatmapGrid(
    lossHistory: List<PacketLossDataPoint>,
    timeWindowMs: Long,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val now = System.currentTimeMillis()
        val cellWidthPx = size.width / 60f // 60 cells = 15min / 15sec per cell
        val cellHeightPx = size.height / 3f // 3 rows (recent, mid, old)

        // Group data into 15-second buckets
        val bucketSizeMs = 15_000L
        val buckets = lossHistory
            .filter { now - it.timestamp <= timeWindowMs }
            .groupBy { (now - it.timestamp) / bucketSizeMs }

        buckets.forEach { (bucketIndex, points) ->
            if (bucketIndex < 60) { // Only show last 60 buckets
                val avgLoss = points.map { it.lossPercent }.average().toFloat()

                // Determine color and alpha based on loss rate
                val (baseColor, alpha) = when {
                    avgLoss < 2f -> Color(0xFF4CAF50) to (avgLoss / 2f).coerceIn(0.2f, 1f)
                    avgLoss < 10f -> Color(0xFFFFC107) to ((avgLoss - 2f) / 8f).coerceIn(0.3f, 1f)
                    else -> Color(0xFFFF5252) to ((avgLoss / 100f)).coerceIn(0.4f, 1f)
                }

                // Draw cell - newer data at the right
                val x = size.width - (bucketIndex + 1) * cellWidthPx
                val row = (bucketIndex / 20).coerceIn(0, 2) // 3 rows: 0-20, 20-40, 40-60
                val y = row * cellHeightPx

                drawRect(
                    color = baseColor.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    // 1px gap
                    size = Size(cellWidthPx - 1f, cellHeightPx - 1f)
                )
            }
        }

        // Draw grid lines
        val gridColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
        for (i in 1..2) {
            drawLine(
                color = gridColor,
                start = Offset(0f, i * cellHeightPx),
                end = Offset(size.width, i * cellHeightPx),
                strokeWidth = 1f
            )
        }
    }
}
