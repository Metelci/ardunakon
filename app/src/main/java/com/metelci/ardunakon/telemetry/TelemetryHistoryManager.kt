package com.metelci.ardunakon.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

class TelemetryHistoryManager(
    private val maxHistorySize: Int = 150  // 10 minutes at 4s intervals
) {
    // Per-slot, per-metric circular buffers
    private val batteryHistory = Array(2) { ConcurrentLinkedDeque<TelemetryDataPoint>() }
    private val rssiHistory = Array(2) { ConcurrentLinkedDeque<TelemetryDataPoint>() }
    private val rttHistory = Array(2) { ConcurrentLinkedDeque<TelemetryDataPoint>() }

    // StateFlow for UI reactivity
    private val _historyUpdated = MutableStateFlow(0L)
    val historyUpdated: StateFlow<Long> = _historyUpdated.asStateFlow()

    fun recordBattery(slot: Int, voltage: Float) {
        if (slot !in 0..1) return
        addDataPoint(batteryHistory[slot], voltage)
    }

    fun recordRssi(slot: Int, rssi: Int) {
        if (slot !in 0..1) return
        addDataPoint(rssiHistory[slot], rssi.toFloat())
    }

    fun recordRtt(slot: Int, rtt: Long) {
        if (slot !in 0..1) return
        addDataPoint(rttHistory[slot], rtt.toFloat())
    }

    private fun addDataPoint(buffer: ConcurrentLinkedDeque<TelemetryDataPoint>, value: Float) {
        val point = TelemetryDataPoint(System.currentTimeMillis(), value)
        buffer.addLast(point)

        // Auto-evict oldest entries when buffer exceeds max size
        while (buffer.size > maxHistorySize) {
            buffer.removeFirst()
        }

        // Notify UI of update
        _historyUpdated.value = System.currentTimeMillis()
    }

    fun getBatteryHistory(slot: Int, timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        if (slot !in 0..1) return emptyList()
        return filterByTimeRange(batteryHistory[slot], timeRangeMs)
    }

    fun getRssiHistory(slot: Int, timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        if (slot !in 0..1) return emptyList()
        return filterByTimeRange(rssiHistory[slot], timeRangeMs)
    }

    fun getRttHistory(slot: Int, timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        if (slot !in 0..1) return emptyList()
        return filterByTimeRange(rttHistory[slot], timeRangeMs)
    }

    private fun filterByTimeRange(
        buffer: ConcurrentLinkedDeque<TelemetryDataPoint>,
        timeRangeMs: Long?
    ): List<TelemetryDataPoint> {
        if (timeRangeMs == null) return buffer.toList()
        val cutoff = System.currentTimeMillis() - timeRangeMs
        return buffer.filter { it.timestamp >= cutoff }
    }

    fun clearHistory(slot: Int) {
        if (slot !in 0..1) return
        batteryHistory[slot].clear()
        rssiHistory[slot].clear()
        rttHistory[slot].clear()
        _historyUpdated.value = System.currentTimeMillis()
    }

    fun clearAllHistory() {
        (0..1).forEach { clearHistory(it) }
    }

    fun getHistorySize(slot: Int): Int {
        if (slot !in 0..1) return 0
        return maxOf(
            batteryHistory[slot].size,
            rssiHistory[slot].size,
            rttHistory[slot].size
        )
    }
}
