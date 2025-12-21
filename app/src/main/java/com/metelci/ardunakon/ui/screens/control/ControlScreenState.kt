package com.metelci.ardunakon.ui.screens.control

import androidx.compose.foundation.layout.PaddingValues
import com.metelci.ardunakon.bluetooth.ConnectionHealth
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.wifi.WifiConnectionState

/**
 * Unified state holder for control screen layouts.
 * Consolidates the 18+ individual parameters into a single data class,
 * reducing parameter passing complexity and improving maintainability.
 */
data class ControlScreenState(
    // Connection state
    val connectionState: ConnectionState,
    val wifiState: WifiConnectionState,
    val isWifiEncrypted: Boolean = false,
    
    // Signal quality
    val rssiValue: Int,
    val wifiRssi: Int,
    val wifiRtt: Long,
    val rttHistory: List<Long>,
    val wifiRttHistory: List<Long>,
    val health: ConnectionHealth?,
    
    // Telemetry and logs
    val telemetry: Telemetry?,
    val debugLogs: List<LogEntry>,
    
    // Control flags
    val autoReconnectEnabled: Boolean,
    val isEStopActive: Boolean
)

/**
 * Environment state that doesn't change during the session.
 * Separated from ControlScreenState to avoid unnecessary recompositions.
 */
data class ControlScreenEnvironment(
    val safeDrawingPadding: PaddingValues,
    val orientationConfig: android.content.res.Configuration,
    val view: android.view.View,
    val context: android.content.Context,
    val isDarkTheme: Boolean
)
