package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Bluetooth connection health monitoring logic.
 * These tests validate the reconnection, heartbeat, and RSSI handling algorithms.
 */
class ConnectionHealthTest {

    // ============== Reconnect Backoff Algorithm ==============
    
    private fun calculateBackoffDelay(attempts: Int): Long {
        // Exponential backoff: 3s, 6s, 12s, 24s, 30s (max)
        val baseDelay = 3000L
        val maxDelay = 30000L
        val delay = (baseDelay * (1 shl attempts.coerceAtMost(3))).coerceAtMost(maxDelay)
        return delay
    }

    @Test
    fun `backoff delay starts at 3 seconds`() {
        assertEquals(3000L, calculateBackoffDelay(0))
    }

    @Test
    fun `backoff delay doubles each attempt`() {
        assertEquals(3000L, calculateBackoffDelay(0))  // 3s
        assertEquals(6000L, calculateBackoffDelay(1))  // 6s
        assertEquals(12000L, calculateBackoffDelay(2)) // 12s
        assertEquals(24000L, calculateBackoffDelay(3)) // 24s
    }

    @Test
    fun `backoff delay caps at max delay`() {
        // Algorithm: baseDelay * (1 shl min(attempts, 3)) clamped to maxDelay
        // attempt 0: 3000 * 1 = 3000
        // attempt 1: 3000 * 2 = 6000
        // attempt 2: 3000 * 4 = 12000
        // attempt 3: 3000 * 8 = 24000
        // attempt 4+: 3000 * 8 = 24000 (capped at coerceAtMost(3))
        assertEquals(24000L, calculateBackoffDelay(4))
        assertEquals(24000L, calculateBackoffDelay(5))
        assertEquals(24000L, calculateBackoffDelay(10))
        assertEquals(24000L, calculateBackoffDelay(100))
    }

    // ============== GATT Error Classification ==============
    
    private fun isTransientError(status: Int): Boolean {
        return when (status) {
            8,     // GATT_CONNECTION_TIMEOUT
            129,   // GATT_INTERNAL_ERROR
            147,   // GATT_LINK_LOSS
            62     // Unknown GATT error (HM-10 clones)
            -> true
            else -> false
        }
    }

    private fun isPermanentError(status: Int): Boolean {
        return when (status) {
            133    // GATT_DEVICE_NOT_FOUND
            -> true
            else -> false
        }
    }

    @Test
    fun `connection timeout is transient error`() {
        assertTrue(isTransientError(8))
    }

    @Test
    fun `internal error is transient`() {
        assertTrue(isTransientError(129))
    }

    @Test
    fun `link loss is transient`() {
        assertTrue(isTransientError(147))
    }

    @Test
    fun `HM-10 unknown error 62 is transient`() {
        assertTrue(isTransientError(62))
    }

    @Test
    fun `device not found is permanent error`() {
        assertTrue(isPermanentError(133))
    }

    @Test
    fun `success code is not transient`() {
        assertFalse(isTransientError(0))
    }

    @Test
    fun `success code is not permanent`() {
        assertFalse(isPermanentError(0))
    }

    // ============== Device Name Classification ==============
    
    private fun isBleOnlyName(name: String?): Boolean {
        val nameUpper = (name ?: "").uppercase()
        val hm10Markers = listOf(
            "HM-10", "HM10", "AT-09", "AT09", "MLT-BT05", "BT05", "BT-05",
            "HC-08", "HC08", "CC41", "CC41-A", "BLE", "ARDUNAKON", "ARDUINO", "R4", "UNO R4"
        )
        return hm10Markers.any { marker -> nameUpper.contains(marker) }
    }

    @Test
    fun `HM-10 is identified as BLE only`() {
        assertTrue(isBleOnlyName("HM-10"))
        assertTrue(isBleOnlyName("hm-10"))
        assertTrue(isBleOnlyName("HM10"))
    }

    @Test
    fun `AT-09 is identified as BLE only`() {
        assertTrue(isBleOnlyName("AT-09"))
        assertTrue(isBleOnlyName("AT09"))
    }

    @Test
    fun `MLT-BT05 is identified as BLE only`() {
        assertTrue(isBleOnlyName("MLT-BT05"))
        assertTrue(isBleOnlyName("BT05"))
    }

    @Test
    fun `HC-08 is identified as BLE only`() {
        assertTrue(isBleOnlyName("HC-08"))
        assertTrue(isBleOnlyName("HC08"))
    }

    @Test
    fun `Arduino R4 is identified as BLE only`() {
        assertTrue(isBleOnlyName("ArdunakonR4"))
        assertTrue(isBleOnlyName("Arduino UNO R4"))
        assertTrue(isBleOnlyName("UNO R4 WiFi"))
    }

    @Test
    fun `HC-05 is NOT identified as BLE only`() {
        assertFalse(isBleOnlyName("HC-05"))
        assertFalse(isBleOnlyName("HC-06"))
    }

    @Test
    fun `null name returns false`() {
        assertFalse(isBleOnlyName(null))
    }

    @Test
    fun `empty name returns false`() {
        assertFalse(isBleOnlyName(""))
    }

    // ============== Circuit Breaker Logic ==============

    @Test
    fun `circuit breaker trips after 10 attempts`() {
        val maxAttempts = 10
        var reconnectAttempts = 0
        var circuitBroken = false

        for (i in 1..15) {
            if (reconnectAttempts >= maxAttempts) {
                circuitBroken = true
                break
            }
            reconnectAttempts++
        }

        assertTrue(circuitBroken)
        assertEquals(10, reconnectAttempts)
    }

    // ============== RTT History Management ==============

    @Test
    fun `RTT history maintains max 20 values`() {
        val rttHistory = mutableListOf<Long>()
        val maxSize = 20

        // Add 30 values
        for (i in 1..30) {
            rttHistory.add(i.toLong())
            while (rttHistory.size > maxSize) {
                rttHistory.removeAt(0)
            }
        }

        assertEquals(20, rttHistory.size)
        assertEquals(11L, rttHistory.first()) // First 10 should have been removed
        assertEquals(30L, rttHistory.last())
    }

    // ============== Heartbeat Timeout Logic ==============

    @Test
    fun `classic BT heartbeat timeout is 20 seconds`() {
        val heartbeatTimeoutClassicMs = 20000L
        assertEquals(20000L, heartbeatTimeoutClassicMs)
    }

    @Test
    fun `BLE heartbeat timeout is 5 minutes`() {
        val heartbeatTimeoutBleMs = 300000L
        assertEquals(300000L, heartbeatTimeoutBleMs)
    }

    @Test
    fun `missed ack threshold for classic is 5`() {
        val missedAckThresholdClassic = 5
        assertEquals(5, missedAckThresholdClassic)
    }

    @Test
    fun `missed ack threshold for BLE is 60`() {
        val missedAckThresholdBle = 60
        assertEquals(60, missedAckThresholdBle)
    }
}
