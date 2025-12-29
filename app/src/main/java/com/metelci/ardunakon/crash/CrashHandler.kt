package com.metelci.ardunakon.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.metelci.ardunakon.monitoring.PerformanceMonitor
import com.metelci.ardunakon.monitoring.SeverityLevel
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Global crash handler that logs crashes to a file for debugging.
 * Crash logs are saved and can be shared via the debug menu.
 */
class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_LOG_FILE = "crash_log.txt"
        private const val MAX_CRASH_LOGS = 5

        @Volatile
        private var instance: CrashHandler? = null

        @Volatile
        private var crashLogWriter: CrashLogWriter = FileCrashLogWriter()

        @VisibleForTesting
        fun setCrashLogWriterForTest(writer: CrashLogWriter) {
            crashLogWriter = writer
        }

        /**
         * Installs the crash handler as the default uncaught exception handler.
         *
         * @param context Application context used for log storage.
         */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                        instance = CrashHandler(context.applicationContext, defaultHandler)
                        Thread.setDefaultUncaughtExceptionHandler(instance)
                        Log.d(TAG, "CrashHandler initialized")
                    }
                }
            }
        }

        /**
         * Returns the crash log file location.
         *
         * @param context Application context used to resolve files.
         * @return Crash log file reference.
         */
        fun getCrashLogFile(context: Context): File = File(context.filesDir, CRASH_LOG_FILE)

        /**
         * Checks whether a crash log is present and non-empty.
         *
         * @param context Application context used to resolve files.
         * @return True if the crash log exists with content.
         */
        fun hasCrashLog(context: Context): Boolean =
            getCrashLogFile(context).exists() && getCrashLogFile(context).length() > 0

        /**
         * Reads the current crash log contents.
         *
         * @param context Application context used to resolve files.
         * @return Crash log contents, or an empty string if missing.
         */
        fun getCrashLog(context: Context): String {
            val file = getCrashLogFile(context)
            return if (file.exists()) file.readText() else ""
        }

        /**
         * Clears the stored crash log.
         *
         * @param context Application context used to resolve files.
         */
        fun clearCrashLog(context: Context) {
            try {
                getCrashLogFile(context).delete()
            } catch (_: Exception) {}
        }

        /**
         * Builds a share intent with the crash log contents.
         *
         * @param context Application context used to read the crash log.
         * @return Intent for sharing the crash log, or null if no log exists.
         */
        fun getShareIntent(context: Context): Intent? {
            val crashLog = getCrashLog(context)
            if (crashLog.isEmpty()) return null

            return Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Ardunakon Crash Report")
                putExtra(Intent.EXTRA_TEXT, crashLog)
            }
        }

        /**
         * Records a non-fatal exception into the crash log and metrics.
         *
         * @param context Application context used for log storage.
         * @param throwable Exception to record.
         * @param message Optional custom message to include.
         * @param severity Severity level for monitoring.
         */
        fun logException(
            context: Context,
            throwable: Throwable,
            message: String? = null,
            severity: SeverityLevel = SeverityLevel.ERROR
        ) {
            try {
                // Record to PerformanceMonitor, but do not block log persistence.
                PerformanceMonitor.getInstance()?.recordCrash(
                    throwable = throwable,
                    severity = severity,
                    isFatal = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record non-fatal exception in monitor", e)
            }

            try {
                instance?.saveCrashLog(Thread.currentThread(), throwable, message)
                    ?: writeFallbackCrashLog(context, throwable, message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log non-fatal exception", e)
            }
        }

        private fun writeFallbackCrashLog(context: Context, throwable: Throwable, message: String? = null) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val crashInfo = buildString {
                appendLine()
                appendLine("=== NON-FATAL ERROR ===")
                appendLine("Timestamp: $timestamp")
                if (message != null) appendLine("Message: $message")
                val sw = java.io.StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                appendLine(sw.toString())
            }

            crashLogWriter.write(context, crashInfo)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Record fatal crash to PerformanceMonitor
            PerformanceMonitor.getInstance()?.recordCrash(
                throwable = throwable,
                severity = SeverityLevel.FATAL,
                isFatal = true
            )

            val crashData = saveCrashLog(thread, throwable)

            // Launch Crash Report Activity
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                putExtra("STACK_TRACE", crashData)
                putExtra("MESSAGE", throwable.localizedMessage ?: "Unknown Error")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)

            // Kill current process
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle crash", e)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable, customMessage: String? = null): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val file = getCrashLogFile(context)

        // Rotate logs - keep only last N crashes
        // For simplicity in this implementation, we will append new log to file directly.
        // A robust rotation would parse the file, but let's just ensure we don't grow forever.
        if (file.length() > 1024 * 1024) { // 1MB limit
            file.delete()
        }

        val crashInfo = buildString {
            appendLine("=== ${if (customMessage != null) "NON-FATAL ERROR" else "CRASH"} ===")
            appendLine("Timestamp: $timestamp")
            val threadId = thread.id
            appendLine("Thread: ${thread.name} ($threadId)")
            if (customMessage != null) {
                appendLine("Message: $customMessage")
            }
            appendLine()
            appendLine("--- Device Info ---")
            appendLine("App Version: ${getAppVersion()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("--- Memory Info ---")
            appendLine("Free: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
            appendLine("Total: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
            appendLine()
            appendLine("--- Breadcrumbs (Last Actions) ---")
            appendLine(BreadcrumbManager.getBreadcrumbs())
            appendLine()
            appendLine("--- Stack Trace ---")
            val sw = java.io.StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            appendLine(sw.toString())
            appendLine()
        }

        // Write combined log
        try {
            crashLogWriter.write(context, crashInfo)
            Log.d(TAG, "Crash log saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing crash log", e)
        }

        return crashInfo
    }

    private fun getAppVersion(): String = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
        "${pInfo.versionName} ($versionCode)"
    } catch (_: Exception) {
        "Unknown"
    }

    interface CrashLogWriter {
        fun write(context: Context, content: String)
    }

    private class FileCrashLogWriter : CrashLogWriter {
        override fun write(context: Context, content: String) {
            val file = getCrashLogFile(context)
            FileWriter(file, true).use { writer ->
                writer.write(content)
            }
        }
    }
}
