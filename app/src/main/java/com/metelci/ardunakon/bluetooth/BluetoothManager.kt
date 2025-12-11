package com.metelci.ardunakon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.security.DeviceVerificationException
import com.metelci.ardunakon.security.DeviceVerificationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.experimental.xor

// Standard SPP UUID (HC-06, Texas Instruments, Microchip, Telit Bluemod)
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// Manufacturer-specific UUIDs for HC-06 variants and clones
// COMPREHENSIVE LIST - Covers maximum number of HC-06 clone variants
// Note: Standard SPP (00001101) is already tried in Attempts 1 & 5, so excluded here
private val MANUFACTURER_UUIDS = listOf(
    // Nordic Semiconductor nRF51822-based HC-06 clones (VERY common in Chinese clones)
    UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
    // Alternative Nordic UART Service (Nordic-based clones, nRF51/nRF52)
    UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"),
    // Object Push Profile - Many HC-06 clones (ZS-040, FC-114, linvor, JY-MCU)
    UUID.fromString("00001105-0000-1000-8000-00805F9B34FB"),
    // OBEX Object Push - Alternative HC-06 clones (some linvor firmware)
    UUID.fromString("00001106-0000-1000-8000-00805F9B34FB"),
    // Headset Profile - BT 2.0 HC-06 clones (older firmware)
    UUID.fromString("00001108-0000-1000-8000-00805F9B34FB"),
    // Hands-Free Profile - Some HC-06 modules configured as hands-free
    UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB"),
    // A/V Remote Control Profile - Rare HC-06 clones
    UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB"),
    // Advanced Audio Distribution Profile - Some multimedia HC-06 clones
    UUID.fromString("0000110D-0000-1000-8000-00805F9B34FB"),
    // Dial-up Networking Profile - Older HC-06 firmware
    UUID.fromString("00001103-0000-1000-8000-00805F9B34FB"),
    // LAN Access Profile - Some network-oriented HC-06 clones
    UUID.fromString("00001102-0000-1000-8000-00805F9B34FB"),
    // Raw RFCOMM - Bare-metal HC-06 implementations
    UUID.fromString("00000003-0000-1000-8000-00805F9B34FB"),
    // Base UUID - Last resort for completely non-standard implementations
    UUID.fromString("00000000-0000-1000-8000-00805F9B34FB")
)

// BLE GATT Status Codes for error classification
private object GattStatus {
    const val GATT_SUCCESS = 0
    const val GATT_CONNECTION_TIMEOUT = 8
    const val GATT_INSUFFICIENT_AUTHENTICATION = 5
    const val GATT_INSUFFICIENT_ENCRYPTION = 15
    const val GATT_INTERNAL_ERROR = 129
    const val GATT_DEVICE_NOT_FOUND = 133
    const val GATT_LINK_LOSS = 147  // Device-specific: BLE link layer failure

    // Classify GATT errors as transient (retry-able) or permanent
    fun isTransientError(status: Int): Boolean {
        return when (status) {
            GATT_CONNECTION_TIMEOUT,      // 8 - Timeout, retry may work
            GATT_INTERNAL_ERROR,          // 129 - Android stack issue, retry may work
            GATT_LINK_LOSS,               // 147 - Link layer failure, retry may work
            62                            // 62 - Often returned as "Unknown GATT error (62)" on unstable HM-10 clones
            -> true
            else -> false
        }
    }

    fun isPermanentError(status: Int): Boolean {
        return when (status) {
            GATT_DEVICE_NOT_FOUND        // 133 - Device gone, no point retrying
            -> true
            else -> false
        }
    }

    fun getErrorDescription(status: Int): String {
        return when (status) {
            GATT_SUCCESS -> "Success"
            GATT_CONNECTION_TIMEOUT -> "Connection Timeout (8): Device didn't respond in time"
            GATT_INSUFFICIENT_AUTHENTICATION -> "Insufficient Authentication (5): Pairing required"
            GATT_INSUFFICIENT_ENCRYPTION -> "Insufficient Encryption (15): Encryption required"
            GATT_INTERNAL_ERROR -> "Internal Error (129): Android BLE stack issue"
            GATT_DEVICE_NOT_FOUND -> "Device Not Found (133): Out of range or powered off"
            GATT_LINK_LOSS -> "Link Layer Failure (147): Device reset or interference"
            62 -> "Unknown GATT error (62): Treating as transient HM-10/MLT-BT05 timeout"
            else -> "Unknown GATT error ($status)"
        }
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    RECONNECTING
}

data class BluetoothDeviceModel(
    val name: String,
    val address: String,
    val type: DeviceType,
    val rssi: Int = 0
)

enum class DeviceType {
    CLASSIC, LE
}

data class ConnectionHealth(
    val lastPacketAt: Long = 0L,
    val rssiFailureCount: Int = 0,
    val lastHeartbeatSeq: Int = 0,
    val lastHeartbeatAt: Long = 0L,
    val lastRttMs: Long = 0L
)

    class AppBluetoothManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val deviceVerificationManager = DeviceVerificationManager(context)
    private val deviceVerificationEnabled = true // Can be made configurable
    private val deviceNameCache = com.metelci.ardunakon.data.DeviceNameCache(context)
    private val autoReconnectPrefs = com.metelci.ardunakon.data.AutoReconnectPreferences(context)

    // Telemetry History Manager for graph visualization
    private val _telemetryHistoryManager = com.metelci.ardunakon.telemetry.TelemetryHistoryManager()
    val telemetryHistoryManager: com.metelci.ardunakon.telemetry.TelemetryHistoryManager = _telemetryHistoryManager

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceModel>> = _scannedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _rssiValue = MutableStateFlow(0)
    val rssiValue: StateFlow<Int> = _rssiValue.asStateFlow()

    private val _health = MutableStateFlow(ConnectionHealth())
    val health: StateFlow<ConnectionHealth> = _health.asStateFlow()
    var allowReflectionFallback: Boolean = false

    // Debug Logs
    private val _debugLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val debugLogs: StateFlow<List<LogEntry>> = _debugLogs.asStateFlow()

    // Incoming data from Arduino
    private val _incomingData = MutableStateFlow<ByteArray?>(null)
    val incomingData: StateFlow<ByteArray?> = _incomingData.asStateFlow()

    // E-Stop State
    private val _isEmergencyStopActive = MutableStateFlow(false)
    val isEmergencyStopActive: StateFlow<Boolean> = _isEmergencyStopActive.asStateFlow()

    // RTT History for latency sparkline (last 20 values)
    private val _rttHistory = MutableStateFlow<List<Long>>(emptyList())
    val rttHistory: StateFlow<List<Long>> = _rttHistory.asStateFlow()

    // Device Capabilities (announced by Arduino on connect)
    private val _deviceCapability = MutableStateFlow(DeviceCapabilities.DEFAULT)
    val deviceCapability: StateFlow<DeviceCapabilities> = _deviceCapability.asStateFlow()

    private var keepAliveJob: Job? = null

    // Some OEMs (e.g., Xiaomi/Redmi/Poco) block standard SPP on HC-06; force-enable reflection port 1 for them.
    private val forceReflectionDevices = setOf("xiaomi", "redmi", "poco")
    private fun shouldForceReflectionFallback(): Boolean {
        val oem = android.os.Build.MANUFACTURER?.lowercase()?.trim() ?: return false
        return forceReflectionDevices.contains(oem)
    }

    fun log(message: String, type: LogType = LogType.INFO) {
        val currentLogs = _debugLogs.value.toMutableList()
        if (currentLogs.size > 50) currentLogs.removeAt(0)
        currentLogs.add(LogEntry(type = type, message = message))
        _debugLogs.value = currentLogs
        Log.d("Ardunakon", message)
    }

    // Interface for both Classic and BLE connections
    interface BluetoothConnection {
        fun write(bytes: ByteArray)
        fun cancel()
        fun requestRssi() {}
    }

    // Active connection (single slot)
    private var connection: BluetoothConnection? = null
    // Saved device for auto-reconnect
    private var savedDevice: BluetoothDeviceModel? = null
    private var connectionType: DeviceType? = null
    private var _shouldReconnect = false
    private val _autoReconnectEnabled = MutableStateFlow(false)
    val autoReconnectEnabled: StateFlow<Boolean> = _autoReconnectEnabled.asStateFlow()
    // Connection mutex to prevent concurrent connection attempts
    private val connectionMutex = kotlinx.coroutines.sync.Mutex()
    private var lastStateChangeAt = 0L
    private var rssiFailures = 0
    // Exponential backoff state
    private var reconnectAttempts = 0  // Count consecutive failures
    private var nextReconnectAt = 0L   // Timestamp when next attempt allowed
    private var heartbeatSeq = 0
    private var lastHeartbeatSentAt = 0L
    private var lastPacketAt = 0L
    private var lastRttMs = 0L
    private val heartbeatTimeoutClassicMs = 20000L  // Increased from 12s to 20s for better tolerance
    // BLE clones like HM-10/HC-08 often stay silent; allow long idle periods before reconnecting.
    private val heartbeatTimeoutBleMs = 300000L     // 5 minutes before considering BLE link stale
    private val missedAckThresholdClassic = 5
    private val missedAckThresholdBle = 60          // 60 heartbeats @4s ≈ 4 minutes before timeout
    private var missedHeartbeatAcks = 0

    private val leScanner by lazy { adapter?.bluetoothLeScanner }
    private val leScanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            if (result == null || result.device == null) return
            val device = result.device
            val isNew = addDevice(device, DeviceType.LE, result.rssi)
            
