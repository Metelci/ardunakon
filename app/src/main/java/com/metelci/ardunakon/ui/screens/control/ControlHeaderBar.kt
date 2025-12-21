@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.crash.CrashHandler
import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.LatencySparkline
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import com.metelci.ardunakon.wifi.WifiConnectionState

/**
 * Reusable header bar for the control screen.
 *
 * Contains signal indicator, connection button, telemetry graph button,
 * E-STOP button, debug toggle, and overflow menu.
 */
@Suppress("FunctionName")
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
    autoReconnectEnabled: Boolean,
    onToggleAutoReconnect: (Boolean) -> Unit,

    // Encryption status
    isWifiEncrypted: Boolean = false,

    // Button sizes
    buttonSize: Dp = 40.dp,
    eStopSize: Dp = 72.dp,

    // Callbacks
    onScanDevices: () -> Unit,
    onReconnectDevice: () -> Unit,
    onSwitchToWifi: () -> Unit,
    onSwitchToBluetooth: () -> Unit,
    onConfigureWifi: () -> Unit,
    onTelemetryGraph: () -> Unit,
    onToggleEStop: () -> Unit,
    onShowSettings: () -> Unit,
    onShowHelp: () -> Unit,
    onShowAbout: () -> Unit,
    onShowCrashLog: () -> Unit,
    onShowOta: () -> Unit,
    onOpenArduinoCloud: () -> Unit,
    onQuitApp: () -> Unit,

    // Context for crash log check
    context: Context,
    view: View,
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Responsive sizing based on orientation + available width.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 8.dp else 4.dp, vertical = 4.dp)
    ) {
        val actionsCount = if (connectionMode == ConnectionMode.BLUETOOTH) 4 else 3

        val sideSectionsMaxPossibleWidth = ((maxWidth - eStopSize) / 2f).coerceAtLeast(0.dp)
        val isTight = sideSectionsMaxPossibleWidth < 160.dp && !isLandscape

        var itemSpacing = if (isTight) 4.dp else if (isLandscape) 12.dp else 8.dp
        var modeSelectorWidth = if (isTight) 32.dp else if (isLandscape) 48.dp else 40.dp
        var statusWidgetWidth = if (isTight) 48.dp else if (isLandscape) 64.dp else 56.dp
        var rightButtonSize = if (isTight) 32.dp else buttonSize
        var eStopButtonSize = if (isTight) 52.dp else eStopSize
        var widgetHeight = if (isTight) 36.dp else 40.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: [BLE|WiFi] [Signal+RTT+Sparkline]
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    ConnectionModeSelector(
                        selectedMode = connectionMode,
                        onModeSelected = { mode ->
                            if (mode == ConnectionMode.WIFI) onSwitchToWifi() else onSwitchToBluetooth()
                        },
                        view = view,
                        segmentWidth = modeSelectorWidth,
                        modifier = Modifier.height(widgetHeight)
                    )

                    ConnectionStatusWidget(
                        connectionMode = connectionMode,
                        btState = bluetoothConnectionState,
                        wifiState = wifiConnectionState,
                        rssi = if (connectionMode == ConnectionMode.BLUETOOTH) rssiValue else wifiRssi,
                        rttHistory = if (connectionMode == ConnectionMode.BLUETOOTH) rttHistory else wifiRttHistory,
                        isEncrypted = connectionMode == ConnectionMode.WIFI && isWifiEncrypted,
                        onScanDevices = onScanDevices,
                        onReconnect = onReconnectDevice,
                        onConfigure = onConfigureWifi,
                        view = view,
                        modifier = Modifier.width(statusWidgetWidth).height(widgetHeight)
                    )
                }
            }

            // Center section: [STOP]
            Box(
                modifier = Modifier.width(eStopButtonSize),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggleEStop()
                    },
                    modifier = Modifier
                        .size(eStopButtonSize)
                        .semantics {
                            contentDescription = if (isEStopActive) {
                                "Emergency stop active. Tap to release and resume control."
                            } else {
                                "Emergency stop. Tap to immediately stop all motors."
                            }
                        }
                        .shadow(2.dp, CircleShape)
                        .background(
                            if (isEStopActive) {
                                Color(0xFFFF5252)
                            } else {
                                Color(0xFF455A64)
                            },
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isEStopActive) {
                                Color(0xFFD32F2F)
                            } else {
                                Color(0xFF90CAF9)
                            },
                            CircleShape
                        )
                ) {
                    Text(
                        if (isEStopActive) "RESET" else "STOP",
                        color = if (isEStopActive) {
                            Color.White
                        } else {
                            Color(0xFF90CAF9)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right section: [Auto Reconnect] [Graph] [Debug] [Menu]
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                HeaderActionsRow(
                    connectionMode = connectionMode,
                    autoReconnectEnabled = autoReconnectEnabled,
                    onToggleAutoReconnect = onToggleAutoReconnect,
                    rightButtonSize = rightButtonSize,
                    itemSpacing = itemSpacing,
                    view = view,
                    onTelemetryGraph = onTelemetryGraph,
                    onShowSettings = onShowSettings,
                    showOverflowMenu = showOverflowMenu,
                    onToggleOverflowMenu = { showOverflowMenu = !showOverflowMenu },
                    onDismissOverflowMenu = { showOverflowMenu = false },
                    context = context,
                    onShowHelp = onShowHelp,
                    onShowAbout = onShowAbout,
                    onShowCrashLog = onShowCrashLog,
                    onShowOta = onShowOta,
                    onOpenArduinoCloud = onOpenArduinoCloud,
                    onQuitApp = onQuitApp
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun HeaderActionsRow(
    connectionMode: ConnectionMode,
    autoReconnectEnabled: Boolean,
    onToggleAutoReconnect: (Boolean) -> Unit,
    rightButtonSize: Dp,
    itemSpacing: Dp,
    view: View,
    onTelemetryGraph: () -> Unit,
    onShowSettings: () -> Unit,
    showOverflowMenu: Boolean,
    onToggleOverflowMenu: () -> Unit,
    onDismissOverflowMenu: () -> Unit,
    context: Context,
    onShowHelp: () -> Unit,
    onShowAbout: () -> Unit,
    onShowCrashLog: () -> Unit,
    onShowOta: () -> Unit,
    onOpenArduinoCloud: () -> Unit,
    onQuitApp: () -> Unit
) {
    val actionIconSize = (rightButtonSize * 0.5f).coerceIn(14.dp, 18.dp)
    val menuIconSize = (rightButtonSize * 0.45f).coerceIn(14.dp, 16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        if (connectionMode == ConnectionMode.BLUETOOTH) {
            AutoReconnectToggle(
                enabled = autoReconnectEnabled,
                onToggle = onToggleAutoReconnect,
                size = rightButtonSize
            )
        }

        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTelemetryGraph()
            },
            modifier = Modifier
                .size(rightButtonSize)
                .shadow(2.dp, CircleShape)
                .background(Color(0xFF455A64), CircleShape)
                .border(1.dp, Color(0xFF00FF00), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = "Telemetry Graphs",
                tint = Color(0xFF00FF00),
                modifier = Modifier.size(actionIconSize)
            )
        }

        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onShowSettings()
            },
            modifier = Modifier
                .size(rightButtonSize)
                .shadow(2.dp, CircleShape)
                .background(Color(0xFF455A64), CircleShape)
                .border(1.dp, Color(0xFF00C853), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF00C853),
                modifier = Modifier.size(actionIconSize)
            )
        }

        Box {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onToggleOverflowMenu()
                },
                modifier = Modifier
                    .size(rightButtonSize)
                    .shadow(2.dp, CircleShape)
                    .background(Color(0xFF455A64), CircleShape)
                    .border(1.dp, Color(0xFF00FF00), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Menu",
                    tint = Color(0xFF00FF00),
                    modifier = Modifier.size(menuIconSize)
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = onDismissOverflowMenu
            ) {
                DropdownMenuItem(
                    text = { Text("Help") },
                    leadingIcon = { Icon(Icons.Default.Help, null) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onShowHelp()
                        onDismissOverflowMenu()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    leadingIcon = { Icon(Icons.Outlined.Info, null) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onShowAbout()
                        onDismissOverflowMenu()
                    }
                )
                if (CrashHandler.hasCrashLog(context)) {
                    DropdownMenuItem(
                        text = { Text("View Crash Log", color = Color(0xFFFF9800)) },
                        leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onShowCrashLog()
                            onDismissOverflowMenu()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Open Arduino Cloud") },
                    leadingIcon = { Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF00FF00)) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOpenArduinoCloud()
                        onDismissOverflowMenu()
                    }
                )
                Divider(color = Color(0xFF455A64), thickness = 1.dp)
                DropdownMenuItem(
                    text = { Text("Quit App", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF5252)) },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onQuitApp()
                        onDismissOverflowMenu()
                    }
                )
            }
        }
    }
}

