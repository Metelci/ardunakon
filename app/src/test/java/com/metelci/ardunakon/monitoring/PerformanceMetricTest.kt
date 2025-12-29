package com.metelci.ardunakon.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PerformanceMetric data classes.
 */
class PerformanceMetricTest {

    @Test
    fun performanceMetric_validCreation() {
        val metric = PerformanceMetric(
            timestamp = System.currentTimeMillis(),
            type = MetricType.APP_STARTUP,
            value = 500L,
            context = "cold_start"
        )

        assertTrue(metric.timestamp > 0)
        assertEquals(MetricType.APP_STARTUP, metric.type)
        assertEquals(500L, metric.value)
        assertEquals("cold_start", metric.context)
    }

    @Test(expected = IllegalArgumentException::class)
    fun performanceMetric_invalidTimestamp_throws() {
        PerformanceMetric(
            timestamp = -1,
            type = MetricType.APP_STARTUP,
            value = 100L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun performanceMetric_invalidValue_throws() {
        PerformanceMetric(
            timestamp = System.currentTimeMillis(),
            type = MetricType.APP_STARTUP,
            value = -1L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun performanceMetric_contextTooLong_throws() {
        PerformanceMetric(
            timestamp = System.currentTimeMillis(),
            type = MetricType.APP_STARTUP,
            value = 100L,
            context = "A".repeat(300)
        )
    }

    @Test
    fun performanceMetric_nullContext_allowed() {
        val metric = PerformanceMetric(
            timestamp = System.currentTimeMillis(),
            type = MetricType.MEMORY_USAGE,
            value = 1024L,
            context = null
        )

        assertEquals(null, metric.context)
    }

    @Test
    fun appSession_validCreation() {
        val session = AppSession(
            sessionId = "test-uuid",
            startTime = System.currentTimeMillis(),
            endTime = null,
            crashCount = 0,
            errorCount = 2,
            avgLatencyMs = 50.5f
        )

        assertEquals("test-uuid", session.sessionId)
        assertTrue(session.startTime > 0)
        assertEquals(null, session.endTime)
        assertEquals(0, session.crashCount)
        assertEquals(2, session.errorCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun appSession_endTimeBeforeStart_throws() {
        AppSession(
            sessionId = "test",
            startTime = 1000L,
            endTime = 500L
        )
    }

    @Test
    fun crashRecord_validCreation() {
        val fingerprint = "a".repeat(64) // SHA-256 is 64 hex chars

        val record = CrashRecord(
            fingerprint = fingerprint,
            timestamp = System.currentTimeMillis(),
            severity = SeverityLevel.ERROR,
            exceptionType = "NullPointerException",
            message = "Object was null",
            topStackFrames = listOf("MainActivity.onCreate", "Application.onStart"),
            occurrenceCount = 3
        )

        assertEquals(fingerprint, record.fingerprint)
        assertEquals(SeverityLevel.ERROR, record.severity)
        assertEquals("NullPointerException", record.exceptionType)
        assertEquals(3, record.occurrenceCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun crashRecord_invalidFingerprint_throws() {
        CrashRecord(
            fingerprint = "too_short",
            timestamp = System.currentTimeMillis(),
            severity = SeverityLevel.FATAL,
            exceptionType = "Exception",
            message = null,
            topStackFrames = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun crashRecord_tooManyStackFrames_throws() {
        CrashRecord(
            fingerprint = "a".repeat(64),
            timestamp = System.currentTimeMillis(),
            severity = SeverityLevel.WARNING,
            exceptionType = "Exception",
            message = null,
            topStackFrames = List(10) { "Frame$it" }
        )
    }

    @Test
    fun performanceStats_defaultValues() {
        val stats = PerformanceStats(
            crashFreeSessionsPercent = 95.5f,
            avgStartupTimeMs = 450L,
            avgLatencyMs = 25.3f,
            totalCrashes = 2,
            totalErrors = 10,
            sessionCount = 20,
            lastCrashTime = null
        )

        assertEquals(95.5f, stats.crashFreeSessionsPercent, 0.01f)
        assertEquals(450L, stats.avgStartupTimeMs)
        assertEquals(25.3f, stats.avgLatencyMs, 0.01f)
        assertEquals(2, stats.totalCrashes)
        assertEquals(null, stats.lastCrashTime)
    }

    @Test
    fun metricType_allTypesExist() {
        val types = MetricType.entries

        assertTrue(types.contains(MetricType.APP_STARTUP))
        assertTrue(types.contains(MetricType.FRAME_DROP))
        assertTrue(types.contains(MetricType.MEMORY_USAGE))
        assertTrue(types.contains(MetricType.ANR_DETECTED))
        assertTrue(types.contains(MetricType.CONNECTION_TIME))
        assertTrue(types.contains(MetricType.COMMAND_LATENCY))
        assertTrue(types.contains(MetricType.NON_FATAL_ERROR))
        assertTrue(types.contains(MetricType.CRASH))
    }

    @Test
    fun severityLevel_ordering() {
        val levels = SeverityLevel.entries

        assertEquals(3, levels.size)
        assertEquals(SeverityLevel.WARNING, levels[0])
        assertEquals(SeverityLevel.ERROR, levels[1])
        assertEquals(SeverityLevel.FATAL, levels[2])
    }
}
