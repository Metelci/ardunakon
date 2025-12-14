package com.metelci.ardunakon.bluetooth

import android.util.Log
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.experimental.xor

data class Telemetry(
    val batteryVoltage: Float,
    val status: String,
    val packetsSent: Long = 0,
    val packetsDropped: Long = 0,
    val packetsFailed: Long = 0
)

class TelemetryManager(
    private val scope: CoroutineScope,
    private val logCallback: (String, LogType) -> Unit // Simple callback for logging
) {

    // History Manager (UI Graphs)
    private val _telemetryHistoryManager = TelemetryHistoryManager()
    val telemetryHistoryManager: TelemetryHistoryManager = _telemetryHistoryManager

    // State Flows
    private val _rssiValue = MutableStateFlow(0)
    val rssiValue: StateFlow<Int> = _rssiValue.asStateFlow()

    private val _health = MutableStateFlow(ConnectionHealth())
    val health: StateFlow<ConnectionHealth> = _health.asStateFlow()

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private val _rttHistory = MutableStateFlow<List<Long>>(emptyList())
    val rttHistory: StateFlow<List<Long>> = _rttHistory.asStateFlow()

    // Internal State
    private var lastPacketAt = 0L
    private var rssiFailures = 0
    private var lastHeartbeatSentAt = 0L
    private var lastRttMs = 0L
    private var heartbeatSeq = 0
    private var missedHeartbeatAcks = 0

    // Packet stats (updated from ConnectionManager)
    private val packetsSent = AtomicLongArray(1)
    private val packetsDropped = AtomicLongArray(1)
    private val packetsFailed = AtomicLongArray(1)
    
    private var lastTelemetryLogTime = 0L
    private val isDebugMode = com.metelci.ardunakon.BuildConfig.DEBUG

    // --- Public API ---

    fun recordInbound() {
        val now = System.currentTimeMillis()
        val wasTimeout = missedHeartbeatAcks >= 3

        lastPacketAt = now
        rssiFailures = 0
        missedHeartbeatAcks = 0 

        if (wasTimeout) {
            logCallback("Heartbeat recovered", LogType.SUCCESS)
        }

        if (lastHeartbeatSentAt > 0 && now >= lastHeartbeatSentAt) {
            lastRttMs = now - lastHeartbeatSentAt
            addRttToHistory(lastRttMs)
        }
        updateHealth()
    }

    fun updateRssi(rssi: Int) {
        _rssiValue.value = rssi
        if (rssi != 0) {
            _telemetryHistoryManager.recordRssi(rssi)
        }
        updateHealth() // Last packet time doesn't change on RSSI update? Original code did pass it.
    }
    
    fun recordRssiFailure() {
        rssiFailures = (rssiFailures + 1).coerceAtMost(10)
        updateHealth()
    }
    
    fun resetRssiFailures() {
        rssiFailures = 0
    }
    
    fun getRssiFailures() = rssiFailures

    fun onHeartbeatSent(seq: Int) {
        heartbeatSeq = seq
        lastHeartbeatSentAt = System.currentTimeMillis()
        missedHeartbeatAcks++
        updateHealth()
    }
    
    fun resetHeartbeat() {
        missedHeartbeatAcks = 0
        lastHeartbeatSentAt = 0L
        rssiFailures = 0
    }
    
    fun getMissedAcks() = missedHeartbeatAcks
    fun getLastPacketTime() = lastPacketAt

    fun updatePacketStats(sent: Long, dropped: Long, failed: Long) {
        packetsSent.set(0, sent)
        packetsDropped.set(0, dropped)
        packetsFailed.set(0, failed)
        
        // Update telemetry object if exists
        val current = _telemetry.value
        if (current != null) {
            _telemetry.value = current.copy(
                packetsSent = sent,
                packetsDropped = dropped,
                packetsFailed = failed
            )
        }
    }

    fun parseTelemetryPacket(packet: ByteArray) {
        val parsed = TelemetryParser.parse(packet) ?: return
        
        // Preserve local packet stats which are not part of the parsed payload
        val fullTelemetry = parsed.copy(
            packetsSent = packetsSent.get(0),
            packetsDropped = packetsDropped.get(0),
            packetsFailed = packetsFailed.get(0)
        )
        
        _telemetry.value = fullTelemetry
        _telemetryHistoryManager.recordBattery(parsed.batteryVoltage)

        if (isDebugMode && (System.currentTimeMillis() - lastTelemetryLogTime > 20000)) {
            logCallback("Telemetry: Bat=${parsed.batteryVoltage}V, Stat=${parsed.status}", LogType.SUCCESS)
            lastTelemetryLogTime = System.currentTimeMillis()
        }
    }

    private fun updateHealth() {
        _health.value = ConnectionHealth(
            lastPacketAt, 
            rssiFailures, 
            heartbeatSeq, 
            lastHeartbeatSentAt, 
            lastRttMs
        )
        if (lastRttMs > 0) {
            _telemetryHistoryManager.recordRtt(lastRttMs)
        }
    }

    private fun addRttToHistory(rtt: Long) {
        val current = _rttHistory.value.toMutableList()
        current.add(rtt)
        while (current.size > BluetoothConfig.MAX_RTT_HISTORY) {
            current.removeAt(0)
        }
        _rttHistory.value = current
    }
}
