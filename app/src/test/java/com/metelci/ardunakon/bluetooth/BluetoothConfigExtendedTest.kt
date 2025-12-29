package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Extended tests for BluetoothConfig constants and calculations.
 */
class BluetoothConfigExtendedTest {

    // ==================== Backoff Calculation ====================

    @Test
    fun `calculateBackoffDelay for 0 attempts returns BACKOFF_BASE_DELAY_MS`() {
        val delay = BluetoothConfig.calculateBackoffDelay(0)
        assertEquals(BluetoothConfig.BACKOFF_BASE_DELAY_MS, delay)
    }

    @Test
    fun `calculateBackoffDelay for 1 attempt doubles base`() {
        val delay = BluetoothConfig.calculateBackoffDelay(1)
        assertEquals(BluetoothConfig.BACKOFF_BASE_DELAY_MS * 2, delay)
    }

    @Test
    fun `calculateBackoffDelay for 2 attempts quadruples base`() {
        val delay = BluetoothConfig.calculateBackoffDelay(2)
        assertEquals(BluetoothConfig.BACKOFF_BASE_DELAY_MS * 4, delay)
    }

    @Test
    fun `calculateBackoffDelay is capped at BACKOFF_MAX_DELAY_MS`() {
        val delay = BluetoothConfig.calculateBackoffDelay(100)
        assertTrue(delay <= BluetoothConfig.BACKOFF_MAX_DELAY_MS)
    }

    @Test
    fun `calculateBackoffDelay increases exponentially`() {
        val delays = (0..5).map { BluetoothConfig.calculateBackoffDelay(it) }

        for (i in 1 until delays.size) {
            // Each delay should be >= previous (exponential growth until cap)
            assertTrue(
                "Delay at $i should be >= delay at ${i - 1}",
                delays[i] >= delays[i - 1]
            )
        }
    }

    // ==================== Timeout Constants ====================

    @Test
    fun `BLE timeout is longer than Classic timeout`() {
        assertTrue(BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS >= BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS)
    }

    @Test
    fun `Heartbeat interval is positive`() {
        assertTrue(BluetoothConfig.HEARTBEAT_INTERVAL_MS > 0)
    }

    @Test
    fun `MAX_RECONNECT_ATTEMPTS is reasonable`() {
        assertTrue(BluetoothConfig.MAX_RECONNECT_ATTEMPTS >= 1)
        assertTrue(BluetoothConfig.MAX_RECONNECT_ATTEMPTS <= 20)
    }

    // ==================== ACK Thresholds ====================

    @Test
    fun `BLE missed ACK threshold is configured`() {
        assertTrue(BluetoothConfig.MISSED_ACK_THRESHOLD_BLE >= 1)
    }

    @Test
    fun `Classic missed ACK threshold is configured`() {
        assertTrue(BluetoothConfig.MISSED_ACK_THRESHOLD_CLASSIC >= 1)
    }

    // ==================== Constant Relationships ====================

    @Test
    fun `BACKOFF_BASE_DELAY_MS is less than BACKOFF_MAX_DELAY_MS`() {
        assertTrue(BluetoothConfig.BACKOFF_BASE_DELAY_MS < BluetoothConfig.BACKOFF_MAX_DELAY_MS)
    }

    @Test
    fun `Heartbeat interval is shorter than timeouts`() {
        assertTrue(BluetoothConfig.HEARTBEAT_INTERVAL_MS < BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS)
        assertTrue(BluetoothConfig.HEARTBEAT_INTERVAL_MS < BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS)
    }
}
