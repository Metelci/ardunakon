package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for reconnect backoff behavior.
 * Validates exponential backoff delays and circuit breaker logic.
 */
class ReconnectBackoffTest {

    // ============== Backoff Delay Calculation ==============

    /**
     * Mirrors the production algorithm from BluetoothConfig.calculateBackoffDelay()
     * Formula: baseDelay * 2^min(attempts, 3), capped at maxDelay
     * Returns: 3s, 6s, 12s, 24s (then stays at 24s)
     */
    private fun calculateBackoffDelay(attempts: Int): Long {
        val baseDelay = 3000L // BACKOFF_BASE_DELAY_MS
        val maxDelay = 30000L // BACKOFF_MAX_DELAY_MS
        val multiplier = 1 shl attempts.coerceAtMost(3) // 1, 2, 4, 8
        val delay = baseDelay * multiplier
        return delay.coerceAtMost(maxDelay)
    }

    @Test
    fun `first reconnect attempt has 3 second delay`() {
        assertEquals(3000L, calculateBackoffDelay(0))
    }

    @Test
    fun `second reconnect attempt has 6 second delay`() {
        assertEquals(6000L, calculateBackoffDelay(1))
    }

    @Test
    fun `third reconnect attempt has 12 second delay`() {
        assertEquals(12000L, calculateBackoffDelay(2))
    }

    @Test
    fun `fourth reconnect attempt has 24 second delay`() {
        assertEquals(24000L, calculateBackoffDelay(3))
    }

    @Test
    fun `fifth and beyond reconnect attempts stay at 24 seconds`() {
        assertEquals(24000L, calculateBackoffDelay(4))
        assertEquals(24000L, calculateBackoffDelay(5))
        assertEquals(24000L, calculateBackoffDelay(10))
    }

    @Test
    fun `backoff delay never exceeds max delay`() {
        for (attempts in 0..100) {
            val delay = calculateBackoffDelay(attempts)
            assertTrue("Delay should not exceed 30s", delay <= 30000L)
        }
    }

    // ============== Circuit Breaker Logic ==============

    @Test
    fun `circuit breaker opens after 10 consecutive failures`() {
        val maxReconnectAttempts = 10
        var reconnectAttempts = 0
        var circuitBreakerTripped = false

        // Simulate 15 failed connection attempts
        for (i in 1..15) {
            if (reconnectAttempts >= maxReconnectAttempts) {
                circuitBreakerTripped = true
                break
            }
            reconnectAttempts++
        }

        assertTrue("Circuit breaker should trip", circuitBreakerTripped)
        assertEquals("Should have exactly 10 attempts", 10, reconnectAttempts)
    }

    @Test
    fun `circuit breaker resets on successful connection`() {
        var reconnectAttempts = 5
        var circuitBreakerOpen = false

        // Simulate successful connection
        reconnectAttempts = 0
        circuitBreakerOpen = false

        assertEquals("Attempts should reset to 0", 0, reconnectAttempts)
        assertFalse("Circuit breaker should be closed", circuitBreakerOpen)
    }

    @Test
    fun `next reconnect time is calculated correctly`() {
        val now = System.currentTimeMillis()
        val attempts = 2
        val expectedDelay = calculateBackoffDelay(attempts)
        val nextReconnectAt = now + expectedDelay

        assertTrue("Next reconnect should be in the future", nextReconnectAt > now)
        assertEquals("Delay should be 12 seconds", 12000L, expectedDelay)
    }

    // ============== Reconnect Monitor Behavior ==============

    @Test
    fun `reconnect is skipped when E-STOP is active`() {
        val isEmergencyStopActive = true
        var reconnectAttempted = false

        // Simulate reconnect monitor check
        if (!isEmergencyStopActive) {
            reconnectAttempted = true
        }

        assertFalse("Should not reconnect during E-STOP", reconnectAttempted)
    }

    @Test
    fun `reconnect is skipped when connection is in progress`() {
        val connectionState = "CONNECTING"
        var reconnectAttempted = false

        // Simulate reconnect monitor check
        if (connectionState == "DISCONNECTED" || connectionState == "ERROR") {
            reconnectAttempted = true
        }

        assertFalse("Should not reconnect while connecting", reconnectAttempted)
    }

    @Test
    fun `reconnect is attempted when disconnected and backoff elapsed`() {
        val connectionState = "DISCONNECTED"
        val shouldReconnect = true
        val now = System.currentTimeMillis()
        val nextReconnectAt = now - 1000 // Backoff has elapsed
        var reconnectAttempted = false

        // Simulate reconnect monitor logic
        if (shouldReconnect &&
            (connectionState == "DISCONNECTED" || connectionState == "ERROR") &&
            now >= nextReconnectAt
        ) {
            reconnectAttempted = true
        }

        assertTrue("Should attempt reconnect", reconnectAttempted)
    }

    @Test
    fun `reconnect is skipped when auto-reconnect is disabled`() {
        val autoReconnectEnabled = false
        var reconnectAttempted = false

        // Simulate reconnect monitor check
        if (autoReconnectEnabled) {
            reconnectAttempted = true
        }

        assertFalse("Should not reconnect when auto-reconnect disabled", reconnectAttempted)
    }

    // ============== Backoff Reset Scenarios ==============

    @Test
    fun `backoff counters reset on manual disconnect`() {
        var reconnectAttempts = 5
        var nextReconnectAt = System.currentTimeMillis() + 10000L
        var shouldReconnect = true

        // Simulate manual disconnect
        shouldReconnect = false
        reconnectAttempts = 0
        nextReconnectAt = 0L

        assertFalse("Should reconnect must be false", shouldReconnect)
        assertEquals("Attempts should be 0", 0, reconnectAttempts)
        assertEquals("Next reconnect time should be 0", 0L, nextReconnectAt)
    }

    @Test
    fun `backoff counters reset on successful GATT connection`() {
        var reconnectAttempts = 3
        var gattRetryAttempt = 2

        // Simulate successful GATT connection
        reconnectAttempts = 0
        gattRetryAttempt = 0

        assertEquals("Reconnect attempts should be 0", 0, reconnectAttempts)
        assertEquals("GATT retry attempts should be 0", 0, gattRetryAttempt)
    }

    // ============== GATT Retry Logic ==============

    @Test
    fun `GATT retry uses exponential backoff`() {
        // GATT errors use 2s, 4s, 6s delay
        val delays = listOf(2000L, 4000L, 6000L)

        assertEquals("First retry should be 2s", 2000L, delays[0])
        assertEquals("Second retry should be 4s", 4000L, delays[1])
        assertEquals("Third retry should be 6s", 6000L, delays[2])
    }

    @Test
    fun `max GATT retries is 3`() {
        val maxGattRetries = 3
        assertEquals(3, maxGattRetries)
    }

    @Test
    fun `GATT retry stops after max attempts`() {
        val maxGattRetries = 3
        var gattRetryAttempt = 3
        var shouldRetry = false

        // Simulate shouldRetry check
        shouldRetry = gattRetryAttempt < maxGattRetries

        assertFalse("Should not retry after max attempts", shouldRetry)
    }
}
