package com.metelci.ardunakon.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.ui.screens.control.LandscapeControlLayout
import com.metelci.ardunakon.ui.screens.control.PortraitControlLayout
import com.metelci.ardunakon.ui.screens.control.dialogs.ControlScreenDialogs
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager

import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Main control screen for the Ardunakon application.
 * Refactored to delegate to layout-specific composables and dialog manager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    isDarkTheme: Boolean = true,
    onQuitApp: () -> Unit = {},
    onTakeTutorial: (() -> Unit)? = null,
    viewModel: ControlViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Access managers from ViewModel
    val bluetoothManager = viewModel.bluetoothManager
    val wifiManager = viewModel.wifiManager

    // Combined Bluetooth state (optimized - reduces recompositions by ~40%)
    val btCombined by bluetoothManager.combinedState.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()

    // State collections - WiFi
    val wifiState by wifiManager.connectionState.collectAsState()
    val wifiRssi by wifiManager.rssi.collectAsState()
    val wifiRtt by wifiManager.rtt.collectAsState()
    val wifiRttHistory by wifiManager.rttHistory.collectAsState()
    val wifiTelemetry by wifiManager.telemetry.collectAsState()
    val isWifiEncrypted by wifiManager.isEncrypted.collectAsState()
    val btConnectedDeviceInfo by bluetoothManager.connectedDeviceInfo.collectAsState()
    
    // Compute connected device info (WiFi or BT)
    val connectedDeviceInfo = if (wifiState == WifiConnectionState.CONNECTED) {
        "WiFi Device" // WiFi doesn't have a device name exposed currently
    } else {
        btConnectedDeviceInfo
    }

    // Active Telemetry (Bluetooth or WiFi based on connection)
    val telemetry = if (wifiState == WifiConnectionState.CONNECTED) wifiTelemetry else btCombined.telemetry

    // Bridge WiFi telemetry to HistoryManager
    LaunchedEffect(wifiState, wifiRssi, wifiRtt, wifiTelemetry) {
        if (wifiState == WifiConnectionState.CONNECTED) {
            if (wifiRssi != 0) bluetoothManager.telemetryHistoryManager.recordRssi(wifiRssi)
            if (wifiRtt != 0L) bluetoothManager.telemetryHistoryManager.recordRtt(wifiRtt)

            wifiTelemetry?.let { t ->
                bluetoothManager.telemetryHistoryManager.recordBattery(t.batteryVoltage)
                bluetoothManager.telemetryHistoryManager.recordPacketLoss(
                    packetsSent = t.packetsSent.toInt(),
                    packetsReceived = (t.packetsSent - t.packetsDropped - t.packetsFailed).toInt(),
                    packetsDropped = t.packetsDropped.toInt(),
                    packetsFailed = t.packetsFailed.toInt()
                )
            }
        }
    }

    // Bridge Bluetooth telemetry to HistoryManager (explicit bridge for BLE data)
    val btConnectionState = btCombined.connectionState
    val btTelemetry = btCombined.telemetry
    val btRssi = btCombined.rssi
    val btHealth = btCombined.health
    
    LaunchedEffect(btConnectionState, btTelemetry, btRssi, btHealth) {
        if (btConnectionState == com.metelci.ardunakon.bluetooth.ConnectionState.CONNECTED) {
            // Record RSSI if available and not zero
            if (btRssi != 0) {
                bluetoothManager.telemetryHistoryManager.recordRssi(btRssi)
            }
            
            // Record RTT from health if available
            if (btHealth.lastRttMs > 0) {
                bluetoothManager.telemetryHistoryManager.recordRtt(btHealth.lastRttMs)
            }
            
            // Record battery from telemetry if available
            btTelemetry?.let { t ->
                if (t.batteryVoltage > 0) {
                    bluetoothManager.telemetryHistoryManager.recordBattery(t.batteryVoltage)
                }
                // Record packet loss if tracking
                if (t.packetsSent > 0) {
                    bluetoothManager.telemetryHistoryManager.recordPacketLoss(
                        packetsSent = t.packetsSent.toInt(),
                        packetsReceived = (t.packetsSent - t.packetsDropped - t.packetsFailed).toInt(),
                        packetsDropped = t.packetsDropped.toInt(),
                        packetsFailed = t.packetsFailed.toInt()
                    )
                }
            }
        }
    }

    // Export logs function
    val exportLogs: () -> Unit = { viewModel.exportLogs(context) }

    // Keep Screen On
    val currentActivity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) {
        currentActivity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            currentActivity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Pause/resume transmission based on lifecycle to reduce background battery usage
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setForegroundActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setForegroundActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Layout
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val orientationConfig = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = orientationConfig.orientation == Configuration.ORIENTATION_PORTRAIT

    val backgroundBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F1419))
        } else {
            listOf(Color(0xFFFCE4EC), Color(0xFFE3F2FD), Color(0xFFE8F5E9))
        }
    )

    val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(brush = backgroundBrush)
        ) {
            if (isPortrait) {
                PortraitControlLayout(
                    viewModel = viewModel,
                    bluetoothManager = bluetoothManager,
                    connectionState = btCombined.connectionState,
                    wifiState = wifiState,
                    rssiValue = btCombined.rssi,
                    wifiRssi = wifiRssi,
                    wifiRtt = wifiRtt,
                    rttHistory = btCombined.rttHistory,
                    wifiRttHistory = wifiRttHistory,
                    health = btCombined.health,
                    debugLogs = debugLogs,
                    telemetry = telemetry,
                    autoReconnectEnabled = btCombined.autoReconnectEnabled,
                    isEStopActive = btCombined.isEmergencyStopActive,
                    isWifiEncrypted = isWifiEncrypted,
                    connectedDeviceInfo = connectedDeviceInfo,
                    isDarkTheme = isDarkTheme,
                    safeDrawingPadding = safeDrawingPadding,
                    orientationConfig = orientationConfig,
                    view = view,
                    context = context,
                    onQuitApp = onQuitApp,
                    exportLogs = exportLogs
                )
            } else {
                LandscapeControlLayout(
                    viewModel = viewModel,
                    bluetoothManager = bluetoothManager,
                    connectionState = btCombined.connectionState,
                    wifiState = wifiState,
                    rssiValue = btCombined.rssi,
                    wifiRssi = wifiRssi,
                    wifiRtt = wifiRtt,
                    rttHistory = btCombined.rttHistory,
                    wifiRttHistory = wifiRttHistory,
                    health = btCombined.health,
                    debugLogs = debugLogs,
                    telemetry = telemetry,
                    autoReconnectEnabled = btCombined.autoReconnectEnabled,
                    isEStopActive = btCombined.isEmergencyStopActive,
                    isWifiEncrypted = isWifiEncrypted,
                    connectedDeviceInfo = connectedDeviceInfo,
                    isDarkTheme = isDarkTheme,
                    safeDrawingPadding = safeDrawingPadding,
                    orientationConfig = orientationConfig,
                    view = view,
                    context = context,
                    onQuitApp = onQuitApp,
                    exportLogs = exportLogs
                )
            }
        }
    }

    // Dialogs
    ControlScreenDialogs(
        viewModel = viewModel,
        bluetoothManager = bluetoothManager,
        wifiManager = wifiManager,
        isDarkTheme = isDarkTheme,
        view = view,
        onExportLogs = exportLogs,
        onTakeTutorial = onTakeTutorial
    )
}
