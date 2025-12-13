package com.metelci.ardunakon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

/**
 * Handles Bluetooth Low Energy connection lifecycle.
 * Supports multiple BLE module variants (HM-10, Nordic UART, Arduino BLE, etc.)
 */
class BleConnection(
    private val device: BluetoothDevice,
    private val context: Context,
    private val callbacks: Callbacks,
    private val config: BluetoothConfig = BluetoothConfig,
    private val scope: CoroutineScope,
    private val connectionMutex: Mutex
) : BluetoothConnection {

    interface Callbacks {
        fun onConnected(connection: BleConnection)
        fun onDisconnected(reason: String)
        fun onDataReceived(data: ByteArray)
        fun onStateChanged(state: ConnectionState)
        fun onRssiRead(rssi: Int)
        fun onMtuChanged(mtu: Int)
        fun log(message: String, type: LogType)
        fun checkBluetoothPermission(): Boolean
        fun onHealthUpdate(seq: Int, packetAt: Long, failures: Int)
        fun getSavedDevice(): BluetoothDeviceModel?
        fun onReconnect(device: BluetoothDeviceModel, isAuto: Boolean)
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var detectedVariant: Int = 0

    private var connectionJob: Job? = null
    private var pollingJob: Job? = null
    private val writeQueueManager = WriteQueueManager(capacity = 100, scope = scope)

    // Descriptor write queue for sequential GATT operations
    private val pendingDescriptorWrites = mutableListOf<Triple<BluetoothGatt, BluetoothGattDescriptor, ByteArray>>()
    private var descriptorWriteIndex = 0

    // GATT retry tracking
    private var gattRetryAttempt = 0
    private val maxGattRetries = 3
    private var rssiFailures = 0

    // Resolved device name
    private var resolvedDeviceName: String = ""

    @SuppressLint("MissingPermission")
    fun connect() {
        if (!callbacks.checkBluetoothPermission()) {
            callbacks.onStateChanged(ConnectionState.ERROR)
            callbacks.log("BLE connect failed: Missing permissions", LogType.ERROR)
            return
        }
        resolvedDeviceName = device.name ?: "Unknown"
        callbacks.log("Connecting to BLE device $resolvedDeviceName...", LogType.INFO)
        connectGattWithTimeout()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION") // BluetoothAdapter#getDefaultAdapter is still required for pre-Q devices
    private fun connectGattWithTimeout() {
        if (!callbacks.checkBluetoothPermission()) {
            callbacks.onStateChanged(ConnectionState.ERROR)
            callbacks.log("BLE connect failed: Missing permissions", LogType.ERROR)
            connectionMutex.unlock()
            return
        }
        if (!hasBluetoothConnectPermission()) {
            callbacks.onStateChanged(ConnectionState.ERROR)
            callbacks.log("BLE connect failed: BLUETOOTH_CONNECT not granted", LogType.ERROR)
            connectionMutex.unlock()
            return
        }

        val adapter = try {
            android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        } catch (se: SecurityException) {
            callbacks.log("BLE connect failed: Missing permission for adapter access", LogType.ERROR)
            callbacks.onStateChanged(ConnectionState.ERROR)
            connectionMutex.unlock()
            null
        }
        if (adapter == null || !adapter.isEnabled) {
            callbacks.onStateChanged(ConnectionState.ERROR)
            callbacks.log("BLE connect failed: Bluetooth is off", LogType.ERROR)
            connectionMutex.unlock()
            return
        }

        // Settling delay
        Thread.sleep(500)

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        // Timeout with retry
        connectionJob = scope.launch {
            delay(15000)
            if (callbacks.getSavedDevice() != null) {
                val state = bluetoothGatt?.let { getConnectionState(it) }
                if (state != BluetoothProfile.STATE_CONNECTED) {
                    callbacks.log("BLE Connection timed out after 15s. Retrying...", LogType.WARNING)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    delay(500)

                    bluetoothGatt = device.connectGatt(context, false, gattCallback)

                    delay(15000)
                    val retryState = bluetoothGatt?.let { getConnectionState(it) }
                    if (retryState != BluetoothProfile.STATE_CONNECTED) {
                        callbacks.log("BLE Connection failed after retry (30s total)", LogType.ERROR)
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        callbacks.onStateChanged(ConnectionState.ERROR)
                        connectionMutex.unlock()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getConnectionState(gatt: BluetoothGatt): Int = try {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        manager.getConnectionState(gatt.device, BluetoothProfile.GATT)
    } catch (e: Exception) {
        BluetoothProfile.STATE_DISCONNECTED
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startRssiPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive && bluetoothGatt != null) {
                delay(2000)
                try {
                    if (!callbacks.checkBluetoothPermission()) {
                        callbacks.log("RSSI polling halted: Missing permissions", LogType.WARNING)
                        delay(2000)
                        continue
                    }
                    bluetoothGatt?.readRemoteRssi()
                } catch (e: SecurityException) {
                    Log.e("BT", "RSSI read failed", e)
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            connectionJob?.cancel()

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleGattError(gatt, status)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    callbacks.log("BLE Connected to GATT server.", LogType.SUCCESS)
                    callbacks.onStateChanged(ConnectionState.CONNECTED)
                    gattRetryAttempt = 0

                    // Request MTU and high priority
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val mtuRequested = gatt.requestMtu(512)
                        if (mtuRequested) {
                            callbacks.log("BLE MTU negotiation requested (512 bytes)", LogType.INFO)
                        }
                    }
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    callbacks.log("BLE High priority connection requested", LogType.INFO)

                    gatt.discoverServices()
                    startRssiPolling()
                    connectionMutex.unlock()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    pollingJob?.cancel()
                    callbacks.log("BLE Disconnected from GATT server.", LogType.WARNING)
                    callbacks.onStateChanged(ConnectionState.DISCONNECTED)
                    callbacks.onDisconnected("GATT disconnected")
                    gatt.close()
                    callbacks.onRssiRead(0)
                    try {
                        if (connectionMutex.isLocked) connectionMutex.unlock()
                    } catch (_: IllegalStateException) {}
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun handleGattError(gatt: BluetoothGatt, status: Int) {
            val errorDesc = GattStatus.getErrorDescription(status)
            callbacks.log("BLE GATT $errorDesc", LogType.ERROR)

            val shouldRetry = GattStatus.isTransientError(status) && gattRetryAttempt < maxGattRetries
            val isPermanent = GattStatus.isPermanentError(status)

            if (isPermanent) {
                callbacks.log("Permanent GATT error detected. Not retrying.", LogType.ERROR)
                callbacks.onStateChanged(ConnectionState.ERROR)
                pollingJob?.cancel()
                gatt.close()
                callbacks.onRssiRead(0)
                connectionMutex.unlock()
                return
            }

            if (shouldRetry) {
                gattRetryAttempt++
                val delay = when (gattRetryAttempt) {
                    1 -> 2000L
                    2 -> 4000L
                    else -> 6000L
                }
                callbacks.log(
                    "Transient GATT error (attempt $gattRetryAttempt/$maxGattRetries). Retrying in ${delay}ms.",
                    LogType.WARNING
                )

                pollingJob?.cancel()
                try {
                    gatt.disconnect()
                } catch (_: Exception) {}
                gatt.close()
                callbacks.onRssiRead(0)
                callbacks.onStateChanged(ConnectionState.ERROR)
                connectionMutex.unlock()

                scope.launch {
                    delay(delay)
                    callbacks.log("Retrying BLE connect after transient error...", LogType.WARNING)
                    callbacks.getSavedDevice()?.let { callbacks.onReconnect(it, true) }
                }
            } else {
                if (gattRetryAttempt >= maxGattRetries) {
                    callbacks.log("Max GATT retries ($maxGattRetries) exhausted.", LogType.ERROR)
                }
                callbacks.onStateChanged(ConnectionState.ERROR)
                pollingJob?.cancel()
                gatt.close()
                callbacks.onRssiRead(0)
                connectionMutex.unlock()
                gattRetryAttempt = 0
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callbacks.onRssiRead(rssi)
                rssiFailures = 0
            } else {
                rssiFailures = (rssiFailures + 1).coerceAtMost(10)
                if (rssiFailures >= 3) {
                    val errorDesc = GattStatus.getErrorDescription(status)
                    callbacks.log("RSSI read failures: $rssiFailures - $errorDesc", LogType.WARNING)
                }
                if (rssiFailures >= 5) {
                    callbacks.onDisconnected("RSSI polling failed $rssiFailures times")
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callbacks.log("BLE MTU changed to $mtu bytes (payload: ${mtu - 3} bytes)", LogType.SUCCESS)
                callbacks.onMtuChanged(mtu)
            } else {
                callbacks.log("BLE MTU negotiation failed - using default 23 bytes", LogType.WARNING)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callbacks.log("BLE Service discovery failed: ${GattStatus.getErrorDescription(status)}", LogType.ERROR)
                return
            }

            val (service, characteristic, variant) = findCompatibleService(gatt)
            if (service != null && characteristic != null) {
                txCharacteristic = characteristic
                detectedVariant = variant
                callbacks.log("✓ BLE Module detected (variant $variant)", LogType.SUCCESS)

                setupNotifications(gatt)
            } else {
                callbacks.log("BLE Service/Characteristic not found!", LogType.ERROR)
                gatt.services?.forEach { svc ->
                    callbacks.log("Available service: ${svc.uuid}", LogType.INFO)
                }
                callbacks.onStateChanged(ConnectionState.ERROR)
                gatt.disconnect()
                gatt.close()
                connectionMutex.unlock()
            }
        }

        private fun findCompatibleService(
            gatt: BluetoothGatt
        ): Triple<android.bluetooth.BluetoothGattService?, BluetoothGattCharacteristic?, Int> {
            // Try all known BLE module variants
            for (variant in BleUuidRegistry.ALL_VARIANTS) {
                val service = gatt.getService(variant.serviceUuid)
                if (service != null) {
                    // Try TX/RX pair first
                    if (variant.rxCharUuid != null) {
                        val char = service.getCharacteristic(variant.rxCharUuid)
                        if (char != null) {
                            callbacks.log("Found ${variant.name} (TX/RX pair)", LogType.INFO)
                            return Triple(service, char, variant.id)
                        }
                    }
                    // Try legacy single characteristic
                    if (variant.legacyCharUuid != null) {
                        val char = service.getCharacteristic(variant.legacyCharUuid)
                        if (char != null) {
                            callbacks.log("Found ${variant.name} (legacy)", LogType.INFO)
                            return Triple(service, char, variant.id)
                        }
                    }
                }
            }

            // Generic discovery fallback
            for (service in gatt.services) {
                for (char in service.characteristics) {
                    val props = char.properties
                    val hasWrite = (
                        props and (
                            BluetoothGattCharacteristic.PROPERTY_WRITE or
                                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                            )
                        ) != 0
                    val hasNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0

                    if (hasWrite && hasNotify) {
                        callbacks.log("Found generic BLE module: ${char.uuid}", LogType.INFO)
                        return Triple(service, char, 99)
                    }
                }
            }

            return Triple(null, null, 0)
        }

        @SuppressLint("MissingPermission")
        private fun setupNotifications(gatt: BluetoothGatt) {
            val txService = txCharacteristic?.service
            val notifyChar = txService?.characteristics?.firstOrNull { char ->
                val props = char.properties
                (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                    (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            } ?: txCharacteristic

            notifyChar?.let { char ->
                gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(BleUuidRegistry.CCCD_UUID)
                if (descriptor != null) {
                    val value = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    }
                    pendingDescriptorWrites.add(Triple(gatt, descriptor, value))
                }
            }

            if (pendingDescriptorWrites.isNotEmpty()) {
                callbacks.log("→ Processing ${pendingDescriptorWrites.size} CCCD descriptor(s)...", LogType.INFO)
                descriptorWriteIndex = 0
                val (g, desc, value) = pendingDescriptorWrites[0]
                writeDescriptorCompat(g, desc, value)
            } else {
                startWriteQueue()
            }
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                callbacks.onDataReceived(data)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (value.isNotEmpty()) {
                callbacks.onDataReceived(value)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callbacks.log(
                    "✓ CCCD ${descriptorWriteIndex + 1}/${pendingDescriptorWrites.size} configured",
                    LogType.INFO
                )
                descriptorWriteIndex++
                if (descriptorWriteIndex < pendingDescriptorWrites.size) {
                    val (g, desc, value) = pendingDescriptorWrites[descriptorWriteIndex]
                    writeDescriptorCompat(g, desc, value)
                } else {
                    callbacks.log("✓ All CCCD descriptors configured!", LogType.SUCCESS)
                    startWriteQueue()
                }
            } else {
                callbacks.log("✗ CCCD write failed - starting queue anyway", LogType.WARNING)
                startWriteQueue()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptorCompat(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray) {
        if (!callbacks.checkBluetoothPermission()) return
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
            callbacks.log("Descriptor write failed: Permission revoked", LogType.ERROR)
        }
    }

    private fun startWriteQueue() {
        writeQueueManager.onLog = { msg, type -> callbacks.log(msg, type) }
        writeQueueManager.start(
            performWrite = { data -> performWrite(data) },
            writeDelayMs = if (detectedVariant == 6) 2L else 10L,
            initialDelayMs = 200L
        )
    }

    @SuppressLint("MissingPermission")
    private fun performWrite(bytes: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val char = txCharacteristic ?: return false

        val writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = gatt.writeCharacteristic(char, bytes, writeType)
            status == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = bytes
            @Suppress("DEPRECATION")
            char.writeType = writeType
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    override fun write(bytes: ByteArray) {
        writeQueueManager.enqueue(bytes)
    }

    @SuppressLint("MissingPermission")
    override fun cancel() {
        connectionJob?.cancel()
        pollingJob?.cancel()
        writeQueueManager.stop()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
    }

    // RSSI is handled by polling job, no override needed

    /**
     * Gets packet statistics from the write queue.
     */
    override fun getPacketStats(): Triple<Long, Long, Long> = Triple(
        writeQueueManager.packetsSent,
        writeQueueManager.packetsDropped,
        writeQueueManager.packetsFailed
    )
}
