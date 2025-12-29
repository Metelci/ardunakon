package com.metelci.ardunakon.monitoring

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for performance monitoring operations.
 *
 * Defines the contract for recording metrics, tracking crashes,
 * and generating reports. Implementations can be mocked for testing.
 */
interface IPerformanceMonitor {

    // --- State Observations ---

    /** Current performance statistics. */
    val stats: StateFlow<PerformanceStats>

    /** Recent crash records (most recent first). */
    val recentCrashes: StateFlow<List<CrashRecord>>

    // --- Metric Recording ---

    /**
     * Records a performance metric.
     *
     * @param type Type of metric.
     * @param value Metric value.
     * @param context Optional context string (will be sanitized).
     * @return True if recorded successfully, false if rate-limited.
     */
    fun recordMetric(type: MetricType, value: Long, context: String? = null): Boolean

    /**
     * Records app startup time.
     *
     * @param durationMs Startup duration in milliseconds.
     */
    fun recordStartupTime(durationMs: Long)

    /**
     * Records a latency measurement.
     *
     * @param latencyMs Latency in milliseconds.
     */
    fun recordLatency(latencyMs: Long)

    /**
     * Records memory usage.
     *
     * @param usedBytes Memory used in bytes.
     */
    fun recordMemoryUsage(usedBytes: Long)

    // --- Crash Recording ---

    /**
     * Records a crash or error.
     *
     * @param throwable Exception that was thrown.
     * @param severity Severity level of the crash.
     * @param isFatal True if the app will terminate.
     */
    fun recordCrash(throwable: Throwable, severity: SeverityLevel = SeverityLevel.ERROR, isFatal: Boolean = false)

    // --- Retrieval ---

    /**
     * Gets the current performance statistics snapshot.
     *
     * @return Current stats.
     */
    fun getStats(): PerformanceStats

    /**
     * Gets the crash history.
     *
     * @return List of crash records.
     */
    fun getCrashHistory(): List<CrashRecord>

    // --- Reporting ---

    /**
     * Generates a sanitized diagnostic report.
     *
     * @return Report as text suitable for sharing.
     */
    fun generateDiagnosticReport(): String

    // --- Lifecycle ---

    /**
     * Persists metrics to storage.
     */
    fun persistMetrics()

    /**
     * Clears all stored metrics and crashes.
     */
    fun clearAll()
}