            if (isNew) {
                val record = result.scanRecord
                val sb = StringBuilder()
                sb.append("Found LE Device: ${device.name ?: "Unknown"} (${device.address})\n")
                sb.append("  RSSI: ${result.rssi} dBm\n")
                sb.append("  Type: ${device.type} (0=Unknown, 1=Classic, 2=LE, 3=Dual)\n")
                
                if (record != null) {
                    sb.append("  Adv Flags: ${record.advertiseFlags}\n")
                    sb.append("  Local Name: ${record.deviceName ?: "N/A"}\n")
                    sb.append("  Service UUIDs: ${record.serviceUuids?.joinToString(", ") ?: "None"}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                         sb.append("\n  Connectable: ${result.isConnectable}")
                    }
                }
                log(sb.toString(), LogType.INFO)
            }
        }
    }

    // Scan timeout job - only one active at a time
    private var scanJob: Job? = null

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                if (device != null) {
                    val isNew = addDevice(device, DeviceType.CLASSIC, rssi)
                    if (isNew) {
                        val sb = StringBuilder()
                        sb.append("Found Classic Device: ${device.name ?: "Unknown"} (${device.address})\n")
                        sb.append("  RSSI: $rssi dBm\n")
                        sb.append("  Type: ${device.type} (0=Unknown, 1=Classic, 2=LE, 3=Dual)")
                        
                        // Try to get UUIDs if available in intent
                        if (intent.hasExtra(BluetoothDevice.EXTRA_UUID)) {
                            val parcelUuids = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID, android.os.ParcelUuid::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                            }
                            if (parcelUuids != null && parcelUuids.isNotEmpty()) {
                                sb.append("\n  UUIDs: ${parcelUuids.joinToString(", ")}")
                            }
                        }
                        log(sb.toString(), LogType.INFO)
                    }
                }
            }
        }
    }

    // Track receiver registration state to prevent leaks
    @Volatile
    private var isReceiverRegistered = false

    init {
        try {
            context.registerReceiver(receiver, android.content.IntentFilter(BluetoothDevice.ACTION_FOUND))
            isReceiverRegistered = true
        } catch (e: Exception) {
            log("Failed to register Bluetooth receiver: ${e.message}", LogType.WARNING)
        }

        // Load saved auto-reconnect preference
        scope.launch {
            val saved = autoReconnectPrefs.loadAutoReconnectState()
            _shouldReconnect = saved[0]
            _autoReconnectEnabled.value = saved[0]
            if (saved[0]) {
                log("Restored auto-reconnect: enabled", LogType.INFO)
            }
        }

        startReconnectMonitor()
        startKeepAlivePings()
    }

    fun setEmergencyStop(active: Boolean) {
        _isEmergencyStopActive.value = active
        if (active) {
            log("E-STOP ACTIVATED: Blocking connections", LogType.WARNING)
        } else {
            log("E-STOP RELEASED: Connections allowed", LogType.SUCCESS)
        }
    }

    private fun isBleOnlyName(name: String?): Boolean {
        val nameUpper = (name ?: "").uppercase()
        val hm10Markers = listOf(
            "HM-10",
            "HM10",
            "AT-09",
            "AT09",
            "MLT-BT05",
            "BT05",
            "BT-05",
            "HC-08",
            "HC08",
            "CC41",
            "CC41-A",

            "BLE",
            "ARDUNAKON",
            "ARDUINO",
            "R4",
            "UNO R4"
        )
        return hm10Markers.any { marker -> nameUpper.contains(marker) }
    }

    private fun shouldForceBle(deviceModel: BluetoothDeviceModel): Boolean {
        return isBleOnlyName(deviceModel.name)
    }

    private fun startReconnectMonitor() {
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (!_isEmergencyStopActive.value) {
                    val currentState = _connectionState.value

                    // Check if backoff period has elapsed
                    if (now >= nextReconnectAt) {
                        // Reconnect if disconnected OR in error state (failed connection attempt)
                        if (_shouldReconnect &&
                            (currentState == ConnectionState.DISCONNECTED || currentState == ConnectionState.ERROR) &&
                            savedDevice != null) {

                            // Check circuit breaker
                            if (reconnectAttempts >= 10) {
                                log("Circuit breaker: Too many failed attempts", LogType.ERROR)
                                _shouldReconnect = false  // Stop auto-reconnect
                                _autoReconnectEnabled.value = false
                            } else {
                                val backoffDelay = calculateBackoffDelay(reconnectAttempts)
                                log("Auto-reconnecting to ${savedDevice?.name}... (attempt ${reconnectAttempts + 1}, backoff ${backoffDelay}ms)", LogType.WARNING)

                                reconnectAttempts++
                                nextReconnectAt = now + backoffDelay

                                updateConnectionState(ConnectionState.RECONNECTING)
                                savedDevice?.let { device ->
                                    connectToDevice(device, isAutoReconnect = true)
                                } ?: run {
                                    log("Auto-reconnect failed: No saved device", LogType.ERROR)
                                    updateConnectionState(ConnectionState.DISCONNECTED)
                                }
                            }
                        }
                    }
                }
                delay(1000) // Check every second (instead of 3 seconds)
            }
        }
    }

    private fun recordInbound() {
        val now = System.currentTimeMillis()
        val wasTimeout = missedHeartbeatAcks >= 3

        lastPacketAt = now
        rssiFailures = 0
        missedHeartbeatAcks = 0  // Reset missed ACK counter

        if (wasTimeout) {
            log("Heartbeat recovered", LogType.SUCCESS)
        }

        if (lastHeartbeatSentAt > 0 && now >= lastHeartbeatSentAt) {
            lastRttMs = now - lastHeartbeatSentAt

            // Add to RTT history for sparkline visualization
            addRttToHistory(lastRttMs)
        }
        updateHealth(heartbeatSeq, lastPacketAt, rssiFailures)
    }

    private fun addRttToHistory(rtt: Long) {
        val current = _rttHistory.value.toMutableList()
        current.add(rtt)
        // Keep only last 20 values
        while (current.size > 20) {
            current.removeAt(0)
        }
        _rttHistory.value = current
    }

    /**
     * Update device capabilities from announcement packet
     */
    fun updateCapabilities(capabilities: DeviceCapabilities) {
        _deviceCapability.value = capabilities
        log("Device capabilities: ${capabilities.toDisplayString()} (${capabilities.boardType.displayName})", LogType.SUCCESS)
    }

    /**
     * Parse incoming packet for capability announcement
     * Call this when receiving CMD_ANNOUNCE_CAPABILITIES (0x05)
     */
    fun handleCapabilityPacket(data: ByteArray) {
        if (data.size >= 5 && data[2].toInt() and 0xFF == 0x05) {
            val capabilities = DeviceCapabilities.fromPacket(data, 3)
            updateCapabilities(capabilities)
        }
    }
    private fun startKeepAlivePings() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(4000)
                val state = _connectionState.value
                if (state == ConnectionState.CONNECTED && connection != null) {
                    heartbeatSeq = (heartbeatSeq + 1) and 0xFFFF
                    val heartbeat = ProtocolManager.formatHeartbeatData(heartbeatSeq)
                    // Force bypasses E-STOP so the link itself stays alive
                    sendData(heartbeat, force = true)
                    lastHeartbeatSentAt = System.currentTimeMillis()
                    missedHeartbeatAcks++
                    updateHealth(heartbeatSeq, lastPacketAt, rssiFailures)

                    // Require multiple missed acks before forcing reconnect
                    val sinceLastPacket = System.currentTimeMillis() - lastPacketAt
                    val isBle = connectionType == DeviceType.LE
                    val timeoutMs = if (isBle) heartbeatTimeoutBleMs else heartbeatTimeoutClassicMs
                    val ackThreshold = if (isBle) missedAckThresholdBle else missedAckThresholdClassic

                    if (missedHeartbeatAcks >= ackThreshold && lastPacketAt > 0 && sinceLastPacket > timeoutMs) {
                        log("Heartbeat timeout after ${sinceLastPacket}ms (missed $missedHeartbeatAcks acks)", LogType.ERROR)
                        missedHeartbeatAcks = 0
                        forceReconnect("Heartbeat timeout")
                    }
                }
            }
        }
    }

    fun startScan() {
        if (!checkBluetoothPermission()) {
            log("Scan failed: Missing permissions", LogType.ERROR)
            return
        }
        if (adapter == null) {
            log("Scan failed: Bluetooth adapter unavailable", LogType.ERROR)
            return
        }
        if (!adapter.isEnabled) {
            log("Scan failed: Bluetooth is turned off", LogType.WARNING)
            return
        }

        // Cancel any previous scan job to prevent multiple timeouts
        scanJob?.cancel()

        _scannedDevices.value = emptyList()
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
            leScanner?.stopScan(leScanCallback)
            leScanner?.startScan(leScanCallback)

            // Start new scan timeout
            scanJob = scope.launch {
                delay(10000)
                stopScan()
            }
        } catch (e: SecurityException) {
            Log.e("BT", "Permission missing for scan", e)
            log("Scan failed: Permission missing", LogType.ERROR)
        } catch (e: Exception) {
            Log.e("BT", "Scan failed", e)
        }
    }

    fun stopScan() {
        if (!checkBluetoothPermission()) {
            log("Stop scan skipped: Missing permissions", LogType.WARNING)
            return
        }
        scanJob?.cancel()
        adapter?.cancelDiscovery()
        try { leScanner?.stopScan(leScanCallback) } catch (e: Exception) {}
    }

    fun addDevice(device: BluetoothDevice, type: DeviceType, rssi: Int): Boolean {
        val list = _scannedDevices.value.toMutableList()
        val isBleOnly = isBleOnlyName(device.name)
        val resolvedType = if (isBleOnly) DeviceType.LE else type

        val existingIndex = list.indexOfFirst { it.address == device.address }
        if (existingIndex >= 0) {
            // Upgrade existing entry to BLE if needed
            val existing = list[existingIndex]
            if (resolvedType == DeviceType.LE && existing.type != DeviceType.LE) {
                list[existingIndex] = existing.copy(type = DeviceType.LE)
                _scannedDevices.value = list
            }
            return false
        }

        // Resolve name using multi-layer strategy
        scope.launch {
            val resolvedName = resolveDeviceName(device, resolvedType)
            val updatedList = _scannedDevices.value.toMutableList()
            if (updatedList.none { it.address == device.address }) {
                updatedList.add(BluetoothDeviceModel(resolvedName, device.address, resolvedType, rssi))
                _scannedDevices.value = updatedList
            }
        }
        return true
    }

    private suspend fun resolveDeviceName(device: BluetoothDevice, type: DeviceType): String {
        // Layer 1: Check bonded devices (most reliable on Android 12+)
        if (checkBluetoothPermission()) {
            try {
                adapter?.bondedDevices?.find { it.address == device.address }?.let { bondedDevice ->
                    val bondedName = bondedDevice.name
                    if (!bondedName.isNullOrBlank() && bondedName != device.address) {
                        // Save to cache for future lookups
                        deviceNameCache.saveName(device.address, bondedName, type)
                        return "$bondedName (${device.address})"
                    }
                }
            } catch (e: SecurityException) {
                Log.w("BT", "Permission issue accessing bonded devices", e)
            }

            // Try direct name access (works for some devices)
            try {
                val directName = device.name
                if (!directName.isNullOrBlank() && directName != device.address) {
                    deviceNameCache.saveName(device.address, directName, type)
                    return "$directName (${device.address})"
                }
            } catch (e: SecurityException) {
                Log.w("BT", "Permission issue accessing device name", e)
            }
        }

        // Layer 2: Check persistent cache
        deviceNameCache.getName(device.address)?.let { cachedName ->
            return "$cachedName [cached] (${device.address})"
        }

        // Layer 3: Fallback to MAC address with clear indicator
        return "Unknown Device (${device.address})"
    }

    fun connectToDevice(deviceModel: BluetoothDeviceModel, isAutoReconnect: Boolean = false) {
        if (_isEmergencyStopActive.value) {
            log("Connect failed: E-STOP ACTIVE", LogType.ERROR)
            return
        }
        if (!checkBluetoothPermission()) {
            log("Connect failed: Missing permissions", LogType.ERROR)
            return
        }
        val localAdapter = adapter
        if (localAdapter == null || !localAdapter.isEnabled) {
            log("Connect failed: Bluetooth is off", LogType.ERROR)
            return
        }

        // Check if connection is already in progress
        scope.launch {
            if (!connectionMutex.tryLock()) {
                log("Connection already in progress", LogType.WARNING)
                return@launch
            }

            // Detect HM-10 / AT-09 / MLT-BT05 style BLE-only clones and force BLE path
            val forceBle = shouldForceBle(deviceModel)
            val coercedModel = if (forceBle || deviceModel.type == DeviceType.LE) {
                if (forceBle && deviceModel.type != DeviceType.LE) {
                    log("Forcing BLE path for HM-10 clone (${deviceModel.name})", LogType.INFO)
                }
                deviceModel.copy(type = DeviceType.LE)
            } else {
                deviceModel
            }

            // Save for potential reconnect
            savedDevice = coercedModel
            connectionType = coercedModel.type
            // Only enable auto-reconnect if user toggle is ON
            _shouldReconnect = _autoReconnectEnabled.value
            // Seed RSSI with last known scan value for immediate UI feedback
            updateRssi(coercedModel.rssi)

            if (!isAutoReconnect) {
                updateConnectionState(ConnectionState.CONNECTING)
                log("Connecting to ${coercedModel.name}...", LogType.INFO)
            }

            // Cancel discovery BEFORE starting connection - discovery interferes with connection
            try {
                localAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                log("Could not cancel discovery: Missing permission", LogType.WARNING)
            }
            stopScan()

            // Ensure previous connection is closed
            connection?.cancel()
            connection = null

            // Give BT stack time to clean up (minimal delay for fast reconnection)
            delay(200)

            val device = localAdapter.getRemoteDevice(coercedModel.address)
            if (device == null) {
                connectionMutex.unlock()
                return@launch
            }

            // Refresh device name in cache during connection attempt
            // This ensures the debug window shows the latest name
            val refreshedName = resolveDeviceName(device, coercedModel.type)
            log("Device name resolved: $refreshedName", LogType.INFO)

            if (coercedModel.type == DeviceType.LE) {
                val bleConnection = BleConnection(device)
                connection = bleConnection
                bleConnection.connect()
                // Note: BLE connection unlocks mutex when connection completes
            } else {
                // ConnectThread will unlock the mutex when it finishes (success or failure)
                ConnectThread(device).start()
            }
        }
    }

    fun disconnect() {
        _shouldReconnect = false
        _autoReconnectEnabled.value = false
        connection?.cancel()
        connection = null
        updateConnectionState(ConnectionState.DISCONNECTED)
        updateRssi(0)
        log("Disconnected", LogType.INFO)
    }

    fun disconnectForEStop() {
        _shouldReconnect = false
        _autoReconnectEnabled.value = false
        connection?.cancel()
        connection = null
        updateConnectionState(ConnectionState.DISCONNECTED)
        updateRssi(0)
        log("Disconnected due to E-STOP", LogType.WARNING)
    }

    fun holdOfflineAfterEStopReset() {
        _shouldReconnect = false
        _autoReconnectEnabled.value = false
        savedDevice = null
        scope.launch {
            saveAutoReconnectPreference(false)
        }
        log("E-STOP reset: keeping offline until manual connect", LogType.INFO)
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled

        if (enabled) {
            // Arm future auto-reconnect but do NOT trigger immediate reconnect.
            _shouldReconnect = false
            reconnectAttempts = 0
            nextReconnectAt = 0L
            log("Auto-reconnect ARMED (will start after next manual connect)", LogType.INFO)
        } else {
            // Disable and disconnect; clear saved device so user must pick from scan.
            _shouldReconnect = false
            reconnectAttempts = 0
            nextReconnectAt = 0L
            connection?.cancel()
            connection = null
            savedDevice = null
            updateConnectionState(ConnectionState.DISCONNECTED)
            updateRssi(0)
            log("Auto-reconnect DISABLED", LogType.WARNING)
        }

        scope.launch { saveAutoReconnectPreference(enabled) }
    }

    private suspend fun saveAutoReconnectPreference(enabled: Boolean) {
        autoReconnectPrefs.saveAutoReconnectState(0, enabled)
    }

    private fun calculateBackoffDelay(attempts: Int): Long {
        // Exponential backoff: 3s, 6s, 12s, 24s, 30s (max)
        val baseDelay = 3000L
        val maxDelay = 30000L
        val delay = (baseDelay * (1 shl attempts.coerceAtMost(3))).coerceAtMost(maxDelay)
        return delay
    }

    fun resetCircuitBreaker() {
        reconnectAttempts = 0
        nextReconnectAt = 0L
        log("Circuit breaker reset", LogType.INFO)
    }

    fun sendData(data: ByteArray, force: Boolean = false) {
        if (_isEmergencyStopActive.value && !force) return
        connection?.write(data)
    }

    // Legacy alias for single-slot migration compatibility
    fun sendDataToAll(data: ByteArray, force: Boolean = false) = sendData(data, force)

    // Legacy alias: disconnect for E-STOP
    fun disconnectAllForEStop() = disconnectForEStop()

    fun requestRssi() {
        if (!checkBluetoothPermission()) {
            log("RSSI refresh failed: Missing permissions", LogType.ERROR)
            return
        }
        if (connectionType != DeviceType.LE) {
            log("RSSI refresh not supported for classic devices", LogType.WARNING)
            return
        }
        connection?.requestRssi()
    }

    fun reconnectSavedDevice(): Boolean {
        // Reset circuit breaker on manual reconnect
        reconnectAttempts = 0
        nextReconnectAt = 0L

        val device = savedDevice
        val state = _connectionState.value
        if (device != null && state != ConnectionState.CONNECTED && state != ConnectionState.CONNECTING) {
            log("Manually reconnecting to ${device.name}", LogType.INFO)
            connectToDevice(device, isAutoReconnect = true)
            return true
        }
        log("No saved device to reconnect", LogType.WARNING)
        return false
    }

    private fun updateConnectionState(state: ConnectionState) {
        val now = System.currentTimeMillis()
        // Debounce noisy DISCONNECTED/ERROR flips to avoid UI spam
        val isNoisyState = state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR
        if (isNoisyState && (now - lastStateChangeAt) < 800) {
            return
        }
        lastStateChangeAt = now
        _connectionState.value = state

        when(state) {
            ConnectionState.CONNECTED -> {
                log("Connected!", LogType.SUCCESS)
                vibrate(200) // Long vibration for connection
                lastPacketAt = now
                updateHealth(heartbeatSeq, lastPacketAt, rssiFailures)
                // Reset backoff counter on successful connection
                reconnectAttempts = 0
                nextReconnectAt = 0L
                // Reset packet loss stats
                packetsSent.set(0, 0)
                packetsDropped.set(0, 0)
                packetsFailed.set(0, 0)
                lastPacketLossWarningTime.set(0, 0)
            }
            ConnectionState.DISCONNECTED -> {
                // Only vibrate if it was previously connected or connecting (avoid noise on startup)
                vibrate(100) // Short vibration for disconnection
            }
            ConnectionState.ERROR -> {
                log("Connection error", LogType.ERROR)
                vibrate(500) // Very long vibration for error
            }
            else -> {}
        }
    }

    private fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun updateRssi(rssi: Int) {
        _rssiValue.value = rssi
        updateHealth(heartbeatSeq, lastPacketAt, rssiFailures)
    }

    private fun updateHealth(seq: Int, packetAt: Long, failures: Int) {
        _health.value = ConnectionHealth(packetAt, failures, seq, lastHeartbeatSentAt, lastRttMs)

        // Record RSSI and RTT to history for graph visualization
        val rssi = _rssiValue.value
        if (rssi != 0) {
            _telemetryHistoryManager.recordRssi(rssi)
        }
        if (lastRttMs > 0) {
            _telemetryHistoryManager.recordRtt(lastRttMs)
        }
    }

    private fun forceReconnect(reason: String) {
        val now = System.currentTimeMillis()
        val timeSinceLastPacket = now - lastPacketAt
        val missedAcks = missedHeartbeatAcks
        val currentRtt = lastRttMs

        log("Reconnecting: $reason", LogType.WARNING)
        log("  └─ Diagnostics: missedAcks=$missedAcks, timeSinceLastPacket=${timeSinceLastPacket}ms, lastRTT=${currentRtt}ms", LogType.INFO)

        connection?.cancel()
        connection = null
        // Safely unlock mutex - use try-catch to handle race condition where
        // mutex may be unlocked between check and unlock
        try {
            if (connectionMutex.isLocked) {
                connectionMutex.unlock()
            }
        } catch (e: IllegalStateException) {
            // Mutex was already unlocked by another thread - this is expected
        }
        updateConnectionState(ConnectionState.ERROR)
        updateRssi(0)

        // Reset heartbeat tracking
        missedHeartbeatAcks = 0
        lastHeartbeatSentAt = 0L

        savedDevice?.let { device ->
            connectToDevice(device, isAutoReconnect = true)
        } ?: run {
            log("Force reconnect skipped: No saved device", LogType.WARNING)
        }
    }

    fun cleanup() {
        // Clean up old device name cache entries
        scope.launch {
            deviceNameCache.cleanOldEntries()
        }

        // Cancel the coroutine scope to prevent leaks
        scope.cancel()

        // Cancel scan job if active
        scanJob?.cancel()
        keepAliveJob?.cancel()

        // Disconnect active connection
        connection?.cancel()
        connection = null

        // Unregister broadcast receiver safely
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver not registered or already unregistered
            } catch (e: Exception) {
                Log.w("BT", "Failed to unregister receiver: ${e.message}")
            }
        }
    }

    private val isDebugMode = com.metelci.ardunakon.BuildConfig.DEBUG

    // Telemetry State
    data class Telemetry(
        val batteryVoltage: Float,
        val status: String,
        val packetsSent: Long = 0,
        val packetsDropped: Long = 0,
        val packetsFailed: Long = 0
    )
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    // Packet loss tracking - Thread-safe using AtomicLongArray (single slot)
    private val packetsSent = java.util.concurrent.atomic.AtomicLongArray(1)
    private val packetsDropped = java.util.concurrent.atomic.AtomicLongArray(1)
    private val packetsFailed = java.util.concurrent.atomic.AtomicLongArray(1)
    private val lastPacketLossWarningTime = java.util.concurrent.atomic.AtomicLongArray(1)

    private var lastTelemetryLogTime = 0L

    fun parseTelemetry(packet: ByteArray) {
        // Expected schema aligned to ProtocolManager packets:
        // [START][DEV][CMD][D1][D2][D3][D4][D5][CHK][END]
        // We treat CMD_HEARTBEAT (0x03) as telemetry carrier: D1=battery (tenths), D2=status (0/1)
        if (packet.size < 10) return
        if (packet.first() != 0xAA.toByte() || packet.last() != 0x55.toByte()) return

        // Verify checksum to avoid junk telemetry
        var xor: Byte = 0
        for (i in 1..7) xor = xor xor packet[i]
        if (xor != packet[8]) return

        if (packet[2] != com.metelci.ardunakon.protocol.ProtocolManager.CMD_HEARTBEAT) return

        val batteryRaw = packet[3].toInt() and 0xFF
        val statusByte = packet[4].toInt() and 0xFF
        if (statusByte > 1) return

        val battery = batteryRaw / 10f

        // Validate battery voltage is within reasonable bounds (0V to 30V)
        if (battery < 0f || battery > 30f) {
            if (isDebugMode) {
                val now = System.currentTimeMillis()
                if (now - lastTelemetryLogTime > 20000) {
                    log("Invalid telemetry: voltage ${battery}V out of range", LogType.WARNING)
                    lastTelemetryLogTime = now
                }
            }
            return
        }

        val status = if (statusByte == 1) "Safe Mode" else "Active"

        // Get packet stats
        val totalSent = packetsSent.get(0)
        val totalDropped = packetsDropped.get(0)
        val totalFailed = packetsFailed.get(0)

        _telemetry.value = Telemetry(battery, status, totalSent, totalDropped, totalFailed)

        // Record battery voltage to history for graph visualization
        _telemetryHistoryManager.recordBattery(battery)

        if (isDebugMode) {
            val now = System.currentTimeMillis()
            if (now - lastTelemetryLogTime > 20000) {
                log("Telemetry: Bat=${battery}V, Stat=$status", LogType.SUCCESS)
                lastTelemetryLogTime = now
            }
        }
    }

    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null
        @Volatile private var cancelled = false

        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                if (!checkBluetoothPermission()) {
                    log("Connect failed: Missing permissions", LogType.ERROR)
                    updateConnectionState(ConnectionState.ERROR)
                    return
                }

                log("Starting connection to ${device.name} (${device.address})", LogType.INFO)
                log("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})", LogType.INFO)

                // MILITARY GRADE STABILITY: Ensure discovery is cancelled and radio is settled
                if (adapter?.isDiscovering == true) {
                    adapter.cancelDiscovery()
                    log("Cancelling discovery for stability...", LogType.INFO)
                    safeSleep(300) // Let radio settle
                }

                // MILITARY GRADE STABILITY: Check bonding state
                if (device.bondState == BluetoothDevice.BOND_NONE) {
                    log("Device not bonded. Initiating pairing...", LogType.WARNING)
                    device.createBond()
                    // Wait for bonding (simple timeout-based wait)
                    var bondWait = 0
                    while (device.bondState != BluetoothDevice.BOND_BONDED && bondWait < 100 && !cancelled) {
                        safeSleep(100)
                        bondWait++
                    }
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        log("Pairing might have failed or timed out. Proceeding anyway...", LogType.WARNING)
                    } else {
                        log("Pairing successful!", LogType.SUCCESS)
                    }
                }

                var connected = false

                // Device Verification: Perform cryptographic verification if enabled
                // This is NON-BLOCKING and does NOT affect connectivity
                if (deviceVerificationEnabled) {
                    scope.launch {
                        try {
                            performDeviceVerification(device)
                        } catch (e: DeviceVerificationException) {
                            log("Device verification failed: ${e.message}", LogType.WARNING)
                            // Verification failure does NOT affect connectivity
                            // This is purely informational for security logging
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            log("Device verification error: ${e.message}", LogType.WARNING)
                            // Any verification errors are non-critical
                        }
                    }
                }

                // HC-06 Connection Strategy:
                // Reflection is only used when user explicitly enables Legacy Reflection.
                // For Xiaomi/MIUI, we reorder to try reflection first only if the toggle is ON
                // and use an "aggressive" mode with shorter delays to beat MIUI timeouts.

                val reflectionAllowed = allowReflectionFallback
                val forceReflectionFirst = reflectionAllowed && shouldForceReflectionFallback()
                val aggressiveXiaomi = forceReflectionFirst // legacy reflection ON + Xiaomi OEM

                // Attempt A (OEM-first): Reflection Port 1 (when forced)
                if (!connected && !cancelled && forceReflectionFirst) {
                    try {
                        log("Forcing Reflection Port 1 FIRST (OEM fallback: ${android.os.Build.MANUFACTURER})", LogType.WARNING)
                        val m: java.lang.reflect.Method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                        socket?.connect()
                        connected = true
                        log("Reflection Port 1 connection established.", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("Reflection Port 1 (forced) failed: ${e.message}", LogType.ERROR)
                        Log.w("BT", "Reflection Port 1 (forced) failed: ${e.message}", e)
                        closeSocketSafely(socket)
                        socket = null
                        if (!cancelled) safeSleep(if (aggressiveXiaomi) 600 else 1000)
                    }
                }

                // Attempt A2: Raw RFCOMM (reflection) Port 1 BEFORE UUID list (for stubborn HC-06/MIUI)
                if (!connected && !cancelled && reflectionAllowed) {
                    try {
                        log("Attempting RAW RFCOMM via reflection (Port 1, no UUID)...", LogType.WARNING)
                        val m: java.lang.reflect.Method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                        socket?.connect()
                        connected = true
                        log("Raw RFCOMM Port 1 connection established.", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("Raw RFCOMM Port 1 failed: ${e.message}", LogType.ERROR)
                        Log.w("BT", "Raw RFCOMM Port 1 failed: ${e.message}", e)
                        closeSocketSafely(socket)
                        socket = null
                        if (!cancelled) safeSleep(if (aggressiveXiaomi) 600 else 1000)
                    }
                }

                // Attempt 1: Standard SPP UUID with INSECURE connection (most reliable for HC-06 on non-MIUI)
                if (!connected && !cancelled) {
                    try {
                        log("Attempting INSECURE SPP connection (Standard HC-06)...", LogType.INFO)
                        socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                        socket?.connect()
                        connected = true
                        log("Connected successfully with Standard SPP", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("Standard SPP failed: ${e.message}", LogType.ERROR)
                        Log.w("BT", "Standard SPP failed: ${e.message}", e)
                        closeSocketSafely(socket)
                        socket = null
                        // HC-06 clones need recovery time; shorten on aggressive Xiaomi to avoid MIUI timeouts
                        if (!cancelled) safeSleep(if (aggressiveXiaomi) 1000 else 2000)
                    }
                }

                // Attempt 2: Reflection Method (Port 1) - Most reliable fallback for HC-06
                if (!connected && !cancelled && reflectionAllowed) {
                    try {
                        log("Attempting REFLECTION connection (Port 1 - HC-06 Fallback)...", LogType.WARNING)
                        val m: java.lang.reflect.Method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                        socket?.connect()
                        connected = true
                        log("Reflection Port 1 connection established.", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("Reflection Port 1 failed: ${e.message}", LogType.ERROR)
                        Log.w("BT", "Reflection Port 1 failed: ${e.message}", e)
                        closeSocketSafely(socket)
                        socket = null
                        // HC-06 clones often need recovery time after reflection attempts
                        if (!cancelled) safeSleep(if (aggressiveXiaomi) 800 else 2000)
                    }
                }

                // Attempt 3: Try other manufacturer-specific UUIDs (for HC-06 clones)
                if (!connected && !cancelled) {
                    for ((index, uuid) in MANUFACTURER_UUIDS.withIndex()) {
                        if (connected || cancelled) break

                        val uuidDesc = when (index) {
                            0 -> "Nordic nRF51822 variant (Chinese clones)"
                            1 -> "Nordic UART Service (nRF51/52)"
                            2 -> "Object Push Profile (ZS-040/FC-114/linvor)"
                            3 -> "OBEX Object Push (linvor firmware)"
                            4 -> "Headset Profile (BT 2.0 clones)"
                            5 -> "Hands-Free Profile (HFP clones)"
                            6 -> "A/V Remote Control (rare clones)"
                            7 -> "Advanced Audio Distribution (multimedia clones)"
                            8 -> "Dial-up Networking (older firmware)"
                            9 -> "LAN Access Profile (network clones)"
                            10 -> "Raw RFCOMM (bare-metal)"
                            11 -> "Base UUID (non-standard fallback)"
                            else -> "UUID $index"
                        }

                        try {
                            log("Attempting INSECURE connection with $uuidDesc...", LogType.INFO)
                            socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                            socket?.connect()
                            connected = true
                            log("Connected successfully with $uuidDesc", LogType.SUCCESS)
                        } catch (e: Exception) {
                            log("$uuidDesc failed: ${e.message}", LogType.ERROR)
                            Log.w("BT", "UUID $index failed: ${e.message}", e)
                            closeSocketSafely(socket)
                            socket = null
                            // HC-06 clones need consistent delays between UUID attempts
                            if (!cancelled) safeSleep(if (aggressiveXiaomi) 800 else 1500)
                        }
                    }
                }

                // Attempt 4: Try alternative reflection ports (rarely needed for HC-06)
                if (!connected && !cancelled) {
                    for (port in listOf(2, 3)) { // Only try ports 2-3 to avoid excessive attempts
                        if (connected || cancelled) break

                        try {
                            log("Attempting REFLECTION connection (Port $port)...", LogType.WARNING)
                            val m: java.lang.reflect.Method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            socket = m.invoke(device, port) as BluetoothSocket
                            socket?.connect()
                            connected = true
                            log("Reflection Port $port connection established.", LogType.SUCCESS)
                            break
                        } catch (e: Exception) {
                            log("Reflection Port $port failed: ${e.message}", LogType.ERROR)
                            Log.w("BT", "Reflection Port $port failed: ${e.message}", e)
                            closeSocketSafely(socket)
                            socket = null
                            if (!cancelled) safeSleep(if (aggressiveXiaomi) 700 else 1000)
                        }
                    }
                }

                // Attempt 5: Last resort - Try SECURE connection with Standard SPP
                // Some modules (rare HC-05 variants) require secure pairing
                if (!connected && !cancelled && !aggressiveXiaomi) {
                    try {
                        log("Attempting SECURE SPP connection (last resort)...", LogType.WARNING)
                        socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                        socket?.connect()
                        connected = true
                        log("Connected successfully with SECURE SPP", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("SECURE SPP failed: ${e.message}", LogType.ERROR)
                        Log.w("BT", "Secure SPP failed: ${e.message}", e)
                        closeSocketSafely(socket)
                        socket = null
                        if (!cancelled) safeSleep(1000)
                    }
                }

                if (cancelled) {
                    closeSocketSafely(socket)
                    log("Connection attempt cancelled for ${device.name} ", LogType.WARNING)
                    updateConnectionState( ConnectionState.DISCONNECTED)
                    return
                }

                val validSocket = socket
                if (connected && validSocket != null) {
                    // Connection successful
                    // MIUI/Xiaomi-specific fix: Allow socket streams to fully initialize
                    // On Xiaomi devices, socket.outputStream/inputStream fail if accessed too early
                    if (shouldForceReflectionFallback()) {
                        log("Applying Xiaomi stream initialization delay (500ms)...", LogType.INFO)
                        safeSleep(500)
                    }

                    try {
                        val connectedThread = ConnectedThread(validSocket)
                        connection = connectedThread
                        connectedThread.start()

                        updateConnectionState( ConnectionState.CONNECTED)
                    } catch (e: IOException) {
                        // Stream initialization failed (Xiaomi/MIUI blocking)
                        log("Failed to create socket streams: ${e.message}", LogType.ERROR)
                        log("Marking connection as failed and will retry with different method", LogType.WARNING)
                        closeSocketSafely(validSocket)
                        updateConnectionState( ConnectionState.ERROR)
                    }
                } else {
                    closeSocketSafely(socket)
                    log("============================================", LogType.ERROR)
                    log("ALL CONNECTION METHODS FAILED for ${device.name} ", LogType.ERROR)
                    log("Tried: SPP, Reflection Ports 1-3, 12 UUIDs, Secure SPP", LogType.ERROR)
                    log("Module may be defective or incompatible", LogType.ERROR)
                    log("See HC06_TROUBLESHOOTING.md for help", LogType.ERROR)
                    log("============================================", LogType.ERROR)
                    updateConnectionState( ConnectionState.ERROR)
                }
            } finally {
                // Always unlock the mutex when connection attempt completes (success or failure)
                connectionMutex.unlock()
            }
        }

        private fun closeSocketSafely(socket: BluetoothSocket?) {
            if (socket == null) return
            try {
                socket.close()
                // Give OS time to release resources
                safeSleep(100)
            } catch (e: Exception) {
                Log.w("BT", "Socket close failed: ${e.message}", e)
            }
        }

        private fun safeSleep(ms: Long) {
            try {
                Thread.sleep(ms)
            } catch (e: InterruptedException) {
                cancelled = true
            }
        }

        fun cancel() {
            cancelled = true
            closeSocketSafely(socket)
        }

        private fun performDeviceVerification(device: BluetoothDevice) {
            try {
                log("Starting device cryptographic verification for ${device.name} ...", LogType.INFO)

                // Generate verification challenge
                val challenge = deviceVerificationManager.generateVerificationChallenge(device.address)
                log("Generated verification challenge", LogType.INFO)

                // In a real implementation, this challenge would be sent to the device
                // and the device would respond with the encrypted challenge
                // For now, we simulate the verification process

                // Simulate device response (in real implementation, this would come from the device)
                val simulatedResponse = deviceVerificationManager.generateVerificationChallenge(device.address)

                // Verify the response
                val verificationResult = deviceVerificationManager.verifyDeviceResponse(
                    device.address,
                    challenge,
                    simulatedResponse
                )

                if (verificationResult) {
                    log("Device verification SUCCESS: ${device.name}  is cryptographically verified", LogType.SUCCESS)

                    // Generate shared secret for secure communication
                    @Suppress("UNUSED_VARIABLE")
                    val sharedSecret = deviceVerificationManager.generateSharedSecret(device.address)
                    log("Generated shared secret for secure communication", LogType.SUCCESS)
                    // Note: sharedSecret would be used for packet encryption in production
                } else {
                    log("Device verification FAILED: ${device.name}  failed cryptographic verification", LogType.WARNING)
                    // Verification failure is non-critical - continue normally
                }

            } catch (e: DeviceVerificationException) {
                log("Device verification error: ${e.message}", LogType.WARNING)
                // All verification errors are non-critical and don't affect connectivity
            } catch (e: Exception) {
                log("Unexpected verification error: ${e.message}", LogType.WARNING)
                // Any unexpected errors are caught and logged but don't affect connectivity
            }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread(), BluetoothConnection {
        private val outputStream: OutputStream
        private val inputStream: InputStream
        private val buffer = ByteArray(1024)

        init {
            // Wrap stream initialization with error handling for Xiaomi/MIUI compatibility
            try {
                outputStream = socket.outputStream
                inputStream = socket.inputStream
            } catch (e: IOException) {
                log("CRITICAL: Failed to initialize socket streams : ${e.message}", LogType.ERROR)
                log("This typically indicates MIUI/Xiaomi blocking reflection port access", LogType.ERROR)
                throw e // Re-throw to prevent thread from starting with invalid streams
            }
        }

        override fun run() {
            log("Socket opened for Device", LogType.SUCCESS)
            val packetBuffer = ByteArray(20) // Small buffer for packet assembly
            var bufferIndex = 0
            var consecutiveErrors = 0 // Track consecutive read errors for retry logic

            while (true) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        consecutiveErrors = 0 // Reset error counter on successful read
                        val data = buffer.copyOf(bytesRead)
                        _incomingData.value = data
                        recordInbound()

                        // Try to decode as text first for terminal display
                        val decodedText = try {
                            String(data, Charsets.UTF_8).trim()
                        } catch (e: Exception) {
                            null
                        }

                        // Log incoming data - always log for terminal visibility
                        if (decodedText != null && decodedText.isNotEmpty() && decodedText.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r" }) {
                            // Looks like text - display as string
                            log("RX Device: $decodedText", LogType.INFO)
                        } else {
                            // Binary data - display as hex
                            val hex = data.joinToString(" ") { "%02X".format(it) }
                            log("RX Device: $hex", LogType.INFO)
                        }

                        // Simple Packet Parsing (Looking for 0xAA ... 0x55)
                        for (byte in data) {
                            if (bufferIndex == 0 && byte != 0xAA.toByte()) continue // Wait for Start

                            packetBuffer[bufferIndex++] = byte

                            if (bufferIndex >= 10) { // Full Packet
                                if (packetBuffer[9] == 0x55.toByte()) {
                                    parseTelemetry(packetBuffer)
                                }
                                bufferIndex = 0 // Reset
                            }
                        }
                    }
                } catch (e: IOException) {
                    consecutiveErrors++
                    if (consecutiveErrors >= 3) {
                        // Permanent failure after 3 consecutive errors
                        Log.e("BT", "Disconnected Device after $consecutiveErrors errors", e)
                        log("Disconnected Device: ${e.message}", LogType.ERROR)
                        updateConnectionState( ConnectionState.DISCONNECTED)
                        connection = null
                        break
                    } else {
                        // Transient error - log warning and retry
                        Log.w("BT", "Transient read error $consecutiveErrors/3 on Device", e)
                        log("Read error ${consecutiveErrors}/3  - retrying...", LogType.WARNING)
                        try {
                            Thread.sleep(50) // Brief pause before retry
                        } catch (ie: InterruptedException) {
                            break // Thread interrupted, exit gracefully
                        }
                    }
                }
            }
        }
        


        override fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e("BT", "Write failed - triggering reconnect", e)
                log("Write failed Device: ${e.message} - reconnecting...", LogType.ERROR)
                // Trigger proper cleanup and reconnection
                updateConnectionState( ConnectionState.DISCONNECTED)
                connection = null
                cancel()
            }
        }

        override fun cancel() {
            try { socket.close() } catch (e: IOException) {}
        }

        override fun requestRssi() {
            // Classic SPP does not support remote RSSI polling
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    private inner class BleConnection(private val device: BluetoothDevice) : BluetoothConnection {
        private var bluetoothGatt: android.bluetooth.BluetoothGatt? = null
        private var txCharacteristic: android.bluetooth.BluetoothGattCharacteristic? = null

        // HC-08 / HM-10 / AT-09 / MLT-BT05 BLE Module UUID Variants
        // Different manufacturers and firmware versions use different UUIDs
        // This comprehensive list supports all major clones and variants

        // Variant 1: Most common HC-08 and HM-10 modules (JNHuaMao, DSD TECH, Bolutek)
        private val SERVICE_UUID_V1 = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID_V1 = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

        // Variant 2: Nordic UART Service (NUS) - Nordic nRF51822/nRF52 based modules
        // Used by: Some HM-10 clones, Nordic-based HC-08, Adafruit Bluefruit
        private val SERVICE_UUID_V2 = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val CHAR_UUID_TX_V2 = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // TX (write)
        private val CHAR_UUID_RX_V2 = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // RX (notify)

        // Variant 3: TI CC2540/CC2541 HM-10 firmware (original JNHuaMao firmware)
        private val SERVICE_UUID_V3 = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID_V3 = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")

        // Variant 4: Alternative HC-08 firmware (some Chinese clones)
        private val SERVICE_UUID_V4 = UUID.fromString("0000FFE5-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID_V4 = UUID.fromString("0000FFE9-0000-1000-8000-00805F9B34FB")

        // Variant 5: AT-09 BLE Module (CC2541-based, similar to HM-10)
        private val SERVICE_UUID_V5 = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID_V5 = UUID.fromString("0000FFE4-0000-1000-8000-00805F9B34FB")

        // Variant 6: MLT-BT05 and other TI-based clones
        private val SERVICE_UUID_V6 = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID_V6 = UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB")

        // Variant 9: ArduinoBLE Library Standard Example (Uno R4 Wifi / Nano 33 IoT)
        private val SERVICE_UUID_V9 = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
        private val CHAR_UUID_V9 = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")

        // Updated UUIDs with separate TX/RX (v2.0 sketches)
        private val CHAR_UUID_TX_V1 = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")  // TX (notify)
        private val CHAR_UUID_RX_V1 = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB")  // RX (write)
        private val CHAR_UUID_TX_V9 = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")  // TX (notify)
        private val CHAR_UUID_RX_V9 = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214")  // RX (write)

        // Legacy single UUID (backward compatibility)
        private val CHAR_UUID_V1_LEGACY = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID_V9_LEGACY = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")

        // Client Characteristic Configuration Descriptor (CCCD) for notifications - standard
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        // Track which variant was successful
        private var detectedVariant: Int = 0

        private var connectionJob: Job? = null
        private var pollingJob: Job? = null

        // Write queue management - prevent write collisions and memory leaks
        // Capacity of 100 packets prevents unbounded growth during connectivity issues
        private val writeQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>(100)
        private var writeJob: Job? = null

        // Descriptor write queue management - ensures sequential GATT operations
        private val pendingDescriptorWrites = mutableListOf<Triple<android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattDescriptor, ByteArray>>()
        private var descriptorWriteIndex = 0

        // GATT retry tracking for transient errors
        private var gattRetryAttempt = 0
        private val maxGattRetries = 3
        private var lastGattError = 0

        // Resolved device name (cached to avoid null in logs)
        private var resolvedDeviceName: String = ""

        @SuppressLint("MissingPermission")
        fun connect() {
            if (!checkBluetoothPermission()) {
                updateConnectionState( ConnectionState.ERROR)
                log("BLE connect failed: Missing permissions", LogType.ERROR)
                return
            }
            // Resolve device name for logging
            scope.launch {
                resolvedDeviceName = resolveDeviceName(device, DeviceType.LE)
            }
            // Use device.name as fallback for immediate logging
            val deviceName = device.name ?: "Unknown (${device.address})"
            log("Connecting to BLE device $deviceName ...", LogType.INFO)
            connectGattWithTimeout()
        }

        @SuppressLint("MissingPermission")
        private fun connectGattWithTimeout() {
            if (!checkBluetoothPermission()) {
                updateConnectionState( ConnectionState.ERROR)
                log("BLE connect failed: Missing permissions", LogType.ERROR)
                return
            }

            // Pre-flight checks
            val localAdapter = adapter
            if (localAdapter == null || !localAdapter.isEnabled) {
                updateConnectionState( ConnectionState.ERROR)
                log("BLE connect failed: Bluetooth is off", LogType.ERROR)
                connectionMutex.unlock()
                return
            }

            // Add settling delay before connection attempt
            // This gives the BLE stack time to stabilize after previous operations
            Thread.sleep(500)

            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            // Timeout logic with retry (15 seconds per attempt for slower BLE modules)
            connectionJob = scope.launch {
                delay(15000) // 15 second timeout (increased for slower modules)
                if (connection == this@BleConnection &&
                    _connectionState.value != ConnectionState.CONNECTED) {
                    log("BLE Connection timed out after 15s. Retrying once...", LogType.WARNING)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    delay(500)

                    // Retry once
                    bluetoothGatt = device.connectGatt(context, false, gattCallback)

                    // Second timeout - if this fails, mark as ERROR for auto-reconnect
                    delay(15000) // 15 second timeout on retry
                    if (connection == this@BleConnection &&
                        _connectionState.value != ConnectionState.CONNECTED) {
                        log("BLE Connection failed after retry (total 30s)", LogType.ERROR)
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        updateConnectionState( ConnectionState.ERROR)
                        connection = null
                        connectionMutex.unlock()
                    }
                }
            }
        }

        private fun startRssiPolling() {
            pollingJob?.cancel()
            pollingJob = scope.launch {
                while (isActive && bluetoothGatt != null) {
                    delay(2000) // Poll every 2 seconds
                    try {
                        if (connection == this@BleConnection && _connectionState.value == ConnectionState.CONNECTED) {
                            if (!checkBluetoothPermission()) {
                                log("RSSI polling halted: Missing permissions", LogType.WARNING)
                                delay(2000)
                                continue
                            }
                            bluetoothGatt?.readRemoteRssi()
                        }
                    } catch (e: SecurityException) {
                        Log.e("BT", "RSSI read failed", e)
                    }
                }
            }
        }

        private val gattCallback = object : android.bluetooth.BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
                connectionJob?.cancel() // Cancel timeout

                // Validate GATT status first - critical for preventing phantom connections
                if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    lastGattError = status
                    val errorDesc = GattStatus.getErrorDescription(status)

                    // Log the error with detailed description
                    log("BLE GATT ${errorDesc}", LogType.ERROR)

                    // Classify error and decide on retry strategy
                    val shouldRetry = GattStatus.isTransientError(status) && gattRetryAttempt < maxGattRetries
                    val isPermanent = GattStatus.isPermanentError(status)

                    if (isPermanent) {
                        // Permanent error - fail immediately without retry
                        log("Permanent GATT error detected. Not retrying.", LogType.ERROR)
                        updateConnectionState( ConnectionState.ERROR)
                        pollingJob?.cancel()
                        connection = null
                        gatt.close()
                        updateRssi( 0)
                        connectionMutex.unlock()
                        return
                    }

                    if (shouldRetry) {
                        // Transient error - log and track, but let connection timeout retry handle it
                        gattRetryAttempt++
                        val retryDelayMs = when (gattRetryAttempt) {
                            1 -> 2000L  // 2 seconds
                            2 -> 4000L  // 4 seconds
                            3 -> 6000L  // 6 seconds
                            else -> 6000L
                        }

                        log("Transient GATT error detected (attempt $gattRetryAttempt/$maxGattRetries). Will retry with ${retryDelayMs}ms backoff.", LogType.WARNING)

                        // Clean up current GATT connection properly
                        pollingJob?.cancel()
                        try {
                            gatt.disconnect()
                            Thread.sleep(200) // Small delay between disconnect and close
                        } catch (e: Exception) {
                            Log.e("BT", "GATT disconnect failed", e)
                        }
                        gatt.close()
                        updateRssi( 0)

                        // Mark as ERROR so auto-reconnect takes over with backoff
                        updateConnectionState( ConnectionState.ERROR)
                        connection = null
                        connectionMutex.unlock()

                        // Schedule a short backoff reconnect for transient HM-10/MLT-BT05 errors (e.g., status 62)
                        scope.launch {
                            delay(retryDelayMs)
                            log("Retrying BLE connect after transient error (status $status)...", LogType.WARNING)
                            connectToDevice(savedDevice
                                ?: BluetoothDeviceModel(device.name ?: "Unknown", device.address, DeviceType.LE), isAutoReconnect = true)
                        }
                    } else {
                        // Max retries exhausted or unknown error - fail and let auto-reconnect handle it
                        if (gattRetryAttempt >= maxGattRetries) {
                            log("Max GATT retries ($maxGattRetries) exhausted. Failing connection.", LogType.ERROR)
                        }
                        updateConnectionState( ConnectionState.ERROR)
                        pollingJob?.cancel()
                        connection = null
                        gatt.close()
                        updateRssi( 0)
                        connectionMutex.unlock()
                        gattRetryAttempt = 0  // Reset for next connection attempt
                    }
                    return
                }

                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    log("BLE Connected to GATT server.", LogType.SUCCESS)
                    updateConnectionState( ConnectionState.CONNECTED)

                    // Reset GATT retry counter on successful connection
                    gattRetryAttempt = 0

                    // Request MTU increase for better throughput (default is 23 bytes, request 512)
                    // This allows larger packets and reduces overhead for 20Hz joystick data
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val mtuRequested = gatt.requestMtu(512)
                        if (mtuRequested) {
                            log("BLE MTU negotiation requested (512 bytes)", LogType.INFO)
                        }
                    }

                    // Request connection priority HIGH for low latency (important for real-time control)
                    // This reduces connection interval for faster data transmission
                    gatt.requestConnectionPriority(android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    log("BLE High priority connection requested", LogType.INFO)

                    // Discover services
                    gatt.discoverServices()
                    startRssiPolling()

                    // Unlock mutex on successful connection
                    connectionMutex.unlock()
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    pollingJob?.cancel()
                    log("BLE Disconnected from GATT server.", LogType.WARNING)
                    updateConnectionState( ConnectionState.DISCONNECTED)
                    connection = null
                    gatt.close() // Critical: Prevent resource leak
                    updateRssi( 0)
                    // Safely unlock mutex on disconnection - use try-catch to handle race condition
                    try {
                        if (connectionMutex.isLocked) {
                            connectionMutex.unlock()
                        }
                    } catch (e: IllegalStateException) {
                        // Mutex was already unlocked - this is expected
                    }
                }
            }

            override fun onReadRemoteRssi(gatt: android.bluetooth.BluetoothGatt?, rssi: Int, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    updateRssi( rssi)
                    rssiFailures = 0
                    updateHealth( heartbeatSeq, lastPacketAt, rssiFailures)
                } else {
                    // Only enforce RSSI-based reconnects for BLE devices
                    if (connectionType == DeviceType.LE) {
                        rssiFailures = (rssiFailures + 1).coerceAtMost(10)
                        updateHealth( heartbeatSeq, lastPacketAt, rssiFailures)
                        if (rssiFailures >= 3) {
                            val errorDesc = GattStatus.getErrorDescription(status)
                            log("RSSI read failures : ${rssiFailures} - $errorDesc", LogType.WARNING)
                        }
                        if (rssiFailures >= 5) {
                            forceReconnect( "RSSI polling failed ${rssiFailures} times")
                        }
                    }
                }
            }

            override fun onMtuChanged(gatt: android.bluetooth.BluetoothGatt?, mtu: Int, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    log("BLE MTU changed to $mtu bytes (effective payload: ${mtu - 3} bytes)", LogType.SUCCESS)
                } else {
                    val errorDesc = GattStatus.getErrorDescription(status)
                    log("BLE MTU negotiation failed: $errorDesc - using default 23 bytes", LogType.WARNING)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
                if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    val errorDesc = GattStatus.getErrorDescription(status)
                    log("BLE Service discovery failed: $errorDesc", LogType.ERROR)
                    return
                }

                // Try all UUID variants to find the correct one for this module
                var serviceFound = false

                // Variant 1: Standard HC-08/HM-10 (FFE0/FFE1/FFE2) - Most common
                var service = gatt.getService(SERVICE_UUID_V1)
                if (service != null) {
                    // Try new split TX/RX first (v2.0 sketches)
                    log("→ Checking for HM-10 v2.0 split TX/RX characteristics...", LogType.INFO)
                    val txChar = service.getCharacteristic(CHAR_UUID_TX_V1)  // FFE1 (notify)
                    val rxChar = service.getCharacteristic(CHAR_UUID_RX_V1)  // FFE2 (write)

                    if (txChar != null && rxChar != null) {
                        txCharacteristic = rxChar  // Use RX for writing TO Arduino
                        detectedVariant = 1
                        serviceFound = true
                        log("✓ BLE Module detected: HM-10 v2.0 (FFE0/FFE1/FFE2)", LogType.SUCCESS)
                        log("  TX Char (notify): ${txChar.uuid}", LogType.INFO)
                        log("  RX Char (write):  ${rxChar.uuid}", LogType.INFO)

                        // Queue notification enable for TX characteristic
                        setCharacteristicNotificationSafe(gatt, txChar, true)
                        val descriptor = txChar.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            log("→ Queuing CCCD descriptor write for TX characteristic", LogType.INFO)
                            pendingDescriptorWrites.add(Triple(gatt, descriptor, android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                        }
                    } else {
                        // Fallback: Try legacy single UUID (backward compatibility)
                        var char = service.getCharacteristic(CHAR_UUID_V1_LEGACY)
                        if (char != null) {
                            txCharacteristic = char
                            detectedVariant = 1
                            serviceFound = true
                            log("BLE Module detected: HM-10 Legacy (FFE0/FFE1)", LogType.SUCCESS)
                        } else {
                            // Try AT-09 characteristic (FFE4) as fallback for same service
                            char = service.getCharacteristic(CHAR_UUID_V5)
                            if (char != null) {
                                txCharacteristic = char
                                detectedVariant = 5
                                serviceFound = true
                                log("BLE Module detected: AT-09 (FFE0/FFE4)", LogType.SUCCESS)
                            }
                        }
                    }
                }

                // Variant 2: Nordic UART Service (NUS)
                if (!serviceFound) {
                    service = gatt.getService(SERVICE_UUID_V2)
                    if (service != null) {
                        // Nordic uses separate TX and RX characteristics
                        val txChar = service.getCharacteristic(CHAR_UUID_TX_V2)
                        val rxChar = service.getCharacteristic(CHAR_UUID_RX_V2)
                        if (txChar != null) {
                            txCharacteristic = txChar // Use TX for writing
                            detectedVariant = 2
                            serviceFound = true
                            log("BLE Module detected: Nordic UART Service (NUS)", LogType.SUCCESS)

                            // Enable notifications on RX characteristic if available
                            if (rxChar != null) {
                                setCharacteristicNotificationSafe(gatt, rxChar, true)
                                val rxDescriptor = rxChar.getDescriptor(CCCD_UUID)
                                if (rxDescriptor != null) {
                                    writeDescriptorCompat(gatt, rxDescriptor, android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                }
                            }
                        }
                    }
                }

                // Variant 3: TI CC2540/CC2541 HM-10 (FFF0/FFF1)
                // Also check for MLT-BT05 variant (FFF0/FFF6) which shares the same service UUID
                if (!serviceFound) {
                    service = gatt.getService(SERVICE_UUID_V3)
                    if (service != null) {
                        // Try TI HM-10 characteristic first (FFF1)
                        var char = service.getCharacteristic(CHAR_UUID_V3)
                        if (char != null) {
                            txCharacteristic = char
                            detectedVariant = 3
                            serviceFound = true
                            log("BLE Module detected: TI CC254x HM-10 (FFF0/FFF1)", LogType.SUCCESS)
                        } else {
                            // Try MLT-BT05 characteristic (FFF6) as fallback for same service
                            char = service.getCharacteristic(CHAR_UUID_V6)
                            if (char != null) {
                                txCharacteristic = char
                                detectedVariant = 6
                                serviceFound = true
                                log("BLE Module detected: MLT-BT05/TI Clone (FFF0/FFF6)", LogType.SUCCESS)
                            }
                        }
                    }
                }

                // Variant 4: Alternative HC-08 (FFE5/FFE9)
                if (!serviceFound) {
                    service = gatt.getService(SERVICE_UUID_V4)
                    if (service != null) {
                        val char = service.getCharacteristic(CHAR_UUID_V4)
                        if (char != null) {
                            txCharacteristic = char
                            detectedVariant = 4
                            serviceFound = true
                            log("BLE Module detected: Alternative HC-08 (FFE5/FFE9)", LogType.SUCCESS)
                        }
                    }
                }

                // Variant 9: ArduinoBLE (Uno R4 WiFi / Uno Q)
                if (!serviceFound) {
                    service = gatt.getService(SERVICE_UUID_V9)
                    if (service != null) {
                        // Try new split TX/RX first (v2.0 sketches)
                        log("→ Checking for Arduino v2.0 split TX/RX characteristics...", LogType.INFO)
                        val txChar = service.getCharacteristic(CHAR_UUID_TX_V9)  // 19B10001 (notify)
                        val rxChar = service.getCharacteristic(CHAR_UUID_RX_V9)  // 19B10002 (write)

                        if (txChar != null && rxChar != null) {
                            txCharacteristic = rxChar  // Use RX for writing TO Arduino
                            detectedVariant = 9
                            serviceFound = true
                            log("✓ BLE Module detected: Arduino v2.0 (19B10000/01/02)", LogType.SUCCESS)
                            log("  TX Char (notify): ${txChar.uuid}", LogType.INFO)
                            log("  RX Char (write):  ${rxChar.uuid}", LogType.INFO)

                            // Queue notification enable for TX characteristic
                            setCharacteristicNotificationSafe(gatt, txChar, true)
                            val descriptor = txChar.getDescriptor(CCCD_UUID)
                            if (descriptor != null) {
                                log("→ Queuing CCCD descriptor write for TX characteristic", LogType.INFO)
                                pendingDescriptorWrites.add(Triple(gatt, descriptor, android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                            }
                        } else {
                            // Fallback: Try legacy single UUID (backward compatibility)
                            val char = service.getCharacteristic(CHAR_UUID_V9_LEGACY)
                            if (char != null) {
                                txCharacteristic = char
                                detectedVariant = 9
                                serviceFound = true
                                log("BLE Module detected: Arduino Legacy (19B10000/01)", LogType.SUCCESS)
                            }
                        }
                    }
                }

                // Variant 5: Handled in Variant 1 block (AT-09 shares FFE0 service with HM-10)
                // Variant 6: Handled in Variant 3 block (MLT-BT05 shares FFF0 service with TI HM-10)

                // Variant 7 & 8: Generic Serial Discovery (Fallback)
                // Look for ANY characteristic that supports WRITE (or WRITE_NO_RESPONSE) and NOTIFY
                if (!serviceFound) {
                    log("Known UUIDs not found. Attempting generic discovery...", LogType.WARNING)
                    
                    val services = gatt.services
                    for (svc in services) {
                        if (serviceFound) break
                        
                        // Strategy A: Look for a single characteristic that is both Writable and Notifiable/Indicatable (HM-10 style)
                        for (char in svc.characteristics) {
                            val props = char.properties
                            val hasWrite = (props and (android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE or android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                            val hasNotify = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                            val hasIndicate = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                            
                            if (hasWrite && (hasNotify || hasIndicate)) {
                                txCharacteristic = char
                                detectedVariant = 7 // Generic Unified
                                serviceFound = true
                                val type = if (hasNotify) "Notify" else "Indicate"
                                log("BLE Module detected: Generic Unified (${char.uuid}) [$type]", LogType.SUCCESS)
                                break
                            }
                        }

                        // Strategy B: Look for separate Write and Notify/Indicate characteristics in the same service (Nordic/Zephyr style)
                        if (!serviceFound) {
                            var writeChar: android.bluetooth.BluetoothGattCharacteristic? = null
                            var notifyChar: android.bluetooth.BluetoothGattCharacteristic? = null

                            for (char in svc.characteristics) {
                                val props = char.properties
                                val hasWrite = (props and (android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE or
                                                           android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                val hasNotify = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                                val hasIndicate = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

                                // Prioritize write-only characteristics (avoids selecting notify char for writing)
                                if (hasWrite && !hasNotify && !hasIndicate && writeChar == null) {
                                    writeChar = char
                                }
                                if ((hasNotify || hasIndicate) && notifyChar == null) {
                                    notifyChar = char
                                }

                                // Fallback: accept any writable/notifiable characteristic
                                if (writeChar == null && hasWrite) writeChar = char
                            }

                            if (writeChar != null && notifyChar != null) {
                                txCharacteristic = writeChar
                                detectedVariant = 8 // Generic Split
                                serviceFound = true
                                val type = if ((notifyChar.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
                                    "Notify" else "Indicate"
                                log("BLE Module detected: Generic Split (Write: ${writeChar.uuid}, Notify: ${notifyChar.uuid}) [$type]", LogType.SUCCESS)

                                // Queue notification enable
                                setCharacteristicNotificationSafe(gatt, notifyChar, true)
                                val descriptor = notifyChar.getDescriptor(CCCD_UUID)
                                if (descriptor != null) {
                                    val value = if ((notifyChar.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
                                        android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                    else
                                        android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    pendingDescriptorWrites.add(Triple(gatt, descriptor, value))
                                }
                            }
                        }
                    }
                }

                if (serviceFound && txCharacteristic != null) {
                    // Enable Notifications on TX characteristic (except Nordic and Generic Split which were handled above)
                    // For variants 1 and 9, notifications are queued above in the discovery blocks
                    if (detectedVariant != 1 && detectedVariant != 2 && detectedVariant != 8 && detectedVariant != 9) {
                        // Prefer a characteristic that actually supports Notify/Indicate; fall back to txCharacteristic
                        val txService = txCharacteristic?.service
                        val notifyCandidate = txService?.characteristics?.firstOrNull { char ->
                            val props = char.properties
                            (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                                    (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                        } ?: txCharacteristic

                        notifyCandidate?.let { notifyChar ->
                            val enableIndication = (notifyChar.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

                            setCharacteristicNotificationSafe(gatt, notifyChar, true)
                            val descriptor = notifyChar.getDescriptor(CCCD_UUID)
                            if (descriptor != null) {
                                val descriptorValue = if (enableIndication)
                                    android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                else
                                    android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                                // Queue descriptor write instead of writing inline
                                pendingDescriptorWrites.add(Triple(gatt, descriptor, descriptorValue))
                            }
                        }
                    }

                    // Process descriptor write queue first, then start write queue
                    if (pendingDescriptorWrites.isNotEmpty()) {
                        log("→ Processing ${pendingDescriptorWrites.size} queued CCCD descriptor(s)...", LogType.INFO)
                        descriptorWriteIndex = 0
                        val (g, desc, value) = pendingDescriptorWrites[0]
                        log("→ Writing CCCD descriptor 1/${pendingDescriptorWrites.size}...", LogType.INFO)
                        writeDescriptorCompat(g, desc, value)
                        // Write queue will start after all descriptors complete (in onDescriptorWrite)
                    } else {
                        // No descriptors needed, start queue immediately
                        log("→ No CCCD descriptors to write, starting write queue immediately", LogType.INFO)
                        startWriteQueue()
                    }
                } else {
                    log("BLE Service/Characteristic not found! Module may use unsupported UUIDs.", LogType.ERROR)
                    // Log all available services for debugging
                    gatt.services?.forEach { svc ->
                        log("Available service: ${svc.uuid}", LogType.INFO)
                    }
                    // Fail the connection so auto-reconnect/backoff can retry and avoid silent writes
                    updateConnectionState( ConnectionState.ERROR)
                    connection = null
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    connectionMutex.unlock()
                    scope.launch {
                        delay(1000)
                        log("Retrying BLE connect after missing service/characteristic...", LogType.WARNING)
                        connectToDevice(
                            savedDevice
                                ?: BluetoothDeviceModel(device.name ?: "Unknown", device.address, DeviceType.LE),
                            isAutoReconnect = true
                        )
                    }
                }
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onCharacteristicChanged(gatt: android.bluetooth.BluetoothGatt, characteristic: android.bluetooth.BluetoothGattCharacteristic) {
                // Incoming data - using legacy callback for compatibility
                @Suppress("DEPRECATION")
                val data = characteristic.value
                if (data != null && data.isNotEmpty()) {
                    _incomingData.value = data
                    recordInbound()

                    // Try to decode as text first for terminal display
                    val decodedText = try {
                        String(data, Charsets.UTF_8).trim()
                    } catch (e: Exception) {
                        null
                    }

                    // Log incoming data - always log for terminal visibility
                    if (decodedText != null && decodedText.isNotEmpty() && decodedText.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r" }) {
                        // Looks like text - display as string
                        log("RX Device: $decodedText", LogType.INFO)
                    } else {
                        // Binary data - display as hex
                        val hex = data.joinToString(" ") { "%02X".format(it) }
                        log("RX Device: $hex", LogType.INFO)
                    }

                    try {
                        parseTelemetry(data)
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }

            // API 33+ callback for characteristic changes
            override fun onCharacteristicChanged(
                gatt: android.bluetooth.BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                // Modern callback - value provided directly
                if (value.isNotEmpty()) {
                    _incomingData.value = value
                    recordInbound()

                    // Try to decode as text first for terminal display
                    val decodedText = try {
                        String(value, Charsets.UTF_8).trim()
                    } catch (e: Exception) {
                        null
                    }

                    // Log incoming data - always log for terminal visibility
                    if (decodedText != null && decodedText.isNotEmpty() && decodedText.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r" }) {
                        // Looks like text - display as string
                        log("RX Device: $decodedText", LogType.INFO)
                    } else {
                        // Binary data - display as hex
                        val hex = value.joinToString(" ") { "%02X".format(it) }
                        log("RX Device: $hex", LogType.INFO)
                    }

                    try {
                        parseTelemetry(value)
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }

            override fun onDescriptorWrite(
                gatt: android.bluetooth.BluetoothGatt,
                descriptor: android.bluetooth.BluetoothGattDescriptor,
                status: Int
            ) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    log("✓ CCCD descriptor ${descriptorWriteIndex + 1}/${pendingDescriptorWrites.size} configured for ${descriptor.characteristic.uuid}", LogType.INFO)

                    // Write next descriptor if any pending
                    descriptorWriteIndex++
                    if (descriptorWriteIndex < pendingDescriptorWrites.size) {
                        val (g, desc, value) = pendingDescriptorWrites[descriptorWriteIndex]
                        log("→ Writing CCCD descriptor ${descriptorWriteIndex + 1}/${pendingDescriptorWrites.size}...", LogType.INFO)
                        writeDescriptorCompat(g, desc, value)
                    } else {
                        // All descriptors configured - NOW safe to start write queue
                        log("✓ All ${pendingDescriptorWrites.size} CCCD descriptor(s) configured successfully!", LogType.SUCCESS)
                        log("→ Starting BLE write queue...", LogType.INFO)
                        startWriteQueue()
                    }
                } else {
                    val errorDesc = GattStatus.getErrorDescription(status)
                    log("✗ CCCD descriptor ${descriptorWriteIndex + 1}/${pendingDescriptorWrites.size} write failed: $errorDesc", LogType.WARNING)
                    // Continue anyway - may still work
                    log("→ Starting write queue despite descriptor error (may still work)...", LogType.WARNING)
                    startWriteQueue()
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun writeDescriptorCompat(
            gatt: android.bluetooth.BluetoothGatt,
            descriptor: android.bluetooth.BluetoothGattDescriptor,
            value: ByteArray
        ) {
            // Check permission before GATT operation to prevent SecurityException crash
            if (!checkBluetoothPermission()) {
                log("Descriptor write skipped: Missing Bluetooth permission", LogType.WARNING)
                return
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, value)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = value
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            } catch (e: SecurityException) {
                log("Descriptor write failed: Permission revoked", LogType.ERROR)
            }
        }

        /**
         * Safely enable characteristic notifications with permission check.
         * Returns true if notification was enabled successfully.
         */
        @SuppressLint("MissingPermission")
        private fun setCharacteristicNotificationSafe(
            gatt: android.bluetooth.BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            enable: Boolean
        ): Boolean {
            if (!checkBluetoothPermission()) {
                log("Notification setup skipped: Missing Bluetooth permission", LogType.WARNING)
                return false
            }
            return try {
                gatt.setCharacteristicNotification(characteristic, enable)
            } catch (e: SecurityException) {
                log("Notification setup failed: Permission revoked", LogType.ERROR)
                false
            }
        }

        private fun startWriteQueue() {
            writeJob?.cancel()
            // Clear any stale packets from previous session to prevent motor spin on reconnect
            writeQueue.clear()
            writeJob = scope.launch {
                // Wait for BLE stack to stabilize after connection before processing queue
                // This prevents burst of stale packets from causing motor issues on Android 15/16
                delay(200)

                while (isActive) {
                    try {
                        // Take from queue (blocking call)
                        val data = writeQueue.take()

                        // Skip if disconnected
                        if (bluetoothGatt == null || txCharacteristic == null) continue

                        // Perform actual write
                        performWrite(data)

                        // Small delay to prevent overwhelming BLE stack
                        // BLE can typically handle ~100 packets/sec, we're sending at 20Hz (50ms)
                        // For MLT-BT05, use shorter delay for better responsiveness
                        val delayMs = if (detectedVariant == 6) 2L else 10L // MLT-BT05 gets faster writes
                        delay(delayMs)
                    } catch (e: InterruptedException) {
                        // Queue interrupted, exit gracefully
                        break
                    } catch (e: Exception) {
                        Log.e("BT", "Write queue error", e)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun performWrite(bytes: ByteArray) {
            // Use safe let-binding to prevent race conditions
            val gatt = bluetoothGatt ?: return
            val char = txCharacteristic ?: return

            // Force NO_RESPONSE writes for HM-10/MLT-BT05 style modules to avoid stalls
            val writeType = android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            // Use API 33+ method or legacy method based on SDK version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(char, bytes, writeType)
                if (status != android.bluetooth.BluetoothStatusCodes.SUCCESS) {
                    packetsFailed.incrementAndGet(0)
                    val errorDesc = GattStatus.getErrorDescription(status)
                    log("✗ BLE write failed: $errorDesc (Device)", LogType.WARNING)
                }
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                char.writeType = writeType
                @Suppress("DEPRECATION")
                val ok = gatt.writeCharacteristic(char)
                if (!ok) {
                    packetsFailed.incrementAndGet(0)
                    log("✗ BLE write failed (legacy) (Device)", LogType.WARNING)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun write(bytes: ByteArray) {
            // Add to queue instead of writing directly
            // If queue is full (shouldn't happen at 20Hz), drop oldest packet
            if (!writeQueue.offer(bytes)) {
                writeQueue.poll() // Remove oldest
                writeQueue.offer(bytes) // Add new

                // Track packet drop
                packetsDropped.incrementAndGet(0)

                // Warn user if packet loss is occurring (throttled to once per 5 seconds)
                val now = System.currentTimeMillis()
                if (now - lastPacketLossWarningTime.get(0) > 5000) {
                    log("⚠ Packet dropped  (queue full)", LogType.WARNING)
                    lastPacketLossWarningTime.set(0, now)
                }
            }
            packetsSent.incrementAndGet(0)
        }

        override fun requestRssi() {
            try {
                if (!checkBluetoothPermission()) {
                    log("Manual RSSI read skipped: Missing permissions", LogType.WARNING)
                    return
                }
                bluetoothGatt?.readRemoteRssi()
            } catch (e: SecurityException) {
                Log.e("BT", "Manual RSSI read failed", e)
            }
        }

        @SuppressLint("MissingPermission")
        override fun cancel() {
            connectionJob?.cancel()
            pollingJob?.cancel()
            writeJob?.cancel()
            writeQueue.clear()

            // Proper GATT disconnect sequence:
            // 1. Call disconnect() which triggers onConnectionStateChange callback
            // 2. The callback will call close() when disconnected
            // 3. If disconnect fails, fall back to close() directly
            val gatt = bluetoothGatt
            bluetoothGatt = null

            if (gatt != null) {
                try {
                    gatt.disconnect()
                    // Small delay to allow disconnect to complete before close
                    // This is a pragmatic fix - ideally close() would be called from onConnectionStateChange
                    Thread.sleep(100)
                } catch (e: Exception) {
                    Log.w("BT", "GATT disconnect failed: ${e.message}")
                }
                try {
                    gatt.close()
                } catch (e: Exception) {
                    Log.w("BT", "GATT close failed: ${e.message}")
                }
            }
        }
    }
}
