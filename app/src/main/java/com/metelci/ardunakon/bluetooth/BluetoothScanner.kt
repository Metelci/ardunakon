package com.metelci.ardunakon.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.metelci.ardunakon.data.DeviceNameCache
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bluetooth Scanner - Handles device discovery for both Classic and BLE
 *
 * Responsibilities:
 * - Classic Bluetooth discovery via BroadcastReceiver
 * - BLE scanning via BluetoothLeScanner
 * - Device list management with deduplication
 * - Name resolution with multi-layer fallback
 * - Scan timeout management
 */
class BluetoothScanner(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
    private val scope: CoroutineScope,
    private val callbacks: ScannerCallbacks,
    private val cryptoEngine: CryptoEngine = SecurityManager()
) {
    /**
     * Callbacks for scanner events
     */
    interface ScannerCallbacks {
        fun onDeviceFound(device: BluetoothDeviceModel)
        fun onDeviceUpdated(device: BluetoothDeviceModel)
        fun onScanLog(message: String, type: LogType)
    }

    // Scanned devices list
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceModel>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Device name cache for resolved names
    private val deviceNameCache = DeviceNameCache(context, cryptoEngine)

    // Scan timeout job
    private var scanJob: Job? = null

    // BLE Scanner
    private val leScanner by lazy { adapter?.bluetoothLeScanner }

    // BLE Scan callback
    private val leScanCallback = object : android.bluetooth.le.ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            if (result == null || result.device == null) return
            if (!checkBluetoothPermission()) return
            val device = result.device
            val isNew = addDevice(device, DeviceType.LE, result.rssi)

            if (isNew) {
                val record = result.scanRecord
                val sb = StringBuilder()
                val name = safeGetDeviceName(device) ?: "Unknown"
                val address = safeGetDeviceAddress(device) ?: "Unknown"
                sb.append("Found LE Device: $name ($address)\n")
                sb.append("  RSSI: ${result.rssi} dBm\n")
                sb.append("  Type: ${device.type} (0=Unknown, 1=Classic, 2=LE, 3=Dual)\n")

                if (record != null) {
                    sb.append("  Adv Flags: ${record.advertiseFlags}\n")
                    sb.append("  Local Name: ${record.deviceName ?: "N/A"}\n")
                    sb.append("  Service UUIDs: ${record.serviceUuids?.joinToString(", ") ?: "None"}")
                    sb.append("\n  Connectable: ${result.isConnectable}")
                }
                callbacks.onScanLog(sb.toString(), LogType.INFO)
            }
        }
    }

    // Classic Bluetooth receiver
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                if (!checkBluetoothPermission()) return
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
                        val name = safeGetDeviceName(device) ?: "Unknown"
                        val address = safeGetDeviceAddress(device) ?: "Unknown"
                        val sb = StringBuilder()
                        sb.append("Found Classic Device: $name ($address)\n")
                        sb.append("  RSSI: $rssi dBm\n")
                        sb.append("  Type: ${device.type} (0=Unknown, 1=Classic, 2=LE, 3=Dual)")

                        // Try to get UUIDs if available
                        if (intent.hasExtra(BluetoothDevice.EXTRA_UUID)) {
                            val parcelUuids = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableArrayExtra(
                                    BluetoothDevice.EXTRA_UUID,
                                    android.os.ParcelUuid::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                            }
                            if (parcelUuids != null && parcelUuids.isNotEmpty()) {
                                sb.append("\n  UUIDs: ${parcelUuids.joinToString(", ")}")
                            }
                        }
                        callbacks.onScanLog(sb.toString(), LogType.INFO)
                    }
                }
            }
        }
    }

    // Track receiver registration state
    @Volatile
    private var isReceiverRegistered = false

    init {
        try {
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e("BluetoothScanner", "Failed to register receiver", e)
        }
    }

    /**
     * Start scanning for both Classic and BLE devices
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!checkBluetoothPermission()) {
            _isScanning.value = false
            callbacks.onScanLog("Scan failed: Missing permissions", LogType.ERROR)
            return
        }
        if (adapter == null) {
            _isScanning.value = false
            callbacks.onScanLog("Scan failed: Bluetooth adapter unavailable", LogType.ERROR)
            return
        }
        if (!adapter.isEnabled) {
            _isScanning.value = false
            callbacks.onScanLog("Scan failed: Bluetooth is turned off", LogType.WARNING)
            return
        }

        // Cancel any previous scan job
        scanJob?.cancel()

        // Clear previous results
        _scannedDevices.value = emptyList()
        _isScanning.value = true

        try {
            // Start Classic discovery
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()

            // Start BLE scan
            leScanner?.stopScan(leScanCallback)
            leScanner?.startScan(leScanCallback)

            // Start scan timeout
            scanJob = scope.launch {
                delay(BluetoothConfig.SCAN_TIMEOUT_MS)
                stopScan()
            }
        } catch (e: SecurityException) {
            _isScanning.value = false
            Log.e("BluetoothScanner", "Permission missing for scan", e)
            callbacks.onScanLog("Scan failed: Permission missing", LogType.ERROR)
        } catch (e: Exception) {
            _isScanning.value = false
            Log.e("BluetoothScanner", "Scan failed", e)
        }
    }

    /**
     * Stop all scanning
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        if (!checkBluetoothPermission()) {
            callbacks.onScanLog("Stop scan skipped: Missing permissions", LogType.WARNING)
            return
        }
        adapter?.cancelDiscovery()
        try {
            leScanner?.stopScan(leScanCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Add a discovered device to the list
     * @return true if device is new, false if already exists
     */
    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice, type: DeviceType, rssi: Int): Boolean {
        if (!checkBluetoothPermission()) return false
        val list = _scannedDevices.value.toMutableList()
        val address = safeGetDeviceAddress(device) ?: return false
        val rawName = safeGetDeviceName(device)
        val isBleOnly = isBleOnlyName(rawName)
        val resolvedType = if (isBleOnly) DeviceType.LE else type

        val existingIndex = list.indexOfFirst { it.address == address }
        if (existingIndex >= 0) {
            // Upgrade to BLE if needed
            val existing = list[existingIndex]
            if (resolvedType == DeviceType.LE && existing.type != DeviceType.LE) {
                val updated = existing.copy(type = DeviceType.LE)
                list[existingIndex] = updated
                _scannedDevices.value = list
                callbacks.onDeviceUpdated(updated)
            }
            return false
        }

        // Determine initial name for immediate feedback
        val initialName = if (!rawName.isNullOrBlank()) {
            "$rawName ($address)"
        } else {
            "Unknown Device ($address)"
        }

        // Add to list immediately to ensure deduplication and test correctness
        val newDevice = BluetoothDeviceModel(initialName, address, resolvedType, rssi)
        list.add(newDevice)
        _scannedDevices.value = list
        callbacks.onDeviceFound(newDevice)

        // Resolve more permanent/cached name in background
        scope.launch {
            try {
                val resolvedName = resolveDeviceName(device, address, resolvedType)
                if (resolvedName != initialName) {
                    val currentList = _scannedDevices.value.toMutableList()
                    val index = currentList.indexOfFirst { it.address == address }
                    if (index >= 0) {
                        val updatedDevice = currentList[index].copy(name = resolvedName)
                        currentList[index] = updatedDevice
                        _scannedDevices.value = currentList
                        callbacks.onDeviceUpdated(updatedDevice)
                    }
                }
            } catch (e: Exception) {
                Log.w("BluetoothScanner", "Failed to resolve name for $address", e)
            }
        }
        return true
    }

    /**
     * Resolve device name using multi-layer fallback
     */
    @SuppressLint("MissingPermission")
    private suspend fun resolveDeviceName(device: BluetoothDevice, address: String, type: DeviceType): String {
        // Layer 1: Bonded devices (most reliable)
        if (checkBluetoothPermission()) {
            try {
                adapter?.bondedDevices?.find { safeGetDeviceAddress(it) == address }?.let { bondedDevice ->
                    val bondedName = bondedDevice.name
                    if (!bondedName.isNullOrBlank() && bondedName != address) {
                        deviceNameCache.saveName(address, bondedName, type)
                        return "$bondedName ($address)"
                    }
                }
            } catch (e: SecurityException) {
                Log.w("BluetoothScanner", "Permission issue accessing bonded devices", e)
            }

            // Try direct name access
            try {
                val directName = device.name
                if (!directName.isNullOrBlank() && directName != address) {
                    deviceNameCache.saveName(address, directName, type)
                    return "$directName ($address)"
                }
            } catch (e: SecurityException) {
                Log.w("BluetoothScanner", "Permission issue accessing device name", e)
            }
        }

        // Layer 2: Persistent cache
        deviceNameCache.getName(address)?.let { cachedName ->
            return "$cachedName [cached] ($address)"
        }

        // Layer 3: Fallback to MAC address
        return "Unknown Device ($address)"
    }

    /**
     * Check if device name indicates BLE-only module
     */
    private fun isBleOnlyName(name: String?): Boolean {
        val nameUpper = (name ?: "").uppercase()
        return BluetoothConfig.BLE_ONLY_NAME_MARKERS.any { marker ->
            nameUpper.contains(marker.uppercase())
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopScan()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.e("BluetoothScanner", "Failed to unregister receiver", e)
            }
        }
    }

    private fun checkBluetoothPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun checkAllPermissions(results: Map<String, Boolean>) = results.values.all { it }

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun safeGetDeviceName(device: BluetoothDevice): String? = try {
        device.name
    } catch (_: SecurityException) {
        null
    }

    private fun safeGetDeviceAddress(device: BluetoothDevice): String? = try {
        device.address
    } catch (_: SecurityException) {
        null
    }
}
