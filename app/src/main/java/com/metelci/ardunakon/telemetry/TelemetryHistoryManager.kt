package com.metelci.ardunakon.telemetry

import java.util.concurrent.ConcurrentLinkedDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores recent telemetry samples and exposes history for charts.
 *
 * @param maxHistorySize Max samples retained per series (defaults to ~10 minutes at 4s interval).
 */
class TelemetryHistoryManager(private val maxHistorySize: Int = 150) { // 10 minutes at 4s intervals
    // Single device circular buffers
    private val batteryHistory = ConcurrentLinkedDeque<TelemetryDataPoint>()
    private val rssiHistory = ConcurrentLinkedDeque<TelemetryDataPoint>()
    private val rttHistory = ConcurrentLinkedDeque<TelemetryDataPoint>()
    private val packetLossHistory = ConcurrentLinkedDeque<PacketLossDataPoint>()

    // Time-based cleanup: remove data older than 10 minutes
    private val maxHistoryAgeMs = 10 * 60 * 1000L

    // StateFlow for UI reactivity
    private val _historyUpdated = MutableStateFlow(0L)

    /**
     * Emits a timestamp whenever history is updated, for UI refresh.
     */
    val historyUpdated: StateFlow<Long> = _historyUpdated.asStateFlow()

    /**
     * Records a battery voltage sample.
     *
     * @param voltage Battery voltage in volts.
     */
    fun recordBattery(voltage: Float) {
        addDataPoint(batteryHistory, voltage)
    }

    /**
     * Records an RSSI sample.
     *
     * @param rssi RSSI in dBm.
     */
    fun recordRssi(rssi: Int) {
        addDataPoint(rssiHistory, rssi.toFloat())
    }

    /**
     * Records an RTT sample.
     *
     * @param rtt Round-trip time in milliseconds.
     */
    fun recordRtt(rtt: Long) {
        addDataPoint(rttHistory, rtt.toFloat())
    }

    /**
     * Records packet loss statistics.
     *
     * @param packetsSent Total packets sent.
     * @param packetsReceived Total packets received.
     * @param packetsDropped Total packets dropped.
     * @param packetsFailed Total packets failed.
     */
    fun recordPacketLoss(packetsSent: Int, packetsReceived: Int, packetsDropped: Int, packetsFailed: Int) {
        val totalLoss = packetsDropped + packetsFailed
        val lossPercent = if (packetsSent > 0) {
            (totalLoss.toFloat() / packetsSent * 100f)
        } else {
            0f
        }

        val point = PacketLossDataPoint(
            timestamp = System.currentTimeMillis(),
            packetsSent = packetsSent,
            packetsReceived = packetsReceived,
            packetsDropped = packetsDropped,
            packetsFailed = packetsFailed,
            lossPercent = lossPercent
        )

        packetLossHistory.addLast(point)

        // Size-based cleanup
        while (packetLossHistory.size > maxHistorySize) {
            packetLossHistory.removeFirst()
        }

        // Time-based cleanup
        cleanupStalePacketLossData()

        _historyUpdated.value = System.currentTimeMillis()
    }

    private fun addDataPoint(buffer: ConcurrentLinkedDeque<TelemetryDataPoint>, value: Float) {
        val point = TelemetryDataPoint(System.currentTimeMillis(), value)
        buffer.addLast(point)

        // Size-based cleanup: evict oldest entries when buffer exceeds max size
        while (buffer.size > maxHistorySize) {
            buffer.removeFirst()
        }

        // Time-based cleanup: remove data older than maxHistoryAgeMs
        cleanupStaleData(buffer)

        // Notify UI of update
        _historyUpdated.value = System.currentTimeMillis()
    }

    /**
     * Remove entries older than maxHistoryAgeMs from a TelemetryDataPoint buffer.
     */
    private fun cleanupStaleData(buffer: ConcurrentLinkedDeque<TelemetryDataPoint>) {
        val cutoff = System.currentTimeMillis() - maxHistoryAgeMs
        while (buffer.isNotEmpty() && buffer.first.timestamp < cutoff) {
            buffer.pollFirst()
        }
    }

    /**
     * Remove entries older than maxHistoryAgeMs from packet loss buffer.
     */
    private fun cleanupStalePacketLossData() {
        val cutoff = System.currentTimeMillis() - maxHistoryAgeMs
        while (packetLossHistory.isNotEmpty() && packetLossHistory.first.timestamp < cutoff) {
            packetLossHistory.pollFirst()
        }
    }

    /**
     * Manually trigger cleanup of all stale data across all buffers.
     * Useful when reconnecting or on session start.
     */
    /**
     * Manually removes stale data across all telemetry buffers.
     */
    fun cleanupAllStaleData() {
        cleanupStaleData(batteryHistory)
        cleanupStaleData(rssiHistory)
        cleanupStaleData(rttHistory)
        cleanupStalePacketLossData()
        _historyUpdated.value = System.currentTimeMillis()
    }

    /**
     * Returns battery history optionally filtered to a time range.
     *
     * @param timeRangeMs Optional window size in milliseconds.
     * @return List of telemetry points.
     */
    fun getBatteryHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> =
        filterByTimeRange(batteryHistory, timeRangeMs)

