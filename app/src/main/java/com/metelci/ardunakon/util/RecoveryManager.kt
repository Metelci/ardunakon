package com.metelci.ardunakon.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Recovery manager that coordinates circuit breakers and retry policies
 * for robust error handling across the application.
 *
 * Provides:
 * - Centralized recovery state tracking
 * - Automatic degradation detection
 * - Recovery metrics
 */
class RecoveryManager {

    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()
    private val recoveryMetrics = mutableMapOf<String, RecoveryMetrics>()

    private val _overallHealth = MutableStateFlow(HealthStatus.HEALTHY)
    val overallHealth: StateFlow<HealthStatus> = _overallHealth.asStateFlow()

    /**
     * Health status levels.
     */
    enum class HealthStatus {
        HEALTHY, // All systems operational
        DEGRADED, // Some systems failing, others working
        CRITICAL, // Most systems failing
        OFFLINE // All systems failing
    }

    /**
     * Recovery metrics for a specific operation.
     */
    data class RecoveryMetrics(
        val operationName: String,
        var totalAttempts: Long = 0,
        var successfulAttempts: Long = 0,
        var failedAttempts: Long = 0,
        var lastSuccessTime: Long? = null,
        var lastFailureTime: Long? = null,
        var totalRecoveryTimeMs: Long = 0,
        var avgRecoveryTimeMs: Long = 0
    ) {
        val successRate: Float
            get() = if (totalAttempts > 0) {
                successfulAttempts.toFloat() / totalAttempts
            } else {
                1f
            }

        fun recordSuccess(recoveryTimeMs: Long = 0) {
            totalAttempts++
            successfulAttempts++
            lastSuccessTime = System.currentTimeMillis()
            if (recoveryTimeMs > 0) {
                totalRecoveryTimeMs += recoveryTimeMs
                avgRecoveryTimeMs = totalRecoveryTimeMs / successfulAttempts
            }
        }

        fun recordFailure() {
            totalAttempts++
            failedAttempts++
            lastFailureTime = System.currentTimeMillis()
        }
    }

    /**
     * Gets or creates a circuit breaker for the specified operation.
     */
    @Synchronized
    fun getCircuitBreaker(name: String, threshold: Int = 3, resetTimeoutMs: Long = 30_000): CircuitBreaker {
        return circuitBreakers.getOrPut(name) {
            CircuitBreaker(
                name = name,
                threshold = threshold,
                resetTimeoutMs = resetTimeoutMs
            )
        }
    }

    /**
     * Gets or creates metrics tracker for an operation.
     */
    @Synchronized
    fun getMetrics(operationName: String): RecoveryMetrics {
        return recoveryMetrics.getOrPut(operationName) {
            RecoveryMetrics(operationName)
        }
    }

    /**
     * Records a successful operation.
     */
    fun recordSuccess(operationName: String, recoveryTimeMs: Long = 0) {
        getMetrics(operationName).recordSuccess(recoveryTimeMs)
        getCircuitBreaker(operationName).recordSuccess()
        updateOverallHealth()
    }

    /**
     * Records a failed operation.
     */
    fun recordFailure(operationName: String) {
        getMetrics(operationName).recordFailure()
        getCircuitBreaker(operationName).recordFailure()
        updateOverallHealth()
    }

    /**
     * Checks if an operation should be allowed based on circuit breaker state.
     */
    fun shouldAllowOperation(operationName: String): Boolean {
        return getCircuitBreaker(operationName).allowRequest()
    }

    /**
     * Resets all circuit breakers and metrics.
     */
    @Synchronized
    fun resetAll() {
        circuitBreakers.values.forEach { it.reset() }
        recoveryMetrics.clear()
        _overallHealth.value = HealthStatus.HEALTHY
    }

    /**
     * Resets a specific operation's circuit breaker.
     */
    fun reset(operationName: String) {
        circuitBreakers[operationName]?.reset()
    }

    /**
     * Gets a summary of all recovery metrics.
     */
    @Synchronized
    fun getSummary(): RecoverySummary {
        val totalOperations = recoveryMetrics.size
        val healthyOperations = circuitBreakers.count { !it.value.isTripped }
        val degradedOperations = circuitBreakers.count {
            it.value.state.value == CircuitBreaker.State.HALF_OPEN
        }
        val failedOperations = circuitBreakers.count {
            it.value.state.value == CircuitBreaker.State.OPEN
        }

        return RecoverySummary(
            totalOperations = totalOperations,
            healthyOperations = healthyOperations,
            degradedOperations = degradedOperations,
            failedOperations = failedOperations,
            overallSuccessRate = recoveryMetrics.values.map { it.successRate }.average().toFloat(),
            metricsSnapshot = recoveryMetrics.toMap()
        )
    }

    private fun updateOverallHealth() {
        val openCount = circuitBreakers.count { it.value.state.value == CircuitBreaker.State.OPEN }
        val halfOpenCount = circuitBreakers.count { it.value.state.value == CircuitBreaker.State.HALF_OPEN }
        val total = circuitBreakers.size

        _overallHealth.value = when {
            total == 0 -> HealthStatus.HEALTHY
            openCount == total -> HealthStatus.OFFLINE
            openCount > total / 2 -> HealthStatus.CRITICAL
            openCount > 0 || halfOpenCount > 0 -> HealthStatus.DEGRADED
            else -> HealthStatus.HEALTHY
        }
    }

    /**
     * Summary of all recovery state.
     */
    data class RecoverySummary(
        val totalOperations: Int,
        val healthyOperations: Int,
        val degradedOperations: Int,
        val failedOperations: Int,
        val overallSuccessRate: Float,
        val metricsSnapshot: Map<String, RecoveryMetrics>
    )

    companion object {
        /** Operation names for common operations */
        const val OP_BLE_CONNECT = "ble_connect"
        const val OP_BLE_SCAN = "ble_scan"
        const val OP_WIFI_CONNECT = "wifi_connect"
        const val OP_WIFI_DISCOVERY = "wifi_discovery"
        const val OP_DATA_SEND = "data_send"
        const val OP_DATA_RECEIVE = "data_receive"
    }
}
