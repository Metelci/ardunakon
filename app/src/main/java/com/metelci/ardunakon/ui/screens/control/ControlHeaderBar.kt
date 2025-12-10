package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.crash.CrashHandler
import com.metelci.ardunakon.ui.components.LatencySparkline
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import com.metelci.ardunakon.wifi.WifiConnectionState

/**
 * Reusable header bar for the control screen.
 * 
 * Contains signal indicator, connection button, telemetry graph button,
 * E-STOP button, debug toggle, and overflow menu.
 */
@Composable
fun ControlHeaderBar(
    // Connection state
    connectionMode: ConnectionMode,
    bluetoothConnectionState: ConnectionState,
    wifiConnectionState: WifiConnectionState,
    rssiValue: Int,
    wifiRssi: Int,
    rttHistory: List<Long>,
    wifiRttHistory: List<Long>,
    
    // E-Stop state
    isEStopActive: Boolean,
    
    // Debug panel state
    isDebugPanelVisible: Boolean,
    
    // Settings
    isDarkTheme: Boolean,
    allowReflection: Boolean,
    
    // Button sizes
    buttonSize: Dp = 36.dp,
    eStopSize: Dp = 72.dp,
    
    // Callbacks
    onReconnectDevice: () -> Unit,
    onSwitchToWifi: () -> Unit,
    onSwitchToBluetooth: () -> Unit,
    onConfigureWifi: () -> Unit,
    onTelemetryGraph: () -> Unit,
    onToggleEStop: () -> Unit,
    onToggleDebugPanel: () -> Unit,
    onShowHelp: () -> Unit,
    onShowAbout: () -> Unit,
    onShowCrashLog: () -> Unit,
    onToggleReflection: () -> Unit,
    onOpenArduinoCloud: () -> Unit,
    onQuitApp: () -> Unit,
    
    // Context for crash log check
    context: Context,
    view: View,
    modifier: Modifier = Modifier
) {
    var showHeaderMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (connectionMode == ConnectionMode.BLUETOOTH) {
            // Bluetooth Mode
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val stateColor = when (bluetoothConnectionState) {
                    ConnectionState.CONNECTED -> Color(0xFF00FF00)
                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD54F)
                    ConnectionState.ERROR -> Color(0xFFFF5252)
                    else -> Color.Gray
                }
                SignalStrengthIcon(
                    rssi = rssiValue,
                    color = stateColor,
                    modifier = Modifier
                )
                LatencySparkline(
                    rttValues = rttHistory,
                    modifier = Modifier.width(40.dp).height(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Bluetooth Reconnect button
            Box {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showHeaderMenu = true
                    },
                    modifier = Modifier
                        .size(buttonSize)
                        .shadow(2.dp, CircleShape)
                        .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                        .border(1.dp, Color(0xFF00FF00), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = "Connect / Reconnect",
                        tint = Color(0xFF00FF00),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showHeaderMenu,
                    onDismissRequest = { showHeaderMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reconnect Device") },
                        onClick = {
                            showHeaderMenu = false
                            onReconnectDevice()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Switch to WiFi") },
                        onClick = {
                            showHeaderMenu = false
                            onSwitchToWifi()
                        }
                    )
                }
            }
        } else {
            // WiFi Mode
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val stateColor = when (wifiConnectionState) {
                    WifiConnectionState.CONNECTED -> Color(0xFF00FF00)
                    WifiConnectionState.CONNECTING -> Color(0xFFFFD54F)
                    WifiConnectionState.ERROR -> Color(0xFFFF5252)
                    else -> Color.Gray
                }
                SignalStrengthIcon(
                    rssi = wifiRssi,
                    color = stateColor,
                    modifier = Modifier,
                    isWifi = true,
                    showLabels = false
                )
                LatencySparkline(
                    rttValues = wifiRttHistory,
                    modifier = Modifier.width(40.dp).height(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // WiFi Config button
            Box {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showHeaderMenu = true
                    },
                    modifier = Modifier
                        .size(buttonSize)
                        .shadow(2.dp, CircleShape)
                        .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                        .border(1.dp, Color(0xFF00C853), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "WiFi Configuration",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showHeaderMenu,
                    onDismissRequest = { showHeaderMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Configure WiFi") },
                        onClick = {
                            showHeaderMenu = false
                            onConfigureWifi()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Switch to Bluetooth") },
                        onClick = {
                            showHeaderMenu = false
                            onSwitchToBluetooth()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Telemetry Graph Button
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTelemetryGraph()
            },
            modifier = Modifier
                .size(buttonSize)
                .shadow(2.dp, CircleShape)
                .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                .border(1.dp, Color(0xFF00FF00), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = "Telemetry Graphs",
                tint = Color(0xFF00FF00),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // E-STOP Button
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onToggleEStop()
            },
            modifier = Modifier
                .size(eStopSize)
                .shadow(2.dp, CircleShape)
                .background(
                    if (isEStopActive) Color(0xFFFF5252)
                    else if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isEStopActive) Color(0xFFD32F2F)
                    else if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF455A64),
                    CircleShape
                )
        ) {
            Text(
                if (isEStopActive) "RESET" else "STOP",
                color = if (isEStopActive) Color.White
                else if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF2D3436),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Debug Panel Toggle Button
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onToggleDebugPanel()
            },
            modifier = Modifier
                .size(buttonSize)
                .shadow(2.dp, CircleShape)
                .background(
                    if (isDebugPanelVisible) Color(0xFF43A047) else Color(0xFF455A64),
                    CircleShape
                )
                .border(1.dp, Color(0xFF00FF00), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Toggle Debug",
                tint = if (isDebugPanelVisible) Color.White else Color(0xFF00FF00),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Overflow Menu Button
        Box {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showOverflowMenu = !showOverflowMenu
                },
                modifier = Modifier
                    .size(buttonSize)
                    .shadow(2.dp, CircleShape)
                    .background(if (isDarkTheme) Color(0xFF455A64) else Color(0xFFE0E0E0), CircleShape)
                    .border(1.dp, Color(0xFF00FF00), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Menu",
                    tint = Color(0xFF00FF00),
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Help") },
                    leadingIcon = { Icon(Icons.Default.Help, null) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onShowHelp()
                        showOverflowMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    leadingIcon = { Icon(Icons.Outlined.Info, null) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onShowAbout()
                        showOverflowMenu = false
                    }
                )
                if (CrashHandler.hasCrashLog(context)) {
                    DropdownMenuItem(
                        text = { Text("View Crash Log", color = Color(0xFFFF9800)) },
                        leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onShowCrashLog()
                            showOverflowMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Legacy Reflection (HC-06): " + if (allowReflection) "On" else "Off") },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggleReflection()
                        showOverflowMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open Arduino Cloud") },
                    leadingIcon = { Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF00FF00)) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOpenArduinoCloud()
                        showOverflowMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Switch to ${if (connectionMode == ConnectionMode.BLUETOOTH) "WiFi" else "Bluetooth"}") },
                    onClick = {
                        showOverflowMenu = false
                        if (connectionMode == ConnectionMode.BLUETOOTH) {
                            onSwitchToWifi()
                        } else {
                            onSwitchToBluetooth()
                        }
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (connectionMode == ConnectionMode.BLUETOOTH) Icons.Default.Wifi else Icons.Default.Bluetooth,
                            contentDescription = "Switch Mode",
                            tint = Color.Gray
                        )
                    }
                )
                Divider(color = if (isDarkTheme) Color(0xFF455A64) else Color(0xFFB0BEC5), thickness = 1.dp)
                DropdownMenuItem(
                    text = { Text("Quit App", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF5252)) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onQuitApp()
                        showOverflowMenu = false
                    }
                )
            }
        }
    }
}
