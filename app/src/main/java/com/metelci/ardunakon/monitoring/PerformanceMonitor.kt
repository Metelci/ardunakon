package com.metelci.ardunakon.monitoring

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local performance monitoring system with secure, file-based persistence.
 *
 * SECURITY FEATURES:
 * - All data stored in app-private internal storage
 * - Rate limiting prevents DoS abuse (max 100 metrics/min)
 * - 10-day automatic data retention with cleanup on startup
 * - No PII, device IDs, or sensitive file paths stored
 * - Input validation on all recorded metrics
 *
 * @see PerformanceMetric
 * @see CrashRecord
 */
class PerformanceMonitor private constructor(private val context: Context) : IPerformanceMonitor {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val METRICS_FILE = "performance_metrics.json"
        private const val SESSIONS_FILE = "app_sessions.json"
        private const val CRASHES_FILE = "crash_history.json"

        /** Data retention period in milliseconds (10 days) */
        private const val RETENTION_PERIOD_MS = 10L * 24 * 60 * 60 * 1000

        /** Maximum metrics per minute to prevent abuse */
        private const val MAX_METRICS_PER_MINUTE = 100

        /** Maximum stored metrics to prevent unbounded growth */
        private const val MAX_STORED_METRICS = 1000

        /** Maximum stored crash records */
        private const val MAX_CRASH_RECORDS = 100

