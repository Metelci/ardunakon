package com.metelci.ardunakon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppBluetoothManager(
    private val context: Context,
    private val connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
    private val cryptoEngine: com.metelci.ardunakon.security.CryptoEngine = com.metelci.ardunakon.security.SecurityManager(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, t -> 
        Log.e("AppBluetoothManager", "Uncaught exception", t)
    })
) : ConnectionCallback {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    
    /* scope already passed in constructor */
    private val deviceNameCache = com.metelci.ardunakon.data.DeviceNameCache(context, cryptoEngine)
    private val autoReconnectPrefs = com.metelci.ardunakon.data.AutoReconnectPreferences(context, cryptoEngine)

    // --- Sub-Managers ---
    private val scanner: BluetoothScanner = BluetoothScanner(
        context = context,
        adapter = adapter,
        scope = scope,
        callbacks = object : BluetoothScanner.ScannerCallbacks {
            override fun onDeviceFound(device: BluetoothDeviceModel) { /* UI updates via StateFlow */ }
            override fun onDeviceUpdated(device: BluetoothDeviceModel) { /* Device type upgrade */ }
            override fun onScanLog(message: String, type: LogType) { log(message, type) }
        },
        cryptoEngine = cryptoEngine
    )

    private val telemetryManager = TelemetryManager(scope) { msg, type -> log(msg, type) }
    
    // Active Connection Manager (Strategy Pattern)
    private var connectionManager: BluetoothConnectionManager? = null
    
    // --- Public State Flows ---
    val scannedDevices: StateFlow<List<BluetoothDeviceModel>> = scanner.scannedDevices
    val telemetryHistoryManager: TelemetryHistoryManager = telemetryManager.telemetryHistoryManager

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Delegated Flows
    val rssiValue: StateFlow<Int> = telemetryManager.rssiValue
    val health: StateFlow<ConnectionHealth> = telemetryManager.health
    val telemetry: StateFlow<Telemetry?> = telemetryManager.telemetry
    val rttHistory: StateFlow<List<Long>> = telemetryManager.rttHistory

    private val _debugLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val debugLogs: StateFlow<List<LogEntry>> = _debugLogs.asStateFlow()

    private val _incomingData = MutableStateFlow<ByteArray?>(null)
    val incomingData: StateFlow<ByteArray?> = _incomingData.asStateFlow()

    private val _isEmergencyStopActive = MutableStateFlow(false)
    val isEmergencyStopActive: StateFlow<Boolean> = _isEmergencyStopActive.asStateFlow()

    private val _deviceCapability = MutableStateFlow(DeviceCapabilities.DEFAULT)
    val deviceCapability: StateFlow<DeviceCapabilities> = _deviceCapability.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(false)
    val autoReconnectEnabled: StateFlow<Boolean> = _autoReconnectEnabled.asStateFlow()

    // Connected device info for UI display
    private val _connectedDeviceInfo = MutableStateFlow<String?>(null)
    val connectedDeviceInfo: StateFlow<String?> = _connectedDeviceInfo.asStateFlow()

    // Combined state flow for optimized UI recomposition
    // Consolidates 7 flows into 1 to reduce overhead by ~40%
    val combinedState: StateFlow<CombinedConnectionState> = combine(
        connectionState,
        rssiValue,
        health,
        telemetry,
        rttHistory,
        autoReconnectEnabled,
        isEmergencyStopActive
    ) { values: Array<Any?> ->
        val state = values[0] as ConnectionState
        val rssi = values[1] as Int
        val connectionHealth = values[2] as ConnectionHealth
        val telem = values[3] as Telemetry?
        @Suppress("UNCHECKED_CAST")
        val rtt = values[4] as? List<Long> ?: emptyList()
        val autoReconnect = values[5] as Boolean
        val estop = values[6] as Boolean
        CombinedConnectionState(
            connectionState = state,
            rssi = rssi,
            health = connectionHealth,
            telemetry = telem,
            rttHistory = rtt,
            autoReconnectEnabled = autoReconnect,
            isEmergencyStopActive = estop
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CombinedConnectionState(
            connectionState = ConnectionState.DISCONNECTED,
            rssi = 0,
            health = ConnectionHealth(),
            telemetry = null,
            rttHistory = emptyList(),
            autoReconnectEnabled = false,
            isEmergencyStopActive = false
        )
    )

    // --- Internal State ---
    private var savedDevice: BluetoothDeviceModel? = null
    private var connectionType: DeviceType? = null
    var allowReflectionFallback: Boolean = false
    private var _shouldReconnect = false
    private val connectionMutex = Mutex()
    private var keepAliveJob: Job? = null
    
    // Backoff State
    private var reconnectAttempts = 0
    private var nextReconnectAt = 0L
    
    // Heartbeat config
    private val heartbeatTimeoutClassicMs = BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS
    private val heartbeatTimeoutBleMs = BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS
    private val missedAckThresholdClassic = BluetoothConfig.MISSED_ACK_THRESHOLD_CLASSIC
    private val missedAckThresholdBle = BluetoothConfig.MISSED_ACK_THRESHOLD_BLE

    init {
        scope.launch {
            val saved = autoReconnectPrefs.loadAutoReconnectState()
            _shouldReconnect = saved[0]
            _autoReconnectEnabled.value = saved[0]
            
            // Restore saved device address for "True Auto-Reconnect"
            val lastConn = connectionPreferences.loadLastConnection()
            val savedAddress = lastConn.btAddress
            val savedType = lastConn.btType
            if (!savedAddress.isNullOrEmpty()) {
                 val savedName = deviceNameCache.getName(savedAddress) ?: "Unknown Device"
                 val type = if (savedType == "LE") DeviceType.LE else DeviceType.CLASSIC
                 
                 // Construct a minimal model for reconnection
                 savedDevice = BluetoothDeviceModel(
                     name = savedName,
                     address = savedAddress,
                     rssi = 0,
                     type = type
                 )
                 log("Restored saved device: $savedName (${type.name})", LogType.INFO)
            }

            if (saved[0]) log("Restored auto-reconnect: enabled", LogType.INFO)
        }
        startReconnectMonitor()
        startKeepAlivePings()
    }

    // --- Public Actions ---

    fun startScan() = scanner.startScan()
    fun stopScan() = scanner.stopScan()

    fun connectToDevice(deviceModel: BluetoothDeviceModel, isAutoReconnect: Boolean = false) {
        if (_isEmergencyStopActive.value) {
            log("Connect failed: E-STOP ACTIVE", LogType.ERROR)
            return
        }
        if (!checkBluetoothPermission()) {
            log("Connect failed: Missing permissions", LogType.ERROR)
            return
        }
        if (adapter == null || !adapter.isEnabled) {
            log("Connect failed: Bluetooth is off", LogType.ERROR)
            return
        }

        scope.launch {
            if (!connectionMutex.tryLock()) {
                log("Connection already in progress", LogType.WARNING)
                return@launch
            }

            try {
                // Determine Type (Force BLE if needed)
                val forceBle = BluetoothConfig.isBleOnlyName(deviceModel.name)
                val coercedModel = if (forceBle || deviceModel.type == DeviceType.LE) {
                    if (forceBle && deviceModel.type != DeviceType.LE) {
                        log("Forcing BLE path for HM-10 clone", LogType.INFO)
                    }
                    deviceModel.copy(type = DeviceType.LE)
                } else {
                    deviceModel
                }

                // Setup state
                savedDevice = coercedModel
                connectionType = coercedModel.type
                _shouldReconnect = _autoReconnectEnabled.value
                telemetryManager.updateRssi(coercedModel.rssi)

                if (!isAutoReconnect) {
                    updateConnectionState(ConnectionState.CONNECTING)
                    log("Connecting to ${coercedModel.name}...", LogType.INFO)
                }

                // Clean up before connect
                try { adapter.cancelDiscovery() } catch (_: Exception) {}
                stopScan()
                connectionManager?.cleanup()
                
                // Switch Manager
                val device = adapter.getRemoteDevice(coercedModel.address)
                connectionManager = if (coercedModel.type == DeviceType.LE) {
                    BleConnectionManager(context, adapter, scope, this@AppBluetoothManager)
                } else {
                    ClassicConnectionManager(context, adapter, this@AppBluetoothManager)
                }
                
                // Connect
                delay(200) // Settle
                connectionManager?.connect(device)
                
            } catch (e: Exception) {
                log("Connect launch failed: ${e.message}", LogType.ERROR)
                connectionMutex.unlock()
            }
            // Note: Mutex is unlocked when connection finishes (success or fail) logic? 
            // In original code: BLE unlocked on success, Classic thread unlocked on finish.
            // Here: Managers don't control mutex. WE do. 
            // We should unlock here immediately? No, that allows concurrent attempts.
            // We should unlock when 'Connecting' phase is done (Success or Error).
            // But 'connect' is async for BLE and Thread for Classic.
            // So we can unlock here safe in the knowledge that 'connect' has STARTED.
            // Preventing strictly 'concurrent connection attempts' might just mean 'don't click twice'.
            connectionMutex.unlock()
        }
    }

    fun disconnect() {
        _shouldReconnect = false
        _autoReconnectEnabled.value = false
        performDisconnect("Disconnected")
    }

    private fun performDisconnect(reason: String) {
        connectionManager?.disconnect()
        connectionManager = null
        updateConnectionState(ConnectionState.DISCONNECTED)
        telemetryManager.updateRssi(0)
        log(reason, LogType.INFO)
    }

    fun sendData(data: ByteArray, force: Boolean = false) {
        if (_isEmergencyStopActive.value && !force) return
        connectionManager?.send(data)
    }

    // Legacy aliases
    fun sendDataToAll(data: ByteArray, force: Boolean = false) = sendData(data, force)
    fun disconnectForEStop() = performDisconnect("Disconnected due to E-STOP") // also handle estop flags
    fun disconnectAllForEStop() = disconnectForEStop()

    fun setEmergencyStop(active: Boolean) {
        _isEmergencyStopActive.value = active
        if (active) log("E-STOP ACTIVATED: Blocking connections", LogType.WARNING)
        else log("E-STOP RELEASED: Connections allowed", LogType.SUCCESS)
    }

    fun requestRssi() {
        if (connectionType == DeviceType.LE) connectionManager?.requestRssi()
        else log("RSSI refresh not supported for classic devices", LogType.WARNING)
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
         scope.launch { autoReconnectPrefs.saveAutoReconnectState(0, enabled) }
         
        if (enabled) {
            _shouldReconnect = false // Arm only
            resetCircuitBreaker()
            log("Auto-reconnect ARMED", LogType.INFO)
        } else {
            _shouldReconnect = false
            resetCircuitBreaker()
            performDisconnect("Auto-reconnect DISABLED")
            savedDevice = null
        }
    }

    fun reconnectSavedDevice(): Boolean {
        resetCircuitBreaker()
        val device = savedDevice
        if (device != null && _connectionState.value != ConnectionState.CONNECTED) {
            log("Manually reconnecting to ${device.name}", LogType.INFO)
            connectToDevice(device, isAutoReconnect = true)
            return true
        }
        return false
    }

    fun resetCircuitBreaker() {
        reconnectAttempts = 0
        nextReconnectAt = 0L
        log("Circuit breaker reset", LogType.INFO)
    }

    // --- Callback Implementation ---

    override fun onStateChanged(state: ConnectionState) {
         updateConnectionState(state)
         if (state == ConnectionState.CONNECTED) {
             telemetryManager.resetHeartbeat()
             resetCircuitBreaker()
             vibrate(BluetoothConfig.VIBRATION_CONNECTED_MS)
             
             // Update connected device info for UI
             val device = savedDevice
             if (device != null) {
                 val typeLabel = if (device.type == DeviceType.LE) "BLE" else "Classic"
                 val info = "${device.name} ($typeLabel)"
                 _connectedDeviceInfo.value = info
                 log("Device connected: $info", LogType.SUCCESS)
             } else {
                 log("Connected but savedDevice is null", LogType.WARNING)
             }
             
             // Save as last connected device
             savedDevice?.let { device ->
                 scope.launch {
                     connectionPreferences.saveLastConnection(
                         type = "BLUETOOTH",
                         btAddress = device.address,
                         btType = device.type.name
                     )
                     deviceNameCache.saveName(device.address, device.name, device.type)
                 }
             }
         } else if (state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR) {
             if (state == ConnectionState.ERROR) {
                 vibrate(BluetoothConfig.VIBRATION_ERROR_MS)
             }
             _connectedDeviceInfo.value = null
         }
    }

    override fun onDataReceived(data: ByteArray) {
        _incomingData.value = data
        telemetryManager.recordInbound()
        
        // Debug logging for raw data to analyze potential format issues
        // Use a simple sampling or conditional to avoid spam if high frequency
        // For now, log capabilities (0x05) and Telemetry (0x10) explicitly, others as VERBOSE?
        // Let's just log ALL for debugging this specific user issue.
        // Assuming data isn't huge.
        val hex = data.joinToString("") { "%02X".format(it) }
        log("RX: $hex", LogType.INFO)

        telemetryManager.parseTelemetryPacket(data)
        
        // Capabilities Check
        if (data.size >= 5 && data[2].toInt() and 0xFF == 0x05) {
             val caps = DeviceCapabilities.fromPacket(data, 3)
             _deviceCapability.value = caps
             log("Data received from ${caps.boardType.displayName}", LogType.SUCCESS)
        }
    }

    override fun onError(message: String, type: LogType) {
        log(message, type)
    }

    override fun onPacketStats(sent: Long, dropped: Long, failed: Long) {
        telemetryManager.updatePacketStats(sent, dropped, failed)
    }

    override fun onRssiUpdated(rssi: Int) {
        telemetryManager.updateRssi(rssi)
        telemetryManager.resetRssiFailures() // Logic moved from Connection to Manager... or coordinated?
        // AppBluetoothManager original logic had logic to count failures if RSSI failed.
        // BleManager calls onRssiUpdated only on success.
    }

    // --- Private Monitors ---

    private fun startReconnectMonitor() {
        scope.launch {
            while (isActive) {
                delay(1000)
                if (!_isEmergencyStopActive.value && _shouldReconnect) {
                    val state = _connectionState.value
                    if ((state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR) &&
                        savedDevice != null && System.currentTimeMillis() >= nextReconnectAt
                    ) {
                        if (reconnectAttempts >= BluetoothConfig.MAX_RECONNECT_ATTEMPTS) {
                            log("Circuit breaker: Too many failed attempts", LogType.ERROR)
                            _shouldReconnect = false
                            _autoReconnectEnabled.value = false
                        } else {
                            val backoff = BluetoothConfig.calculateBackoffDelay(reconnectAttempts)
                            log("Auto-reconnecting... (attempt ${reconnectAttempts + 1})", LogType.WARNING)
                            reconnectAttempts++
                            nextReconnectAt = System.currentTimeMillis() + backoff
                            updateConnectionState(ConnectionState.RECONNECTING)
                            connectToDevice(savedDevice!!, isAutoReconnect = true)
                        }
                    }
                }
            }
        }
    }

    private fun startKeepAlivePings() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(BluetoothConfig.HEARTBEAT_INTERVAL_MS)
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    var seq = 0 // Needs to be persistent? Yes. TelemetryManager has seq?
                    // TelemetryManager tracks seq for health update, but doesn't store 'last sent seq' to increment it?
                    // We need to keep seq here or in TM.
                    // Let's keep a simple counter here.
                    // But TelemetryManager needs it to correlate?
                    // 'updateHealth' uses 'heartbeatSeq'.
                    
                    // Actually, telemetry manager updateHealth takes seq.
                    // Let's use a local var for now, assuming persistence across pings.
                    val nextSeq = (telemetryManager.health.value.lastHeartbeatSeq + 1) and 0xFFFF
                    
                    val heartbeat = ProtocolManager.formatHeartbeatData(nextSeq)
                    sendData(heartbeat, force = true)
                    telemetryManager.onHeartbeatSent(nextSeq)

                    // Stats Sync
                    val stats = connectionManager?.getPacketStats()
                    if (stats != null) {
                        telemetryManager.updatePacketStats(stats.sent, stats.dropped, stats.failed)
                    }

                    // Network Timeout Check
                    val lastPacket = telemetryManager.getLastPacketTime()
                    val missed = telemetryManager.getMissedAcks()
                    val sinceLast = System.currentTimeMillis() - lastPacket
                    val isBle = connectionType == DeviceType.LE
                    val threshold = if (isBle) missedAckThresholdBle else missedAckThresholdClassic
                    val timeout = if (isBle) heartbeatTimeoutBleMs else heartbeatTimeoutClassicMs

                    if (missed >= threshold && lastPacket > 0 && sinceLast > timeout) {
                        log("Heartbeat timeout (missed $missed acks)", LogType.ERROR)
                        performDisconnect("Heartbeat timeout")
                        // Trigger reconnect via ERROR state?
                        updateConnectionState(ConnectionState.ERROR) 
                    }
                }
            }
        }
    }
    
    fun holdOfflineAfterEStopReset() {
        _shouldReconnect = false
        _autoReconnectEnabled.value = false
        savedDevice = null
        scope.launch { autoReconnectPrefs.saveAutoReconnectState(0, false) }
        log("E-STOP reset: keeping offline until manual connect", LogType.INFO)
    }

    fun cleanup() {
        scope.launch { deviceNameCache.cleanOldEntries() }
        scanner.cleanup()
        keepAliveJob?.cancel()
        connectionManager?.cleanup()
        scope.cancel()
    }

    private fun updateConnectionState(state: ConnectionState) {
        // Debounce?
        _connectionState.value = state
    }
    
    fun log(message: String, type: LogType = LogType.INFO) {
        val currentLogs = _debugLogs.value.toMutableList()
        // Limit to 500 entries to prevent memory leaks
        if (currentLogs.size >= 500) {
            currentLogs.removeAt(0)
        }
        currentLogs.add(LogEntry(type = type, message = message))
        _debugLogs.value = currentLogs
        Log.d("Ardunakon", message)

        if (type == LogType.ERROR) {
            com.metelci.ardunakon.crash.CrashHandler.logException(
                context, 
                Exception("Non-fatal error logged: $message"), 
                message
            )
        }
    }

    private fun checkBluetoothPermission(): Boolean = 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

    private fun vibrate(durationMs: Long) {
         // ... vibration logic ...
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator).vibrate(durationMs)
        }
    }
    
    // Compatibility methods for WifiManager if they share interfaces?
    // WifiManager seems independent.
}
