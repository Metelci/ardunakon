package com.metelci.ardunakon.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.monitoring.MetricType
import com.metelci.ardunakon.monitoring.PerformanceMonitor
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runtime Application Self-Protection (RASP) Manager.
 *
 * Provides runtime security monitoring and protection against:
 * - Debugging attacks
 * - Root/jailbreak detection
 * - App tampering/integrity violations
 * - Emulator detection
 * - Suspicious runtime behavior
 *
 * @param context Application context used for package and filesystem checks.
 */
class RASPManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Security state
    private val _securityViolations = MutableStateFlow<List<LogEntry>>(emptyList())

    /**
     * Stream of detected security violations, newest to oldest.
     */
    val securityViolations: StateFlow<List<LogEntry>> = _securityViolations.asStateFlow()

    private val _isSecurityCompromised = MutableStateFlow(false)

    /**
     * True when any critical violation has been detected.
     */
    val isSecurityCompromised: StateFlow<Boolean> = _isSecurityCompromised.asStateFlow()

    @VisibleForTesting
    fun setSecurityViolationsForTest(violations: List<LogEntry>) {
        _securityViolations.value = violations
        _isSecurityCompromised.value = violations.any { it.type == LogType.ERROR }
    }

    // Expected APK signature hash (should be configured per build)
    private val expectedSignatureHash = SecurityConfig.EXPECTED_APK_SIGNATURE_HASH

    init {
        performInitialSecurityChecks()
        startRuntimeMonitoring()
    }

    /**
     * Perform initial security checks on app startup
     */
    private fun performInitialSecurityChecks() {
        scope.launch {
            val violations = mutableListOf<LogEntry>()

            // Check for debugging
            if (isDebuggerAttached()) {
                violations.add(
                    LogEntry(
                        type = LogType.ERROR,
                        message = "SECURITY: Debugger detected - potential reverse engineering attempt"
                    )
                )
                PerformanceMonitor.getInstance()?.recordMetric(MetricType.SECURITY_EVENT, 1, "Debug detected")
            }

            // Check for root
            if (isDeviceRooted()) {
                violations.add(
                    LogEntry(
                        type = LogType.ERROR,
                        message = "SECURITY: Rooted device detected - security risk"
                    )
                )
                PerformanceMonitor.getInstance()?.recordMetric(MetricType.SECURITY_EVENT, 1, "Root detected")
            }

            // Check app integrity
            if (!verifyAppIntegrity()) {
                violations.add(
                    LogEntry(
                        type = LogType.ERROR,
                        message = "SECURITY: App integrity compromised - APK may be tampered"
                    )
                )
                PerformanceMonitor.getInstance()?.recordMetric(MetricType.SECURITY_EVENT, 1, "Integrity check failed")
            }

            // Check for emulator
            if (isRunningOnEmulator()) {
                violations.add(
                    LogEntry(
                        type = LogType.WARNING,
                        message = "SECURITY: Emulator environment detected"
                    )
                )
                PerformanceMonitor.getInstance()?.recordMetric(MetricType.SECURITY_EVENT, 0, "Emulator detected")
            }

            // Update state
            _securityViolations.value = violations
            _isSecurityCompromised.value = violations.any { it.type == LogType.ERROR }

            // Log violations
            violations.forEach { violation ->
                Log.w("RASP", violation.message)
            }
        }
    }

    /**
     * Start continuous runtime monitoring
     */
    private fun startRuntimeMonitoring() {
        scope.launch {
            while (true) {
                // Periodic checks every 30 seconds
                kotlinx.coroutines.delay(30000)

                // Re-check critical security features
                val currentViolations = _securityViolations.value.toMutableList()

                if (isDebuggerAttached() && !hasViolation("Debugger detected")) {
                    currentViolations.add(
                        LogEntry(
                            type = LogType.ERROR,
                            message = "SECURITY: Debugger attached during runtime"
                        )
                    )
                    PerformanceMonitor.getInstance()?.recordMetric(
                        MetricType.SECURITY_EVENT,
                        1,
                        "Runtime debugger attached"
                    )
                }

                _securityViolations.value = currentViolations
                _isSecurityCompromised.value = currentViolations.any { it.type == LogType.ERROR }
            }
        }
    }

    /**
     * Check if debugger is attached
     */
    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Check if device is rooted
     */
    private fun isDeviceRooted(): Boolean {
        val rootIndicators = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/Magisk.apk"
        )

        // Check for root binaries
        rootIndicators.forEach { path ->
            if (File(path).exists()) {
                return true
            }
        }

        // Check for su command availability
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo test"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify APK integrity by checking signature
     */
    private fun verifyAppIntegrity(): Boolean {
        if (expectedSignatureHash.isBlank()) {
            // If no expected hash configured, skip integrity check
            return true
        }

        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.any { signature ->
                val cert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    signature.toByteArray()
                } else {
                    signature.toByteArray()
                }

                val md = MessageDigest.getInstance("SHA-256")
                val hash = md.digest(cert)
                val hashString = hash.joinToString("") { "%02x".format(it) }

                hashString.equals(expectedSignatureHash, ignoreCase = true)
            } ?: false
        } catch (e: Exception) {
            Log.e("RASP", "Failed to verify app integrity", e)
            false
        }
    }

    /**
     * Check if running on emulator
     */
    private fun isRunningOnEmulator(): Boolean {
        val emulatorIndicators = listOf(
            Build.MODEL.contains("sdk", ignoreCase = true),
            Build.MODEL.contains("emulator", ignoreCase = true),
            Build.MANUFACTURER.contains("genymotion", ignoreCase = true),
            Build.BRAND.contains("generic", ignoreCase = true),
            Build.DEVICE.contains("generic", ignoreCase = true),
            Build.PRODUCT.contains("sdk", ignoreCase = true),
            Build.HARDWARE.contains("goldfish", ignoreCase = true),
            Build.FINGERPRINT.contains("generic", ignoreCase = true)
        )

        return emulatorIndicators.any { it }
    }

    /**
     * Check if a specific violation already exists
     */
    private fun hasViolation(keyword: String): Boolean {
        return _securityViolations.value.any { it.message.contains(keyword) }
    }

    /**
     * Wipes sensitive data from the key-value storage.
     * Should be called when a critical security violation is detected.
     */
    fun wipeSensitiveData() {
        try {
            // Clear known preferences files
            val prefsToClear = listOf(
                "connection_prefs",
                "onboarding_prefs",
                // Assuming these are the names or similar
                "custom_commands"
            )

            prefsToClear.forEach { prefName ->
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            }

            Log.w("RASP", "Sensitive data wiped due to security violation")
            PerformanceMonitor.getInstance()?.recordMetric(MetricType.SECURITY_EVENT, 1, "Data wiped")
        } catch (e: Exception) {
            Log.e("RASP", "Failed to wipe sensitive data", e)
        }
    }

    /**
     * Builds a snapshot summary of the current security status.
     *
     * @return Aggregated security status values.
     */
    fun getSecurityStatus(): SecurityStatus {
        val violations = _securityViolations.value
        val compromised = _isSecurityCompromised.value

        return SecurityStatus(
            isCompromised = compromised,
            violationCount = violations.size,
            criticalViolations = violations.count { it.type == LogType.ERROR },
            warnings = violations.count { it.type == LogType.WARNING }
        )
    }

    /**
     * Aggregated security status summary.
     *
     * @property isCompromised True when critical violations exist.
     * @property violationCount Total violation count.
     * @property criticalViolations Count of critical violations.
     * @property warnings Count of warning-level violations.
     */
    data class SecurityStatus(
        val isCompromised: Boolean,
        val violationCount: Int,
        val criticalViolations: Int,
        val warnings: Int
    )

    companion object {
        private var instance: RASPManager? = null

        /**
         * Returns a singleton instance of the RASP manager.
         *
         * @param context Application context for initialization.
         * @return Singleton RASPManager instance.
         */
        fun getInstance(context: Context): RASPManager {
            return instance ?: synchronized(this) {
                instance ?: RASPManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Initializes the singleton instance if it has not been created yet.
         *
         * @param context Application context for initialization.
         */
        fun init(context: Context) {
            getInstance(context)
        }
    }
}