/**
 * Pill-shaped connection status widget showing signal strength, RTT, and latency sparkline.
 * Tapping opens a menu for reconnect/configure options.
 */
@Suppress("FunctionName")
@Composable
fun ConnectionStatusWidget(
    connectionMode: ConnectionMode,
    btState: ConnectionState,
    wifiState: WifiConnectionState,
    rssi: Int,
    rttHistory: List<Long>,
    isEncrypted: Boolean = false,
    onReconnect: () -> Unit,
    onConfigure: () -> Unit,
    onScanDevices: () -> Unit = {},
    view: View,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val lastRtt = rttHistory.lastOrNull() ?: 0L

    val stateColor = when {
        connectionMode == ConnectionMode.BLUETOOTH -> when (btState) {
            ConnectionState.CONNECTED -> Color(0xFF00FF00)
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD54F)
            ConnectionState.ERROR -> Color(0xFFFF5252)
            else -> Color.Gray
        }
        else -> when (wifiState) {
            WifiConnectionState.CONNECTED -> Color(0xFF00FF00)
            WifiConnectionState.CONNECTING -> Color(0xFFFFD54F)
            WifiConnectionState.ERROR -> Color(0xFFFF5252)
            else -> Color.Gray
        }
    }

    Box(modifier = modifier) {
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                showMenu = true
            },
            shape = CircleShape,
            color = Color(0xFF455A64),
            border = BorderStroke(1.dp, stateColor),
            modifier = Modifier.matchParentSize()
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
                    // Lock icon when encrypted
                    if (isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted connection",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    SignalStrengthIcon(
                        rssi = rssi,
                        color = stateColor,
                        isWifi = connectionMode == ConnectionMode.WIFI,
                        showLabels = false,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${lastRtt}ms",
                        fontSize = 9.sp,
                        color = stateColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                LatencySparkline(
                    rttValues = rttHistory,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (connectionMode == ConnectionMode.BLUETOOTH) {
                DropdownMenuItem(
                    text = { Text("Scan for Devices") },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showMenu = false
                        onScanDevices()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Reconnect Device") },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showMenu = false
                        onReconnect()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Configure WiFi") },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showMenu = false
                        onConfigure()
                    }
                )
            }
        }
    }
}

