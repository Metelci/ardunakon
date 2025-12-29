package com.metelci.ardunakon.util

import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Rich error context for debugging and diagnostics.
 *
 * Captures comprehensive information about errors including:
 * - Stack trace with sanitization
 * - Operation context and parameters
 * - Device and app state
 * - Timing information
 *
 * All data is sanitized to remove PII before logging/reporting.
 */
data class ErrorContext(
    val errorId: String = UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    val errorType: String,
    val message: String?,
    val stackTrace: List<String> = emptyList(),
    val parameters: Map<String, Any?> = emptyMap(),
    val deviceInfo: DeviceInfo = DeviceInfo.current(),
    val severity: Severity = Severity.ERROR,
    val tags: Set<String> = emptySet(),
    val retryCount: Int = 0,
    val parentContextId: String? = null
) {
    /**
     * Error severity levels.
     */
    enum class Severity {
        DEBUG, // Development info
        INFO, // Informational
        WARNING, // Recoverable issue
        ERROR, // Operation failed
        CRITICAL // System-level failure
    }

    /**
     * Device information snapshot.
     */
    data class DeviceInfo(
        val sdkVersion: Int,
        val manufacturer: String,
        val model: String,
        val appVersion: String? = null
    ) {
        companion object {
            fun current(appVersion: String? = null) = DeviceInfo(
                sdkVersion = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                appVersion = appVersion
            )
        }
    }

    /**
     * Generates a formatted debug string for logging.
     */
    fun toDebugString(): String = buildString {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        appendLine("═══════════════════════════════════════════════════════════════")
        appendLine("ERROR CONTEXT [$errorId] - ${severity.name}")
        appendLine("═══════════════════════════════════════════════════════════════")
        appendLine("Time:      ${dateFormat.format(Date(timestamp))}")
        appendLine("Operation: $operation")
        appendLine("Type:      $errorType")
        appendLine("Message:   ${message ?: "(no message)"}")

        if (retryCount > 0) {
            appendLine("Retries:   $retryCount")
        }

        if (tags.isNotEmpty()) {
            appendLine("Tags:      ${tags.joinToString(", ")}")
        }

        if (parameters.isNotEmpty()) {
            appendLine("─────────────────────────────────────────────────────────────────")
            appendLine("PARAMETERS:")
            parameters.forEach { (key, value) ->
                appendLine("  $key: ${sanitizeValue(value)}")
            }
        }

        appendLine("─────────────────────────────────────────────────────────────────")
        appendLine("DEVICE INFO:")
        appendLine("  Android SDK: ${deviceInfo.sdkVersion}")
        appendLine("  Device:      ${deviceInfo.manufacturer} ${deviceInfo.model}")
        deviceInfo.appVersion?.let { appendLine("  App Version: $it") }

        if (stackTrace.isNotEmpty()) {
            appendLine("─────────────────────────────────────────────────────────────────")
            appendLine("STACK TRACE (top ${stackTrace.size} frames):")
            stackTrace.forEachIndexed { index, frame ->
                appendLine("  ${index + 1}. $frame")
            }
        }

        parentContextId?.let {
            appendLine("─────────────────────────────────────────────────────────────────")
            appendLine("Parent Context: $it")
        }

        appendLine("═══════════════════════════════════════════════════════════════")
    }

    /**
     * Generates a compact one-line summary.
     */
    fun toSummary(): String {
        return "[$errorId] ${severity.name}: $operation - $errorType: ${message?.take(50) ?: "(no message)"}"
    }

    /**
     * Creates a child context for nested operations.
     */
    fun child(operation: String, errorType: String, message: String?): ErrorContext {
        return copy(
            errorId = UUID.randomUUID().toString().take(8),
            timestamp = System.currentTimeMillis(),
            operation = operation,
            errorType = errorType,
            message = message,
            parentContextId = this.errorId
        )
    }

    private fun sanitizeValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is ByteArray -> "[ByteArray size=${value.size}]"
            is CharArray -> "[CharArray size=${value.size}]"
            is String -> sanitizeString(value)
            is Collection<*> -> "[Collection size=${value.size}]"
            is Map<*, *> -> "[Map size=${value.size}]"
            else -> value.toString().take(100)
        }
    }

    private fun sanitizeString(value: String): String {
        // Remove potential sensitive data patterns
        var result = value

        // Mask email addresses
        result = result.replace(
            Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
            "[email]"
        )

        // Mask IP addresses
        result = result.replace(
            Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"),
            "[ip]"
        )

        // Mask MAC addresses
        result = result.replace(
            Regex("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}"),
            "[mac]"
        )

        // Truncate long strings
        return if (result.length > 200) result.take(200) + "..." else result
    }

    companion object {
        private const val MAX_STACK_FRAMES = 10
        private val APP_PACKAGE_PREFIX = "com.metelci.ardunakon"

        /**
         * Creates an ErrorContext from a Throwable.
         */
        fun fromThrowable(
            throwable: Throwable,
            operation: String,
            parameters: Map<String, Any?> = emptyMap(),
            severity: Severity = Severity.ERROR,
            tags: Set<String> = emptySet()
        ): ErrorContext {
            return ErrorContext(
                operation = operation,
                errorType = throwable.javaClass.simpleName,
                message = throwable.message,
                stackTrace = extractStackTrace(throwable),
                parameters = parameters,
                severity = severity,
                tags = tags
            )
        }

        /**
         * Creates a simple ErrorContext without a throwable.
         */
        fun simple(
            operation: String,
            errorType: String,
            message: String?,
            severity: Severity = Severity.ERROR,
            parameters: Map<String, Any?> = emptyMap()
        ): ErrorContext {
            return ErrorContext(
                operation = operation,
                errorType = errorType,
                message = message,
                parameters = parameters,
                severity = severity
            )
        }

        private fun extractStackTrace(throwable: Throwable): List<String> {
            return throwable.stackTrace
                .take(MAX_STACK_FRAMES)
                .map { frame ->
                    val className = frame.className.substringAfterLast('.')
                    val highlight = if (frame.className.startsWith(APP_PACKAGE_PREFIX)) "→ " else "  "
                    "$highlight$className.${frame.methodName}(${frame.lineNumber})"
                }
        }

        /**
         * Extracts the full stack trace as a string.
         */
        fun fullStackTrace(throwable: Throwable): String {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }
}

