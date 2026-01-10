@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.screens.control

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
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
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.ui.components.LatencySparkline
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import com.metelci.ardunakon.ui.utils.hapticTap
import com.metelci.ardunakon.wifi.WifiConnectionState

/**
 * Reusable header bar for the control screen.
 *
 * Contains signal indicator, connection button, telemetry graph button,
 * E-STOP button, debug toggle, and overflow menu.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Suppress("FunctionName")
@Composable
@UiComposable
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

    // Button sizes (48dp minimum for accessibility)
    buttonSize: Dp = 48.dp,
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
    onShowPerformanceStats: () -> Unit,
    onOpenArduinoCloud: () -> Unit,
    onQuitApp: () -> Unit,

    // Tooltip state
    showBluetoothTooltip: Boolean = false,
    onDismissBluetoothTooltip: () -> Unit = {},

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
        if (connectionMode == ConnectionMode.BLUETOOTH) 4 else 3
        val containerMaxWidth = maxWidth

        val sideSectionsMaxPossibleWidth = ((containerMaxWidth - eStopSize) / 2f).coerceAtLeast(0.dp)
        val isTight = sideSectionsMaxPossibleWidth < 160.dp && !isLandscape

        var itemSpacing = if (isTight) 4.dp else if (isLandscape) 12.dp else 8.dp
        var modeSelectorWidth = if (isTight) 40.dp else if (isLandscape) 48.dp else 48.dp
        var statusWidgetWidth = if (isTight) 48.dp else if (isLandscape) 64.dp else 56.dp
        var rightButtonSize = if (isTight) 48.dp else buttonSize
        var eStopButtonSize = if (isTight) 56.dp else eStopSize
        var widgetHeight = 48.dp // 48dp minimum for accessibility

        // Status widget width - match the BLE/WiFi selector's total width
        val effectiveStatusWidth = (modeSelectorWidth * 2f).coerceAtLeast(0.dp)
        val sectionSpacing = itemSpacing
        val sideSectionsAvailableWidth =
            ((containerMaxWidth - eStopSize - (sectionSpacing * 2f)) / 2f).coerceAtLeast(0.dp)
        val rightActionsHeight = widgetHeight
        val rightActionsDividerWidth = 1.dp
        val rightActionsPreferredWidth = (rightActionsHeight * 1.25f).coerceAtLeast(rightButtonSize * 1.25f)
        val rightActionsMaxSegmentWidth =
            ((sideSectionsAvailableWidth - (rightActionsDividerWidth * 3f)) / 4f).coerceAtLeast(0.dp)
        val rightActionsSegmentWidth = if (rightActionsMaxSegmentWidth > 0.dp) {
            rightActionsPreferredWidth.coerceAtMost(rightActionsMaxSegmentWidth)
        } else {
            rightActionsPreferredWidth
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
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
                    Box(contentAlignment = Alignment.TopCenter) {
                        ConnectionModeSelector(
                            selectedMode = connectionMode,
                            onModeSelected = { mode ->
                                if (mode == ConnectionMode.WIFI) onSwitchToWifi() else onSwitchToBluetooth()
                            },
                            view = view,
                            segmentWidth = modeSelectorWidth,
                            btConnectionState = bluetoothConnectionState,
                            wifiConnectionState = wifiConnectionState,
                            btRssi = rssiValue,
                            wifiRssi = wifiRssi,
                            modifier = Modifier.height(widgetHeight)
                        )

                        if (showBluetoothTooltip) {
                            com.metelci.ardunakon.ui.components.Tooltip(
                                text = "Turn on Bluetooth",
                                modifier = Modifier.padding(top = widgetHeight + 4.dp),
                                onDismiss = onDismissBluetoothTooltip
                            )
                        }
                    }

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
                        modifier = Modifier.width(effectiveStatusWidth).height(widgetHeight)
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
                        view.hapticTap()
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
                            Color(0xFFFF0000),
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
                    pillHeight = rightActionsHeight,
                    segmentWidth = rightActionsSegmentWidth,
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
                    onShowPerformanceStats = onShowPerformanceStats,
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
    pillHeight: Dp,
    segmentWidth: Dp,
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
    onShowPerformanceStats: () -> Unit,
    onOpenArduinoCloud: () -> Unit,
    onQuitApp: () -> Unit
) {
    val actionIconSize = (minOf(pillHeight, segmentWidth) * 0.6f).coerceIn(14.dp, 22.dp)
    val menuIconSize = (minOf(pillHeight, segmentWidth) * 0.55f).coerceIn(14.dp, 20.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        val dividerWidth = 1.dp
        val pillShape = CircleShape
        val pillBorderColor = Color(0xFF00FF00)
        val pillWidth = (segmentWidth * 4f) + (dividerWidth * 3f)
        val autoReconnectBackground = if (autoReconnectEnabled) Color(0xFF43A047) else Color.Transparent
        val autoReconnectTint = if (autoReconnectEnabled) Color.White else Color(0xFFFF5252)

        Box(
            modifier = Modifier
                .size(width = pillWidth, height = pillHeight)
                .shadow(2.dp, pillShape)
                .background(Color(0xFF455A64), pillShape)
                .border(1.dp, pillBorderColor, pillShape)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderActionSegment(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
                    background = autoReconnectBackground,
                    icon = if (autoReconnectEnabled) Icons.Filled.Sync else Icons.Filled.SyncDisabled,
                    iconSize = actionIconSize,
                    tint = autoReconnectTint,
                    contentDescription = if (autoReconnectEnabled) {
                        "Auto-reconnect enabled. Tap to disable automatic reconnection."
                    } else {
                        "Auto-reconnect disabled. Tap to enable automatic reconnection."
                    },
                    onClick = { onToggleAutoReconnect(!autoReconnectEnabled) },
                    view = view
                )

                HeaderActionDivider(color = pillBorderColor, width = dividerWidth)

                HeaderActionSegment(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(0.dp),
                    icon = Icons.Default.ShowChart,
                    iconSize = actionIconSize,
                    tint = Color(0xFF00FF00),
                    contentDescription = "Telemetry Graphs",
                    onClick = onTelemetryGraph,
                    view = view
                )

                HeaderActionDivider(color = pillBorderColor, width = dividerWidth)

                HeaderActionSegment(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(0.dp),
                    icon = Icons.Default.Settings,
                    iconSize = actionIconSize,
                    tint = Color(0xFF00C853),
                    contentDescription = "Settings",
                    onClick = onShowSettings,
                    view = view
                )

                HeaderActionDivider(color = pillBorderColor, width = dividerWidth)

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    HeaderActionSegment(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                        icon = Icons.Default.Help,
                        iconSize = menuIconSize,
                        tint = Color(0xFF00FF00),
                        contentDescription = "Help menu",
                        onClick = { onToggleOverflowMenu() },
                        view = view
                    )

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = onDismissOverflowMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Help") },
                            leadingIcon = { Icon(Icons.Default.Help, null) },
                            onClick = {
                                view.hapticTap()
                                onShowHelp()
                                onDismissOverflowMenu()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            leadingIcon = { Icon(Icons.Outlined.Info, null) },
                            onClick = {
                                view.hapticTap()
                                onShowAbout()
                                onDismissOverflowMenu()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Crash Log", color = Color(0xFFFF9800)) },
                            leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
                            onClick = {
                                view.hapticTap()
                                onShowCrashLog()
                                onDismissOverflowMenu()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Performance Stats") },
                            leadingIcon = { Icon(Icons.Default.ShowChart, null, tint = Color(0xFF00E5FF)) },
                            onClick = {
                                view.hapticTap()
                                onShowPerformanceStats()
                                onDismissOverflowMenu()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open Arduino Cloud") },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF00FF00)) },
                            onClick = {
                                view.hapticTap()
                                onOpenArduinoCloud()
                                onDismissOverflowMenu()
                            }
                        )
                        Divider(color = Color(0xFF455A64), thickness = 1.dp)
                        DropdownMenuItem(
                            text = { Text("Quit App", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Default.Close, null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                view.hapticTap()
                                onQuitApp()
                                onDismissOverflowMenu()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun HeaderActionSegment(
    modifier: Modifier,
    shape: Shape,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconSize: Dp,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    view: View,
    enabled: Boolean = true,
    background: Color = Color.Transparent
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = Modifier.clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = LocalIndication.current
    ) {
        view.hapticTap()
        onClick()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(clickModifier)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun HeaderActionDivider(color: Color, width: Dp) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .background(color)
    )
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
        // Determine if connected
        val isConnected = when (connectionMode) {
            ConnectionMode.BLUETOOTH -> btState == ConnectionState.CONNECTED
            ConnectionMode.WIFI -> wifiState == WifiConnectionState.CONNECTED
        }
        val isConnecting = when (connectionMode) {
            ConnectionMode.BLUETOOTH -> btState == ConnectionState.CONNECTING || btState == ConnectionState.RECONNECTING
            ConnectionMode.WIFI -> wifiState == WifiConnectionState.CONNECTING
        }

        Surface(
            onClick = {
                view.hapticTap()
                if (!isConnected && !isConnecting) {
                    // When disconnected, tapping directly triggers scan
                    if (connectionMode == ConnectionMode.BLUETOOTH) {
                        onScanDevices()
                    } else {
                        onConfigure()
                    }
                } else {
                    showMenu = true
                }
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
                if (isConnected) {
                    // Connected: Show RSSI + latency info
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
                } else if (isConnecting) {
                    // Connecting: Show connecting indicator
                    Text(
                        text = "...",
                        fontSize = 12.sp,
                        color = stateColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Disconnected: Show SCAN button
                    Text(
                        text = "SCAN",
                        fontSize = 11.sp,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Bold
                    )
                }
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
                        view.hapticTap()
                        showMenu = false
                        onScanDevices()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Reconnect Device") },
                    onClick = {
                        view.hapticTap()
                        showMenu = false
                        onReconnect()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Configure WiFi") },
                    onClick = {
                        view.hapticTap()
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
 * Icons change color based on signal strength:
 * - Green: Good signal (RSSI > -60)
 * - Yellow: Medium signal (RSSI -60 to -80)
 * - Blinking Red: Disconnected or poor signal
 */
