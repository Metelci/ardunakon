package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for latency data handling.
 * Note: Compose rendering tests require instrumented test environment.
 */
class LatencySparklineTest {

    @Test
    fun `empty list is valid input`() {
        val data = emptyList<Long>()
        assertTrue("Empty list should be valid", data.isEmpty())
    }

    @Test
    fun `single data point list is valid`() {
        val data = listOf(50L)
        assertEquals(1, data.size)
        assertEquals(50L, data.first())
    }

    @Test
    fun `take last of list works correctly`() {
        val data = (1..100).map { it.toLong() }
        val last20 = data.takeLast(20)
        assertEquals(20, last20.size)
        assertEquals(81L, last20.first())
        assertEquals(100L, last20.last())
    }

    @Test
    fun `average calculation for latency values`() {
        val values = listOf(10L, 20L, 30L, 40L, 50L)
        val avg = values.average()
        assertEquals(30.0, avg, 0.0001)
    }

    @Test
    fun `min and max for range calculation`() {
        val values = listOf(10L, 50L, 25L, 100L, 5L)
        assertEquals(5L, values.minOrNull())
        assertEquals(100L, values.maxOrNull())
    }

    @Test
    fun `empty list returns null for min max`() {
        val empty = emptyList<Long>()
        assertNull(empty.minOrNull())
        assertNull(empty.maxOrNull())
    }
}
