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

    // Active connections
    private val connections = arrayOfNulls<ConnectedThread>(2)
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
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                device?.let { addDevice(it, DeviceType.CLASSIC, rssi) }
            }
        }
    }

    init {
        context.registerReceiver(receiver, android.content.IntentFilter(BluetoothDevice.ACTION_FOUND))
        startReconnectMonitor()
    }

    private fun startReconnectMonitor() {
        scope.launch {
            while (isActive) {
                for (slot in 0..1) {
                    if (shouldReconnect[slot] && 
                        _connectionStates.value[slot] == ConnectionState.DISCONNECTED && 
                        savedDevices[slot] != null) {
                        
                        log("Auto-reconnecting to ${savedDevices[slot]?.name}...", LogType.WARNING)
                        updateConnectionState(slot, ConnectionState.RECONNECTING)
                        connectToDevice(savedDevices[slot]!!, slot, isAutoReconnect = true)
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

        val device = adapter?.getRemoteDevice(deviceModel.address) ?: return
        ConnectThread(device, slot).start()
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

    fun sendDataToAll(data: ByteArray) {
        connections.forEach { it?.write(data) }
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
        scope.cancel()
        disconnect(0)
        disconnect(1)
    }

    private inner class ConnectThread(private val device: BluetoothDevice, private val slot: Int) : Thread() {
        override fun run() {
            try {
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                
                updateConnectionState(slot, ConnectionState.CONNECTED)
                
                val thread = ConnectedThread(socket, slot)
                connections[slot] = thread
                thread.start()
            } catch (e: IOException) {
                Log.e("BT", "Connection failed", e)
                updateConnectionState(slot, ConnectionState.ERROR)
                log("Failed to connect to ${device.name}", LogType.ERROR)
                
                if (shouldReconnect[slot]) {
                    try { Thread.sleep(2000) } catch(e: InterruptedException) {}
                    updateConnectionState(slot, ConnectionState.DISCONNECTED)
                }
            }
        }
    }

    // Debug Mode Flag
    // Debug Mode Flag
    var isDebugMode: Boolean = com.metelci.ardunakon.BuildConfig.DEBUG

    // Telemetry State
    data class Telemetry(val batteryVoltage: Float, val status: String)
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private inner class ConnectedThread(private val socket: BluetoothSocket, private val slot: Int) : Thread() {
        private val outputStream: OutputStream = socket.outputStream
        private val inputStream: InputStream = socket.inputStream
        private val buffer = ByteArray(1024)

        override fun run() {
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
                    updateConnectionState(slot, ConnectionState.DISCONNECTED)
                    connections[slot] = null
                    break
                }
            }
        }
        
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

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e("BT", "Write failed", e)
            }
        }

        fun cancel() {
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
}
