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
import androidx.compose.ui.text.font.FontWeight
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
@Composable
fun PacketLossHeatmap(
    lossHistory: List<PacketLossDataPoint>,
    timeWindowMs: Long = 900_000L,  // 15 minutes
    isDarkTheme: Boolean,
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
            LegendItem("Good (0-2%)", Color(0xFF4CAF50), isDarkTheme)
            LegendItem("Moderate (2-10%)", Color(0xFFFFC107), isDarkTheme)
            LegendItem("High (>10%)", Color(0xFFFF5252), isDarkTheme)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Heatmap grid
        if (lossHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No packet loss data available",
                    color = if (isDarkTheme) Color(0xFFB0BEC5) else Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            HeatmapGrid(
                lossHistory = lossHistory,
                timeWindowMs = timeWindowMs,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    isDarkTheme: Boolean
) {
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
            color = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF37474F)
        )
    }
}

@Composable
private fun HeatmapGrid(
    lossHistory: List<PacketLossDataPoint>,
    timeWindowMs: Long,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val now = System.currentTimeMillis()
        val cellWidthPx = size.width / 60f  // 60 cells = 15min / 15sec per cell
        val cellHeightPx = size.height / 3f  // 3 rows (recent, mid, old)
        
        // Group data into 15-second buckets
        val bucketSizeMs = 15_000L
        val buckets = lossHistory
            .filter { now - it.timestamp <= timeWindowMs }
            .groupBy { (now - it.timestamp) / bucketSizeMs }
        
        buckets.forEach { (bucketIndex, points) ->
            if (bucketIndex < 60) {  // Only show last 60 buckets
                val avgLoss = points.map { it.lossPercent }.average().toFloat()
                
                // Determine color and alpha based on loss rate
                val (baseColor, alpha) = when {
                    avgLoss < 2f -> Color(0xFF4CAF50) to (avgLoss / 2f).coerceIn(0.2f, 1f)
                    avgLoss < 10f -> Color(0xFFFFC107) to ((avgLoss - 2f) / 8f).coerceIn(0.3f, 1f)
                    else -> Color(0xFFFF5252) to ((avgLoss / 100f)).coerceIn(0.4f, 1f)
                }
                
                // Draw cell - newer data at the right
                val x = size.width - (bucketIndex + 1) * cellWidthPx
                val row = (bucketIndex / 20).coerceIn(0, 2)  // 3 rows: 0-20, 20-40, 40-60
                val y = row * cellHeightPx
                
                drawRect(
                    color = baseColor.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = Size(cellWidthPx - 1f, cellHeightPx - 1f)  // 1px gap
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