    /**
     * Returns RSSI history optionally filtered to a time range.
     *
     * @param timeRangeMs Optional window size in milliseconds.
     * @return List of telemetry points.
     */
    fun getRssiHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> =
        filterByTimeRange(rssiHistory, timeRangeMs)

    /**
     * Returns RTT history optionally filtered to a time range.
     *
     * @param timeRangeMs Optional window size in milliseconds.
     * @return List of telemetry points.
     */
    fun getRttHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> = filterByTimeRange(rttHistory, timeRangeMs)

    /**
     * Returns packet loss history optionally filtered to a time range.
     *
     * @param timeRangeMs Optional window size in milliseconds.
     * @return List of packet loss points.
     */
    fun getPacketLossHistory(timeRangeMs: Long? = null): List<PacketLossDataPoint> {
        if (timeRangeMs == null) return packetLossHistory.toList()
        val cutoff = System.currentTimeMillis() - timeRangeMs
        return packetLossHistory.filter { it.timestamp >= cutoff }
    }

    /**
     * Calculate connection quality score (0-100%) based on RSSI, RTT, and packet loss.
     * - RSSI weight: 40% (signal strength)
     * - RTT weight: 30% (latency)
     * - Packet loss weight: 30% (reliability)
     */
    /**
     * Returns a derived connection quality history series.
     *
     * @param timeRangeMs Optional window size in milliseconds.
     * @return List of quality score points (0-100%).
     */
    fun getConnectionQualityHistory(timeRangeMs: Long? = null): List<TelemetryDataPoint> {
        val rssiData = getRssiHistory(timeRangeMs)
        val rttData = getRttHistory(timeRangeMs)
        val lossData = getPacketLossHistory(timeRangeMs)

        if (rssiData.isEmpty() && rttData.isEmpty() && lossData.isEmpty()) {
            return emptyList()
        }

        // Merge time series by finding closest timestamps
        val allTimestamps = (
            rssiData.map { it.timestamp } +
                rttData.map { it.timestamp } +
                lossData.map { it.timestamp }
            ).distinct().sorted()

        return allTimestamps.mapNotNull { timestamp ->
            val rssi = rssiData.findClosest(timestamp)?.value?.toInt() ?: -70 // Default: moderate signal
            val rtt = rttData.findClosest(timestamp)?.value?.toLong() ?: 100L // Default: 100ms
            val loss = lossData.findClosest(timestamp)?.lossPercent ?: 0f // Default: no loss

            val quality = calculateQualityScore(rssi, rtt, loss)
            TelemetryDataPoint(timestamp, quality)
        }
    }

    private fun List<TelemetryDataPoint>.findClosest(timestamp: Long): TelemetryDataPoint? {
        return minByOrNull { kotlin.math.abs(it.timestamp - timestamp) }
    }

    private fun List<PacketLossDataPoint>.findClosest(timestamp: Long): PacketLossDataPoint? {
        return minByOrNull { kotlin.math.abs(it.timestamp - timestamp) }
    }

    private fun calculateQualityScore(rssi: Int, rtt: Long, packetLoss: Float): Float {
        // RSSI score: -100dBm = 0%, 0dBm = 100%
        val rssiScore = ((rssi + 100) / 100f).coerceIn(0f, 1f)

        // RTT score: 0ms = 100%, 1000ms = 0%
        val rttScore = (1f - (rtt / 1000f)).coerceIn(0f, 1f)

        // Loss score: 0% = 100%, 100% = 0%
        val lossScore = (1f - (packetLoss / 100f)).coerceIn(0f, 1f)

        // Weighted average
        return (rssiScore * 0.4f + rttScore * 0.3f + lossScore * 0.3f) * 100f
    }

    private fun filterByTimeRange(
        buffer: ConcurrentLinkedDeque<TelemetryDataPoint>,
        timeRangeMs: Long?
    ): List<TelemetryDataPoint> {
        if (timeRangeMs == null) return buffer.toList()
        val cutoff = System.currentTimeMillis() - timeRangeMs
        return buffer.filter { it.timestamp >= cutoff }
    }

    /**
     * Clears all stored telemetry history.
     */
    fun clearAllHistory() {
        batteryHistory.clear()
        rssiHistory.clear()
        rttHistory.clear()
        packetLossHistory.clear()
        _historyUpdated.value = System.currentTimeMillis()
    }

    /**
     * Returns the maximum size across all telemetry buffers.
     *
     * @return Maximum history size across all series.
     */
    fun getHistorySize(): Int = maxOf(
        batteryHistory.size,
        rssiHistory.size,
        rttHistory.size,
        packetLossHistory.size
    )
}

/**
 * Data point for packet loss tracking.
 *
 * @property timestamp Sample timestamp in milliseconds.
 * @property packetsSent Total packets sent.
 * @property packetsReceived Total packets received.
 * @property packetsDropped Total packets dropped.
 * @property packetsFailed Total packets failed.
 * @property lossPercent Packet loss percentage (0-100).
 */
data class PacketLossDataPoint(
    val timestamp: Long,
    val packetsSent: Int,
    val packetsReceived: Int,
    val packetsDropped: Int,
    val packetsFailed: Int,
    val lossPercent: Float
)
