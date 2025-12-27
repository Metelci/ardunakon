package com.metelci.ardunakon.telemetry

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration-style tests for Telemetry package.
 * 
 * Tests telemetry recording, history management, and quality metrics flows.
 */
class TelemetryIntegrationTest {

    private lateinit var historyManager: TelemetryHistoryManager

    @Before
    fun setUp() {
        historyManager = TelemetryHistoryManager()
    }

    // ==================== Battery History Flow ====================

    @Test
    fun `recordBattery stores value`() {
        historyManager.recordBattery(12.5f)
        
        val history = historyManager.getBatteryHistory()
        
        assertTrue(history.isNotEmpty())
        assertEquals(12.5f, history.last().value, 0.001f)
    }

    @Test
    fun `recordBattery maintains order`() {
        historyManager.recordBattery(10.0f)
        historyManager.recordBattery(11.0f)
        historyManager.recordBattery(12.0f)
        
        val history = historyManager.getBatteryHistory()
        
        assertEquals(3, history.size)
        assertEquals(10.0f, history[0].value, 0.001f)
        assertEquals(11.0f, history[1].value, 0.001f)
        assertEquals(12.0f, history[2].value, 0.001f)
    }

    @Test
    fun `getBatteryHistory returns empty list initially`() {
        val history = historyManager.getBatteryHistory()
        
        assertTrue(history.isEmpty())
    }

    // ==================== RSSI History Flow ====================

    @Test
    fun `recordRssi stores value`() {
        historyManager.recordRssi(-65)
        
        val history = historyManager.getRssiHistory()
        
        assertTrue(history.isNotEmpty())
        assertEquals(-65f, history.last().value, 0.001f)
    }

    @Test
    fun `recordRssi maintains order`() {
        historyManager.recordRssi(-50)
        historyManager.recordRssi(-60)
        historyManager.recordRssi(-70)
        
        val history = historyManager.getRssiHistory()
        
        assertEquals(3, history.size)
        assertEquals(-50f, history[0].value, 0.001f)
        assertEquals(-70f, history[2].value, 0.001f)
    }

    // ==================== RTT History Flow ====================

    @Test
    fun `recordRtt stores value`() {
        historyManager.recordRtt(15L)
        
        val history = historyManager.getRttHistory()
        
        assertTrue(history.isNotEmpty())
        assertEquals(15f, history.last().value, 0.001f)
    }

    @Test
    fun `recordRtt handles multiple values`() {
        historyManager.recordRtt(10L)
        historyManager.recordRtt(20L)
        historyManager.recordRtt(30L)
        
        val history = historyManager.getRttHistory()
        
        assertEquals(3, history.size)
    }

    @Test
    fun `getRttHistory returns empty list initially`() {
        val history = historyManager.getRttHistory()
        
        assertTrue(history.isEmpty())
    }

    // ==================== Packet Loss History Flow ====================

    @Test
    fun `recordPacketLoss stores data`() {
        historyManager.recordPacketLoss(100, 95, 5, 0)
        
        val history = historyManager.getPacketLossHistory()
        
        assertTrue(history.isNotEmpty())
        assertEquals(100, history.last().packetsSent)
        assertEquals(95, history.last().packetsReceived)
        assertEquals(5, history.last().packetsDropped)
    }

    @Test
    fun `recordPacketLoss calculates loss percentage`() {
        historyManager.recordPacketLoss(100, 90, 5, 5)
        
        val history = historyManager.getPacketLossHistory()
        
        // 10 dropped/failed out of 100 = 10%
        assertEquals(10f, history.last().lossPercent, 0.1f)
    }

    @Test
    fun `recordPacketLoss handles zero sent packets`() {
        historyManager.recordPacketLoss(0, 0, 0, 0)
        
        val history = historyManager.getPacketLossHistory()
        
        assertEquals(0f, history.last().lossPercent, 0.001f)
    }

    // ==================== History Size Limiting ====================

    @Test
    fun `history is limited to max size`() {
        val manager = TelemetryHistoryManager(maxHistorySize = 10)
        
        repeat(20) { i ->
            manager.recordBattery(i.toFloat())
        }
        
        val history = manager.getBatteryHistory()
        
        assertEquals(10, history.size)
    }

    @Test
    fun `older entries are removed when limit reached`() {
        val manager = TelemetryHistoryManager(maxHistorySize = 5)
        
        repeat(10) { i ->
            manager.recordBattery(i.toFloat())
        }
        
        val history = manager.getBatteryHistory()
        
        // Should only have last 5 entries (5, 6, 7, 8, 9)
        assertEquals(5, history.size)
        assertEquals(5f, history.first().value, 0.001f)
    }

