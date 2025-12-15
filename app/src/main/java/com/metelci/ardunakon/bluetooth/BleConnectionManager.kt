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
import java.util.concurrent.atomic.AtomicInteger

class BleConnectionManager(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
    private val scope: CoroutineScope,
    private val callback: ConnectionCallback
) : BluetoothConnectionManager {

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var currentDevice: BluetoothDevice? = null
    
    // Connection Management
    private var connectionJob: Job? = null
    private var timeoutJob: Job? = null
    private var writeJob: Job? = null
    private var pollingJob: Job? = null
    private var serviceDiscoveryJob: Job? = null
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

    // Retry handling for transient GATT errors (e.g., status 8/129 on flaky stacks)
    private var gattRetryAttempt = 0
    private val maxGattRetries = 3
    private val isRetrying = java.util.concurrent.atomic.AtomicBoolean(false)

    // Service discovery retry (some stacks return empty services on first discovery)
    private val serviceDiscoveryAttempts = AtomicInteger(0)
    private val maxServiceDiscoveryAttempts = 2

    // Pending descriptor writes for notification enablement
    private val pendingDescriptorWrites = java.util.concurrent.ConcurrentLinkedQueue<Triple<BluetoothGatt, BluetoothGattDescriptor, ByteArray>>()
    private var descriptorWriteIndex = 0

    @SuppressLint("MissingPermission")
    override fun connect(device: BluetoothDevice) {
        if (!checkBluetoothPermission()) {
            callback.onError("BLE connect failed: Missing permissions", LogType.ERROR)
            cleanupGatt(ConnectionState.ERROR)
            return
        }

        if (adapter == null || !adapter.isEnabled) {
            callback.onError("BLE connect failed: Bluetooth is off", LogType.ERROR)
            cleanupGatt(ConnectionState.ERROR)
            return
        }

        currentDevice = device
        gattRetryAttempt = 0
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
            currentDevice = device
            serviceDiscoveryAttempts.set(0)
            serviceDiscoveryJob?.cancel()

            // Cleanup existing
            bluetoothGatt?.let { gatt ->
                try {
                    gatt.disconnect()
                    gatt.close()
                } catch (_: Exception) {}
            }
            bluetoothGatt = null
            delay(250) // Give the BLE stack a moment to settle after close()

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
                cleanupGatt(ConnectionState.ERROR)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        cleanupGatt(ConnectionState.DISCONNECTED)
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

    @SuppressLint("MissingPermission")
    private fun cleanupGatt(finalState: ConnectionState? = null) {
        connectionJob?.cancel()
        timeoutJob?.cancel()
        pollingJob?.cancel()
        writeJob?.cancel()
        serviceDiscoveryJob?.cancel()
        writeQueue.clear()

        val gatt = bluetoothGatt
        bluetoothGatt = null

        if (gatt != null) {
            try {
                try {
                    // Best-effort cache clear; helps after transient errors on some Android stacks.
                    val refresh = BluetoothGatt::class.java.getMethod("refresh")
                    refresh.invoke(gatt)
                } catch (_: Exception) {}

                gatt.disconnect()
                Thread.sleep(100) // Pragmatic delay
                gatt.close()
            } catch (e: Exception) {
                Log.w("BT", "Gatt close failed", e)
            }
        }

        isConnected.set(false)
        isGattConnected.set(false)
        if (finalState != null) {
            callback.onStateChanged(finalState)
        }
    }

    // --- GATT Callback ---
    @SuppressLint("MissingPermission")
    private inner class BleGattCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleGattError(gatt, status)
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isRetrying.set(false)
                isGattConnected.set(true)
                timeoutJob?.cancel()
                callback.onError("BLE Connected - Negotiating...", LogType.INFO)
                
                // Request High Priority
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                // Best-effort MTU; some devices behave better when requested early.
                gatt.requestMtu(512)
                
                // Discover services after a short settle; ESP32-based peripherals can return empty services if queried immediately.
                serviceDiscoveryJob?.cancel()
                serviceDiscoveryJob = scope.launch(Dispatchers.IO) {
                    delay(650)
                    if (!isGattConnected.get()) return@launch
                    callback.onError("Discovering BLE services...", LogType.INFO)
                    gatt.discoverServices()
                }
                
                // Start RSSI polling
                startRssiPolling()
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (isRetrying.get()) return
                callback.onError("BLE Disconnected", LogType.INFO)
                isConnected.set(false)
                isGattConnected.set(false)
                callback.onStateChanged(ConnectionState.DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callback.onError("BLE service discovery failed: ${GattStatus.getErrorDescription(status)}", LogType.ERROR)
                cleanupGatt(ConnectionState.ERROR)
                return
            }
            val resolved = resolveGattProfile(gatt)
            if (resolved == null) {
                val attempt = serviceDiscoveryAttempts.incrementAndGet()
                if (attempt < maxServiceDiscoveryAttempts) {
                    val serviceCount = gatt.services?.size ?: 0
                    callback.onError(
                        "BLE Service/Characteristic not found (attempt $attempt/$maxServiceDiscoveryAttempts, services=$serviceCount). Retrying...",
                        LogType.WARNING
                    )
                    serviceDiscoveryJob?.cancel()
                    serviceDiscoveryJob = scope.launch(Dispatchers.IO) {
                        delay(800)
                        if (!isGattConnected.get()) return@launch
                        gatt.discoverServices()
                    }
                    return
                }

                callback.onError("BLE Service/Characteristic not found", LogType.ERROR)
                gatt.services?.forEach { svc -> callback.onError("Available service: ${svc.uuid}", LogType.INFO) }
                if ((gatt.services?.isEmpty() != false) &&
                    (resolvedDeviceName.contains("R4", ignoreCase = true) ||
                        resolvedDeviceName.contains("ARDUNAKON", ignoreCase = true))
                ) {
                    callback.onError(
                        "Tip: If you're using the UNO R4 WiFi sketch, ensure it was uploaded in BLE mode (USE_BLE_MODE=1). Otherwise use WiFi mode in the app.",
                        LogType.INFO
                    )
                }
                cleanupGatt(ConnectionState.ERROR)
                return
            }

            detectedVariant = resolved.variantId
            txCharacteristic = resolved.writeChar // We write to RX/write characteristic of the device
            callback.onError("BLE ready: ${resolved.variantName} (variant ${resolved.variantId})", LogType.SUCCESS)

            if (resolved.notifyChar == null) {
                callback.onError("BLE notifications not available (write-only profile).", LogType.INFO)
                onReady()
                return
            }

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

            // MTU already requested on connect; keep this as no-op if stack ignores duplicates.
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
        @Deprecated("Deprecated in Java")
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

        private fun handleGattError(gatt: BluetoothGatt, status: Int) {
            timeoutJob?.cancel()

            val desc = GattStatus.getErrorDescription(status)
            val shouldRetry = GattStatus.isTransientError(status) && gattRetryAttempt < maxGattRetries
            val permanent = GattStatus.isPermanentError(status)

            if (permanent) {
                isRetrying.set(false)
                callback.onError("BLE GATT $desc (permanent)", LogType.ERROR)
                cleanupGatt(ConnectionState.ERROR)
                return
            }

            if (shouldRetry) {
                isRetrying.set(true)
                gattRetryAttempt++
                val delayMs = when (gattRetryAttempt) {
                    1 -> 1200L
                    2 -> 2500L
                    else -> 4000L
                }
                callback.onError(
                    "BLE GATT $desc (retry $gattRetryAttempt/$maxGattRetries in ${delayMs}ms)",
                    LogType.WARNING
                )

                // Close the broken GATT but keep the overall connection state as "connecting".
                cleanupGatt(finalState = null)

                val device = currentDevice ?: gatt.device
                scope.launch(Dispatchers.IO) {
                    delay(delayMs)
                    if (isConnected.get() || isGattConnected.get()) return@launch
                    callback.onError("Retrying BLE connect...", LogType.WARNING)
                    connectGattWithTimeout(device)
                }
                return
            }

            isRetrying.set(false)
            callback.onError("BLE GATT $desc", LogType.ERROR)
            cleanupGatt(ConnectionState.ERROR)
        }

        private fun resolveGattProfile(gatt: BluetoothGatt): ResolvedGattProfile? {
            // Try known variants first.
            for (variant in BleUuidRegistry.ALL_VARIANTS) {
                val service = gatt.getService(variant.serviceUuid) ?: continue

                val writeChar = variant.rxCharUuid?.let(service::getCharacteristic)
                    ?: variant.legacyCharUuid?.let(service::getCharacteristic)
                    ?: variant.txCharUuid?.let(service::getCharacteristic)
                val notifyCandidate = variant.txCharUuid?.let(service::getCharacteristic)
                    ?: variant.legacyCharUuid?.let(service::getCharacteristic)

                if (writeChar != null) {
                    val notifyChar = notifyCandidate?.takeIf { c ->
                        (c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                            (c.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    }
                    val usesIndication = notifyChar != null &&
                        (notifyChar.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 &&
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

            // Write-only fallback: allow connecting to simple sketches that only expose a writable characteristic.
            for (service in gatt.services) {
                val chars = service.characteristics.orEmpty()
                val candidateWrite = chars.firstOrNull { c ->
                    (c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                        (c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }
                if (candidateWrite != null) {
                    return ResolvedGattProfile(
                        variantId = 100,
                        variantName = "Write-only BLE",
                        writeChar = candidateWrite,
                        notifyChar = null,
                        usesIndication = false
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
        val notifyChar: BluetoothGattCharacteristic?,
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
