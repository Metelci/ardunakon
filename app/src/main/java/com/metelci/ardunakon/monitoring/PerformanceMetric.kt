package com.metelci.ardunakon.monitoring

/**
 * Types of performance metrics tracked by the application.
 */
enum class MetricType {
    /** Cold/warm start time in milliseconds */
    APP_STARTUP,

    /** Number of dropped frames detected */
    FRAME_DROP,

    /** Heap memory usage in bytes */
    MEMORY_USAGE,

    /** Application Not Responding event detected */
    ANR_DETECTED,

    /** BLE/WiFi connection establishment time in ms */
    CONNECTION_TIME,

    /** Command round-trip time in ms */
    COMMAND_LATENCY,

    /** Non-fatal error recorded */
    NON_FATAL_ERROR,

    /** Fatal crash recorded */
    CRASH,

    /** Security violation detected (RASP) */
    SECURITY_EVENT
}

/**
 * Severity level for logged errors and crashes.
 */
enum class SeverityLevel {
    /** Warning - non-critical issue */
    WARNING,

    /** Error - recoverable failure */
    ERROR,

    /** Fatal - unrecoverable crash */
    FATAL
}

/**
 * A single performance metric data point.
 *
 * SECURITY: Context field is sanitized before storage - no PII, file paths, or device IDs.
 *
 * @param timestamp Unix timestamp in milliseconds
 * @param type The type of metric being recorded
 * @param value The metric value (meaning depends on type)
 * @param context Optional sanitized context string
 */
data class PerformanceMetric(
    val timestamp: Long,
    val type: MetricType,
    val value: Long,
    val context: String? = null
) {
    init {
        require(timestamp > 0) { "Invalid timestamp" }
        require(value >= 0) { "Invalid metric value" }
        require(context == null || context.length <= MAX_CONTEXT_LENGTH) {
            "Context exceeds maximum length"
        }
    }

    companion object {
        /** Maximum allowed context string length to prevent memory issues */
        const val MAX_CONTEXT_LENGTH = 256
    }
}

/**
 * Summary of an app session for analytics.
 *
 * @param sessionId Unique session identifier (UUID, no device info)
 * @param startTime Session start timestamp
 * @param endTime Session end timestamp (null if ongoing)
 * @param crashCount Number of crashes during session
 * @param errorCount Number of non-fatal errors
 * @param avgLatencyMs Average command latency in milliseconds
 */
data class AppSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val crashCount: Int = 0,
    val errorCount: Int = 0,
    val avgLatencyMs: Float = 0f
) {
    init {
        require(startTime > 0) { "Invalid start time" }
        require(endTime == null || endTime >= startTime) { "End time before start time" }
        require(crashCount >= 0) { "Invalid crash count" }
        require(errorCount >= 0) { "Invalid error count" }
    }
}

/**
 * Structured crash record for history tracking.
 *
 * SECURITY: All fields are sanitized - no PII, file paths stripped to class names only,
 * fingerprint is SHA-256 hash of anonymized crash signature.
 *
 * @param fingerprint SHA-256 hash of crash signature for duplicate detection
 * @param timestamp When the crash occurred
 * @param severity Severity level of the crash
 * @param exceptionType Simple exception class name (e.g., "NullPointerException")
 * @param message Sanitized exception message (PII stripped)
 * @param topStackFrame Top 3 sanitized stack frames (class.method only)
 * @param occurrenceCount Number of times this crash has occurred
 */
data class CrashRecord(
    val fingerprint: String,
    val timestamp: Long,
    val severity: SeverityLevel,
    val exceptionType: String,
    val message: String?,
    val topStackFrames: List<String>,
    val occurrenceCount: Int = 1
) {
    init {
        require(fingerprint.length == 64) { "Invalid fingerprint format" }
        require(timestamp > 0) { "Invalid timestamp" }
        require(topStackFrames.size <= MAX_STACK_FRAMES) { "Too many stack frames" }
    }

    companion object {
        const val MAX_STACK_FRAMES = 5
    }
}

/**
 * Aggregated performance statistics for display.
 */
data class PerformanceStats(
    val crashFreeSessionsPercent: Float,
    val avgStartupTimeMs: Long,
    val avgLatencyMs: Float,
    val totalCrashes: Int,
    val totalErrors: Int,
    val sessionCount: Int,
    val lastCrashTime: Long?
)
