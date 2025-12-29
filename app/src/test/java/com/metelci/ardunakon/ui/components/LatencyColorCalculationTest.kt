package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for latency sparkline color calculation logic.
 *
 * These tests verify the RTT-to-color mapping used in LatencySparkline.
 */
class LatencyColorCalculationTest {

    // Color constants (matching the component)
    private val colorGreen = 0xFF00C853.toInt() // Excellent - < 50ms
    private val colorYellow = 0xFFFFD54F.toInt() // Acceptable - < 100ms
    private val colorRed = 0xFFFF5252.toInt() // Poor - >= 100ms

    /**
     * Replicates the latency-to-color calculation from LatencySparkline.
     * Returns the color code based on average RTT.
     */
    private fun calculateColor(avgRtt: Double): Int = when {
        avgRtt < 50 -> colorGreen
        avgRtt < 100 -> colorYellow
        else -> colorRed
    }

    /**
     * Calculate average RTT from a list of values.
     */
    private fun calculateAverage(values: List<Long>): Double = if (values.isNotEmpty()) values.average() else 0.0

    // ==================== Empty/Zero Data ====================

    @Test
    fun `empty list gives 0 average`() {
        val avg = calculateAverage(emptyList())
        assertEquals(0.0, avg, 0.001)
    }

    @Test
    fun `zero average gives green`() {
        assertEquals(colorGreen, calculateColor(0.0))
    }

    // ==================== Excellent Latency (Green, < 50ms) ====================

    @Test
    fun `latency under 50ms gives green`() {
        assertEquals(colorGreen, calculateColor(10.0))
        assertEquals(colorGreen, calculateColor(25.0))
        assertEquals(colorGreen, calculateColor(49.0))
        assertEquals(colorGreen, calculateColor(49.9))
    }

    @Test
    fun `latency exactly 50ms gives yellow`() {
        assertEquals(colorYellow, calculateColor(50.0))
    }

    // ==================== Acceptable Latency (Yellow, 50-99ms) ====================

    @Test
    fun `latency 50 to 99ms gives yellow`() {
        assertEquals(colorYellow, calculateColor(50.0))
        assertEquals(colorYellow, calculateColor(75.0))
        assertEquals(colorYellow, calculateColor(99.0))
        assertEquals(colorYellow, calculateColor(99.9))
    }

    @Test
    fun `latency exactly 100ms gives red`() {
        assertEquals(colorRed, calculateColor(100.0))
    }

    // ==================== Poor Latency (Red, >= 100ms) ====================

    @Test
    fun `latency 100ms and above gives red`() {
        assertEquals(colorRed, calculateColor(100.0))
        assertEquals(colorRed, calculateColor(150.0))
        assertEquals(colorRed, calculateColor(500.0))
        assertEquals(colorRed, calculateColor(1000.0))
    }

    // ==================== Average Calculation ====================

    @Test
    fun `single value list gives that value as average`() {
        val avg = calculateAverage(listOf(50L))
        assertEquals(50.0, avg, 0.001)
    }

    @Test
    fun `multiple values give correct average`() {
        val avg = calculateAverage(listOf(10L, 20L, 30L))
        assertEquals(20.0, avg, 0.001)
    }

    @Test
    fun `large values calculate correctly`() {
        val avg = calculateAverage(listOf(100L, 200L, 300L))
        assertEquals(200.0, avg, 0.001)
    }

    @Test
    fun `mixed values give correct average`() {
        val avg = calculateAverage(listOf(5L, 95L, 50L, 50L))
        assertEquals(50.0, avg, 0.001)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `list with excellent average gives green`() {
        val values = listOf(10L, 20L, 30L, 40L) // avg = 25
        val avg = calculateAverage(values)
        assertEquals(colorGreen, calculateColor(avg))
    }

    @Test
    fun `list with acceptable average gives yellow`() {
        val values = listOf(50L, 60L, 70L, 80L) // avg = 65
        val avg = calculateAverage(values)
        assertEquals(colorYellow, calculateColor(avg))
    }

    @Test
    fun `list with poor average gives red`() {
        val values = listOf(100L, 150L, 200L) // avg = 150
        val avg = calculateAverage(values)
        assertEquals(colorRed, calculateColor(avg))
    }

    @Test
    fun `boundary average 49_9 gives green`() {
        val values = listOf(49L, 50L, 50L, 50L) // avg = 49.75
        val avg = calculateAverage(values)
        assertEquals(colorGreen, calculateColor(avg))
    }

    @Test
    fun `boundary average 99_9 gives yellow`() {
        val values = listOf(99L, 100L, 100L, 100L) // avg = 99.75
        val avg = calculateAverage(values)
        assertEquals(colorYellow, calculateColor(avg))
    }

    // ==================== Max Values Limit ====================

    @Test
    fun `takeLast limits to maxValues`() {
        val fullList = (1L..100L).toList()
        val maxValues = 20
        val limited = fullList.takeLast(maxValues)

        assertEquals(20, limited.size)
        assertEquals(81L, limited.first())
        assertEquals(100L, limited.last())
    }

    @Test
    fun `takeLast returns all if under limit`() {
        val smallList = listOf(1L, 2L, 3L, 4L, 5L)
        val maxValues = 20
        val limited = smallList.takeLast(maxValues)

        assertEquals(5, limited.size)
    }
}
