package com.metelci.ardunakon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import com.metelci.ardunakon.model.LogType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Handles Classic Bluetooth SPP connection lifecycle.
 * Supports HC-05, HC-06, and compatible serial modules.
 *
 * Implements multi-strategy connection with fallbacks for OEM compatibility.
 */
class ClassicConnection(
    private val device: BluetoothDevice,
    private val callbacks: Callbacks,
    private val config: BluetoothConfig = BluetoothConfig,
    private val allowReflectionFallback: Boolean = false,
    private val forceReflectionFirst: Boolean = false,
    private val scope: CoroutineScope,
    private val connectionMutex: Mutex
) : Thread(),
    BluetoothConnection {

    interface Callbacks {
        fun onConnected(connection: ClassicConnection)
        fun onDisconnected(reason: String)
        fun onDataReceived(data: ByteArray)
        fun onStateChanged(state: ConnectionState)
        fun onRssiSnapshot(): Int? // Classic BT doesn't support RSSI polling directly
        fun log(message: String, type: LogType)
        fun checkBluetoothPermission(): Boolean
        fun performDeviceVerification(device: BluetoothDevice)
    }

    // Standard SPP UUID
    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Manufacturer-specific UUIDs for HC-06 clones and variants
        val MANUFACTURER_UUIDS: List<UUID> = listOf(
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), // Standard SPP
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"), // HM-10/HC-08
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"), // Nordic UART
            UUID.fromString("00001105-0000-1000-8000-00805f9b34fb"), // Object Push
            UUID.fromString("00001106-0000-1000-8000-00805f9b34fb"), // OBEX File Transfer
            UUID.fromString("00001108-0000-1000-8000-00805f9b34fb"), // Headset Profile
            UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb"), // Hands-Free Profile
            UUID.fromString("0000110e-0000-1000-8000-00805f9b34fb"), // A/V Remote Control
            UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb"), // Audio Distribution
            UUID.fromString("00001103-0000-1000-8000-00805f9b34fb"), // Dial-up Networking
            UUID.fromString("00001102-0000-1000-8000-00805f9b34fb"), // LAN Access
            UUID.fromString("00000003-0000-1000-8000-00805f9b34fb"), // RFCOMM
            UUID.fromString("00000000-0000-1000-8000-00805f9b34fb") // Base UUID
        )
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    @Volatile private var cancelled = false

    @Volatile private var isConnected = false

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION") // getDefaultAdapter is deprecated but needed for broad device support
    override fun run() {
        try {
            if (!callbacks.checkBluetoothPermission()) {
                callbacks.log("Connect failed: Missing permissions", LogType.ERROR)
                callbacks.onStateChanged(ConnectionState.ERROR)
                return
            }

            callbacks.log("Starting connection to ${device.name} (${device.address})", LogType.INFO)
            callbacks.log(
                "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                LogType.INFO
            )

            // Pre-flight: Cancel discovery
            try {
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                if (adapter?.isDiscovering == true) {
                    adapter.cancelDiscovery()
                    callbacks.log("Cancelling discovery for stability...", LogType.INFO)
                    safeSleep(300)
                }
            } catch (e: SecurityException) {
                callbacks.log("Could not cancel discovery: Missing permission", LogType.WARNING)
            }

            // Bonding check
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                callbacks.log("Device not bonded. Initiating pairing...", LogType.WARNING)
                device.createBond()
                var bondWait = 0
                while (device.bondState != BluetoothDevice.BOND_BONDED && bondWait < 100 && !cancelled) {
                    safeSleep(100)
                    bondWait++
                }
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    callbacks.log("Pairing might have failed or timed out. Proceeding anyway...", LogType.WARNING)
                } else {
                    callbacks.log("Pairing successful!", LogType.SUCCESS)
                }
            }

            // Device verification (non-blocking)
            scope.launch {
                try {
                    callbacks.performDeviceVerification(device)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    callbacks.log("Device verification error: ${e.message}", LogType.WARNING)
                }
            }

            // Connection attempts
            var connected = attemptConnection()

            if (cancelled) {
                closeSocketSafely()
                callbacks.log("Connection attempt cancelled for ${device.name}", LogType.WARNING)
                callbacks.onStateChanged(ConnectionState.DISCONNECTED)
                return
            }

            val validSocket = socket
            if (connected && validSocket != null) {
                // Apply Xiaomi stream initialization delay
                if (forceReflectionFirst) {
                    callbacks.log("Applying Xiaomi stream initialization delay (500ms)...", LogType.INFO)
                    safeSleep(500)
                }

                try {
                    outputStream = validSocket.outputStream
                    inputStream = validSocket.inputStream
                    isConnected = true

                    callbacks.onStateChanged(ConnectionState.CONNECTED)
                    callbacks.onConnected(this)

                    // Start reading loop
                    readLoop()
                } catch (e: IOException) {
                    callbacks.log("Failed to create socket streams: ${e.message}", LogType.ERROR)
                    callbacks.log("Marking connection as failed", LogType.WARNING)
                    closeSocketSafely()
                    callbacks.onStateChanged(ConnectionState.ERROR)
                }
            } else {
                closeSocketSafely()
                callbacks.log("============================================", LogType.ERROR)
                callbacks.log("ALL CONNECTION METHODS FAILED for ${device.name}", LogType.ERROR)
                callbacks.log("Tried: SPP, Reflection Ports 1-3, 13 UUIDs, Secure SPP", LogType.ERROR)
                callbacks.log("Module may be defective or incompatible", LogType.ERROR)
                callbacks.log("============================================", LogType.ERROR)
                callbacks.onStateChanged(ConnectionState.ERROR)
            }
        } finally {
            connectionMutex.unlock()
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptConnection(): Boolean {
        val aggressiveXiaomi = forceReflectionFirst

        // Attempt A: Forced Reflection Port 1 (OEM-first for Xiaomi)
        if (forceReflectionFirst && !cancelled) {
            if (tryReflectionPort(1, "Forcing Reflection Port 1 FIRST (OEM)", aggressiveXiaomi)) {
                return true
            }
        }

        // Attempt A2: Raw RFCOMM via reflection
        if (allowReflectionFallback && !cancelled) {
            if (tryReflectionPort(1, "RAW RFCOMM via reflection (Port 1)", aggressiveXiaomi)) {
                return true
            }
        }

        // Attempt 1: Standard SPP INSECURE
        if (!cancelled) {
            if (tryUuidConnection(SPP_UUID, "INSECURE SPP connection (Standard HC-06)", false, aggressiveXiaomi)) {
                return true
            }
        }

        // Attempt 2: Reflection Port 1 (if allowed)
        if (allowReflectionFallback && !cancelled) {
            if (tryReflectionPort(1, "REFLECTION connection (Port 1 - HC-06 Fallback)", aggressiveXiaomi)) {
                return true
            }
        }

        // Attempt 3: Manufacturer-specific UUIDs
        if (!cancelled) {
            for ((index, uuid) in MANUFACTURER_UUIDS.withIndex()) {
                if (cancelled) break

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

                if (tryUuidConnection(uuid, uuidDesc, false, aggressiveXiaomi)) {
                    return true
                }
            }
        }

        // Attempt 4: Alternative reflection ports
        if (!cancelled) {
            for (port in listOf(2, 3)) {
                if (cancelled) break
                if (tryReflectionPort(port, "REFLECTION connection (Port $port)", aggressiveXiaomi)) {
                    return true
                }
            }
        }

        // Attempt 5: Secure SPP (last resort)
        if (!cancelled && !aggressiveXiaomi) {
            if (tryUuidConnection(SPP_UUID, "SECURE SPP connection (last resort)", true, aggressiveXiaomi)) {
                return true
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun tryUuidConnection(uuid: UUID, desc: String, secure: Boolean, fastRecovery: Boolean): Boolean {
        try {
            callbacks.log("Attempting $desc...", LogType.INFO)
            socket = if (secure) {
                device.createRfcommSocketToServiceRecord(uuid)
            } else {
                device.createInsecureRfcommSocketToServiceRecord(uuid)
            }
            socket?.connect()
            callbacks.log("Connected successfully with $desc", LogType.SUCCESS)
            return true
        } catch (e: Exception) {
            callbacks.log("$desc failed: ${e.message}", LogType.ERROR)
            Log.w("BT", "$desc failed", e)
            closeSocketSafely()
            if (!cancelled) safeSleep(if (fastRecovery) 1000 else 2000)
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun tryReflectionPort(port: Int, desc: String, fastRecovery: Boolean): Boolean {
        try {
            callbacks.log("Attempting $desc...", LogType.WARNING)
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            socket = m.invoke(device, port) as BluetoothSocket
            socket?.connect()
            callbacks.log("$desc established.", LogType.SUCCESS)
            return true
        } catch (e: Exception) {
            callbacks.log("$desc failed: ${e.message}", LogType.ERROR)
            Log.w("BT", "$desc failed", e)
            closeSocketSafely()
            if (!cancelled) safeSleep(if (fastRecovery) 600 else 1000)
        }
        return false
    }

    private fun readLoop() {
        val buffer = ByteArray(1024)
        val packetBuffer = ByteArray(20)
        var bufferIndex = 0
        var consecutiveErrors = 0

        while (!cancelled && isConnected) {
            try {
                val bytesRead = inputStream?.read(buffer) ?: -1
                if (bytesRead > 0) {
                    consecutiveErrors = 0
                    val data = buffer.copyOf(bytesRead)
                    callbacks.onDataReceived(data)

                    // Packet parsing (looking for 0xAA ... 0x55)
                    for (byte in data) {
                        if (bufferIndex == 0 && byte != 0xAA.toByte()) continue

                        packetBuffer[bufferIndex++] = byte

                        if (bufferIndex >= 10) {
                            if (packetBuffer[9] == 0x55.toByte()) {
                                val packet = packetBuffer.copyOf(10)
                                val result = TelemetryParser.parse(packet)
                                if (result != null) {
                                    callbacks.log(
                                        "Telemetry: Bat=${result.batteryVoltage}V, Status=${result.status}",
                                        LogType.SUCCESS
                                    )
                                }
                            }
                            bufferIndex = 0
                        }
                    }
                }
            } catch (e: IOException) {
                consecutiveErrors++
                if (consecutiveErrors >= 3) {
                    Log.e("BT", "Disconnected after $consecutiveErrors errors", e)
                    callbacks.log("Disconnected: ${e.message}", LogType.ERROR)
                    isConnected = false
                    callbacks.onDisconnected(e.message ?: "Unknown error")
                    callbacks.onStateChanged(ConnectionState.DISCONNECTED)
                    break
                } else {
                    Log.w("BT", "Transient read error $consecutiveErrors/3", e)
                    callbacks.log("Read error $consecutiveErrors/3 - retrying...", LogType.WARNING)
                    try {
                        Thread.sleep(50)
                    } catch (ie: InterruptedException) {
                        break
                    }
                }
            }
        }
    }

    override fun write(bytes: ByteArray) {
        try {
            outputStream?.write(bytes)
        } catch (e: IOException) {
            Log.e("BT", "Write failed - triggering reconnect", e)
            callbacks.log("Write failed: ${e.message} - reconnecting...", LogType.ERROR)
            callbacks.onStateChanged(ConnectionState.DISCONNECTED)
            isConnected = false
            cancel()
        }
    }

    override fun cancel() {
        cancelled = true
        closeSocketSafely()
    }

    // Classic SPP does not support remote RSSI polling
    // Default implementation from interface is used

    private fun closeSocketSafely() {
        try {
            socket?.close()
            socket = null
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
}
