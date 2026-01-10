package com.metelci.ardunakon.ui.screens.control.dialogs

import android.content.Intent
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metelci.ardunakon.monitoring.CrashRecord
import com.metelci.ardunakon.monitoring.PerformanceMonitor
import com.metelci.ardunakon.monitoring.PerformanceStats
import com.metelci.ardunakon.monitoring.SeverityLevel
import com.metelci.ardunakon.ui.utils.hapticTap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog displaying performance statistics and crash history.
 *
 * Shows aggregated metrics only - no raw data or PII exposed.
 */
@Suppress("FunctionName")
@Composable
fun PerformanceStatsDialog(view: View, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val monitor = PerformanceMonitor.getInstance() ?: return
    val stats by monitor.stats.collectAsStateWithLifecycle()
    val crashes by monitor.recentCrashes.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val widthFraction = if (isLandscape) 0.75f else 0.98f

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .fillMaxHeight(0.9f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Performance Stats", color = Color(0xFF00E5FF))
                }
                IconButton(onClick = {
                    view.hapticTap()
                    val report = monitor.generateDiagnosticReport()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Ardunakon Performance Report")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                }) {
                    Icon(Icons.Default.Share, "Export", tint = Color(0xFF00E5FF))
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Health Score Card
                item {
                    HealthScoreCard(stats)
                }

                // Stats Grid
                item {
                    StatsGrid(stats)
                }

                // Crash History Section
                item {
                    Text(
                        "Recent Issues",
                        color = Color(0xFFB0BEC5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (crashes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "No recent issues",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(crashes.take(5)) { crash ->
                        CrashHistoryItem(crash)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF00E5FF))
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Suppress("FunctionName")
@Composable
@androidx.compose.ui.UiComposable
private fun HealthScoreCard(stats: PerformanceStats) {
    val healthScore = stats.crashFreeSessionsPercent
    val healthColor = when {
        healthScore >= 95f -> Color(0xFF4CAF50) // Green
        healthScore >= 80f -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFFF5252) // Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "App Health",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${String.format(Locale.getDefault(), "%.0f", healthScore)}%",
                color = healthColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { healthScore / 100f },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = healthColor,
                trackColor = Color(0xFF424242)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Crash-free sessions",
                color = Color(0xFF757575),
                fontSize = 10.sp
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
@androidx.compose.ui.UiComposable
private fun StatsGrid(stats: PerformanceStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Startup",
            value = "${stats.avgStartupTimeMs}ms",
            color = Color(0xFF00E5FF)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Latency",
            value = "${String.format(Locale.getDefault(), "%.0f", stats.avgLatencyMs)}ms",
            color = Color(0xFF7C4DFF)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Sessions",
            value = "${stats.sessionCount}",
            color = Color(0xFFFFAB40)
        )
    }
}

@Suppress("FunctionName")
@Composable
@androidx.compose.ui.UiComposable
private fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                color = Color(0xFF757575),
                fontSize = 10.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
@androidx.compose.ui.UiComposable
private fun CrashHistoryItem(crash: CrashRecord) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    val severityColor = when (crash.severity) {
        SeverityLevel.FATAL -> Color(0xFFFF5252)
        SeverityLevel.ERROR -> Color(0xFFFF9800)
        SeverityLevel.WARNING -> Color(0xFFFFC107)
    }
    val severityIcon = when (crash.severity) {
        SeverityLevel.FATAL -> Icons.Default.Error
        SeverityLevel.ERROR -> Icons.Default.Warning
        SeverityLevel.WARNING -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        severityIcon,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        crash.exceptionType,
                        color = severityColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "x${crash.occurrenceCount}",
                    color = Color(0xFF757575),
                    fontSize = 11.sp
                )
            }

            if (!crash.message.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    crash.message,
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                dateFormat.format(Date(crash.timestamp)),
                color = Color(0xFF616161),
                fontSize = 10.sp
            )

            if (crash.topStackFrames.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFF424242), thickness = 0.5.dp)
                Spacer(Modifier.height(6.dp))
                crash.topStackFrames.take(3).forEach { frame ->
                    Text(
                        "  at $frame",
                        color = Color(0xFF757575),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}
