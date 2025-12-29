package com.metelci.ardunakon.util

import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.delay

/**
 * Retry policy for fault-tolerant operations.
 *
 * Provides configurable retry behavior with exponential backoff,
 * jitter, and condition-based retry decisions.
 *
 * Usage:
 * ```kotlin
 * val policy = RetryPolicy(maxAttempts = 3)
 *
 * policy.execute {
 *     riskyOperation()
 * }
 *
 * // Or with recovery:
 * val result = policy.executeWithRecovery(
 *     operation = { riskyOperation() },
 *     recover = { error -> fallbackValue }
 * )
 * ```
 *
 * @param maxAttempts Maximum number of attempts (includes initial attempt)
 * @param baseDelayMs Initial delay between retries
 * @param maxDelayMs Maximum delay cap
 * @param exponentialBase Multiplier for exponential backoff
 * @param jitterFactor Random jitter factor (0.0 - 1.0)
 * @param retryOn Predicate to determine if exception should trigger retry
 */
class RetryPolicy(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    private val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    private val exponentialBase: Double = DEFAULT_EXPONENTIAL_BASE,
    private val jitterFactor: Float = DEFAULT_JITTER_FACTOR,
    private val retryOn: (Throwable) -> Boolean = { true }
) {
    /**
     * Result of a retry operation.
     *
     * @param T The type of the success value
     */
    sealed class Result<out T> {
        /**
         * Successful result containing the operation's return value.
         * @property value The successful result value
         */
        data class Success<T>(val value: T) : Result<T>()

        /**
         * Failed result after all retry attempts exhausted.
         * @property error The last exception that was thrown
         * @property attempts Total number of attempts made
         * @property totalTimeMs Total time spent including delays
         */
        data class Failure(
            val error: Throwable,
            val attempts: Int,
            val totalTimeMs: Long
        ) : Result<Nothing>()

        /** Returns true if this result represents success. */
        val isSuccess: Boolean get() = this is Success

        /** Returns true if this result represents failure. */
        val isFailure: Boolean get() = this is Failure

        /** Returns the value if success, null otherwise. */
        fun getOrNull(): T? = (this as? Success)?.value

        /**
         * Returns the value if success, throws the error otherwise.
         * @throws Throwable The captured error if this is a Failure
         */
        fun getOrThrow(): T = when (this) {
            is Success -> value
            is Failure -> throw error
        }
    }

    /**
     * Executes an operation with retry logic.
     *
     * @param operation The suspending operation to execute
     * @return Result containing success value or failure info
     */
    suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        val startTime = System.currentTimeMillis()
        var lastError: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                val result = operation()
                return Result.Success(result)
            } catch (e: Throwable) {
                lastError = e

                if (!retryOn(e)) {
                    return Result.Failure(
                        error = e,
                        attempts = attempt,
                        totalTimeMs = System.currentTimeMillis() - startTime
                    )
                }

                if (attempt < maxAttempts) {
                    val delayMs = calculateDelay(attempt - 1)
                    delay(delayMs)
                }
            }
        }

        return Result.Failure(
            error = lastError ?: IllegalStateException("Retry exhausted with no error"),
            attempts = maxAttempts,
            totalTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Executes an operation with retry and fallback recovery.
     *
     * @param operation The suspending operation to execute
     * @param recover Fallback function if all retries fail
     * @return Success value or recovered value
     */
    suspend fun <T> executeWithRecovery(operation: suspend () -> T, recover: suspend (Throwable) -> T): T {
        return when (val result = execute(operation)) {
            is Result.Success -> result.value
            is Result.Failure -> recover(result.error)
        }
    }

    /**
     * Executes an operation with callback notifications.
     *
     * @param operation The suspending operation
     * @param onRetry Called before each retry with attempt number and delay
     * @param onSuccess Called on successful completion
     * @param onExhausted Called when all retries are exhausted
     */
    suspend fun <T> executeWithCallbacks(
        operation: suspend () -> T,
        onRetry: (attempt: Int, delayMs: Long, error: Throwable) -> Unit = { _, _, _ -> },
        onSuccess: (T, attempts: Int) -> Unit = { _, _ -> },
        onExhausted: (Throwable, attempts: Int) -> Unit = { _, _ -> }
    ): Result<T> {
        val startTime = System.currentTimeMillis()
        var lastError: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                val result = operation()
                onSuccess(result, attempt)
                return Result.Success(result)
            } catch (e: Throwable) {
                lastError = e

                if (!retryOn(e) || attempt >= maxAttempts) {
                    onExhausted(e, attempt)
                    return Result.Failure(
                        error = e,
                        attempts = attempt,
                        totalTimeMs = System.currentTimeMillis() - startTime
                    )
                }

                val delayMs = calculateDelay(attempt - 1)
                onRetry(attempt, delayMs, e)
                delay(delayMs)
            }
        }

        return Result.Failure(
            error = lastError ?: IllegalStateException("Retry exhausted"),
            attempts = maxAttempts,
            totalTimeMs = System.currentTimeMillis() - startTime
        )
    }

    fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = (baseDelayMs * exponentialBase.pow(attempt.toDouble())).toLong()
        val cappedDelay = min(exponentialDelay, maxDelayMs)
        val jitter = (cappedDelay * jitterFactor * Math.random()).toLong()
        return cappedDelay + jitter
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_BASE_DELAY_MS = 1000L
        const val DEFAULT_MAX_DELAY_MS = 30_000L
        const val DEFAULT_EXPONENTIAL_BASE = 2.0
        const val DEFAULT_JITTER_FACTOR = 0.2f

        /**
         * Creates a policy for network operations with sensible defaults.
         */
        fun forNetwork(): RetryPolicy = RetryPolicy(
            maxAttempts = 3,
            baseDelayMs = 1000,
            maxDelayMs = 10_000,
            retryOn = { e ->
                e is java.io.IOException ||
                    e is java.net.SocketException ||
                    e is java.net.SocketTimeoutException
            }
        )

        /**
         * Creates a policy for Bluetooth operations.
         */
        fun forBluetooth(): RetryPolicy = RetryPolicy(
            maxAttempts = 3,
            baseDelayMs = 500,
            maxDelayMs = 5_000,
            jitterFactor = 0.3f
        )

        /**
         * Creates an aggressive retry policy for critical operations.
         */
        fun aggressive(): RetryPolicy = RetryPolicy(
            maxAttempts = 5,
            baseDelayMs = 200,
            maxDelayMs = 5_000
        )

        /**
         * Creates a conservative retry policy with longer delays.
         */
        fun conservative(): RetryPolicy = RetryPolicy(
            maxAttempts = 2,
            baseDelayMs = 2000,
            maxDelayMs = 10_000
        )
    }
}