/**
 * Extension to create ErrorContext from any Throwable.
 */
fun Throwable.toErrorContext(
    operation: String,
    parameters: Map<String, Any?> = emptyMap(),
    severity: ErrorContext.Severity = ErrorContext.Severity.ERROR,
    tags: Set<String> = emptySet()
): ErrorContext = ErrorContext.fromThrowable(this, operation, parameters, severity, tags)

/**
 * Builder for fluent ErrorContext creation.
 */
class ErrorContextBuilder(private val operation: String) {
    private var errorType: String = "Unknown"
    private var message: String? = null
    private var throwable: Throwable? = null
    private var parameters: MutableMap<String, Any?> = mutableMapOf()
    private var severity: ErrorContext.Severity = ErrorContext.Severity.ERROR
    private var tags: MutableSet<String> = mutableSetOf()
    private var retryCount: Int = 0

    fun errorType(type: String) = apply { this.errorType = type }
    fun message(msg: String?) = apply { this.message = msg }
    fun throwable(t: Throwable) = apply {
        this.throwable = t
        this.errorType = t.javaClass.simpleName
        this.message = t.message
    }
    fun param(key: String, value: Any?) = apply { parameters[key] = value }
    fun params(map: Map<String, Any?>) = apply { parameters.putAll(map) }
    fun severity(s: ErrorContext.Severity) = apply { this.severity = s }
    fun tag(t: String) = apply { tags.add(t) }
    fun tags(vararg t: String) = apply { tags.addAll(t) }
    fun retryCount(count: Int) = apply { this.retryCount = count }

    fun build(): ErrorContext {
        val stackTrace = throwable?.let {
            ErrorContext.fromThrowable(it, operation).stackTrace
        } ?: emptyList()

        return ErrorContext(
            operation = operation,
            errorType = errorType,
            message = message,
            stackTrace = stackTrace,
            parameters = parameters.toMap(),
            severity = severity,
            tags = tags.toSet(),
            retryCount = retryCount
        )
    }
}

/**
 * DSL function for creating ErrorContext.
 */
fun errorContext(operation: String, init: ErrorContextBuilder.() -> Unit): ErrorContext {
    return ErrorContextBuilder(operation).apply(init).build()
}
