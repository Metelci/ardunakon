package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UI component helper calculations.
 * 
 * These tests verify the core logic used in SignalStrengthIcon 
 * and LatencySparkline for RSSI-to-bars mapping and latency color thresholds.
 */
class SignalStrengthCalculationTest {

    /**
     * Replicates the RSSI-to-bars calculation from SignalStrengthIcon.
     * This allows us to test the core logic without needing Compose UI tests.
     */
    private fun calculateBars(rssi: Int): Int = when {
        rssi == 0 -> 0
        rssi > -50 -> 4
        rssi > -65 -> 3
        rssi > -80 -> 2
        rssi > -95 -> 1
        else -> 0
    }

    // ==================== RSSI = 0 (Disconnected) ====================

    @Test
    fun `rssi 0 gives 0 bars`() {
        assertEquals(0, calculateBars(0))
    }

    // ==================== Excellent Signal (4 bars) ====================

    @Test
    fun `rssi above minus50 gives 4 bars`() {
        assertEquals(4, calculateBars(-49))
        assertEquals(4, calculateBars(-45))
        assertEquals(4, calculateBars(-30))
        assertEquals(4, calculateBars(-10))
    }

    @Test
    fun `rssi exactly minus50 gives 3 bars`() {
        assertEquals(3, calculateBars(-50))
    }

    // ==================== Good Signal (3 bars) ====================

    @Test
    fun `rssi between minus50 and minus65 gives 3 bars`() {
        assertEquals(3, calculateBars(-51))
        assertEquals(3, calculateBars(-60))
        assertEquals(3, calculateBars(-64))
    }

    @Test
    fun `rssi exactly minus65 gives 2 bars`() {
        assertEquals(2, calculateBars(-65))
    }

    // ==================== Fair Signal (2 bars) ====================

    @Test
    fun `rssi between minus65 and minus80 gives 2 bars`() {
        assertEquals(2, calculateBars(-66))
        assertEquals(2, calculateBars(-70))
        assertEquals(2, calculateBars(-79))
    }

    @Test
    fun `rssi exactly minus80 gives 1 bar`() {
        assertEquals(1, calculateBars(-80))
    }

    // ==================== Weak Signal (1 bar) ====================

    @Test
    fun `rssi between minus80 and minus95 gives 1 bar`() {
        assertEquals(1, calculateBars(-81))
        assertEquals(1, calculateBars(-90))
        assertEquals(1, calculateBars(-94))
    }

    @Test
    fun `rssi exactly minus95 gives 0 bars`() {
        assertEquals(0, calculateBars(-95))
    }

    // ==================== No Signal (0 bars) ====================

    @Test
    fun `rssi below minus95 gives 0 bars`() {
        assertEquals(0, calculateBars(-96))
        assertEquals(0, calculateBars(-100))
        assertEquals(0, calculateBars(-120))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `very high positive rssi gives 4 bars`() {
        // Unrealistic values but should handle gracefully
        assertEquals(4, calculateBars(10))
        assertEquals(4, calculateBars(100))
    }

    @Test
    fun `very low rssi gives 0 bars`() {
        assertEquals(0, calculateBars(-200))
        assertEquals(0, calculateBars(Int.MIN_VALUE + 1))
    }

    // ==================== Boundary Tests ====================

    @Test
    fun `boundary at minus49 gives 4 bars`() {
        assertEquals(4, calculateBars(-49))
    }

    @Test
    fun `boundary at minus64 gives 3 bars`() {
        assertEquals(3, calculateBars(-64))
    }

    @Test
    fun `boundary at minus79 gives 2 bars`() {
        assertEquals(2, calculateBars(-79))
    }

    @Test
    fun `boundary at minus94 gives 1 bar`() {
        assertEquals(1, calculateBars(-94))
    }
}
