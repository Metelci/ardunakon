package com.metelci.ardunakon.util

import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Generic Circuit Breaker implementation for fault-tolerant operations.
 *
 * The circuit breaker has three states:
 * - **CLOSED**: Normal operation, requests pass through
 * - **OPEN**: Failures exceeded threshold, requests fail fast
 * - **HALF_OPEN**: Testing period, limited requests allowed to probe recovery
 *
 * Usage:
 * ```kotlin
 * val breaker = CircuitBreaker(name = "WiFiConnection", threshold = 3, resetTimeoutMs = 30_000)
 *
 * if (breaker.allowRequest()) {
 *     try {
 *         connect()
 *         breaker.recordSuccess()
 *     } catch (e: Exception) {
 *         breaker.recordFailure()
 *     }
 * }
 * ```
 *
 * @param name Identifier for logging and debugging
 * @param threshold Number of consecutive failures before opening the circuit
 * @param resetTimeoutMs Time in ms before attempting to close after opening
 * @param halfOpenMaxAttempts Max attempts allowed in half-open state
 */
class CircuitBreaker(
    val name: String,
    private val threshold: Int = DEFAULT_THRESHOLD,
    private val resetTimeoutMs: Long = DEFAULT_RESET_TIMEOUT_MS,
    private val halfOpenMaxAttempts: Int = DEFAULT_HALF_OPEN_ATTEMPTS
) {
    /**
     * Circuit breaker states.
     */
    enum class State {
        /** Normal operation - requests pass through */
        CLOSED,

        /** Circuit is open - requests fail fast */
        OPEN,

        /** Testing recovery - limited requests allowed */
        HALF_OPEN
    }

    private val _state = MutableStateFlow(State.CLOSED)
    val state: StateFlow<State> = _state.asStateFlow()

    private var failureCount = 0
    private var successCount = 0
    private var lastFailureTime = 0L
    private var halfOpenAttempts = 0
    private var totalFailures = 0L
    private var totalSuccesses = 0L
    private var lastStateChange = System.currentTimeMillis()

    /**
     * Checks if a request should be allowed through.
     *
     * @return true if the request can proceed, false if it should fail fast
     */
    @Synchronized
    fun allowRequest(): Boolean {
        val now = System.currentTimeMillis()

        return when (_state.value) {
            State.CLOSED -> true

            State.OPEN -> {
                // Check if reset timeout has elapsed
                if (now - lastFailureTime >= resetTimeoutMs) {
                    transitionTo(State.HALF_OPEN)
                    halfOpenAttempts = 0
                    true
                } else {
                    false // Fail fast
                }
            }

            State.HALF_OPEN -> {
                // Allow limited attempts in half-open state
                if (halfOpenAttempts < halfOpenMaxAttempts) {
                    halfOpenAttempts++
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * Records a successful operation.
     * In half-open state, this closes the circuit.
     */
    @Synchronized
    fun recordSuccess() {
        totalSuccesses++
        successCount++

        when (_state.value) {
            State.HALF_OPEN -> {
                // Recovery confirmed, close the circuit
                failureCount = 0
                transitionTo(State.CLOSED)
            }
            State.CLOSED -> {
                // Reset failure count on success
                failureCount = 0
            }
            State.OPEN -> {
                // Shouldn't happen, but handle gracefully
                failureCount = 0
                transitionTo(State.CLOSED)
            }
        }
    }

    /**
     * Records a failed operation.
     * Increments failure count and may trip the circuit.
     */
    @Synchronized
    fun recordFailure() {
        totalFailures++
        failureCount++
        successCount = 0
        lastFailureTime = System.currentTimeMillis()

        when (_state.value) {
            State.CLOSED -> {
                if (failureCount >= threshold) {
                    transitionTo(State.OPEN)
                }
            }
            State.HALF_OPEN -> {
                // Failed during probe, reopen the circuit
                transitionTo(State.OPEN)
            }
            State.OPEN -> {
                // Already open, just update failure time
            }
        }
    }

    /**
     * Manually resets the circuit breaker to closed state.
     */
    @Synchronized
    fun reset() {
        failureCount = 0
        successCount = 0
        halfOpenAttempts = 0
        transitionTo(State.CLOSED)
    }

    /**
     * Manually trips the circuit breaker to open state.
     */
    @Synchronized
    fun trip() {
        lastFailureTime = System.currentTimeMillis()
        failureCount = threshold
        transitionTo(State.OPEN)
    }

    /**
     * Checks if the circuit is currently tripped (open or half-open).
     */
    val isTripped: Boolean
        get() = _state.value != State.CLOSED

    /**
     * Gets the current failure count.
     */
    val currentFailureCount: Int
        get() = failureCount

    /**
     * Gets time remaining until reset timeout expires (ms).
     * Returns 0 if not in OPEN state.
     */
    val timeUntilReset: Long
        get() = if (_state.value == State.OPEN) {
            maxOf(0, resetTimeoutMs - (System.currentTimeMillis() - lastFailureTime))
        } else {
            0
        }

    /**
     * Gets statistics about the circuit breaker.
     */
    fun getStats(): Stats = Stats(
        name = name,
        state = _state.value,
        failureCount = failureCount,
        threshold = threshold,
        totalFailures = totalFailures,
        totalSuccesses = totalSuccesses,
        lastStateChange = lastStateChange,
        timeUntilReset = timeUntilReset
    )

    private fun transitionTo(newState: State) {
        if (_state.value != newState) {
            lastStateChange = System.currentTimeMillis()
            _state.value = newState
        }
    }

    /**
     * Statistics about the circuit breaker.
     */
    data class Stats(
        val name: String,
        val state: State,
        val failureCount: Int,
        val threshold: Int,
        val totalFailures: Long,
        val totalSuccesses: Long,
        val lastStateChange: Long,
        val timeUntilReset: Long
    ) {
        val successRate: Float
            get() = if (totalSuccesses + totalFailures > 0) {
                totalSuccesses.toFloat() / (totalSuccesses + totalFailures)
            } else {
                1f
            }
    }

    companion object {
        const val DEFAULT_THRESHOLD = 3
        const val DEFAULT_RESET_TIMEOUT_MS = 30_000L
        const val DEFAULT_HALF_OPEN_ATTEMPTS = 1

        /**
         * Calculates exponential backoff delay.
         *
         * @param attempt Current attempt number (0-indexed)
         * @param baseDelayMs Base delay in milliseconds
         * @param maxDelayMs Maximum delay cap
         * @param jitterFactor Random jitter factor (0.0 - 1.0)
         * @return Delay in milliseconds
         */
        fun calculateBackoff(
            attempt: Int,
            baseDelayMs: Long = 1000L,
            maxDelayMs: Long = 60_000L,
            jitterFactor: Float = 0.2f
        ): Long {
            val exponentialDelay = baseDelayMs * 2.0.pow(attempt.toDouble()).toLong()
            val cappedDelay = min(exponentialDelay, maxDelayMs)
            val jitter = (cappedDelay * jitterFactor * Math.random()).toLong()
            return cappedDelay + jitter
        }
    }
}
