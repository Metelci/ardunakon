package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogEntry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted

/**
 * Combined state flow that reduces recompositions by consolidating
 * multiple related flows into a single state object.
 * 
 * This is more efficient than collecting 11+ individual flows in the UI layer.
 */
data class CombinedConnectionState(
    val connectionState: ConnectionState,
    val rssi: Int,
    val health: ConnectionHealth,
    val telemetry: Telemetry?,
    val rttHistory: List<Long>,
    val autoReconnectEnabled: Boolean,
    val isEmergencyStopActive: Boolean
)

/**
 * Extension function to create a combined state flow from AppBluetoothManager.
 * Call this once in the manager to expose a consolidated state.
 */
fun AppBluetoothManager.combinedState(scope: CoroutineScope
): StateFlow<CombinedConnectionState> {
    return combine(
        connectionState,
        rssiValue,
        health,
        telemetry,
        rttHistory,
        autoReconnectEnabled,
        isEmergencyStopActive
    ) { values: Array<Any?> ->
        val state = values[0] as ConnectionState
        val rssi = values[1] as Int
        val connectionHealth = values[2] as ConnectionHealth
        val telem = values[3] as Telemetry?
        val rtt = (values[4] as? List<*>)?.filterIsInstance<Long>() ?: emptyList()
        val autoReconnect = values[5] as Boolean
        val estop = values[6] as Boolean
        CombinedConnectionState(
            connectionState = state,
            rssi = rssi,
            health = connectionHealth,
            telemetry = telem,
            rttHistory = rtt,
            autoReconnectEnabled = autoReconnect,
            isEmergencyStopActive = estop
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CombinedConnectionState(
            connectionState = ConnectionState.DISCONNECTED,
            rssi = 0,
            health = ConnectionHealth(),
            telemetry = null,
            rttHistory = emptyList(),
            autoReconnectEnabled = false,
            isEmergencyStopActive = false
        )
    )
}
