package com.metelci.ardunakon.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

class TelemetryHistoryManager(
    private val maxHistorySize: Int = 150  // 10 minutes at 4s intervals
) {
    // Single device circular buffers
    private val batteryHistory = ConcurrentLinkedDeque<TelemetryDataPoint>()
    private val rssiHistory = ConcurrentLinkedDeque<TelemetryDataPoint>()
    private val rttHistory = ConcurrentLinkedDeque<TelemetryDataPoint>()

    // StateFlow for UI reactivity
    private val _historyUpdated = MutableStateFlow(0L)
    val historyUpdated: StateFlow<Long> = _historyUpdated.asStateFlow()

    fun recordBattery(voltage: Float) {
        addDataPoint(batteryHistory, voltage)
    }

    fun recordRssi(rssi: Int) {
        addDataPoint(rssiHistory, rssi.toFloat())
    }

    fun recordRtt(rtt: Long) {
        addDataPoint(rttHistory, rtt.toFloat())
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

    fun getBatteryHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        return filterByTimeRange(batteryHistory, timeRangeMs)
    }

    fun getRssiHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        return filterByTimeRange(rssiHistory, timeRangeMs)
    }

    fun getRttHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        return filterByTimeRange(rttHistory, timeRangeMs)
    }

    private fun filterByTimeRange(
        buffer: ConcurrentLinkedDeque<TelemetryDataPoint>,
        timeRangeMs: Long?
    ): List<TelemetryDataPoint> {
        if (timeRangeMs == null) return buffer.toList()
        val cutoff = System.currentTimeMillis() - timeRangeMs
        return buffer.filter { it.timestamp >= cutoff }
    }

    fun clearAllHistory() {
        batteryHistory.clear()
        rssiHistory.clear()
        rttHistory.clear()
        _historyUpdated.value = System.currentTimeMillis()
    }

    fun getHistorySize(): Int {
        return maxOf(
            batteryHistory.size,
            rssiHistory.size,
            rttHistory.size
        )
    }
}
