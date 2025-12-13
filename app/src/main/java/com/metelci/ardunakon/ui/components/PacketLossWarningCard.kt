package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Displays a warning card when packet loss exceeds thresholds.
 *
 * Color coding:
 * - Yellow: > 1% loss
 * - Orange: > 5% loss
 * - Red: > 10% loss
 */
@Composable
fun PacketLossWarningCard(packetsSent: Long, packetsDropped: Long, packetsFailed: Long, modifier: Modifier = Modifier) {
    val totalLoss = packetsDropped + packetsFailed
    val lossPercent = if (packetsSent > 0) {
        (totalLoss.toFloat() / packetsSent * 100)
    } else {
        0f
    }

    // Only show if loss > 1%
    if (lossPercent <= 1.0f) return

    val containerColor = when {
        lossPercent > 10f -> Color(0xFFFF5252) // Red
        lossPercent > 5f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFFFC107) // Yellow
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "⚠ Packet Loss Detected",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${"%.2f".format(lossPercent)}% loss ($totalLoss/$packetsSent packets)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.8f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (packetsDropped > 0) {
                    Text(
                        "Dropped: $packetsDropped",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
                if (packetsFailed > 0) {
                    Text(
                        "Failed: $packetsFailed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
