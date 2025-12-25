package com.metelci.ardunakon.ui.screens.control

import com.metelci.ardunakon.ui.utils.hapticTap

import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.crash.BreadcrumbManager
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.security.EncryptionException
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
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
@dagger.hilt.android.lifecycle.HiltViewModel
class ControlViewModel @javax.inject.Inject constructor(
    val bluetoothManager: AppBluetoothManager,
    val wifiManager: WifiManager,
    private val connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
    private val onboardingManager: com.metelci.ardunakon.data.OnboardingManager,
    val customCommandRegistry: com.metelci.ardunakon.protocol.CustomCommandRegistry,
    private val hapticPreferences: com.metelci.ardunakon.data.HapticPreferences
) : ViewModel() {

    private val transmissionDispatcher: CoroutineDispatcher = Dispatchers.Default

    var showDeviceList by mutableStateOf(false)
    var showDebugConsole by mutableStateOf(false)
    var showHelpDialog by mutableStateOf(false)
    var showAboutDialog by mutableStateOf(false)
    var showOverflowMenu by mutableStateOf(false)
    var showTelemetryGraph by mutableStateOf(false)
    var isDebugPanelVisible by mutableStateOf(true)
    var showMaximizedDebug by mutableStateOf(false)
    var showWifiConfig by mutableStateOf(false)
    var showCrashLog by mutableStateOf(false)
    var showSettingsDialog by mutableStateOf(false)
    var showHeaderMenu by mutableStateOf(false)

    // Custom Commands
    var showCustomCommandsDialog by mutableStateOf(false)
    var showCustomCommandEditor by mutableStateOf(false)
    var editingCommand by mutableStateOf<com.metelci.ardunakon.model.CustomCommand?>(null)

    // User Feedback (Snackbars)
    private val _userMessage = Channel<String>(Channel.CONFLATED)
    val userMessage = _userMessage.receiveAsFlow()

    fun showMessage(message: String) {
        viewModelScope.launch {
            _userMessage.trySend(message)
        }
    }

    /**
     * Resets the onboarding tutorial so it appears on next launch.
     */
    fun resetTutorial() {
        viewModelScope.launch {
            onboardingManager.resetOnboarding()
            bluetoothManager.log("Tutorial reset - will show on next launch", LogType.INFO)
            showMessage("Tutorial reset! Restart app to see it.")
        }
    }

    // ========== Connection State ==========
    var connectionMode by mutableStateOf(ConnectionMode.BLUETOOTH)
    var allowReflection by mutableStateOf(false)

    // WiFi Auto-Reconnect State (Delegated to Manager)
    val wifiAutoReconnectEnabled = wifiManager.autoReconnectEnabled

    var securityErrorMessage by mutableStateOf<String?>(null)

    // ========== Encryption State ==========
    var encryptionError by mutableStateOf<EncryptionException?>(null)
    var requireEncryption by mutableStateOf(false)

    // Haptic feedback state
    var isHapticEnabled by mutableStateOf(true)
        private set

    // Sensitivity state
    var joystickSensitivity by mutableStateOf(1.0f)
        private set

    fun updateJoystickSensitivity(sensitivity: Float) {
        joystickSensitivity = sensitivity
        viewModelScope.launch {
            connectionPreferences.saveLastConnection(joystickSensitivity = sensitivity)
        }
    }

    fun updateHapticEnabled(enabled: Boolean) {
        isHapticEnabled = enabled
        viewModelScope.launch {
            hapticPreferences.setHapticEnabled(enabled)
        }
    }

    // ========== Control State ==========
    var leftJoystick by mutableStateOf(Pair(0f, 0f))
    var servoX by mutableStateOf(0f)
    var servoY by mutableStateOf(0f)
    var servoZ by mutableStateOf(0f)

    // ========== Transmission ==========
    private var transmissionJob: Job? = null
    private var isForegroundActive = true
    private val inputEpsilon = 0.0001f

    private fun computeAuxBits(): Byte {
        var auxBits = 0
        if (servoZ < -inputEpsilon) auxBits = auxBits or (ProtocolManager.AUX_W.toInt() and 0xFF)
        if (servoZ > inputEpsilon) auxBits = auxBits or (ProtocolManager.AUX_B.toInt() and 0xFF)
        return auxBits.toByte()
    }

    private fun sendJoystickNow(force: Boolean) {
        if (!isForegroundActive) return
        if (bluetoothManager.isEmergencyStopActive.value) return

        val auxBits = computeAuxBits()
        val inputActive =
            abs(leftJoystick.first) > inputEpsilon ||
                abs(leftJoystick.second) > inputEpsilon ||
                abs(servoX) > inputEpsilon ||
                abs(servoY) > inputEpsilon ||
                auxBits.toInt() != 0

        if (!force && !inputActive) return

        val btConnected = bluetoothManager.connectionState.value == ConnectionState.CONNECTED
        val wifiConnected = wifiManager.connectionState.value == WifiConnectionState.CONNECTED
        val connected = when (connectionMode) {
            ConnectionMode.BLUETOOTH -> btConnected
            ConnectionMode.WIFI -> wifiConnected
        }
        if (!connected) return

        val packet = ProtocolManager.formatJoystickData(
            leftX = leftJoystick.first,
            leftY = leftJoystick.second,
            rightX = servoX,
            rightY = servoY,
            auxBits = auxBits
        )

        if (!force && !ProtocolManager.shouldSendJoystickPacket(packet)) return

        if (connectionMode == ConnectionMode.WIFI) {
            wifiManager.sendData(packet)
        } else {
            bluetoothManager.sendDataToAll(packet)
        }
    }

    init {
        // Restore Connection Mode + persisted sensitivity + haptic preference
        viewModelScope.launch {
            val lastConn = connectionPreferences.loadLastConnection()
            joystickSensitivity = lastConn.joystickSensitivity
            isHapticEnabled = hapticPreferences.isHapticEnabled()
            com.metelci.ardunakon.ui.utils.HapticController.isEnabled = isHapticEnabled
            if (lastConn.type == "WIFI") {
                connectionMode = ConnectionMode.WIFI
                bluetoothManager.log("Restored Connection Mode: WiFi", LogType.INFO)
            } else {
                connectionMode = ConnectionMode.BLUETOOTH // Default
            }
        }

        wifiManager.setRequireEncryption(requireEncryption)
        startTransmissionLoop()
        syncReflectionSetting()
        observeConnectionState()
        observeEncryptionErrors()

        // Initialize custom commands registry
        customCommandRegistry.initialize()
    }

    private fun syncReflectionSetting() {
        viewModelScope.launch {
            bluetoothManager.allowReflectionFallback = allowReflection
        }
    }

    fun setForegroundActive(active: Boolean) {
        if (isForegroundActive == active) return
        isForegroundActive = active
        bluetoothManager.setForegroundMode(active)
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
            var wasConnected = false
            var wasInputActive = false

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

                val connected = when (connectionMode) {
                    ConnectionMode.BLUETOOTH -> btConnected
                    ConnectionMode.WIFI -> wifiConnected
                }

                if (!connected) {
                    wasConnected = false
                    wasInputActive = false
                    delay(50)
                    continue
                }

                wasConnected = true

                val auxBits = computeAuxBits()
                val inputActive =
                    abs(leftJoystick.first) > inputEpsilon ||
                        abs(leftJoystick.second) > inputEpsilon ||
                        abs(servoX) > inputEpsilon ||
                        abs(servoY) > inputEpsilon ||
                        auxBits.toInt() != 0

                if (!inputActive) {
                    if (wasInputActive) {
                        // Flush a final neutral packet once, then stop sending in idle.
                        sendJoystickNow(force = true)
                    }
                    wasInputActive = false
                    delay(50)
                    continue
                }

                wasInputActive = true
                sendJoystickNow(force = false)
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
                    servoZ = 0f
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
        view.hapticTap()
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
            x * joystickSensitivity,
            y * joystickSensitivity
        )
    }

    fun updateServo(x: Float, y: Float, z: Float) {
        val previousZ = servoZ
        servoX = x
        servoY = y
        servoZ = z

        // A/Z must be reflected immediately in CMD_JOYSTICK auxBits (no heartbeat involvement).
        if (previousZ != z) {
            sendJoystickNow(force = true)
        }
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
            "L" -> {
                servoX = if (servoX == -1f) 0f else -1f
                bluetoothManager.log("Servo: ${if (servoX == -1f) "LEFT" else "CENTER"} (L)", LogType.INFO)
            }
            "R" -> {
                servoX = if (servoX == 1f) 0f else 1f
                bluetoothManager.log("Servo: ${if (servoX == 1f) "RIGHT" else "CENTER"} (R)", LogType.INFO)
            }
            "A" -> {
                servoZ = if (servoZ == -1f) 0f else -1f
                bluetoothManager.log("Servo Z: ${if (servoZ == -1f) "MIN" else "CENTER"} (A)", LogType.INFO)
            }
            "Z" -> {
                servoZ = if (servoZ == 1f) 0f else 1f
                bluetoothManager.log("Servo Z: ${if (servoZ == 1f) "MAX" else "CENTER"} (Z)", LogType.INFO)
            }
            "CRASH" -> {
                bluetoothManager.log("Executing Test Crash...", LogType.ERROR)
                throw RuntimeException("Test Crash triggered by user command")
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
        ProtocolManager.resetPacketCache()
        connectionMode = ConnectionMode.WIFI
        viewModelScope.launch { connectionPreferences.saveLastConnection(type = "WIFI") }
        bluetoothManager.log("WiFi mode active", LogType.SUCCESS)
        BreadcrumbManager.leave("Mode", "Switched to WiFi")
    }

    fun switchToBluetooth() {
        if (connectionMode == ConnectionMode.WIFI) {
            bluetoothManager.log("Switching from WiFi to Bluetooth mode", LogType.INFO)
            wifiManager.disconnect()
            bluetoothManager.log("WiFi disconnected", LogType.WARNING)
        }
        ProtocolManager.resetPacketCache()
        connectionMode = ConnectionMode.BLUETOOTH
        viewModelScope.launch { connectionPreferences.saveLastConnection(type = "BLUETOOTH") }
        bluetoothManager.log("Bluetooth mode active", LogType.SUCCESS)
        BreadcrumbManager.leave("Mode", "Switched to Bluetooth")
    }

    fun toggleWifiAutoReconnect(enabled: Boolean) {
        wifiManager.setAutoReconnectEnabled(enabled)
    }

    fun reconnectBluetoothDevice(): Boolean = bluetoothManager.reconnectSavedDevice()

    // ========== Custom Commands ==========
    fun sendCustomCommand(command: com.metelci.ardunakon.model.CustomCommand) {
        val packet = ProtocolManager.formatCustomCommandData(
            command.commandId,
            command.payload
        )
        when (connectionMode) {
            ConnectionMode.WIFI -> wifiManager.sendData(packet)
            ConnectionMode.BLUETOOTH -> bluetoothManager.sendDataToAll(packet)
        }
        bluetoothManager.log("Sent custom command: ${command.name}", LogType.INFO)
    }

    fun saveCustomCommand(command: com.metelci.ardunakon.model.CustomCommand) {
        viewModelScope.launch {
            if (editingCommand != null) {
                customCommandRegistry.updateCommand(command)
                bluetoothManager.log("Updated command: ${command.name}", LogType.SUCCESS)
            } else {
                customCommandRegistry.registerCommand(command)
                bluetoothManager.log("Created command: ${command.name}", LogType.SUCCESS)
            }
            editingCommand = null
            showCustomCommandEditor = false
        }
    }

    fun deleteCustomCommand(id: String) {
        viewModelScope.launch {
            val command = customCommandRegistry.getCommand(id)
            customCommandRegistry.unregisterCommand(id)
            bluetoothManager.log("Deleted command: ${command?.name ?: id}", LogType.WARNING)
        }
    }

    fun getAvailableCommandIds(): List<Byte> = customCommandRegistry.getAvailableCommandIds()

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

                // Device Info Header
                appendLine("Device Info:")
                appendLine("  App Version: ${getAppVersion(context)}")
                appendLine("  Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("  Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                appendLine("  Connection Mode: ${connectionMode.name}")
                appendLine()
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
            showMessage("Export failed: ${e.localizedMessage}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        transmissionJob?.cancel()
    }

    @Suppress("DEPRECATION")
    private fun getAppVersion(context: Context): String = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            pInfo.versionCode.toLong()
        }
        "${pInfo.versionName} ($versionCode)"
    } catch (_: Exception) {
        "Unknown"
    }
}
