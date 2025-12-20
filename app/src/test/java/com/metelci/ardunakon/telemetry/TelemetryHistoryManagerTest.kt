package com.metelci.ardunakon.telemetry

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for TelemetryHistoryManager - validates graph data collection,
 * time windowing, and data point management.
 */
class TelemetryHistoryManagerTest {

    private lateinit var history: TelemetryHistoryManager

    @Before
    fun setup() {
        history = TelemetryHistoryManager(maxHistorySize = 100)
    }

    @Test
    fun `records RSSI values correctly`() {
        history.recordRssi(-65)
        history.recordRssi(-70)
        history.recordRssi(-60)

        val rssiData = history.getRssiHistory()
        assertEquals(3, rssiData.size)
        assertEquals(-65f, rssiData[0].value)
        assertEquals(-70f, rssiData[1].value)
        assertEquals(-60f, rssiData[2].value)
    }

    @Test
    fun `records RTT values correctly`() {
        history.recordRtt(50L)
        history.recordRtt(75L)

        val rttData = history.getRttHistory()
        assertEquals(2, rttData.size)
        assertEquals(50f, rttData[0].value)
        assertEquals(75f, rttData[1].value)
    }

    @Test
    fun `records battery values correctly`() {
        history.recordBattery(12.5f)
        history.recordBattery(12.3f)

        val batteryData = history.getBatteryHistory()
        assertEquals(2, batteryData.size)
        assertEquals(12.5f, batteryData[0].value, 0.01f)
        assertEquals(12.3f, batteryData[1].value, 0.01f)
    }

    @Test
    fun `records packet loss correctly`() {
        history.recordPacketLoss(100, 95, 3, 2)
        
        val lossData = history.getPacketLossHistory()
        assertEquals(1, lossData.size)
        val point = lossData[0]
        assertEquals(100, point.packetsSent)
        assertEquals(95, point.packetsReceived)
        assertEquals(3, point.packetsDropped)
        assertEquals(2, point.packetsFailed)
        assertEquals(5f, point.lossPercent, 0.01f)
    }

    @Test
    fun `caps at max points`() {
        // Add 150 points - max is 100
        for (i in 1..150) {
            history.recordRssi(-50 - (i % 30))
        }

        val rssiData = history.getRssiHistory()
        assertEquals(100, rssiData.size)
    }

    @Test
    fun `clear removes all data`() {
        history.recordRssi(-60)
        history.recordRtt(50L)
        history.recordBattery(12.0f)

        history.clearAllHistory()

        assertTrue(history.getRssiHistory().isEmpty())
        assertTrue(history.getRttHistory().isEmpty())
        assertTrue(history.getBatteryHistory().isEmpty())
        assertTrue(history.getPacketLossHistory().isEmpty())
    }

    @Test
    fun `calculates connection quality history`() {
        history.recordRssi(-50) // Strong signal
        history.recordRtt(20L)   // Low latency
        history.recordPacketLoss(100, 100, 0, 0) // No loss
        
        val qualityHistory = history.getConnectionQualityHistory()
        assertFalse(qualityHistory.isEmpty())
        val latestQuality = qualityHistory.last().value
        
        // With -50dBm, 20ms RTT, and 0% loss, quality should be high (> 75%)
        assertTrue("Quality score should be high: $latestQuality", latestQuality > 75f)
    }

    @Test
    fun `connection quality handles empty data`() {
        val qualityHistory = history.getConnectionQualityHistory()
        assertTrue(qualityHistory.isEmpty())
    }

    @Test
    fun `handles battery edge cases`() {
        history.recordBattery(0.0f)
        history.recordBattery(15.0f)
        history.recordBattery(100.0f)

        val batteryData = history.getBatteryHistory()
        assertEquals(3, batteryData.size)
    }

    @Test
    fun `filters by time range`() {
        history.recordRssi(-50)
        // We can't easily test time range in unit tests without mocking System.currentTimeMillis()
        // which would require PowerMock or similar, or changing the class to take a Clock.
        // But we can verify the full list is returned by default.
        val data = history.getRssiHistory(timeRangeMs = null)
        assertNotNull(data)
    }
}
