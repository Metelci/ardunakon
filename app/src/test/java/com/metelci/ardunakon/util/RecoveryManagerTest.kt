package com.metelci.ardunakon.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for RecoveryManager.
 * Covers circuit breaker coordination, metrics tracking, and health status.
 */
class RecoveryManagerTest {

    private lateinit var manager: RecoveryManager

    @Before
    fun setup() {
        manager = RecoveryManager()
    }

    // ========== Health Status Tests ==========

    @Test
    fun `initial health status is HEALTHY`() {
        assertEquals(RecoveryManager.HealthStatus.HEALTHY, manager.overallHealth.value)
    }

    @Test
    fun `health remains HEALTHY after single success`() {
        manager.recordSuccess("test_op")
        assertEquals(RecoveryManager.HealthStatus.HEALTHY, manager.overallHealth.value)
    }

    @Test
    fun `health changes after multiple failures`() {
        repeat(3) { manager.recordFailure("test_op") }
        // After multiple failures, health should no longer be HEALTHY
        assertNotEquals(RecoveryManager.HealthStatus.HEALTHY, manager.overallHealth.value)
    }

    // ========== Circuit Breaker Tests ==========

    @Test
    fun `getCircuitBreaker creates new breaker for unknown operation`() {
        val breaker = manager.getCircuitBreaker("new_op")
        assertNotNull(breaker)
        assertEquals("new_op", breaker.name)
    }

    @Test
    fun `getCircuitBreaker returns same instance for same name`() {
        val breaker1 = manager.getCircuitBreaker("op1")
        val breaker2 = manager.getCircuitBreaker("op1")
        assertSame(breaker1, breaker2)
    }

    @Test
    fun `getCircuitBreaker accepts custom threshold`() {
        // Just verify it creates successfully with custom threshold
        val breaker = manager.getCircuitBreaker("custom_op", threshold = 5)
        assertNotNull(breaker)
    }

    @Test
    fun `shouldAllowOperation returns true initially`() {
        assertTrue(manager.shouldAllowOperation("new_operation"))
    }

    @Test
    fun `shouldAllowOperation returns false after circuit trips`() {
        val opName = "failing_op"
        repeat(3) { manager.recordFailure(opName) }
        assertFalse(manager.shouldAllowOperation(opName))
    }

    // ========== Metrics Tests ==========

    @Test
    fun `getMetrics creates new metrics for unknown operation`() {
        val metrics = manager.getMetrics("new_metrics_op")
        assertNotNull(metrics)
        assertEquals("new_metrics_op", metrics.operationName)
        assertEquals(0, metrics.totalAttempts)
    }

    @Test
    fun `recordSuccess increments metrics correctly`() {
        manager.recordSuccess("metric_op", recoveryTimeMs = 100)
        val metrics = manager.getMetrics("metric_op")
        assertEquals(1, metrics.totalAttempts)
        assertEquals(1, metrics.successfulAttempts)
        assertEquals(0, metrics.failedAttempts)
        assertNotNull(metrics.lastSuccessTime)
    }

    @Test
    fun `recordFailure increments metrics correctly`() {
        manager.recordFailure("fail_op")
        val metrics = manager.getMetrics("fail_op")
        assertEquals(1, metrics.totalAttempts)
        assertEquals(0, metrics.successfulAttempts)
        assertEquals(1, metrics.failedAttempts)
        assertNotNull(metrics.lastFailureTime)
    }

    @Test
    fun `success rate calculates correctly`() {
        repeat(3) { manager.recordSuccess("rate_op") }
        manager.recordFailure("rate_op")
        val metrics = manager.getMetrics("rate_op")
        assertEquals(0.75f, metrics.successRate, 0.01f)
    }

    @Test
    fun `success rate is 1 when no attempts`() {
        val metrics = manager.getMetrics("empty_op")
        assertEquals(1f, metrics.successRate, 0.01f)
    }

    // ========== Reset Tests ==========

    @Test
    fun `reset clears specific operation`() {
        repeat(3) { manager.recordFailure("reset_op") }
        assertFalse(manager.shouldAllowOperation("reset_op"))
        manager.reset("reset_op")
        assertTrue(manager.shouldAllowOperation("reset_op"))
    }

    @Test
    fun `resetAll clears all state`() {
        repeat(3) { manager.recordFailure("op1") }
        repeat(3) { manager.recordFailure("op2") }
        manager.resetAll()
        assertEquals(RecoveryManager.HealthStatus.HEALTHY, manager.overallHealth.value)
        assertTrue(manager.shouldAllowOperation("op1"))
        assertTrue(manager.shouldAllowOperation("op2"))
    }

    // ========== Summary Tests ==========

    @Test
    fun `getSummary returns correct counts`() {
        manager.recordSuccess("healthy_op")
        repeat(3) { manager.recordFailure("failed_op") }
        
        val summary = manager.getSummary()
        assertEquals(2, summary.totalOperations)
        assertEquals(1, summary.healthyOperations)
        assertTrue(summary.failedOperations >= 1)
    }

    @Test
    fun `getSummary calculates overall success rate`() {
        repeat(8) { manager.recordSuccess("op1") }
        repeat(2) { manager.recordFailure("op1") }
        
        val summary = manager.getSummary()
        assertEquals(0.8f, summary.overallSuccessRate, 0.01f)
    }

    // ========== Operation Constants Tests ==========

    @Test
    fun `operation constants are defined`() {
        assertNotNull(RecoveryManager.OP_BLE_CONNECT)
        assertNotNull(RecoveryManager.OP_BLE_SCAN)
        assertNotNull(RecoveryManager.OP_WIFI_CONNECT)
        assertNotNull(RecoveryManager.OP_WIFI_DISCOVERY)
        assertNotNull(RecoveryManager.OP_DATA_SEND)
        assertNotNull(RecoveryManager.OP_DATA_RECEIVE)
    }

    // ========== Recovery Time Tests ==========

    @Test
    fun `average recovery time calculates correctly`() {
        manager.recordSuccess("recovery_op", recoveryTimeMs = 100)
        manager.recordSuccess("recovery_op", recoveryTimeMs = 200)
        manager.recordSuccess("recovery_op", recoveryTimeMs = 300)
        
        val metrics = manager.getMetrics("recovery_op")
        assertEquals(200, metrics.avgRecoveryTimeMs)
    }
}