        @Volatile
        @SuppressLint("StaticFieldLeak")
        private var instance: PerformanceMonitor? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PerformanceMonitor(context.applicationContext)
                        instance?.startSession()
                        instance?.cleanupStaleData()
                        Log.d(TAG, "PerformanceMonitor initialized")
                    }
                }
            }
        }

        fun getInstance(): PerformanceMonitor? = instance

        @VisibleForTesting
        fun resetForTests() {
            instance = null
        }

        /**
         * Convenience method for recording startup time.
         * Safe to call even if not initialized.
         */
        fun recordStartupTime(durationMs: Long) {
            instance?.recordMetric(MetricType.APP_STARTUP, durationMs)
        }
    }

    // In-memory buffers (thread-safe)
    private val recentMetrics = ConcurrentLinkedDeque<PerformanceMetric>()
    private val crashRecords = ConcurrentLinkedDeque<CrashRecord>()

    // Rate limiting
    private val metricsThisMinute = AtomicInteger(0)
    private val lastMinuteReset = AtomicLong(System.currentTimeMillis())

    // Current session tracking
    private var currentSessionId: String = UUID.randomUUID().toString()
    private var sessionStartTime: Long = System.currentTimeMillis()
    private var sessionCrashCount = AtomicInteger(0)
    private var sessionErrorCount = AtomicInteger(0)

    // Observable stats
    private val _stats = MutableStateFlow(calculateStats())
    override val stats: StateFlow<PerformanceStats> = _stats.asStateFlow()

    private val _recentCrashes = MutableStateFlow<List<CrashRecord>>(emptyList())
    override val recentCrashes: StateFlow<List<CrashRecord>> = _recentCrashes.asStateFlow()

    init {
        loadPersistedData()
    }

    /**
     * Record a performance metric.
     *
     * @param type The type of metric
     * @param value The metric value
     * @param context Optional context (will be sanitized)
     * @return true if recorded, false if rate-limited or invalid
     */
    override fun recordMetric(type: MetricType, value: Long, context: String?): Boolean {
        // Rate limiting check
        if (!checkRateLimit()) {
            Log.w(TAG, "Metric recording rate-limited")
            return false
        }

        // Validate and sanitize
        if (value < 0) {
            Log.w(TAG, "Invalid metric value: $value")
            return false
        }

        val sanitizedContext = context?.let { sanitizeContext(it) }

        try {
            val metric = PerformanceMetric(
                timestamp = System.currentTimeMillis(),
                type = type,
                value = value,
                context = sanitizedContext
            )

            recentMetrics.addLast(metric)

            // Trim in-memory buffer
            while (recentMetrics.size > MAX_STORED_METRICS) {
                recentMetrics.pollFirst()
            }

            // Track session errors
            if (type == MetricType.NON_FATAL_ERROR) {
                sessionErrorCount.incrementAndGet()
            }

            _stats.value = calculateStats()
            return true
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to create metric: ${e.message}")
            return false
        }
    }

    override fun recordCrash(throwable: Throwable, severity: SeverityLevel, isFatal: Boolean) {
        val fingerprint = generateCrashFingerprint(throwable)
        val existingCrash = crashRecords.find { it.fingerprint == fingerprint }

        if (existingCrash != null) {
            // Update occurrence count for duplicate crash
            crashRecords.remove(existingCrash)
            crashRecords.addFirst(
                existingCrash.copy(
                    timestamp = System.currentTimeMillis(),
                    occurrenceCount = existingCrash.occurrenceCount + 1
                )
            )
        } else {
            val record = CrashRecord(
                fingerprint = fingerprint,
                timestamp = System.currentTimeMillis(),
                severity = severity,
                exceptionType = throwable.javaClass.simpleName,
                message = sanitizeMessage(throwable.message),
                topStackFrames = sanitizeStackTrace(throwable)
            )
            crashRecords.addFirst(record)
        }

        // Trim crash history
        while (crashRecords.size > MAX_CRASH_RECORDS) {
            crashRecords.pollLast()
        }

        // Track in session
        if (isFatal) {
            sessionCrashCount.incrementAndGet()
            recordMetric(MetricType.CRASH, 1)
        } else {
            sessionErrorCount.incrementAndGet()
            recordMetric(MetricType.NON_FATAL_ERROR, 1)
        }

        // Update observable state
        _recentCrashes.value = crashRecords.toList()
        _stats.value = calculateStats()

        // Persist immediately for crashes
        persistData()
    }

    override fun recordStartupTime(durationMs: Long) {
        recordMetric(MetricType.APP_STARTUP, durationMs)
    }

    /**
     * Record connection time metric.
     */
    fun recordConnectionTime(durationMs: Long, connectionType: String) {
        recordMetric(
            MetricType.CONNECTION_TIME,
            durationMs,
            // "BLE" or "WiFi" only
            connectionType
        )
    }

    /**
     * Record command latency.
     */
    override fun recordLatency(latencyMs: Long) {
        recordMetric(MetricType.COMMAND_LATENCY, latencyMs)
    }

    override fun recordMemoryUsage(usedBytes: Long) {
        recordMetric(MetricType.MEMORY_USAGE, usedBytes)
    }

    override fun getStats(): PerformanceStats = _stats.value

    override fun getCrashHistory(): List<CrashRecord> = crashRecords.toList()

    override fun persistMetrics() {
        persistData()
    }

    override fun clearAll() {
        recentMetrics.clear()
        crashRecords.clear()
        _recentCrashes.value = emptyList()
        _stats.value = calculateStats()
        persistData()
    }

    /**
     * Generate a sanitized diagnostic report for export.
     *
     * SECURITY: No PII, device IDs, or sensitive paths included.
     */
    override fun generateDiagnosticReport(): String {
        val stats = calculateStats()
        return buildString {
            appendLine("=== Ardunakon Performance Report ===")
            appendLine(
                "Generated: ${java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.US
                ).format(java.util.Date())}"
            )
            appendLine()
            appendLine("--- Summary ---")
            appendLine("Sessions: ${stats.sessionCount}")
            appendLine(
                "Crash-free rate: ${String.format(Locale.getDefault(), "%.1f", stats.crashFreeSessionsPercent)}%"
            )
            appendLine("Avg startup time: ${stats.avgStartupTimeMs}ms")
            appendLine("Avg latency: ${String.format(Locale.getDefault(), "%.1f", stats.avgLatencyMs)}ms")
            appendLine("Total crashes: ${stats.totalCrashes}")
            appendLine("Total errors: ${stats.totalErrors}")
            appendLine()
            appendLine("--- Recent Crashes ---")
            crashRecords.take(5).forEach { crash ->
                appendLine("[${crash.severity}] ${crash.exceptionType} (x${crash.occurrenceCount})")
                crash.topStackFrames.forEach { frame ->
                    appendLine("  at $frame")
                }
            }
        }
    }

    // === Private Implementation ===

    private fun startSession() {
        currentSessionId = UUID.randomUUID().toString()
        sessionStartTime = System.currentTimeMillis()
        sessionCrashCount.set(0)
        sessionErrorCount.set(0)
    }

    private fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        val lastReset = lastMinuteReset.get()

        // Reset counter every minute
        if (now - lastReset > 60_000) {
            metricsThisMinute.set(0)
            lastMinuteReset.set(now)
        }

        return metricsThisMinute.incrementAndGet() <= MAX_METRICS_PER_MINUTE
    }

    /**
     * Sanitize context string to remove sensitive information.
     */
    private fun sanitizeContext(input: String): String {
        var sanitized = input

        // Remove file paths
        sanitized = sanitized.replace(Regex("/data/[^\\s]+"), "[path]")
        sanitized = sanitized.replace(Regex("/storage/[^\\s]+"), "[path]")

        // Remove potential email addresses
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[email]")

        // Remove potential phone numbers
        sanitized = sanitized.replace(Regex("\\+?\\d{10,}"), "[phone]")

        // Truncate to max length
        return sanitized.take(PerformanceMetric.MAX_CONTEXT_LENGTH)
    }

    /**
     * Sanitize exception message.
     */
    private fun sanitizeMessage(message: String?): String? {
        return message?.let { sanitizeContext(it).take(200) }
    }

    /**
     * Extract and sanitize stack trace to class.method format only.
     */
    private fun sanitizeStackTrace(throwable: Throwable): List<String> {
        return throwable.stackTrace
            .take(CrashRecord.MAX_STACK_FRAMES)
            .map { frame ->
                // Only keep class name and method, strip file/line info
                val className = frame.className.substringAfterLast('.')
                "$className.${frame.methodName}"
            }
    }

    /**
     * Generate SHA-256 fingerprint from anonymized crash signature.
     */
    private fun generateCrashFingerprint(throwable: Throwable): String {
        val signature = buildString {
            append(throwable.javaClass.name)
            throwable.stackTrace.take(3).forEach { frame ->
                append("|${frame.className}.${frame.methodName}")
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(signature.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun calculateStats(): PerformanceStats {
        val startupMetrics = recentMetrics.filter { it.type == MetricType.APP_STARTUP }
        val latencyMetrics = recentMetrics.filter { it.type == MetricType.COMMAND_LATENCY }

        val totalCrashes = crashRecords.sumOf { it.occurrenceCount }
        val totalErrors = recentMetrics.count { it.type == MetricType.NON_FATAL_ERROR }

        // Estimate session count from startup metrics
        val sessionCount = maxOf(1, startupMetrics.size)
        val crashFreeSessions = sessionCount - crashRecords.size
        val crashFreePercent = if (sessionCount > 0) {
            (crashFreeSessions.toFloat() / sessionCount * 100).coerceIn(0f, 100f)
        } else {
            100f
        }

        return PerformanceStats(
            crashFreeSessionsPercent = crashFreePercent,
            avgStartupTimeMs = startupMetrics.map { it.value }.average().toLong().coerceAtLeast(0),
            avgLatencyMs = latencyMetrics.map { it.value.toFloat() }.average().toFloat().coerceAtLeast(0f),
            totalCrashes = totalCrashes,
            totalErrors = totalErrors,
            sessionCount = sessionCount,
            lastCrashTime = crashRecords.firstOrNull()?.timestamp
        )
    }

    /**
     * Clean up data older than retention period.
     */
    private fun cleanupStaleData() {
        val cutoff = System.currentTimeMillis() - RETENTION_PERIOD_MS

        // Remove old metrics
        recentMetrics.removeIf { it.timestamp < cutoff }

        // Remove old crashes
        crashRecords.removeIf { it.timestamp < cutoff }

        Log.d(TAG, "Cleanup complete: ${recentMetrics.size} metrics, ${crashRecords.size} crashes retained")

        // Persist after cleanup
        persistData()
    }

    private fun loadPersistedData() {
        try {
            // Load metrics
            val metricsFile = File(context.filesDir, METRICS_FILE)
            if (metricsFile.exists()) {
                val json = JSONArray(metricsFile.readText())
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    try {
                        val metric = PerformanceMetric(
                            timestamp = obj.getLong("timestamp"),
                            type = MetricType.valueOf(obj.getString("type")),
                            value = obj.getLong("value"),
                            context = obj.optString("context", "").ifBlank { null }
                        )
                        recentMetrics.add(metric)
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            }

            // Load crashes
            val crashesFile = File(context.filesDir, CRASHES_FILE)
            if (crashesFile.exists()) {
                val json = JSONArray(crashesFile.readText())
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    try {
                        val frames = mutableListOf<String>()
                        val framesArray = obj.getJSONArray("topStackFrames")
                        for (j in 0 until framesArray.length()) {
                            frames.add(framesArray.getString(j))
                        }

                        val record = CrashRecord(
                            fingerprint = obj.getString("fingerprint"),
                            timestamp = obj.getLong("timestamp"),
                            severity = SeverityLevel.valueOf(obj.getString("severity")),
                            exceptionType = obj.getString("exceptionType"),
                            message = obj.optString("message", "").ifBlank { null },
                            topStackFrames = frames,
                            occurrenceCount = obj.optInt("occurrenceCount", 1)
                        )
                        crashRecords.add(record)
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            }

            _recentCrashes.value = crashRecords.toList()
            _stats.value = calculateStats()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted data", e)
        }
    }

    private fun persistData() {
        try {
            // Save metrics
            val metricsJson = JSONArray()
            recentMetrics.forEach { metric ->
                metricsJson.put(
                    JSONObject().apply {
                        put("timestamp", metric.timestamp)
                        put("type", metric.type.name)
                        put("value", metric.value)
                        metric.context?.let { put("context", it) }
                    }
                )
            }
            File(context.filesDir, METRICS_FILE).writeText(metricsJson.toString())

            // Save crashes
            val crashesJson = JSONArray()
            crashRecords.forEach { crash ->
                crashesJson.put(
                    JSONObject().apply {
                        put("fingerprint", crash.fingerprint)
                        put("timestamp", crash.timestamp)
                        put("severity", crash.severity.name)
                        put("exceptionType", crash.exceptionType)
                        crash.message?.let { put("message", it) }
                        put("topStackFrames", JSONArray(crash.topStackFrames))
                        put("occurrenceCount", crash.occurrenceCount)
                    }
                )
            }
            File(context.filesDir, CRASHES_FILE).writeText(crashesJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist data", e)
        }
    }
}
