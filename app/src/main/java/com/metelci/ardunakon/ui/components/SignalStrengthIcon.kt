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
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // RSSI Levels (in dBm)
    // > -50: 4 bars
    // -50 to -65: 3 bars
    // -65 to -80: 2 bars
    // -80 to -95: 1 bar
    // <= -95 or 0 (unknown): 0 bars

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
        Text(
            text = if (rssi == 0) "N/A" else "$rssi dBm",
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