@Suppress("FunctionName")
@Composable
fun ConnectionModeSelector(
    selectedMode: ConnectionMode,
    onModeSelected: (ConnectionMode) -> Unit,
    view: View,
    segmentWidth: Dp = 48.dp,
    btConnectionState: ConnectionState = ConnectionState.DISCONNECTED,
    wifiConnectionState: WifiConnectionState = WifiConnectionState.DISCONNECTED,
    btRssi: Int = 0,
    wifiRssi: Int = 0,
    modifier: Modifier = Modifier
) {
    // Scale icon size based on segment width
    val iconSize = (segmentWidth * 0.6f).coerceIn(16.dp, 24.dp)

    // Blinking animation for disconnected state
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    // Calculate Bluetooth icon color based on connection state and RSSI
    val btIconColor = when {
        btConnectionState == ConnectionState.DISCONNECTED ->
            Color(0xFFFF5252).copy(alpha = if (selectedMode == ConnectionMode.BLUETOOTH) blinkAlpha else 0.5f)
        btConnectionState == ConnectionState.CONNECTING || btConnectionState == ConnectionState.RECONNECTING ->
            Color(0xFFFFD600) // Yellow for connecting
        btConnectionState == ConnectionState.ERROR ->
            Color(0xFFFF5252) // Red for error
        btRssi > -60 -> Color(0xFF00E676) // Green - excellent
        btRssi > -75 -> Color(0xFFFFD600) // Yellow - good
        btRssi > -85 -> Color(0xFFFF9800) // Orange - fair
        else -> Color(0xFFFF5252) // Red - poor
    }

    // Calculate WiFi icon color based on connection state and RSSI
    val wifiIconColor = when {
        wifiConnectionState == WifiConnectionState.DISCONNECTED ->
            Color(0xFFFF5252).copy(alpha = if (selectedMode == ConnectionMode.WIFI) blinkAlpha else 0.5f)
        wifiConnectionState == WifiConnectionState.CONNECTING ->
            Color(0xFFFFD600) // Yellow for connecting
        wifiConnectionState == WifiConnectionState.ERROR ->
            Color(0xFFFF5252) // Red for error
        wifiRssi > -50 -> Color(0xFF00E676) // Green - excellent
        wifiRssi > -65 -> Color(0xFFFFD600) // Yellow - good
        wifiRssi > -75 -> Color(0xFFFF9800) // Orange - fair
        else -> Color(0xFFFF5252) // Red - poor
    }

    // Border color based on selected mode's connection status
    val borderColor = if (selectedMode == ConnectionMode.BLUETOOTH) {
        when (btConnectionState) {
            ConnectionState.CONNECTED -> Color(0xFF00FF00)
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFD600)
            else -> Color(0xFFFF5252).copy(alpha = blinkAlpha)
        }
    } else {
        when (wifiConnectionState) {
            WifiConnectionState.CONNECTED -> Color(0xFF00FF00)
            WifiConnectionState.CONNECTING -> Color(0xFFFFD600)
            else -> Color(0xFFFF5252).copy(alpha = blinkAlpha)
        }
    }

    Row(
        modifier = modifier
            .border(1.dp, borderColor, CircleShape)
            .background(Color(0xFF455A64), CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BLE Button
        Surface(
            onClick = {
                view.hapticTap()
                onModeSelected(ConnectionMode.BLUETOOTH)
            },
            color = if (selectedMode == ConnectionMode.BLUETOOTH) {
                btIconColor.copy(alpha = 0.25f)
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
                    tint = if (selectedMode == ConnectionMode.BLUETOOTH) {
                        btIconColor
                    } else {
                        btIconColor.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(borderColor.copy(alpha = 0.5f))
        )

        // WiFi Button
        Surface(
            onClick = {
                view.hapticTap()
                onModeSelected(ConnectionMode.WIFI)
            },
            color = if (selectedMode == ConnectionMode.WIFI) {
                wifiIconColor.copy(alpha = 0.25f)
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
                    tint = if (selectedMode == ConnectionMode.WIFI) wifiIconColor else wifiIconColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
