package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.data.Profile
import com.metelci.ardunakon.data.ProfileManager
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.security.AuthRequiredException
import com.metelci.ardunakon.security.EncryptionException
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Connection mode for the control screen.
 */
enum class ConnectionMode {
    BLUETOOTH,
    WIFI
}

/**
 * ViewModel for ControlScreen - manages all control state and business logic.
 *
 * This extracts state management from the composable to improve testability,
 * reduce recomposition, and separate concerns.
 */
class ControlViewModel(
    val bluetoothManager: AppBluetoothManager,
    val wifiManager: WifiManager,
    private val profileManager: ProfileManager,
    private val transmissionDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    var showDeviceList by mutableStateOf(false)
    var showDebugConsole by mutableStateOf(false)
    var showHelpDialog by mutableStateOf(false)
    var showAboutDialog by mutableStateOf(false)
    var showOverflowMenu by mutableStateOf(false)
    var showTelemetryGraph by mutableStateOf(false)
    var isDebugPanelVisible by mutableStateOf(true)
    var showMaximizedDebug by mutableStateOf(false)
    var showOtaDialog by mutableStateOf(false)
    var showWifiConfig by mutableStateOf(false)
    var showCrashLog by mutableStateOf(false)
    var showHeaderMenu by mutableStateOf(false)

    // ========== Connection State ==========
    var connectionMode by mutableStateOf(ConnectionMode.BLUETOOTH)
    var allowReflection by mutableStateOf(false)

    val profiles = mutableStateListOf<Profile>()
    var currentProfileIndex by mutableStateOf(0)
    var securityErrorMessage by mutableStateOf<String?>(null)

    // ========== Encryption State ==========
    var encryptionError by mutableStateOf<EncryptionException?>(null)
    var requireEncryption by mutableStateOf(true)

    val currentProfile: Profile
        get() = if (profiles.isNotEmpty() && currentProfileIndex in profiles.indices) {
            profiles[currentProfileIndex]
        } else {
            if (profiles.isEmpty()) {
                val defaults = profileManager.createDefaultProfiles()
                profiles.addAll(defaults)
                defaults[0]
            } else {
                profiles[0]
            }
        }

    // ========== Control State ==========
    var leftJoystick by mutableStateOf(Pair(0f, 0f))
    var servoX by mutableStateOf(0f)
    var servoY by mutableStateOf(0f)

    // ========== Transmission ==========
    private var transmissionJob: Job? = null
    private var isForegroundActive = true

    init {
        wifiManager.setRequireEncryption(requireEncryption)
        loadProfiles()
        startTransmissionLoop()
        syncReflectionSetting()
        observeConnectionState()
        observeEncryptionErrors()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            try {
                profiles.addAll(profileManager.loadProfiles())
            } catch (e: AuthRequiredException) {
                securityErrorMessage = e.message ?: "Unlock your device to load encrypted profiles."
                profiles.addAll(profileManager.createDefaultProfiles())
            } catch (e: java.io.IOException) {
                bluetoothManager.log("Failed to load profiles: ${e.message}", LogType.ERROR)
                profiles.addAll(profileManager.createDefaultProfiles())
            } catch (e: Exception) {
                bluetoothManager.log("Unexpected error loading profiles: ${e.message}", LogType.ERROR)
                profiles.addAll(profileManager.createDefaultProfiles())
            }
        }
    }

    private fun syncReflectionSetting() {
        viewModelScope.launch {
            bluetoothManager.allowReflectionFallback = allowReflection
        }
    }

    fun setForegroundActive(active: Boolean) {
        if (isForegroundActive == active) return
        isForegroundActive = active
        if (active) {
            startTransmissionLoop()
        } else {
            transmissionJob?.cancel()
            transmissionJob = null
        }
    }

    fun updateAllowReflection(enabled: Boolean) {
        allowReflection = enabled
        bluetoothManager.allowReflectionFallback = enabled
    }

    private fun startTransmissionLoop() {
        transmissionJob?.cancel()
        transmissionJob = viewModelScope.launch(transmissionDispatcher) {
            while (isActive) {
                if (!isForegroundActive) {
                    delay(200)
                    continue
                }
                // Check E-STOP state - block all transmissions if active
                if (bluetoothManager.isEmergencyStopActive.value) {
                    delay(50)
                    continue
                }

                // Only send when actually connected - prevents stale packet buffering
                val btConnected = bluetoothManager.connectionState.value == ConnectionState.CONNECTED
                val wifiConnected = wifiManager.connectionState.value == WifiConnectionState.CONNECTED

                if ((connectionMode == ConnectionMode.BLUETOOTH && !btConnected) ||
                    (connectionMode == ConnectionMode.WIFI && !wifiConnected)
                ) {
                    delay(50)
                    continue
                }

                val packet = ProtocolManager.formatJoystickData(
                    leftX = leftJoystick.first,
                    leftY = leftJoystick.second,
                    rightX = servoX,
                    rightY = servoY,
                    auxBits = 0
                )

                if (connectionMode == ConnectionMode.WIFI) {
                    wifiManager.sendData(packet)
                } else {
                    bluetoothManager.sendDataToAll(packet)
                }
                delay(50) // 20Hz
            }
        }
    }

    /**
     * Observes connection state changes and resets control inputs on disconnect.
     * This prevents stale steering/throttle values from causing motor spin on reconnect.
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                if (state == ConnectionState.DISCONNECTED) {
                    // Reset control inputs to neutral on disconnect
                    // This prevents stale steering values from causing motor spin on reconnect
                    // (Arcade drive mixing: leftSpeed = leftY + leftX - any non-zero leftX causes spin)
                    leftJoystick = Pair(0f, 0f)
                    servoX = 0f
                    servoY = 0f
                }
            }
        }
    }

    /**
     * Observes encryption errors from WifiManager and updates the UI state.
     */
    private fun observeEncryptionErrors() {
        viewModelScope.launch {
            wifiManager.encryptionError.collect { error ->
                encryptionError = error
            }
        }
    }

    /**
     * Retries the WiFi connection with encryption enabled.
     */
    fun retryWithEncryption() {
        encryptionError = null
        wifiManager.clearEncryptionError()
        requireEncryption = true
        wifiManager.setRequireEncryption(true)
        // User should reconnect via WiFi config dialog
        bluetoothManager.log("Encryption required - please reconnect", LogType.INFO)
    }

    /**
     * Continues the connection without encryption requirement.
     */
    fun continueWithoutEncryption() {
        encryptionError = null
        wifiManager.clearEncryptionError()
        requireEncryption = false
        wifiManager.setRequireEncryption(false)
        bluetoothManager.log("Continuing without encryption", LogType.WARNING)
    }

    /**
     * Dismisses encryption error and disconnects.
     */
    fun dismissEncryptionError() {
        encryptionError = null
        wifiManager.clearEncryptionError()
        wifiManager.disconnect()
    }

    /**
     * Updates the encryption requirement setting.
     */
    fun updateRequireEncryption(required: Boolean) {
        requireEncryption = required
        wifiManager.setRequireEncryption(required)
    }

    // ========== E-Stop Handling ==========
    fun toggleEStop(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val isActive = bluetoothManager.isEmergencyStopActive.value
        if (isActive) {
            bluetoothManager.setEmergencyStop(false)
            bluetoothManager.holdOfflineAfterEStopReset()
        } else {
            bluetoothManager.setEmergencyStop(true)
            bluetoothManager.disconnectAllForEStop()
            val stopPacket = ProtocolManager.formatEStopData()
            bluetoothManager.sendDataToAll(stopPacket, force = true)
        }
    }

    // ========== Joystick/Servo Updates ==========
    fun updateJoystick(x: Float, y: Float) {
        leftJoystick = Pair(
            x * currentProfile.sensitivity,
            y * currentProfile.sensitivity
        )
    }

    fun updateServo(x: Float, y: Float) {
        servoX = x
        servoY = y
    }

    // ========== Servo Command Handler (from debug terminal) ==========
    fun handleServoCommand(cmd: String) {
        when (cmd.uppercase().trim()) {
            "W" -> {
                servoY = if (servoY == 1f) 0f else 1f
                bluetoothManager.log("Servo: ${if (servoY == 1f) "FORWARD" else "CENTER"} (W)", LogType.INFO)
            }
            "B" -> {
                servoY = if (servoY == -1f) 0f else -1f
                bluetoothManager.log("Servo: ${if (servoY == -1f) "BACKWARD" else "CENTER"} (B)", LogType.INFO)
            }
            "L", "A" -> {
                servoX = if (servoX == -1f) 0f else -1f
                bluetoothManager.log("Servo: ${if (servoX == -1f) "LEFT" else "CENTER"} (L)", LogType.INFO)
            }
            "R" -> {
                servoX = if (servoX == 1f) 0f else 1f
                bluetoothManager.log("Servo: ${if (servoX == 1f) "RIGHT" else "CENTER"} (R)", LogType.INFO)
            }
            else -> {
                // Send raw command to connection
                val bytes = "$cmd\n".toByteArray()
                if (connectionMode == ConnectionMode.WIFI) {
                    wifiManager.sendData(bytes)
                    bluetoothManager.log("TX (WiFi): $cmd", LogType.INFO)
                } else {
                    bluetoothManager.sendDataToAll(bytes, force = true)
                    bluetoothManager.log("TX (BT): $cmd", LogType.INFO)
                }
            }
        }
    }

    // ========== Profile Save (internal use) ==========
    private fun saveProfiles() {
        viewModelScope.launch {
            try {
                profileManager.saveProfiles(profiles)
            } catch (e: AuthRequiredException) {
                securityErrorMessage = e.message ?: "Unlock your device to save encrypted profiles."
            }
        }
    }

    // ========== Connection Mode ==========
    fun toggleConnectionMode() {
        if (connectionMode == ConnectionMode.BLUETOOTH) {
            switchToWifi()
        } else {
            switchToBluetooth()
        }
    }

    fun switchToWifi() {
        if (connectionMode == ConnectionMode.BLUETOOTH) {
            bluetoothManager.log("Switching from Bluetooth to WiFi mode", LogType.INFO)
            bluetoothManager.disconnect()
            bluetoothManager.log("Bluetooth disconnected", LogType.WARNING)
        }
        connectionMode = ConnectionMode.WIFI
        bluetoothManager.log("WiFi mode active", LogType.SUCCESS)
    }

    fun switchToBluetooth() {
        if (connectionMode == ConnectionMode.WIFI) {
            bluetoothManager.log("Switching from WiFi to Bluetooth mode", LogType.INFO)
            wifiManager.disconnect()
            bluetoothManager.log("WiFi disconnected", LogType.WARNING)
        }
        connectionMode = ConnectionMode.BLUETOOTH
        bluetoothManager.log("Bluetooth mode active", LogType.SUCCESS)
    }

    fun reconnectBluetoothDevice(): Boolean = bluetoothManager.reconnectSavedDevice()

    // ========== Log Export ==========
    fun exportLogs(context: Context) {
        val debugLogs = bluetoothManager.debugLogs.value
        val telemetry = bluetoothManager.telemetry.value

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "ardunakon_logs_$timestamp.txt"
            val exportDir = File(context.cacheDir, "shared_logs").apply { mkdirs() }
            val file = File(exportDir, fileName)

            val logContent = buildString {
                appendLine("Ardunakon Debug Logs")
                appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("=".repeat(50))
                appendLine()

                if (debugLogs.isEmpty()) {
                    appendLine("No logs available")
                } else {
                    debugLogs.forEach { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))
                        val typeStr = log.type.name.padEnd(8)
                        appendLine("[$timeStr] $typeStr: ${log.message}")
                    }
                }

                appendLine()
                appendLine("=".repeat(50))
                telemetry?.let { telem ->
                    appendLine("Telemetry:")
                    appendLine("  Battery Voltage: ${telem.batteryVoltage}V")
                    appendLine("  Status: ${telem.status}")
                    appendLine("  Packets Sent: ${telem.packetsSent}")
                    appendLine("  Packets Dropped: ${telem.packetsDropped}")
                    appendLine("  Packets Failed: ${telem.packetsFailed}")
                    val totalLoss = telem.packetsDropped + telem.packetsFailed
                    val lossPercent = if (telem.packetsSent > 0) {
                        (totalLoss.toFloat() / telem.packetsSent * 100)
                    } else {
                        0f
                    }
                    appendLine("  Packet Loss: ${"%.2f".format(lossPercent)}%")
                }
            }

            file.writeText(logContent)

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ardunakon Debug Logs")
                putExtra(Intent.EXTRA_TEXT, "Debug logs from Ardunakon app")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
        } catch (e: Exception) {
            bluetoothManager.log("Export failed: ${e.message}", LogType.ERROR)
        }
    }

    override fun onCleared() {
        super.onCleared()
        transmissionJob?.cancel()
    }
}
