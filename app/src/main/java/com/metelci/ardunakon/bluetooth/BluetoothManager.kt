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

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceModel>> = _scannedDevices.asStateFlow()

    private val _connectionStates = MutableStateFlow<List<ConnectionState>>(listOf(ConnectionState.DISCONNECTED, ConnectionState.DISCONNECTED))
    val connectionStates: StateFlow<List<ConnectionState>> = _connectionStates.asStateFlow()

    private val _rssiValues = MutableStateFlow<List<Int>>(listOf(0, 0))
    val rssiValues: StateFlow<List<Int>> = _rssiValues.asStateFlow()

    private val _health = MutableStateFlow<List<ConnectionHealth>>(listOf(ConnectionHealth(), ConnectionHealth()))
    val health: StateFlow<List<ConnectionHealth>> = _health.asStateFlow()

    // Debug Logs
    private val _debugLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val debugLogs: StateFlow<List<LogEntry>> = _debugLogs.asStateFlow()

    // Incoming data from Arduino
    private val _incomingData = MutableStateFlow<ByteArray?>(null)
    val incomingData: StateFlow<ByteArray?> = _incomingData.asStateFlow()

    private var keepAliveJob: Job? = null

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

    // Active connections
    private val connections = arrayOfNulls<BluetoothConnection>(2)
    // Saved devices for auto-reconnect
    private val savedDevices = arrayOfNulls<BluetoothDeviceModel>(2)
    private val connectionTypes = arrayOfNulls<DeviceType>(2)
    private val shouldReconnect = booleanArrayOf(false, false)
    // Connection mutex to prevent concurrent connection attempts
    private val connectionMutex = arrayOf(kotlinx.coroutines.sync.Mutex(), kotlinx.coroutines.sync.Mutex())
    private val lastStateChangeAt = longArrayOf(0L, 0L)
    private val rssiFailures = intArrayOf(0, 0)
    private val heartbeatSeq = intArrayOf(0, 0)
    private val lastHeartbeatSentAt = longArrayOf(0L, 0L)
    private val lastPacketAt = longArrayOf(0L, 0L)
    private val lastRttMs = longArrayOf(0L, 0L)
    private val heartbeatTimeoutMs = 12000L
    private val missedHeartbeatAcks = intArrayOf(0, 0)

    private val leScanner by lazy { adapter?.bluetoothLeScanner }
    private val leScanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            result?.device?.let { addDevice(it, DeviceType.LE, result.rssi) }
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
                device?.let { addDevice(it, DeviceType.CLASSIC, rssi) }
            }
        }
    }

    init {
        context.registerReceiver(receiver, android.content.IntentFilter(BluetoothDevice.ACTION_FOUND))
        startReconnectMonitor()
        startKeepAlivePings()
    }

    // E-Stop State
    private val _isEmergencyStopActive = MutableStateFlow(false)
    val isEmergencyStopActive: StateFlow<Boolean> = _isEmergencyStopActive.asStateFlow()

    fun setEmergencyStop(active: Boolean) {
        _isEmergencyStopActive.value = active
        if (active) {
            log("E-STOP ACTIVATED: Blocking connections", LogType.WARNING)
        } else {
            log("E-STOP RELEASED: Connections allowed", LogType.SUCCESS)
        }
    }

    private fun startReconnectMonitor() {
        scope.launch {
            while (isActive) {
                if (!_isEmergencyStopActive.value) {
                    for (slot in 0..1) {
                        val currentState = _connectionStates.value[slot]
                        // Reconnect if disconnected OR in error state (failed connection attempt)
                        if (shouldReconnect[slot] &&
                            (currentState == ConnectionState.DISCONNECTED || currentState == ConnectionState.ERROR) &&
                            savedDevices[slot] != null) {

                            log("Auto-reconnecting to ${savedDevices[slot]?.name}...", LogType.WARNING)
                            updateConnectionState(slot, ConnectionState.RECONNECTING)
                            connectToDevice(savedDevices[slot]!!, slot, isAutoReconnect = true)
                        }
                    }
                }
                delay(3000) // Check every 3 seconds for fast recovery
            }
        }
    }

    private fun recordInbound(slot: Int) {
        val now = System.currentTimeMillis()
        lastPacketAt[slot] = now
        rssiFailures[slot] = 0
        missedHeartbeatAcks[slot] = 0
        if (lastHeartbeatSentAt[slot] > 0 && now >= lastHeartbeatSentAt[slot]) {
            lastRttMs[slot] = now - lastHeartbeatSentAt[slot]
        }
        updateHealth(slot, heartbeatSeq[slot], lastPacketAt[slot], rssiFailures[slot])
    }

    private fun startKeepAlivePings() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(4000)
                val states = _connectionStates.value
                states.forEachIndexed { index, state ->
                    if (state == ConnectionState.CONNECTED && connections[index] != null) {
                        heartbeatSeq[index] = (heartbeatSeq[index] + 1) and 0xFFFF
                        val heartbeat = ProtocolManager.formatHeartbeatData(heartbeatSeq[index])
                        // Force bypasses E-STOP so the link itself stays alive
                        sendDataToSlot(heartbeat, index, force = true)
                        lastHeartbeatSentAt[index] = System.currentTimeMillis()
                        missedHeartbeatAcks[index]++
                        updateHealth(index, heartbeatSeq[index], lastPacketAt[index], rssiFailures[index])

                        // Require multiple missed acks before forcing reconnect
                        val sinceLastPacket = System.currentTimeMillis() - lastPacketAt[index]
                        if (missedHeartbeatAcks[index] >= 3 && lastPacketAt[index] > 0 && sinceLastPacket > heartbeatTimeoutMs) {
                            log("Heartbeat timeout on Slot ${index + 1} after ${sinceLastPacket}ms (missed ${missedHeartbeatAcks[index]} acks)", LogType.ERROR)
                            missedHeartbeatAcks[index] = 0
                            forceReconnect(index, "Heartbeat timeout")
                        }
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
        scanJob?.cancel()
        adapter?.cancelDiscovery()
        try { leScanner?.stopScan(leScanCallback) } catch (e: Exception) {}
    }

    fun addDevice(device: BluetoothDevice, type: DeviceType, rssi: Int) {
        val list = _scannedDevices.value.toMutableList()
        if (list.none { it.address == device.address }) {
            val name = if (checkBluetoothPermission()) device.name else "Unknown"
            list.add(BluetoothDeviceModel(name ?: device.address, device.address, type, rssi))
            _scannedDevices.value = list
        }
    }

    fun connectToDevice(deviceModel: BluetoothDeviceModel, slot: Int, isAutoReconnect: Boolean = false) {
        if (slot !in 0..1) return
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

        // Check if connection is already in progress
        scope.launch {
            if (!connectionMutex[slot].tryLock()) {
                log("Connection already in progress for Slot ${slot + 1}", LogType.WARNING)
                return@launch
            }

            // Save for reconnect
            savedDevices[slot] = deviceModel
            connectionTypes[slot] = deviceModel.type
            shouldReconnect[slot] = true
            // Seed RSSI with last known scan value for immediate UI feedback
            updateRssi(slot, deviceModel.rssi)

            if (!isAutoReconnect) {
                updateConnectionState(slot, ConnectionState.CONNECTING)
                log("Connecting to ${deviceModel.name}...", LogType.INFO)
            }

            // Cancel discovery BEFORE starting connection - discovery interferes with connection
            try {
                adapter?.cancelDiscovery()
            } catch (e: SecurityException) {
                log("Could not cancel discovery: Missing permission", LogType.WARNING)
            }
            stopScan()

            // Ensure previous connection is closed
            connections[slot]?.cancel()
            connections[slot] = null

            // Give BT stack time to clean up (minimal delay for fast reconnection)
            delay(200)

            val device = adapter?.getRemoteDevice(deviceModel.address)
            if (device == null) {
                connectionMutex[slot].unlock()
                return@launch
            }

            if (deviceModel.type == DeviceType.LE) {
                val bleConnection = BleConnection(device, slot)
                connections[slot] = bleConnection
                bleConnection.connect()
                // Note: BLE connection unlocks mutex when connection completes
            } else {
                // ConnectThread will unlock the mutex when it finishes (success or failure)
                ConnectThread(device, slot).start()
            }
        }
    }

    fun disconnect(slot: Int) {
        if (slot in 0..1) {
            shouldReconnect[slot] = false
            connections[slot]?.cancel()
            connections[slot] = null
            updateConnectionState(slot, ConnectionState.DISCONNECTED)
            updateRssi(slot, 0)
            log("Disconnected from Slot ${slot + 1}", LogType.INFO)
        }
    }

    fun sendDataToAll(data: ByteArray, force: Boolean = false) {
        if (_isEmergencyStopActive.value && !force) return
        connections.forEach { it?.write(data) }
    }

    fun sendDataToSlot(data: ByteArray, slot: Int, force: Boolean = false) {
        if (_isEmergencyStopActive.value && !force) return
        if (slot in 0..1) {
            connections[slot]?.write(data)
        }
    }

    fun requestRssi(slot: Int) {
        if (slot !in 0..1) return
        if (connectionTypes[slot] != DeviceType.LE) {
            log("RSSI refresh not supported for classic devices", LogType.WARNING)
            return
        }
        connections[slot]?.requestRssi()
    }

    fun reconnectSavedDevices(): Boolean {
        var started = false
        for (slot in 0..1) {
            val device = savedDevices[slot]
            val state = _connectionStates.value[slot]
            if (device != null && state != ConnectionState.CONNECTED && state != ConnectionState.CONNECTING) {
                log("Manually reconnecting to ${device.name} (Slot ${slot + 1})", LogType.INFO)
                connectToDevice(device, slot, isAutoReconnect = true)
                started = true
            }
        }
        if (!started) {
            log("No saved devices to reconnect", LogType.WARNING)
        }
        return started
    }

    private fun updateConnectionState(slot: Int, state: ConnectionState) {
        val now = System.currentTimeMillis()
        // Debounce noisy DISCONNECTED/ERROR flips to avoid UI spam
        val isNoisyState = state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR
        if (isNoisyState && (now - lastStateChangeAt[slot]) < 800) {
            return
        }
        lastStateChangeAt[slot] = now
        val list = _connectionStates.value.toMutableList()
        list[slot] = state
        _connectionStates.value = list
        
        when(state) {
            ConnectionState.CONNECTED -> {
                log("Connected to Slot ${slot + 1}!", LogType.SUCCESS)
                vibrate(200) // Long vibration for connection
                lastPacketAt[slot] = now
                updateHealth(slot, heartbeatSeq[slot], lastPacketAt[slot], rssiFailures[slot])
            }
            ConnectionState.DISCONNECTED -> {
                // Only vibrate if it was previously connected or connecting (avoid noise on startup)
                vibrate(100) // Short vibration for disconnection
            }
            ConnectionState.ERROR -> {
                log("Connection error on Slot ${slot + 1}", LogType.ERROR)
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

    private fun updateRssi(slot: Int, rssi: Int) {
        val list = _rssiValues.value.toMutableList()
        list[slot] = rssi
        _rssiValues.value = list
        updateHealth(slot, heartbeatSeq[slot], lastPacketAt[slot], rssiFailures[slot])
    }

    private fun updateHealth(slot: Int, seq: Int, packetAt: Long, failures: Int) {
        val current = _health.value.toMutableList()
        current[slot] = ConnectionHealth(packetAt, failures, seq, lastHeartbeatSentAt[slot], lastRttMs[slot])
        _health.value = current
    }

    private fun forceReconnect(slot: Int, reason: String) {
        log("Reconnecting Slot ${slot + 1}: $reason", LogType.WARNING)
        connections[slot]?.cancel()
        connections[slot] = null
        if (connectionMutex[slot].isLocked) {
            connectionMutex[slot].unlock()
        }
        updateConnectionState(slot, ConnectionState.ERROR)
        updateRssi(slot, 0)
        if (savedDevices[slot] != null) {
            connectToDevice(savedDevices[slot]!!, slot, isAutoReconnect = true)
        }
    }

    fun cleanup() {
        // Cancel the coroutine scope to prevent leaks
        scope.cancel()

        // Cancel scan job if active
        scanJob?.cancel()
        keepAliveJob?.cancel()

        // Disconnect all active connections
        for (i in 0..1) {
            connections[i]?.cancel()
            connections[i] = null
        }

        // Unregister broadcast receiver
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered or already unregistered
        }
    }

    private val isDebugMode = com.metelci.ardunakon.BuildConfig.DEBUG

    // Telemetry State
    data class Telemetry(val batteryVoltage: Float, val status: String)
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private fun parseTelemetry(packet: ByteArray) {
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
                log("Invalid telemetry: voltage ${battery}V out of range", LogType.WARNING)
            }
            return
        }

        val status = if (statusByte == 1) "Safe Mode" else "Active"

        _telemetry.value = Telemetry(battery, status)
        if (isDebugMode) {
            log("Telemetry: Bat=${battery}V, Stat=$status", LogType.SUCCESS)
        }
    }

    private inner class ConnectThread(private val device: BluetoothDevice, private val slot: Int) : Thread() {
        private var socket: BluetoothSocket? = null
        @Volatile private var cancelled = false

        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                if (!checkBluetoothPermission()) {
                    log("Connect failed: Missing permissions", LogType.ERROR)
                    updateConnectionState(slot, ConnectionState.ERROR)
                    return
                }

                log("Starting connection to ${device.name} (${device.address})", LogType.INFO)

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

                // HC-06 Connection Strategy:
                // Most HC-06 modules work with INSECURE connection using Standard SPP UUID
                // or Reflection Method (Port 1). We focus on these two methods first.

                // Attempt 1: Standard SPP UUID with INSECURE connection (most reliable for HC-06)
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
                        // HC-06 clones need longer delay to recover from failed attempts
                        if (!cancelled) safeSleep(2000)
                    }
                }

                // Attempt 2: Reflection Method (Port 1) - Most reliable fallback for HC-06
                if (!connected && !cancelled) {
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
                        if (!cancelled) safeSleep(2000)
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
                            if (!cancelled) safeSleep(1500)
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
                            if (!cancelled) safeSleep(1000)
                        }
                    }
                }

                // Attempt 5: Last resort - Try SECURE connection with Standard SPP
                // Some modules (rare HC-05 variants) require secure pairing
                if (!connected && !cancelled) {
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
                    log("Connection attempt cancelled for ${device.name}", LogType.WARNING)
                    updateConnectionState(slot, ConnectionState.DISCONNECTED)
                    return
                }

                if (connected && socket != null) {
                    // Connection successful
                    val connectedThread = ConnectedThread(socket!!, slot)
                    connections[slot] = connectedThread
                    connectedThread.start()

                    updateConnectionState(slot, ConnectionState.CONNECTED)
                } else {
                    closeSocketSafely(socket)
                    log("============================================", LogType.ERROR)
                    log("ALL CONNECTION METHODS FAILED for ${device.name}", LogType.ERROR)
                    log("Tried: SPP, Reflection Ports 1-3, 12 UUIDs, Secure SPP", LogType.ERROR)
                    log("Module may be defective or incompatible", LogType.ERROR)
                    log("See HC06_TROUBLESHOOTING.md for help", LogType.ERROR)
                    log("============================================", LogType.ERROR)
                    updateConnectionState(slot, ConnectionState.ERROR)
                }
            } finally {
                // Always unlock the mutex when connection attempt completes (success or failure)
                connectionMutex[slot].unlock()
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
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket, private val slot: Int) : Thread(), BluetoothConnection {
        private val outputStream: OutputStream = socket.outputStream
        private val inputStream: InputStream = socket.inputStream
        private val buffer = ByteArray(1024)

        override fun run() {
            log("Socket opened for Slot ${slot + 1}", LogType.SUCCESS)
            val packetBuffer = ByteArray(20) // Small buffer for packet assembly
            var bufferIndex = 0

            while (true) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        _incomingData.value = data
                        recordInbound(slot)

                        // Try to decode as text first for terminal display
                        val decodedText = try {
                            String(data, Charsets.UTF_8).trim()
                        } catch (e: Exception) {
                            null
                        }

                        // Log incoming data - always log for terminal visibility
                        if (decodedText != null && decodedText.isNotEmpty() && decodedText.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r" }) {
                            // Looks like text - display as string
                            log("RX Slot ${slot + 1}: $decodedText", LogType.INFO)
                        } else {
                            // Binary data - display as hex
                            val hex = data.joinToString(" ") { "%02X".format(it) }
                            log("RX Slot ${slot + 1}: $hex", LogType.INFO)
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
                    Log.e("BT", "Disconnected Slot $slot", e)
                    log("Disconnected Slot ${slot + 1}: ${e.message}", LogType.ERROR)
                    updateConnectionState(slot, ConnectionState.DISCONNECTED)
                    connections[slot] = null
                    break
                }
            }
        }
        


        override fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e("BT", "Write failed", e)
                log("Write failed Slot ${slot + 1}: ${e.message}", LogType.ERROR)
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
    private inner class BleConnection(private val device: BluetoothDevice, private val slot: Int) : BluetoothConnection {
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

        // Client Characteristic Configuration Descriptor (CCCD) for notifications - standard
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        // Track which variant was successful
        private var detectedVariant: Int = 0

        private var connectionJob: Job? = null
        private var pollingJob: Job? = null

        // Write queue management - prevent write collisions
        private val writeQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
        private var writeJob: Job? = null

        @SuppressLint("MissingPermission")
        fun connect() {
            log("Connecting to BLE device ${device.name}...", LogType.INFO)
            connectGattWithTimeout()
        }

        @SuppressLint("MissingPermission")
        private fun connectGattWithTimeout() {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            // Timeout logic with retry
            connectionJob = scope.launch {
                delay(10000) // 10 second timeout
                if (connections[slot] == this@BleConnection &&
                    _connectionStates.value[slot] != ConnectionState.CONNECTED) {
                    log("BLE Connection timed out. Retrying once...", LogType.WARNING)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    delay(500)

                    // Retry once
                    bluetoothGatt = device.connectGatt(context, false, gattCallback)

                    // Second timeout - if this fails, mark as ERROR for auto-reconnect
                    delay(10000)
                    if (connections[slot] == this@BleConnection &&
                        _connectionStates.value[slot] != ConnectionState.CONNECTED) {
                        log("BLE Connection failed after retry", LogType.ERROR)
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        updateConnectionState(slot, ConnectionState.ERROR)
                        connections[slot] = null
                        connectionMutex[slot].unlock()
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
                        if (connections[slot] == this@BleConnection && _connectionStates.value[slot] == ConnectionState.CONNECTED) {
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
                    log("BLE GATT operation failed with status: $status", LogType.ERROR)
                    updateConnectionState(slot, ConnectionState.ERROR)
                    pollingJob?.cancel()
                    connections[slot] = null
                    gatt.close()
                    updateRssi(slot, 0)
                    // Unlock mutex on failure
                    connectionMutex[slot].unlock()
                    return
                }

                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    log("BLE Connected to GATT server.", LogType.SUCCESS)
                    updateConnectionState(slot, ConnectionState.CONNECTED)

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
                    connectionMutex[slot].unlock()
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    pollingJob?.cancel()
                    log("BLE Disconnected from GATT server.", LogType.WARNING)
                    updateConnectionState(slot, ConnectionState.DISCONNECTED)
                    connections[slot] = null
                    gatt.close() // Critical: Prevent resource leak
                    updateRssi(slot, 0)
                    // Unlock mutex on disconnection (if it was locked during connection attempt)
                    if (connectionMutex[slot].isLocked) {
                        connectionMutex[slot].unlock()
                    }
                }
            }

            override fun onReadRemoteRssi(gatt: android.bluetooth.BluetoothGatt?, rssi: Int, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    updateRssi(slot, rssi)
                    rssiFailures[slot] = 0
                    updateHealth(slot, heartbeatSeq[slot], lastPacketAt[slot], rssiFailures[slot])
                } else {
                    // Only enforce RSSI-based reconnects for BLE devices
                    if (connectionTypes[slot] == DeviceType.LE) {
                        rssiFailures[slot] = (rssiFailures[slot] + 1).coerceAtMost(10)
                        updateHealth(slot, heartbeatSeq[slot], lastPacketAt[slot], rssiFailures[slot])
                        if (rssiFailures[slot] >= 3) {
                            log("RSSI read failures on Slot ${slot + 1}: ${rssiFailures[slot]}", LogType.WARNING)
                        }
                        if (rssiFailures[slot] >= 5) {
                            forceReconnect(slot, "RSSI polling failed ${rssiFailures[slot]} times")
                        }
                    }
                }
            }

            override fun onMtuChanged(gatt: android.bluetooth.BluetoothGatt?, mtu: Int, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    log("BLE MTU changed to $mtu bytes (effective payload: ${mtu - 3} bytes)", LogType.SUCCESS)
                } else {
                    log("BLE MTU negotiation failed, using default 23 bytes", LogType.WARNING)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
                if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    log("BLE Service discovery failed: $status", LogType.ERROR)
                    return
                }

                // Try all UUID variants to find the correct one for this module
                var serviceFound = false

                // Variant 1: Standard HC-08/HM-10 (FFE0/FFE1) - Most common
                // Also check for AT-09 variant (FFE0/FFE4) which shares the same service UUID
                var service = gatt.getService(SERVICE_UUID_V1)
                if (service != null) {
                    // Try standard HM-10 characteristic first (FFE1)
                    var char = service.getCharacteristic(CHAR_UUID_V1)
                    if (char != null) {
                        txCharacteristic = char
                        detectedVariant = 1
                        serviceFound = true
                        log("BLE Module detected: Standard HC-08/HM-10 (FFE0/FFE1)", LogType.SUCCESS)
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
                                gatt.setCharacteristicNotification(rxChar, true)
                                val rxDescriptor = rxChar.getDescriptor(CCCD_UUID)
                                if (rxDescriptor != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        gatt.writeDescriptor(rxDescriptor, android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        rxDescriptor.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                        @Suppress("DEPRECATION")
                                        gatt.writeDescriptor(rxDescriptor)
                                    }
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

                // Variant 9: ArduinoBLE Library Standard Example (Uno R4 Wifi / Nano 33 IoT)
                if (!serviceFound) {
                    service = gatt.getService(SERVICE_UUID_V9)
                    if (service != null) {
                        val char = service.getCharacteristic(CHAR_UUID_V9)
                        if (char != null) {
                            txCharacteristic = char
                            detectedVariant = 9
                            serviceFound = true
                            log("BLE Module detected: Arduino R4 Wifi / Nano 33 IoT", LogType.SUCCESS)
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
                                val hasWrite = (props and (android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE or android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                val hasNotify = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                                val hasIndicate = (props and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

                                if (hasWrite && writeChar == null) writeChar = char
                                if ((hasNotify || hasIndicate) && notifyChar == null) notifyChar = char
                            }

                            if (writeChar != null && notifyChar != null) {
                                txCharacteristic = writeChar
                                detectedVariant = 8 // Generic Split
                                serviceFound = true
                                val type = if ((notifyChar.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) "Notify" else "Indicate"
                                log("BLE Module detected: Generic Split (TX: ${writeChar.uuid}, RX: ${notifyChar.uuid}) [$type]", LogType.SUCCESS)

                                // Enable notifications/indications on the separate RX characteristic
                                val enableIndication = (notifyChar.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                                gatt.setCharacteristicNotification(notifyChar, true)
                                val rxDescriptor = notifyChar.getDescriptor(CCCD_UUID)
                                if (rxDescriptor != null) {
                                    val descriptorValue = if (enableIndication) 
                                        android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE 
                                    else 
                                        android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                        
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        gatt.writeDescriptor(rxDescriptor, descriptorValue)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        rxDescriptor.value = descriptorValue
                                        @Suppress("DEPRECATION")
                                        gatt.writeDescriptor(rxDescriptor)
                                    }
                                }
                            }
                        }
                    }
                }

                if (serviceFound && txCharacteristic != null) {
                    // Start write queue processor
                    startWriteQueue()

                    // Enable Notifications on TX characteristic (except Nordic and Generic Split which were handled above)
                    if (detectedVariant != 2 && detectedVariant != 8) {
                        // Check if we need to enable Indication or Notification
                        val enableIndication = (txCharacteristic!!.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                        
                        gatt.setCharacteristicNotification(txCharacteristic, true)
                        val descriptor = txCharacteristic!!.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            val descriptorValue = if (enableIndication) 
                                android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE 
                            else 
                                android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                            // Use API 33+ method or legacy method based on SDK version
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(descriptor, descriptorValue)
                            } else {
                                @Suppress("DEPRECATION")
                                descriptor.value = descriptorValue
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                } else {
                    log("BLE Service/Characteristic not found! Module may use unsupported UUIDs.", LogType.ERROR)
                    // Log all available services for debugging
                    gatt.services?.forEach { svc ->
                        log("Available service: ${svc.uuid}", LogType.INFO)
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
                    recordInbound(slot)

                    // Try to decode as text first for terminal display
                    val decodedText = try {
                        String(data, Charsets.UTF_8).trim()
                    } catch (e: Exception) {
                        null
                    }

                    // Log incoming data - always log for terminal visibility
                    if (decodedText != null && decodedText.isNotEmpty() && decodedText.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r" }) {
                        // Looks like text - display as string
                        log("RX Slot ${slot + 1}: $decodedText", LogType.INFO)
                    } else {
                        // Binary data - display as hex
                        val hex = data.joinToString(" ") { "%02X".format(it) }
                        log("RX Slot ${slot + 1}: $hex", LogType.INFO)
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
                    recordInbound(slot)

                    // Try to decode as text first for terminal display
                    val decodedText = try {
                        String(value, Charsets.UTF_8).trim()
                    } catch (e: Exception) {
                        null
                    }

                    // Log incoming data - always log for terminal visibility
                    if (decodedText != null && decodedText.isNotEmpty() && decodedText.all { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r" }) {
                        // Looks like text - display as string
                        log("RX Slot ${slot + 1}: $decodedText", LogType.INFO)
                    } else {
                        // Binary data - display as hex
                        val hex = value.joinToString(" ") { "%02X".format(it) }
                        log("RX Slot ${slot + 1}: $hex", LogType.INFO)
                    }

                    try {
                        parseTelemetry(value)
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }
        }

        private fun startWriteQueue() {
            writeJob?.cancel()
            writeJob = scope.launch {
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
                        delay(10) // Extra 10ms buffer for safety
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

            // Check if characteristic supports WRITE_NO_RESPONSE
            val writeType = if ((char.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }

            // Use API 33+ method or legacy method based on SDK version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, bytes, writeType)
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                char.writeType = writeType
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        }

        @SuppressLint("MissingPermission")
        override fun write(bytes: ByteArray) {
            // Add to queue instead of writing directly
            // If queue is full (shouldn't happen at 20Hz), drop oldest packet
            if (!writeQueue.offer(bytes)) {
                writeQueue.poll() // Remove oldest
                writeQueue.offer(bytes) // Add new
            }
        }

        override fun requestRssi() {
            try {
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
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
    }
}
