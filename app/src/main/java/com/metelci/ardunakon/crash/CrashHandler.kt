package com.metelci.ardunakon.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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

        fun getCrashLogFile(context: Context): File {
            return File(context.filesDir, CRASH_LOG_FILE)
        }

        fun hasCrashLog(context: Context): Boolean {
            return getCrashLogFile(context).exists() && getCrashLogFile(context).length() > 0
        }

        fun getCrashLog(context: Context): String {
            val file = getCrashLogFile(context)
            return if (file.exists()) file.readText() else ""
        }

        fun clearCrashLog(context: Context) {
            try {
                getCrashLogFile(context).delete()
            } catch (_: Exception) {}
        }

        fun getShareIntent(context: Context): Intent? {
            val crashLog = getCrashLog(context)
            if (crashLog.isEmpty()) return null

            return Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Ardunakon Crash Report")
                putExtra(Intent.EXTRA_TEXT, crashLog)
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }

        // Call default handler to let app crash normally
        defaultHandler?.uncaughtException(thread, throwable)
            ?: exitProcess(1)
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val file = getCrashLogFile(context)

        // Rotate logs - keep only last N crashes
        val existingLog = if (file.exists()) file.readText() else ""
        val crashes = existingLog.split("=== CRASH ===").filter { it.isNotBlank() }
        val recentCrashes = crashes.takeLast(MAX_CRASH_LOGS - 1)

        val crashInfo = buildString {
            appendLine("=== CRASH ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine()
            appendLine("--- Device Info ---")
            appendLine("App Version: ${getAppVersion()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("--- Memory Info ---")
            appendLine("Free: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
            appendLine("Max: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
            appendLine("Total: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
            appendLine()
            appendLine("--- Stack Trace ---")
            val sw = java.io.StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            appendLine(sw.toString())
            appendLine()
        }

        // Write combined log
        FileWriter(file).use { writer ->
            recentCrashes.forEach { crash ->
                writer.write("=== CRASH ===$crash")
            }
            writer.write(crashInfo)
        }

        Log.d(TAG, "Crash log saved to ${file.absolutePath}")
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
