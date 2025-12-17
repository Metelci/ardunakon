package com.metelci.ardunakon.ui.screens.onboarding

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.model.InterfaceElement
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.LatencySparkline
import com.metelci.ardunakon.ui.components.SignalStrengthIcon

/**
 * Demo version of the control layout for onboarding tutorial.
 * Shows all UI elements with mock/demo data, no service dependencies.
 */
@Composable
fun DemoControlLayout(
    highlightedElement: InterfaceElement?,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F1419))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Demo Header Bar (Always at top)
            DemoHeaderBar(
                highlightedElement = highlightedElement,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isPortrait) {
                // PORTRAIT: Vertical Stack
                // Debug -> Servo -> Joystick
                
                // Demo Debug Panel (Always visible to maintain layout)
                DemoDebugPanel(
                    isHighlighted = false, // Never highlighted in this tour step? Or add logic if needed
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Servo buttons
                DemoServoPanel(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Demo Joystick
                DemoJoystickPanel(
                    isHighlighted = highlightedElement == InterfaceElement.LEFT_JOYSTICK,
                    size = 160.dp,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                )
            } else {
                // LANDSCAPE: Row Layout
                // Left: Joystick | Right: Column(Debug, Servo)
                
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Side: Joystick
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                         DemoJoystickPanel(
                            isHighlighted = highlightedElement == InterfaceElement.LEFT_JOYSTICK,
                            size = 160.dp, // Smaller in landscape? or consistent
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Right Side: Servo + Debug
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        DemoDebugPanel(
                            isHighlighted = false,
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxWidth()
                        )
                        
                        DemoServoPanel(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoHeaderBar(
    highlightedElement: InterfaceElement?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 4.dp).height(56.dp)
    ) {
        // Left: Connection mode selector & Status
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DemoConnectionModeSelector(
                isHighlighted = highlightedElement == InterfaceElement.CONNECTION_MODE,
                modifier = Modifier.height(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            DemoConnectionStatusWidget(
                isHighlighted = highlightedElement == InterfaceElement.CONNECTION_STATUS,
                modifier = Modifier.width(56.dp).height(40.dp)
            )
        }

        // E-Stop button (Exact Center)
        DemoEStopButton(
            isHighlighted = highlightedElement == InterfaceElement.ESTOP,
            size = 56.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Right side buttons
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DemoIconButton(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = "Telemetry",
                size = 32.dp
            )
            DemoIconButton(
                icon = Icons.Default.Info,
                contentDescription = "Debug",
                size = 32.dp
            )
        }
    }
}

@Composable
private fun DemoConnectionModeSelector(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isHighlighted) Color(0xFF00C853) else Color(0xFF00FF00)
    val borderWidth = if (isHighlighted) 3.dp else 1.dp

    Row(
        modifier = modifier
            .border(borderWidth, borderColor, CircleShape)
            .background(Color(0xFF455A64), CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bluetooth segment (selected)
        Surface(
            color = Color(0xFF00FF00).copy(alpha = 0.25f),
            shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = Color(0xFF00FF00),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Color(0xFF00FF00).copy(alpha = 0.5f))
        )

        // WiFi segment
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "WiFi",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DemoConnectionStatusWidget(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isHighlighted) Color(0xFF00C853) else Color(0xFF00FF00)
    val borderWidth = if (isHighlighted) 3.dp else 1.dp
    val mockRttHistory = listOf(45L, 42L, 48L, 44L, 46L, 43L, 47L, 45L)

    Surface(
        shape = CircleShape,
        color = Color(0xFF455A64),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SignalStrengthIcon(
                    rssi = -55,
                    color = Color(0xFF00FF00),
                    isWifi = false,
                    showLabels = false,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "45ms",
                    fontSize = 9.sp,
                    color = Color(0xFF00FF00),
                    fontWeight = FontWeight.Medium
                )
            }
            LatencySparkline(
                rttValues = mockRttHistory,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
private fun DemoEStopButton(
    isHighlighted: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isHighlighted) Color(0xFF00C853) else Color(0xFF90CAF9)
    val borderWidth = if (isHighlighted) 3.dp else 1.dp

    Surface(
        shape = CircleShape,
        color = Color(0xFF455A64),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .size(size)
            .shadow(2.dp, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "STOP",
                color = Color(0xFF90CAF9),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DemoIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF455A64),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00)),
        modifier = Modifier
            .size(size)
            .shadow(2.dp, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF00FF00),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DemoDebugPanel(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isHighlighted) Color(0xFF00C853) else Color(0xFF00FF00).copy(alpha = 0.5f)
    val borderWidth = if (isHighlighted) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A2E).copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                "Debug Console",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF00FF00)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "[INFO] Ready to connect...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                "[INFO] Bluetooth initialized",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4FC3F7),
                fontSize = 10.sp
            )
            Text(
                "[SUCCESS] Scan complete",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF00C853),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DemoServoPanel(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // W button
        DemoServoButton("W")
        Spacer(modifier = Modifier.width(8.dp))
        // L B R row
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoServoButton("L")
                DemoServoButton("B")
                DemoServoButton("R")
            }
        }
    }
}

@Composable
private fun DemoServoButton(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF455A64),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF90CAF9)),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DemoJoystickPanel(
    isHighlighted: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Highlight ring if this is the current tutorial element
        if (isHighlighted) {
            Box(
                modifier = Modifier
                    .size(size + 16.dp)
                    .border(3.dp, Color(0xFF00C853), CircleShape)
            )
        }
        
        JoystickControl(
            onMoved = { /* Demo - no action */ },
            size = size,
            isThrottle = false,
            connectionLatencyMs = 45L
        )
    }
}
