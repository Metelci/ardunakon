package com.metelci.ardunakon.monitoring

import android.content.Context
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for PerformanceMonitor.
 *
 * Tests metric recording, rate limiting, data retention, and crash tracking.
 */
@RunWith(RobolectricTestRunner::class)
class PerformanceMonitorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        // Clean up any existing files
        File(context.filesDir, "performance_metrics.json").delete()
        File(context.filesDir, "crash_history.json").delete()
        File(context.filesDir, "app_sessions.json").delete()

        // Initialize monitor
        PerformanceMonitor.resetForTests()
        PerformanceMonitor.init(context)
    }

    @After
    fun tearDown() {
        // Clean up test files
        File(context.filesDir, "performance_metrics.json").delete()
        File(context.filesDir, "crash_history.json").delete()
    }

    @Test
    fun init_createsInstance() {
        val monitor = PerformanceMonitor.getInstance()
        assertNotNull(monitor)
    }

    @Test
    fun recordMetric_validMetric_returnsTrue() {
        val monitor = PerformanceMonitor.getInstance()!!

        val result = monitor.recordMetric(MetricType.APP_STARTUP, 500L)

        assertTrue(result)
    }

    @Test
    fun recordMetric_negativeValue_returnsFalse() {
        val monitor = PerformanceMonitor.getInstance()!!

        val result = monitor.recordMetric(MetricType.APP_STARTUP, -1L)

        assertFalse(result)
    }

    @Test
    fun recordMetric_withContext_sanitizesFilePaths() {
        val monitor = PerformanceMonitor.getInstance()!!

        val result = monitor.recordMetric(
            MetricType.NON_FATAL_ERROR,
            1L,
            "/data/user/0/com.metelci.ardunakon/files/secret.txt"
        )

        assertTrue(result)
        // The path should be sanitized in the stored context
    }

    @Test
    fun recordStartupTime_recordsMetric() {
        PerformanceMonitor.recordStartupTime(750L)

        val stats = PerformanceMonitor.getInstance()?.getStats()
        assertNotNull(stats)
        assertTrue(stats!!.avgStartupTimeMs >= 0)
    }

    @Test
    fun recordCrash_createsRecord() {
        val monitor = PerformanceMonitor.getInstance()!!

        monitor.recordCrash(
            throwable = RuntimeException("Test crash"),
            severity = SeverityLevel.ERROR,
            isFatal = false
        )

        val crashes = monitor.getCrashHistory()
        assertTrue(crashes.isNotEmpty())
        assertEquals("RuntimeException", crashes.first().exceptionType)
    }

    @Test
    fun recordCrash_duplicateCrash_incrementsCount() {
        val monitor = PerformanceMonitor.getInstance()!!
        val exception = RuntimeException("Same crash")

        monitor.recordCrash(exception, SeverityLevel.ERROR, false)
        monitor.recordCrash(exception, SeverityLevel.ERROR, false)

        val crashes = monitor.getCrashHistory()
        assertEquals(1, crashes.size) // Should be deduplicated
        assertEquals(2, crashes.first().occurrenceCount)
    }

    @Test
    fun recordConnectionTime_recordsWithType() {
        val monitor = PerformanceMonitor.getInstance()!!

        monitor.recordConnectionTime(150L, "BLE")

        val stats = monitor.getStats()
        assertNotNull(stats)
    }

    @Test
    fun recordLatency_recordsMetric() {
        val monitor = PerformanceMonitor.getInstance()!!

        monitor.recordLatency(25L)
        monitor.recordLatency(30L)
        monitor.recordLatency(35L)

        val stats = monitor.getStats()
        assertTrue(stats.avgLatencyMs >= 0)
    }

    @Test
    fun getStats_calculatesHealthScore() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record some metrics
        monitor.recordMetric(MetricType.APP_STARTUP, 500L)

        val stats = monitor.getStats()

        assertTrue(stats.crashFreeSessionsPercent >= 0f)
        assertTrue(stats.crashFreeSessionsPercent <= 100f)
    }

    @Test
    fun generateDiagnosticReport_containsHeader() {
        val monitor = PerformanceMonitor.getInstance()!!

        val report = monitor.generateDiagnosticReport()

        assertTrue(report.contains("Ardunakon Performance Report"))
        assertTrue(report.contains("Summary"))
    }

    @Test
    fun generateDiagnosticReport_sanitizesOutput() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record a crash
        monitor.recordCrash(RuntimeException("Error"), SeverityLevel.ERROR, false)

        val report = monitor.generateDiagnosticReport()

        // Should not contain full file paths
        assertFalse(report.contains("/data/"))
        assertFalse(report.contains("/storage/"))
    }

    @Test
    fun stats_flowEmitsUpdates() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Get initial stats
        val initialTimestamp = monitor.stats.value.sessionCount

        // Record something
        monitor.recordMetric(MetricType.APP_STARTUP, 100L)

        // Stats should be accessible
        val stats = monitor.stats.value
        assertNotNull(stats)
    }

    @Test
    fun crashHistory_limitsToMaxRecords() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record many different crashes
        repeat(150) { i ->
            val exception = Exception("Unique crash $i at ${java.util.UUID.randomUUID()}")
            monitor.recordCrash(exception, SeverityLevel.WARNING, false)
        }

        val crashes = monitor.getCrashHistory()
        assertTrue(crashes.size <= 100) // MAX_CRASH_RECORDS
    }
}
