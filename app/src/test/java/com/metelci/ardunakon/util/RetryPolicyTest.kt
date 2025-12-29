package com.metelci.ardunakon.util

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RetryPolicy.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryPolicyTest {

    @Test
    fun execute_successOnFirstAttempt_returnsSuccess() = runTest {
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 10)
        var attempts = 0

        val result = policy.execute {
            attempts++
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attempts)
    }

    @Test
    fun execute_successAfterRetries_returnsSuccess() = runTest {
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 10)
        var attempts = 0

        val result = policy.execute {
            attempts++
            if (attempts < 3) throw IOException("temporary failure")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(3, attempts)
    }

    @Test
    fun execute_allAttemptsFail_returnsFailure() = runTest {
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 10)
        var attempts = 0

        val result = policy.execute {
            attempts++
            throw IOException("persistent failure")
        }

        assertTrue(result.isFailure)
        assertEquals(3, attempts)
        assertTrue((result as RetryPolicy.Result.Failure).error is IOException)
    }

    @Test
    fun execute_retryOnCondition_respectsPredicate() = runTest {
        val policy = RetryPolicy(
            maxAttempts = 5,
            baseDelayMs = 10,
            retryOn = { it is IOException }
        )
        var attempts = 0

        val result = policy.execute {
            attempts++
            throw IllegalStateException("non-retryable")
        }

        assertTrue(result.isFailure)
        assertEquals(1, attempts) // Should not retry for IllegalStateException
    }

    @Test
    fun execute_retryOnCondition_retriesMatchingExceptions() = runTest {
        val policy = RetryPolicy(
            maxAttempts = 3,
            baseDelayMs = 10,
            retryOn = { it is IOException }
        )
        var attempts = 0

        val result = policy.execute {
            attempts++
            throw IOException("retryable")
        }

        assertEquals(3, attempts) // Should retry for IOException
    }

    @Test
    fun executeWithRecovery_usesRecoveryOnFailure() = runTest {
        val policy = RetryPolicy(maxAttempts = 2, baseDelayMs = 10)

        val result = policy.executeWithRecovery(
            operation = { throw IOException("fail") },
            recover = { "recovered" }
        )

        assertEquals("recovered", result)
    }

    @Test
    fun executeWithRecovery_returnsValueOnSuccess() = runTest {
        val policy = RetryPolicy(maxAttempts = 2, baseDelayMs = 10)

        val result = policy.executeWithRecovery(
            operation = { "success" },
            recover = { "recovered" }
        )

        assertEquals("success", result)
    }

    @Test
    fun executeWithCallbacks_notifiesOnRetry() = runTest {
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 10)
        val retryNotifications = mutableListOf<Int>()
        var attempts = 0

        policy.executeWithCallbacks(
            operation = {
                attempts++
                if (attempts < 3) throw IOException("fail")
                "success"
            },
            onRetry = { attempt, _, _ -> retryNotifications.add(attempt) }
        )

        assertEquals(listOf(1, 2), retryNotifications)
    }

    @Test
    fun executeWithCallbacks_notifiesOnSuccess() = runTest {
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 10)
        var successValue: String? = null
        var successAttempts = 0

        policy.executeWithCallbacks(
            operation = { "success" },
            onSuccess = { value, attempts ->
                successValue = value
                successAttempts = attempts
            }
        )

        assertEquals("success", successValue)
        assertEquals(1, successAttempts)
    }

    @Test
    fun executeWithCallbacks_notifiesOnExhausted() = runTest {
        val policy = RetryPolicy(maxAttempts = 2, baseDelayMs = 10)
        var exhaustedError: Throwable? = null
        var exhaustedAttempts = 0

        policy.executeWithCallbacks(
            operation = { throw IOException("fail") },
            onExhausted = { error, attempts ->
                exhaustedError = error
                exhaustedAttempts = attempts
            }
        )

        assertTrue(exhaustedError is IOException)
        assertEquals(2, exhaustedAttempts)
    }

    @Test
    fun forNetwork_createsNetworkPolicy() {
        val policy = RetryPolicy.forNetwork()
        assertNotNull(policy)
    }

    @Test
    fun forBluetooth_createsBluetoothPolicy() {
        val policy = RetryPolicy.forBluetooth()
        assertNotNull(policy)
    }

    @Test
    fun result_getOrThrow_throwsOnFailure() = runTest {
        val policy = RetryPolicy(maxAttempts = 1, baseDelayMs = 10)

        val result = policy.execute<String> { throw IOException("fail") }

        assertThrows(IOException::class.java) {
            result.getOrThrow()
        }
    }

    @Test
    fun result_getOrNull_returnsNullOnFailure() = runTest {
        val policy = RetryPolicy(maxAttempts = 1, baseDelayMs = 10)

        val result = policy.execute<String> { throw IOException("fail") }

        assertNull(result.getOrNull())
    }
}
