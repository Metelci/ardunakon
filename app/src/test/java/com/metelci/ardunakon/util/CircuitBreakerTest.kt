package com.metelci.ardunakon.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CircuitBreaker.
 */
class CircuitBreakerTest {

    private lateinit var breaker: CircuitBreaker

    @Before
    fun setup() {
        breaker = CircuitBreaker(
            name = "TestBreaker",
            threshold = 3,
            resetTimeoutMs = 1000,
            halfOpenMaxAttempts = 1
        )
    }

    @Test
    fun initialState_isClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state.value)
        assertTrue(breaker.allowRequest())
    }

    @Test
    fun recordFailure_belowThreshold_staysClosed() {
        breaker.recordFailure()
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state.value)
        breaker.recordFailure()
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state.value)
    }

    @Test
    fun recordFailure_atThreshold_opensCircuit() {
        breaker.recordFailure()
        breaker.recordFailure()
        breaker.recordFailure()

        assertEquals(CircuitBreaker.State.OPEN, breaker.state.value)
        assertFalse(breaker.allowRequest())
    }

    @Test
    fun openCircuit_failsFast() {
        // Trip the breaker
        repeat(3) { breaker.recordFailure() }

        assertFalse(breaker.allowRequest())
        assertTrue(breaker.isTripped)
    }

    @Test
    fun recordSuccess_resetsFailureCount() {
        breaker.recordFailure()
        breaker.recordFailure()
        assertEquals(2, breaker.currentFailureCount)

        breaker.recordSuccess()
        assertEquals(0, breaker.currentFailureCount)
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state.value)
    }

    @Test
    fun openCircuit_transitionsToHalfOpen_afterTimeout() {
        // Trip the breaker
        repeat(3) { breaker.recordFailure() }
        assertEquals(CircuitBreaker.State.OPEN, breaker.state.value)

        // Wait for timeout
        Thread.sleep(1100)

        // Should transition to half-open when request allowed
        assertTrue(breaker.allowRequest())
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state.value)
    }

    @Test
    fun halfOpenState_successClosesCircuit() {
        // Trip and wait
        repeat(3) { breaker.recordFailure() }
        Thread.sleep(1100)
        breaker.allowRequest() // Transition to half-open

        breaker.recordSuccess()

        assertEquals(CircuitBreaker.State.CLOSED, breaker.state.value)
        assertFalse(breaker.isTripped)
    }

    @Test
    fun halfOpenState_failureReopensCircuit() {
        // Trip and wait
        repeat(3) { breaker.recordFailure() }
        Thread.sleep(1100)
        breaker.allowRequest() // Transition to half-open

        breaker.recordFailure()

        assertEquals(CircuitBreaker.State.OPEN, breaker.state.value)
    }

    @Test
    fun halfOpenState_limitsAttempts() {
        // Use a breaker with 2 half-open attempts to test limiting
        val testBreaker = CircuitBreaker(
            name = "TestHalfOpen",
            threshold = 3,
            resetTimeoutMs = 1000,
            halfOpenMaxAttempts = 2
        )

        // Trip and wait
        repeat(3) { testBreaker.recordFailure() }
        Thread.sleep(1100)

        // First call transitions from OPEN to HALF_OPEN (doesn't count as attempt)
        assertTrue(testBreaker.allowRequest())
        assertEquals(CircuitBreaker.State.HALF_OPEN, testBreaker.state.value)

        // These count as half-open attempts
        assertTrue(testBreaker.allowRequest()) // Attempt 1 of 2
        assertTrue(testBreaker.allowRequest()) // Attempt 2 of 2
        assertFalse(testBreaker.allowRequest()) // Denied - exceeds max
    }

    @Test
    fun reset_closesCircuit() {
        repeat(3) { breaker.recordFailure() }
        assertEquals(CircuitBreaker.State.OPEN, breaker.state.value)

        breaker.reset()

        assertEquals(CircuitBreaker.State.CLOSED, breaker.state.value)
        assertEquals(0, breaker.currentFailureCount)
    }

    @Test
    fun trip_opensCircuit() {
        breaker.trip()

        assertEquals(CircuitBreaker.State.OPEN, breaker.state.value)
        assertFalse(breaker.allowRequest())
    }

    @Test
    fun getStats_returnsCorrectValues() {
        breaker.recordSuccess()
        breaker.recordSuccess()
        breaker.recordFailure()

        val stats = breaker.getStats()

        assertEquals("TestBreaker", stats.name)
        assertEquals(CircuitBreaker.State.CLOSED, stats.state)
        assertEquals(1, stats.failureCount)
        assertEquals(3, stats.threshold)
        assertEquals(1L, stats.totalFailures)
        assertEquals(2L, stats.totalSuccesses)
    }

    @Test
    fun successRate_calculatesCorrectly() {
        breaker.recordSuccess()
        breaker.recordSuccess()
        breaker.recordSuccess()
        breaker.recordFailure()

        val stats = breaker.getStats()

        assertEquals(0.75f, stats.successRate, 0.01f) // 3 success / 4 total
    }

    @Test
    fun timeUntilReset_returnsZeroWhenClosed() {
        assertEquals(0L, breaker.timeUntilReset)
    }

    @Test
    fun timeUntilReset_returnsPositiveWhenOpen() {
        repeat(3) { breaker.recordFailure() }

        assertTrue(breaker.timeUntilReset > 0)
        assertTrue(breaker.timeUntilReset <= 1000)
    }

    @Test
    fun calculateBackoff_exponentialGrowth() {
        val delay0 = CircuitBreaker.calculateBackoff(0, baseDelayMs = 1000, jitterFactor = 0f)
        val delay1 = CircuitBreaker.calculateBackoff(1, baseDelayMs = 1000, jitterFactor = 0f)
        val delay2 = CircuitBreaker.calculateBackoff(2, baseDelayMs = 1000, jitterFactor = 0f)

        assertEquals(1000L, delay0)
        assertEquals(2000L, delay1)
        assertEquals(4000L, delay2)
    }

    @Test
    fun calculateBackoff_respectsMaxDelay() {
        val delay = CircuitBreaker.calculateBackoff(
            attempt = 10,
            baseDelayMs = 1000,
            maxDelayMs = 5000,
            jitterFactor = 0f
        )

        assertEquals(5000L, delay)
    }

    @Test
    fun customThreshold_works() {
        val customBreaker = CircuitBreaker(
            name = "Custom",
            threshold = 5
        )

        repeat(4) { customBreaker.recordFailure() }
        assertEquals(CircuitBreaker.State.CLOSED, customBreaker.state.value)

        customBreaker.recordFailure()
        assertEquals(CircuitBreaker.State.OPEN, customBreaker.state.value)
    }
}