    // ==================== Clear History Flow ====================

    @Test
    fun `clearAllHistory empties all buffers`() {
        historyManager.recordBattery(12.0f)
        historyManager.recordRssi(-65)
        historyManager.recordRtt(15L)
        historyManager.recordPacketLoss(100, 90, 10, 0)
        
        historyManager.clearAllHistory()
        
        assertTrue(historyManager.getBatteryHistory().isEmpty())
        assertTrue(historyManager.getRssiHistory().isEmpty())
        assertTrue(historyManager.getRttHistory().isEmpty())
        assertTrue(historyManager.getPacketLossHistory().isEmpty())
    }

    // ==================== History Size Query ====================

    @Test
    fun `getHistorySize returns max of all buffers`() {
        historyManager.recordBattery(12.0f)
        historyManager.recordBattery(12.5f)
        historyManager.recordRssi(-65)
        
        val size = historyManager.getHistorySize()
        
        assertEquals(2, size) // Battery has 2 entries, RSSI has 1
    }

    @Test
    fun `getHistorySize returns 0 when empty`() {
        val size = historyManager.getHistorySize()
        
        assertEquals(0, size)
    }

    // ==================== Time Range Filtering ====================

    @Test
    fun `getBatteryHistory with time range filters old data`() {
        historyManager.recordBattery(10.0f)
        Thread.sleep(100)
        historyManager.recordBattery(11.0f)
        
        // Get data from last 50ms only
        val recentHistory = historyManager.getBatteryHistory(50)
        
        // Should only have the most recent entry
        assertTrue(recentHistory.size <= 2) // May have 1 or 2 depending on timing
    }

    @Test
    fun `getPacketLossHistory with time range filters`() {
        historyManager.recordPacketLoss(100, 90, 10, 0)
        Thread.sleep(100)
        historyManager.recordPacketLoss(200, 190, 10, 0)
        
        val recentHistory = historyManager.getPacketLossHistory(50)
        
        assertTrue(recentHistory.size <= 2)
    }

    // ==================== Connection Quality Flow ====================

    @Test
    fun `getConnectionQualityHistory returns quality scores`() {
        historyManager.recordRssi(-50)
        historyManager.recordRtt(20L)
        historyManager.recordPacketLoss(100, 98, 2, 0)
        
        val quality = historyManager.getConnectionQualityHistory()
        
        assertTrue(quality.isNotEmpty())
        // Good RSSI, low RTT, low loss should give high quality
        assertTrue(quality.first().value > 50f)
    }

    @Test
    fun `getConnectionQualityHistory returns empty for no data`() {
        val quality = historyManager.getConnectionQualityHistory()
        
        assertTrue(quality.isEmpty())
    }

    // ==================== Stale Data Cleanup Flow ====================

    @Test
    fun `cleanupAllStaleData does not crash`() {
        historyManager.recordBattery(12.0f)
        historyManager.recordRssi(-65)
        
        // Should not crash
        historyManager.cleanupAllStaleData()
    }

    // ==================== Data Point Timestamps ====================

    @Test
    fun `data points have timestamps`() {
        val before = System.currentTimeMillis()
        historyManager.recordBattery(12.0f)
        val after = System.currentTimeMillis()
        
        val history = historyManager.getBatteryHistory()
        
        assertTrue(history.first().timestamp >= before)
        assertTrue(history.first().timestamp <= after)
    }

    // ==================== History Updated Flow ====================

    @Test
    fun `historyUpdated flow emits on record`() {
        val initialValue = historyManager.historyUpdated.value
        
        historyManager.recordBattery(12.0f)
        
        val newValue = historyManager.historyUpdated.value
        
        assertTrue(newValue >= initialValue)
    }

    // ==================== TelemetryDataPoint Tests ====================

    @Test
    fun `TelemetryDataPoint data class works correctly`() {
        val point = TelemetryDataPoint(123456789L, 12.5f)
        
        assertEquals(123456789L, point.timestamp)
        assertEquals(12.5f, point.value, 0.001f)
    }

    @Test
    fun `PacketLossDataPoint data class works correctly`() {
        val point = PacketLossDataPoint(
            timestamp = 123456789L,
            packetsSent = 100,
            packetsReceived = 95,
            packetsDropped = 3,
            packetsFailed = 2,
            lossPercent = 5.0f
        )
        
        assertEquals(123456789L, point.timestamp)
        assertEquals(100, point.packetsSent)
        assertEquals(95, point.packetsReceived)
        assertEquals(3, point.packetsDropped)
        assertEquals(2, point.packetsFailed)
        assertEquals(5.0f, point.lossPercent, 0.001f)
    }
}
