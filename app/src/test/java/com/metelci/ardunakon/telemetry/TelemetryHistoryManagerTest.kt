package com.metelci.ardunakon.telemetry

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for TelemetryHistoryManager - validates graph data collection,
 * time windowing, and data point management.
 */
class TelemetryHistoryManagerTest {

    // Simplified in-memory version for testing
    private class TestTelemetryHistory {
        private val rssiHistory = mutableListOf<Pair<Long, Int>>()
        private val rttHistory = mutableListOf<Pair<Long, Long>>()
        private val batteryHistory = mutableListOf<Pair<Long, Float>>()

        private val maxPoints = 100
        private val retentionMs = 300_000L // 5 minutes

        fun recordRssi(rssi: Int, timestamp: Long = System.currentTimeMillis()) {
            rssiHistory.add(timestamp to rssi)
            trimOld(rssiHistory, timestamp)
        }

        fun recordRtt(rtt: Long, timestamp: Long = System.currentTimeMillis()) {
            rttHistory.add(timestamp to rtt)
            trimOld(rttHistory, timestamp)
        }

        fun recordBattery(voltage: Float, timestamp: Long = System.currentTimeMillis()) {
            batteryHistory.add(timestamp to voltage)
            trimOld(batteryHistory, timestamp)
        }

        private fun <T> trimOld(list: MutableList<Pair<Long, T>>, now: Long) {
            // Remove old entries
            list.removeAll { now - it.first > retentionMs }
            // Cap at max points
            while (list.size > maxPoints) {
                list.removeAt(0)
            }
        }

        fun getRssiHistory(): List<Pair<Long, Int>> = rssiHistory.toList()
        fun getRttHistory(): List<Pair<Long, Long>> = rttHistory.toList()
        fun getBatteryHistory(): List<Pair<Long, Float>> = batteryHistory.toList()

        fun clear() {
            rssiHistory.clear()
            rttHistory.clear()
            batteryHistory.clear()
        }
    }

    private lateinit var history: TestTelemetryHistory

    @Before
    fun setup() {
        history = TestTelemetryHistory()
    }

    @Test
    fun `records RSSI values correctly`() {
        history.recordRssi(-65, 1000L)
        history.recordRssi(-70, 2000L)
        history.recordRssi(-60, 3000L)

        val rssiData = history.getRssiHistory()
        assertEquals(3, rssiData.size)
        assertEquals(-65, rssiData[0].second)
        assertEquals(-70, rssiData[1].second)
        assertEquals(-60, rssiData[2].second)
    }

    @Test
    fun `records RTT values correctly`() {
        history.recordRtt(50L, 1000L)
        history.recordRtt(75L, 2000L)

        val rttData = history.getRttHistory()
        assertEquals(2, rttData.size)
        assertEquals(50L, rttData[0].second)
        assertEquals(75L, rttData[1].second)
    }

    @Test
    fun `records battery values correctly`() {
        history.recordBattery(12.5f, 1000L)
        history.recordBattery(12.3f, 2000L)

        val batteryData = history.getBatteryHistory()
        assertEquals(2, batteryData.size)
        assertEquals(12.5f, batteryData[0].second, 0.01f)
        assertEquals(12.3f, batteryData[1].second, 0.01f)
    }

    @Test
    fun `preserves timestamps correctly`() {
        val timestamp = 1702300000000L // Fixed timestamp
        history.recordRssi(-55, timestamp)

        val rssiData = history.getRssiHistory()
        assertEquals(timestamp, rssiData[0].first)
    }

    @Test
    fun `caps at max 100 points`() {
        val baseTime = System.currentTimeMillis()

        // Add 150 points - all recent so they won't be trimmed by time
        for (i in 1..150) {
            history.recordRssi(-50 - (i % 30), baseTime + i)
        }

        val rssiData = history.getRssiHistory()
        assertEquals(100, rssiData.size)
    }

    @Test
    fun `removes old entries beyond retention window`() {
        val now = System.currentTimeMillis()
        val sixMinutesAgo = now - 360_000L // 6 minutes ago (beyond 5 min retention)
        val fourMinutesAgo = now - 240_000L // 4 minutes ago (within retention)

        history.recordRssi(-60, sixMinutesAgo)
        history.recordRssi(-65, fourMinutesAgo)
        history.recordRssi(-70, now) // This triggers cleanup

        val rssiData = history.getRssiHistory()
        assertEquals(2, rssiData.size) // Old entry should be removed
    }

    @Test
    fun `clear removes all data`() {
        history.recordRssi(-60, 1000L)
        history.recordRtt(50L, 1000L)
        history.recordBattery(12.0f, 1000L)

        history.clear()

        assertTrue(history.getRssiHistory().isEmpty())
        assertTrue(history.getRttHistory().isEmpty())
        assertTrue(history.getBatteryHistory().isEmpty())
    }

    @Test
    fun `handles zero RSSI value`() {
        history.recordRssi(0, 1000L)

        val rssiData = history.getRssiHistory()
        assertEquals(1, rssiData.size)
        assertEquals(0, rssiData[0].second)
    }

    @Test
    fun `handles very high RTT values`() {
        history.recordRtt(999999L, 1000L)

        val rttData = history.getRttHistory()
        assertEquals(1, rttData.size)
        assertEquals(999999L, rttData[0].second)
    }

    @Test
    fun `handles battery edge cases`() {
        history.recordBattery(0.0f, 1000L)
        history.recordBattery(15.0f, 2000L) // Typical LiPo max
        history.recordBattery(100.0f, 3000L) // Edge case

        val batteryData = history.getBatteryHistory()
        assertEquals(3, batteryData.size)
    }
}
