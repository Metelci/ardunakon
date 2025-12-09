package com.metelci.ardunakon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.DeviceCapabilities

/**
 * Sensor Dashboard - Dynamic UI showing ONLY connected sensors
 * Adapts to DeviceCapabilities - no clutter with disabled sensors
 */
@Composable
fun SensorDashboard(
    capabilities: DeviceCapabilities,
    modifier: Modifier = Modifier
) {
    // Collect sensor values (would come from BluetoothManager in real use)
    var thermoValue by remember { mutableStateOf<Float?>(null) }
    var distanceValue by remember { mutableStateOf<Int?>(null) }
    var knobValue by remember { mutableStateOf<Int?>(null) }
    var movementActive by remember { mutableStateOf(false) }
    
    // Only show if ANY sensor is available
    val hasSensors = capabilities.hasModulinoThermo || 
                     capabilities.hasModulinoDistance || 
                     capabilities.hasModulinoKnob || 
                     capabilities.hasModulinoMovement ||
                     capabilities.hasModulinoPixels
    
    AnimatedVisibility(
        visible = hasSensors,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .background(Color(0xFF1A1A2E).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header
            Text(
                "Sensors",
                color = Color(0xFF00FF00),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Only show connected sensors
            if (capabilities.hasModulinoThermo) {
                SensorCard(
                    icon = Icons.Default.Thermostat,
                    label = "Temp",
                    value = thermoValue?.let { "%.1f°C".format(it) } ?: "--",
                    color = Color(0xFFFF5722)
                )
            }
            
            if (capabilities.hasModulinoDistance) {
                SensorCard(
                    icon = Icons.Default.Straighten,
                    label = "Dist",
                    value = distanceValue?.let { "${it}cm" } ?: "--",
                    color = Color(0xFF2196F3)
                )
            }
            
            if (capabilities.hasModulinoKnob) {
                SensorCard(
                    icon = Icons.Default.TurnLeft,
                    label = "Knob",
                    value = knobValue?.let { "$it%" } ?: "--",
                    color = Color(0xFF9C27B0)
                )
            }
            
            if (capabilities.hasModulinoMovement) {
                SensorCard(
                    icon = Icons.Default.Vibration,
                    label = "Motion",
                    value = if (movementActive) "Active" else "Still",
                    color = if (movementActive) Color(0xFFFFEB3B) else Color(0xFF757575)
                )
            }
            
            if (capabilities.hasModulinoPixels) {
                SensorCard(
                    icon = Icons.Default.LightMode,
                    label = "Pixels",
                    value = "Ready",
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D3D), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                label,
                color = Color(0xFFB0BEC5),
                fontSize = 10.sp
            )
        }
        Text(
            value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
