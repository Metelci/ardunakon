package com.metelci.ardunakon.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.ui.screens.control.ControlViewModel
import com.metelci.ardunakon.ui.screens.control.ControlViewModelFactory
import com.metelci.ardunakon.ui.screens.control.LandscapeControlLayout
import com.metelci.ardunakon.ui.screens.control.PortraitControlLayout
import com.metelci.ardunakon.ui.screens.control.dialogs.ControlScreenDialogs
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager

/**
 * Main control screen for the Ardunakon application.
 * Refactored to delegate to layout-specific composables and dialog manager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    bluetoothManager: AppBluetoothManager,
    wifiManager: WifiManager,
    isDarkTheme: Boolean = true,
    onQuitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ViewModel instantiation
    val viewModel: ControlViewModel = viewModel(
        factory = ControlViewModelFactory(
            bluetoothManager = bluetoothManager,
            wifiManager = wifiManager,
            context = context
        )
    )

    // State collections - Bluetooth
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val rssiValue by bluetoothManager.rssiValue.collectAsState()
    val health by bluetoothManager.health.collectAsState()
    val debugLogs by bluetoothManager.debugLogs.collectAsState()
    val btTelemetry by bluetoothManager.telemetry.collectAsState()
    val autoReconnectEnabled by bluetoothManager.autoReconnectEnabled.collectAsState()
    val rttHistory by bluetoothManager.rttHistory.collectAsState()
    val isEStopActive by bluetoothManager.isEmergencyStopActive.collectAsState()

    // State collections - WiFi
    val wifiState by wifiManager.connectionState.collectAsState()
    val wifiRssi by wifiManager.rssi.collectAsState()
    val wifiRtt by wifiManager.rtt.collectAsState()
    val wifiRttHistory by wifiManager.rttHistory.collectAsState()
    val wifiTelemetry by wifiManager.telemetry.collectAsState()

    // Active Telemetry (Bluetooth or WiFi based on connection)
    val telemetry = if (wifiState == WifiConnectionState.CONNECTED) wifiTelemetry else btTelemetry

    // Bridge WiFi telemetry to HistoryManager
    LaunchedEffect(wifiState, wifiRssi, wifiRtt, wifiTelemetry) {
        if (wifiState == WifiConnectionState.CONNECTED) {
            if (wifiRssi != 0) bluetoothManager.telemetryHistoryManager.recordRssi(wifiRssi)
            if (wifiRtt != 0L) bluetoothManager.telemetryHistoryManager.recordRtt(wifiRtt)

            wifiTelemetry?.let { t ->
                bluetoothManager.telemetryHistoryManager.recordBattery(t.batteryVoltage)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        if (isPortrait) {
            PortraitControlLayout(
                viewModel = viewModel,
                bluetoothManager = bluetoothManager,
                connectionState = connectionState,
                wifiState = wifiState,
                rssiValue = rssiValue,
                wifiRssi = wifiRssi,
                wifiRtt = wifiRtt,
                rttHistory = rttHistory,
                wifiRttHistory = wifiRttHistory,
                health = health,
                debugLogs = debugLogs,
                telemetry = telemetry,
                autoReconnectEnabled = autoReconnectEnabled,
                isEStopActive = isEStopActive,
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
                connectionState = connectionState,
                wifiState = wifiState,
                rssiValue = rssiValue,
                wifiRssi = wifiRssi,
                wifiRtt = wifiRtt,
                rttHistory = rttHistory,
                wifiRttHistory = wifiRttHistory,
                health = health,
                debugLogs = debugLogs,
                telemetry = telemetry,
                autoReconnectEnabled = autoReconnectEnabled,
                isEStopActive = isEStopActive,
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

    // Dialogs
    ControlScreenDialogs(
        viewModel = viewModel,
        bluetoothManager = bluetoothManager,
        wifiManager = wifiManager,
        isDarkTheme = isDarkTheme,
        view = view,
        onExportLogs = exportLogs
    )
}
