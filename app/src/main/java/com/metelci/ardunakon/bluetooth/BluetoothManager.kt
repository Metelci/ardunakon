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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

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

    // Debug Logs
    private val _debugLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val debugLogs: StateFlow<List<LogEntry>> = _debugLogs.asStateFlow()

    // Incoming data from Arduino
    private val _incomingData = MutableStateFlow<ByteArray?>(null)
    val incomingData: StateFlow<ByteArray?> = _incomingData.asStateFlow()

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
    }

    // Active connections
    private val connections = arrayOfNulls<BluetoothConnection>(2)
    // Saved devices for auto-reconnect
    private val savedDevices = arrayOfNulls<BluetoothDeviceModel>(2)
    private val shouldReconnect = booleanArrayOf(false, false)

    private val leScanner by lazy { adapter?.bluetoothLeScanner }
    private val leScanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            result?.device?.let { addDevice(it, DeviceType.LE, result.rssi) }
        }
    }

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
                        if (shouldReconnect[slot] && 
                            _connectionStates.value[slot] == ConnectionState.DISCONNECTED && 
                            savedDevices[slot] != null) {
                            
                            log("Auto-reconnecting to ${savedDevices[slot]?.name}...", LogType.WARNING)
                            updateConnectionState(slot, ConnectionState.RECONNECTING)
                            connectToDevice(savedDevices[slot]!!, slot, isAutoReconnect = true)
                        }
                    }
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    fun startScan() {
        if (!checkBluetoothPermission()) {
            log("Scan failed: Missing permissions", LogType.ERROR)
            return
        }
        if (adapter == null || !adapter.isEnabled) return
        _scannedDevices.value = emptyList()
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
            leScanner?.stopScan(leScanCallback)
            leScanner?.startScan(leScanCallback)
            scope.launch { delay(10000); stopScan() }
        } catch (e: SecurityException) {
            Log.e("BT", "Permission missing for scan", e)
            log("Scan failed: Permission missing", LogType.ERROR)
        } catch (e: Exception) {
            Log.e("BT", "Scan failed", e)
        }
    }

    fun stopScan() {
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
        
        // Save for reconnect
        savedDevices[slot] = deviceModel
        shouldReconnect[slot] = true

        if (!isAutoReconnect) {
            updateConnectionState(slot, ConnectionState.CONNECTING)
            log("Connecting to ${deviceModel.name}...", LogType.INFO)
        }
        stopScan()

        // Ensure previous connection is closed
        connections[slot]?.cancel()
        connections[slot] = null

        val device = adapter?.getRemoteDevice(deviceModel.address) ?: return
        
        if (deviceModel.type == DeviceType.LE) {
            val bleConnection = BleConnection(device, slot)
            connections[slot] = bleConnection
            bleConnection.connect()
        } else {
            ConnectThread(device, slot).start()
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
        val list = _connectionStates.value.toMutableList()
        list[slot] = state
        _connectionStates.value = list
        
        when(state) {
            ConnectionState.CONNECTED -> log("Connected to Slot ${slot + 1}!", LogType.SUCCESS)
            ConnectionState.ERROR -> log("Connection error on Slot ${slot + 1}", LogType.ERROR)
            else -> {}
        }
    }

    private fun updateRssi(slot: Int, rssi: Int) {
        val list = _rssiValues.value.toMutableList()
        list[slot] = rssi
        _rssiValues.value = list
    }

    fun cleanup() {
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
        // Example Mapping: [START, TYPE, BAT_H, BAT_L, STAT, ..., END]
        // This is a placeholder for the actual protocol
        // Assuming Byte 2 is Battery Voltage (scaled x10)
        val batteryRaw = packet[2].toInt() and 0xFF
        val statusByte = packet[3].toInt() and 0xFF
        
        // Validation: Ignore noise
        // Battery: 0-255 (0-25.5V) is physically possible, but let's filter obvious junk if needed.
        // Status: Must be 0 or 1
        if (statusByte > 1) return 

        val battery = batteryRaw / 10f
        val status = if (statusByte == 1) "Safe Mode" else "Active"
        
        _telemetry.value = Telemetry(battery, status)
        if (isDebugMode) {
            log("Telemetry: Bat=${battery}V, Stat=$status", LogType.SUCCESS)
        }
    }

    private inner class ConnectThread(private val device: BluetoothDevice, private val slot: Int) : Thread() {
        private var socket: BluetoothSocket? = null

        @SuppressLint("MissingPermission")
        override fun run() {

            if (!checkBluetoothPermission()) {
                log("Connect failed: Missing permissions", LogType.ERROR)
                updateConnectionState(slot, ConnectionState.ERROR)
                return
            }

            adapter?.cancelDiscovery()

            log("Starting connection to ${device.name} (${device.address})", LogType.INFO)
            log("Using SPP UUID: $SPP_UUID", LogType.INFO)

            var connected = false
            
            // Attempt 1: Insecure Connection (Primary for HC-06)
            try {
                log("Attempting INSECURE connection...", LogType.INFO)
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                connected = true
                log("Insecure connection established.", LogType.SUCCESS)
            } catch (e: Exception) {
                Log.w("BT", "Insecure connection failed", e)
                log("Insecure connection failed: ${e.message}", LogType.WARNING)
                try { socket?.close() } catch (e2: Exception) {}
            }

            // Attempt 2: Reflection Method (Port 1 Hack) - Fallback for stubborn HC-06
            if (!connected) {
                try {
                    // Give the stack time to reset
                    try { Thread.sleep(500) } catch (e: InterruptedException) {}

                    log("Attempting REFLECTION connection (Port 1)...", LogType.WARNING)
                    val m: java.lang.reflect.Method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = m.invoke(device, 1) as BluetoothSocket
                    socket?.connect()
                    connected = true
                    log("Reflection connection established.", LogType.SUCCESS)
                } catch (e: Exception) {
                    Log.e("BT", "Reflection connection failed", e)
                    log("Reflection connection failed: ${e.message}", LogType.ERROR)
                    try { socket?.close() } catch (e2: Exception) {}
                }
            }

            if (connected && socket != null) {
                // Connection successful
                val connectedThread = ConnectedThread(socket!!, slot)
                connections[slot] = connectedThread
                connectedThread.start()

                updateConnectionState(slot, ConnectionState.CONNECTED)
            } else {
                log("Connect failed: Could not establish socket to ${device.name}", LogType.ERROR)
                updateConnectionState(slot, ConnectionState.ERROR)
            }
        }

        fun cancel() {
            try { socket?.close() } catch (e: Exception) {}
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
                        
                        // Log raw data only in Debug Mode
                        if (isDebugMode) {
                            val hex = data.joinToString("") { "%02X".format(it) }
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

        // HM-10 / HC-08 Default UUIDs
        private val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

        // Client Characteristic Configuration Descriptor (CCCD) for notifications
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        private var connectionJob: Job? = null

        @SuppressLint("MissingPermission")
        fun connect() {
            log("Connecting to BLE device ${device.name}...", LogType.INFO)
            connectGattWithTimeout()
        }

        @SuppressLint("MissingPermission")
        private fun connectGattWithTimeout() {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            
            // Timeout logic
            connectionJob = scope.launch {
                delay(10000) // 10 second timeout
                if (connections[slot] == this@BleConnection && 
                    _connectionStates.value[slot] != ConnectionState.CONNECTED) {
                    log("BLE Connection timed out. Retrying...", LogType.WARNING)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    // Simple retry once or switch to autoConnect=true could be better, 
                    // but for now let's just try one more direct connect after a brief pause
                    delay(500)
                    bluetoothGatt = device.connectGatt(context, false, gattCallback)
                }
            }
        }

        private fun startRssiPolling() {
            scope.launch {
                while (isActive && bluetoothGatt != null) {
                    delay(2000) // Poll every 2 seconds
                    try {
                        bluetoothGatt?.readRemoteRssi()
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
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    log("BLE Connected to GATT server.", LogType.SUCCESS)
                    updateConnectionState(slot, ConnectionState.CONNECTED)
                    // Discover services
                    gatt.discoverServices()
                    startRssiPolling()
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    log("BLE Disconnected from GATT server.", LogType.WARNING)
                    updateConnectionState(slot, ConnectionState.DISCONNECTED)
                    connections[slot] = null
                    gatt.close() // Critical: Prevent resource leak
                    updateRssi(slot, 0)
                }
            }

            override fun onReadRemoteRssi(gatt: android.bluetooth.BluetoothGatt?, rssi: Int, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    updateRssi(slot, rssi)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERVICE_UUID)
                    if (service != null) {
                        txCharacteristic = service.getCharacteristic(CHAR_UUID)
                        if (txCharacteristic != null) {
                            log("BLE Service & Characteristic found.", LogType.SUCCESS)
                            
                            // Enable Notifications
                            gatt.setCharacteristicNotification(txCharacteristic, true)
                            val descriptor = txCharacteristic!!.getDescriptor(CCCD_UUID)
                            if (descriptor != null) {
                                descriptor.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        } else {
                            log("BLE Characteristic not found!", LogType.ERROR)
                        }
                    } else {
                        log("BLE Service not found!", LogType.ERROR)
                    }
                } else {
                    log("BLE Service discovery failed: $status", LogType.ERROR)
                }
            }

            override fun onCharacteristicChanged(gatt: android.bluetooth.BluetoothGatt, characteristic: android.bluetooth.BluetoothGattCharacteristic) {
                // Incoming data
                val data = characteristic.value
                if (data != null && data.isNotEmpty()) {
                    try {
                        parseTelemetry(data)
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun write(bytes: ByteArray) {
            if (bluetoothGatt != null && txCharacteristic != null) {
                txCharacteristic!!.value = bytes
                // Write type: NO_RESPONSE is faster and usually standard for UART
                txCharacteristic!!.writeType = android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                bluetoothGatt!!.writeCharacteristic(txCharacteristic)
            }
        }

        @SuppressLint("MissingPermission")
        override fun cancel() {
            connectionJob?.cancel()
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
    }
}