/**
 * Compact pill-shaped segmented button for switching between BLE and WiFi modes.
 * Custom implementation for precise size control and centered content.
 */
@Suppress("FunctionName")
@Composable
fun ConnectionModeSelector(
    selectedMode: ConnectionMode,
    onModeSelected: (ConnectionMode) -> Unit,
    view: View,
    segmentWidth: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    // Scale icon size based on segment width
    val iconSize = (segmentWidth * 0.6f).coerceIn(16.dp, 24.dp)

    Row(
        modifier = modifier
            .border(1.dp, Color(0xFF00FF00), CircleShape)
            .background(Color(0xFF455A64), CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BLE Button
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onModeSelected(ConnectionMode.BLUETOOTH)
            },
            color = if (selectedMode == ConnectionMode.BLUETOOTH) {
                Color(0xFF00FF00).copy(alpha = 0.25f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
            modifier = Modifier
                .width(segmentWidth)
                .fillMaxHeight()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = if (selectedMode == ConnectionMode.BLUETOOTH) Color(0xFF00FF00) else Color.Gray,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF00FF00).copy(alpha = 0.5f))
        )

        // WiFi Button
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onModeSelected(ConnectionMode.WIFI)
            },
            color = if (selectedMode == ConnectionMode.WIFI) {
                Color(0xFF00C853).copy(alpha = 0.25f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
            modifier = Modifier
                .width(segmentWidth)
                .fillMaxHeight()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "WiFi",
                    tint = if (selectedMode == ConnectionMode.WIFI) Color(0xFF00C853) else Color.Gray,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
