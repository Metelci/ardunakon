package com.metelci.ardunakon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong

class BleConnectionManager(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
    private val scope: CoroutineScope,
    private val callback: ConnectionCallback
) : BluetoothConnectionManager {

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    
    // Connection Management
    private var connectionJob: Job? = null
    private var timeoutJob: Job? = null
    private var writeJob: Job? = null
    private var pollingJob: Job? = null
    private val writeQueue = LinkedBlockingQueue<ByteArray>()
    private val connectionMutex = Mutex()
    private val isConnected = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isGattConnected = java.util.concurrent.atomic.AtomicBoolean(false)

    // Stats
    private val packetsSent = AtomicLong(0)
    private val packetsDropped = AtomicLong(0)
    private val packetsFailed = AtomicLong(0)

    // UUID Handling
    private var detectedVariant = 0
    private var resolvedDeviceName = "Unknown"

    // Pending descriptor writes for notification enablement
    private val pendingDescriptorWrites = java.util.concurrent.ConcurrentLinkedQueue<Triple<BluetoothGatt, BluetoothGattDescriptor, ByteArray>>()
    private var descriptorWriteIndex = 0

    @SuppressLint("MissingPermission")
    override fun connect(device: BluetoothDevice) {
        if (!checkBluetoothPermission()) {
            callback.onError("BLE connect failed: Missing permissions", LogType.ERROR)
            callback.onStateChanged(ConnectionState.ERROR)
            return
        }

        if (adapter == null || !adapter.isEnabled) {
            callback.onError("BLE connect failed: Bluetooth is off", LogType.ERROR)
            callback.onStateChanged(ConnectionState.ERROR)
            return
        }

        resolvedDeviceName = device.name ?: "Unknown"
        callback.onError("Connecting to BLE device $resolvedDeviceName...", LogType.INFO) // Info via generic log

        connectionJob?.cancel()
        timeoutJob?.cancel()
        connectionJob = scope.launch(Dispatchers.IO) {
            connectGattWithTimeout(device)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectGattWithTimeout(device: BluetoothDevice) {
        connectionMutex.withLock {
            isConnected.set(false)
            isGattConnected.set(false)
            txCharacteristic = null

            // Cleanup existing
            bluetoothGatt?.let { gatt ->
                try {
                    gatt.disconnect()
                    gatt.close()
                } catch (_: Exception) {}
            }
            bluetoothGatt = null

            // Connection attempt
            val gattCallback = BleGattCallback()
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        }

        // Timeout: require the GATT link to come up; service discovery can be slow on some stacks.
        timeoutJob?.cancel()
        timeoutJob = scope.launch(Dispatchers.IO) {
            delay(15000)
            if (!isGattConnected.get()) {
                callback.onError("BLE Connection timed out", LogType.ERROR)
                disconnect()
                callback.onStateChanged(ConnectionState.ERROR)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        connectionJob?.cancel()
        timeoutJob?.cancel()
        pollingJob?.cancel()
        writeJob?.cancel()
        writeQueue.clear()

        val gatt = bluetoothGatt
        bluetoothGatt = null
        
        if (gatt != null) {
            try {
                gatt.disconnect()
                Thread.sleep(100) // Pragmatic delay
                gatt.close()
            } catch (e: Exception) {
                Log.w("BT", "Gatt close failed", e)
            }
        }
        
        isConnected.set(false)
        isGattConnected.set(false)
        callback.onStateChanged(ConnectionState.DISCONNECTED)
    }

    override fun send(data: ByteArray) {
        if (!isConnected.get()) {
            packetsDropped.incrementAndGet()
            return
        }
        
        if (!writeQueue.offer(data)) {
            packetsDropped.incrementAndGet()
            callback.onError("Write queue full", LogType.WARNING)
        }
    }

    override fun requestRssi() {
        if (!checkBluetoothPermission()) return
        bluetoothGatt?.readRemoteRssi()
    }

    override fun cleanup() {
        disconnect()
    }

    override fun getPacketStats(): NetworkStats {
        return NetworkStats(packetsSent.get(), packetsDropped.get(), packetsFailed.get())
    }

    private fun checkBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    // --- GATT Callback ---
    @SuppressLint("MissingPermission")
    private inner class BleGattCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callback.onError("BLE GATT Error: $status", LogType.ERROR)
                disconnect() // Will trigger state change
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isGattConnected.set(true)
                timeoutJob?.cancel()
                callback.onError("BLE Connected - Negotiating...", LogType.INFO)
                
                // Request High Priority
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                
                // Discover Services
                gatt.discoverServices()
                
                // Start RSSI polling
                startRssiPolling()
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                callback.onError("BLE Disconnected", LogType.INFO)
                isConnected.set(false)
                isGattConnected.set(false)
                callback.onStateChanged(ConnectionState.DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val resolved = resolveGattProfile(gatt)
            if (resolved == null) {
                callback.onError("BLE Service/Characteristic not found", LogType.ERROR)
                gatt.services?.forEach { svc ->
                    callback.onError("Available service: ${svc.uuid}", LogType.INFO)
                }
                disconnect()
                callback.onStateChanged(ConnectionState.ERROR)
                return
            }

            detectedVariant = resolved.variantId
            txCharacteristic = resolved.writeChar // We write to RX/write characteristic of the device
            callback.onError("BLE ready: ${resolved.variantName} (variant ${resolved.variantId})", LogType.SUCCESS)

            // Enable notifications/indications for inbound data.
            if (!gatt.setCharacteristicNotification(resolved.notifyChar, true)) {
                callback.onError("Failed to enable notifications locally", LogType.WARNING)
                onReady()
                return
            }

            val descriptor = resolved.notifyChar.getDescriptor(BleUuidRegistry.CCCD_UUID)
            if (descriptor == null) {
                // Some stacks/services expose notify without CCCD.
                onReady()
                return
            }

            val value = if (resolved.usesIndication) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                val enqueued = writeDescriptorLegacy(gatt, descriptor, value)
                if (!enqueued) {
                    callback.onError("Descriptor write failed to enqueue (legacy)", LogType.WARNING)
                    onReady()
                }
            }

            // Request high MTU (best-effort)
            gatt.requestMtu(512)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callback.onError("Notifications enabled", LogType.SUCCESS)
                onReady()
            } else {
                callback.onError("Failed to write descriptor", LogType.WARNING)
                onReady() // Try anyway
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
             callback.onError("MTU changed to $mtu", LogType.INFO)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
             // API 33+
             processIncoming(value)
        }
        
        // Legacy callback
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
             processIncoming(characteristic.value)
        }
        
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callback.onRssiUpdated(rssi)
            }
        }
        
        // --- Helper for writes ---
        private fun processIncoming(data: ByteArray) {
            if (data.isNotEmpty()) {
                callback.onDataReceived(data)
            }
        }
        
        private fun onReady() {
            isConnected.set(true)
            callback.onStateChanged(ConnectionState.CONNECTED)
            startWriteQueue()
        }

        private fun resolveGattProfile(gatt: BluetoothGatt): ResolvedGattProfile? {
            // Try known variants first.
            for (variant in BleUuidRegistry.ALL_VARIANTS) {
                val service = gatt.getService(variant.serviceUuid) ?: continue

                val writeChar = variant.rxCharUuid?.let(service::getCharacteristic)
                    ?: variant.legacyCharUuid?.let(service::getCharacteristic)
                    ?: variant.txCharUuid?.let(service::getCharacteristic)
                val notifyChar = variant.txCharUuid?.let(service::getCharacteristic)
                    ?: variant.legacyCharUuid?.let(service::getCharacteristic)

                if (writeChar != null && notifyChar != null) {
                    val usesIndication = (notifyChar.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 &&
                        (notifyChar.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0
                    return ResolvedGattProfile(
                        variantId = variant.id,
                        variantName = variant.name,
                        writeChar = writeChar,
                        notifyChar = notifyChar,
                        usesIndication = usesIndication
                    )
                }
            }

            // Generic fallback: locate a write characteristic and a notify/indicate characteristic.
            for (service in gatt.services) {
                val chars = service.characteristics.orEmpty()

                val candidateNotify = chars.firstOrNull { c ->
                    (c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                        (c.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                }

                val candidateWrite = chars.firstOrNull { c ->
                    (c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                        (c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }

                if (candidateNotify != null && candidateWrite != null) {
                    val usesIndication = (candidateNotify.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 &&
                        (candidateNotify.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0
                    return ResolvedGattProfile(
                        variantId = 99,
                        variantName = "Generic BLE",
                        writeChar = candidateWrite,
                        notifyChar = candidateNotify,
                        usesIndication = usesIndication
                    )
                }
            }

            return null
        }

        private fun writeDescriptorLegacy(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            value: ByteArray
        ): Boolean {
            return try {
                val setValue = BluetoothGattDescriptor::class.java.getMethod("setValue", ByteArray::class.java)
                setValue.invoke(descriptor, value)
                val write = BluetoothGatt::class.java.getMethod("writeDescriptor", BluetoothGattDescriptor::class.java)
                write.invoke(gatt, descriptor) as? Boolean ?: false
            } catch (_: Exception) {
                false
            }
        }
    }

    private data class ResolvedGattProfile(
        val variantId: Int,
        val variantName: String,
        val writeChar: BluetoothGattCharacteristic,
        val notifyChar: BluetoothGattCharacteristic,
        val usesIndication: Boolean
    )

    private fun startWriteQueue() {
        writeJob?.cancel()
        writeQueue.clear()
        writeJob = scope.launch(Dispatchers.IO) {
            delay(200) // Stabilize
            
            while (isActive) {
                try {
                    val data = writeQueue.take() // Blocking
                    val gatt = bluetoothGatt
                    val char = txCharacteristic
                    
                    if (gatt != null && char != null) {
                        // Write
                         val writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                         
                         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                             gatt.writeCharacteristic(char, data, writeType)
                         } else {
                             @Suppress("DEPRECATION")
                             char.value = data
                             @Suppress("DEPRECATION")
                             char.writeType = writeType
                             @Suppress("DEPRECATION")
                             gatt.writeCharacteristic(char)
                         }
                         packetsSent.incrementAndGet()
                    }
                    
                    val delayMs = if (detectedVariant == 6) 2L else 10L // Simple logic for now
                    delay(delayMs)
                    
                } catch (e: Exception) {
                    if (e is InterruptedException) break
                    packetsFailed.incrementAndGet()
                }
            }
        }
    }
    
    private fun startRssiPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
             while (isActive) {
                 delay(2000)
                 if (isConnected.get()) requestRssi()
             }
        }
    }
}
