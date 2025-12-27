package com.metelci.ardunakon.telemetry

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Telemetry data models.
 */
class TelemetryDataModelTest {

    // ==================== TelemetryDataPoint ====================

    @Test
    fun `TelemetryDataPoint creation`() {
        val point = TelemetryDataPoint(timestamp = 12345L, value = 3.7f)
        
        assertEquals(12345L, point.timestamp)
        assertEquals(3.7f, point.value, 0.001f)
    }

    @Test
    fun `TelemetryDataPoint equality`() {
        val point1 = TelemetryDataPoint(100L, 1.5f)
        val point2 = TelemetryDataPoint(100L, 1.5f)
        
        assertEquals(point1, point2)
    }

    @Test
    fun `TelemetryDataPoint inequality by timestamp`() {
        val point1 = TelemetryDataPoint(100L, 1.5f)
        val point2 = TelemetryDataPoint(200L, 1.5f)
        
        assertNotEquals(point1, point2)
    }

    @Test
    fun `TelemetryDataPoint inequality by value`() {
        val point1 = TelemetryDataPoint(100L, 1.5f)
        val point2 = TelemetryDataPoint(100L, 2.5f)
        
        assertNotEquals(point1, point2)
    }

    @Test
    fun `TelemetryDataPoint copy`() {
        val original = TelemetryDataPoint(100L, 1.5f)
        val copy = original.copy(value = 2.0f)
        
        assertEquals(1.5f, original.value, 0.001f)
        assertEquals(2.0f, copy.value, 0.001f)
        assertEquals(original.timestamp, copy.timestamp)
    }

    // ==================== TelemetrySnapshot ====================

    @Test
    fun `TelemetrySnapshot with all values`() {
        val snapshot = TelemetrySnapshot(
            timestamp = 12345L,
            batteryVoltage = 12.5f,
            rssi = -65,
            rtt = 25L,
            status = "Active"
        )
        
        assertEquals(12345L, snapshot.timestamp)
        assertEquals(12.5f, snapshot.batteryVoltage!!, 0.001f)
        assertEquals(-65, snapshot.rssi)
        assertEquals(25L, snapshot.rtt)
        assertEquals("Active", snapshot.status)
    }

    @Test
    fun `TelemetrySnapshot with null values`() {
        val snapshot = TelemetrySnapshot(
            timestamp = 12345L,
            batteryVoltage = null,
            rssi = null,
            rtt = null,
            status = null
        )
        
        assertEquals(12345L, snapshot.timestamp)
        assertNull(snapshot.batteryVoltage)
        assertNull(snapshot.rssi)
        assertNull(snapshot.rtt)
        assertNull(snapshot.status)
    }

    @Test
    fun `TelemetrySnapshot partial values`() {
        val snapshot = TelemetrySnapshot(
            timestamp = 12345L,
            batteryVoltage = 11.1f,
            rssi = null,
            rtt = 50L,
            status = null
        )
        
        assertNotNull(snapshot.batteryVoltage)
        assertNull(snapshot.rssi)
        assertNotNull(snapshot.rtt)
        assertNull(snapshot.status)
    }

    @Test
    fun `TelemetrySnapshot equality with same values`() {
        val snapshot1 = TelemetrySnapshot(100L, 12.0f, -50, 20L, "OK")
        val snapshot2 = TelemetrySnapshot(100L, 12.0f, -50, 20L, "OK")
        
        assertEquals(snapshot1, snapshot2)
    }

    @Test
    fun `TelemetrySnapshot copy`() {
        val original = TelemetrySnapshot(100L, 12.0f, -50, 20L, "OK")
        val copy = original.copy(status = "Safe Mode")
        
        assertEquals("OK", original.status)
        assertEquals("Safe Mode", copy.status)
    }

    // ==================== MetricType Enum ====================

    @Test
    fun `MetricType has 4 values`() {
        assertEquals(4, MetricType.entries.size)
    }

    @Test
    fun `MetricType values exist`() {
        assertNotNull(MetricType.BATTERY_VOLTAGE)
        assertNotNull(MetricType.RSSI)
        assertNotNull(MetricType.RTT)
        assertNotNull(MetricType.STATUS)
    }

    @Test
    fun `MetricType names are correct`() {
        assertEquals("BATTERY_VOLTAGE", MetricType.BATTERY_VOLTAGE.name)
        assertEquals("RSSI", MetricType.RSSI.name)
        assertEquals("RTT", MetricType.RTT.name)
        assertEquals("STATUS", MetricType.STATUS.name)
    }

    // ==================== Boundary Values ====================

    @Test
    fun `TelemetryDataPoint with zero values`() {
        val point = TelemetryDataPoint(0L, 0f)
        
        assertEquals(0L, point.timestamp)
        assertEquals(0f, point.value, 0.001f)
    }

    @Test
    fun `TelemetryDataPoint with negative value`() {
        val point = TelemetryDataPoint(100L, -5.5f)
        
        assertEquals(-5.5f, point.value, 0.001f)
    }

    @Test
    fun `TelemetrySnapshot batteryVoltage at 30V max`() {
        val snapshot = TelemetrySnapshot(100L, 30.0f, null, null, null)
        
        assertEquals(30.0f, snapshot.batteryVoltage!!, 0.001f)
    }

    @Test
    fun `TelemetrySnapshot rssi at -100 dBm`() {
        val snapshot = TelemetrySnapshot(100L, null, -100, null, null)
        
        assertEquals(-100, snapshot.rssi)
    }

    @Test
    fun `TelemetrySnapshot rssi at 0 dBm`() {
        val snapshot = TelemetrySnapshot(100L, null, 0, null, null)
        
        assertEquals(0, snapshot.rssi)
    }
}
