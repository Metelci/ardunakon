package com.metelci.ardunakon.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.metelci.ardunakon.model.LogType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ClassicConnectionManager(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
    private val callback: ConnectionCallback
) : BluetoothConnectionManager {

    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private val isConnecting = AtomicBoolean(false)

    // Standard SPP UUID
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    override fun connect(device: BluetoothDevice) {
        if (isConnecting.getAndSet(true)) {
            callback.onError("Connection already in progress", LogType.WARNING)
            return
        }

        disconnect() // Clean up existing connection

        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    override fun disconnect() {
        isConnecting.set(false)
        connectThread?.cancel()
        connectThread = null
        connectedThread?.cancel()
        connectedThread = null
        callback.onStateChanged(ConnectionState.DISCONNECTED)
    }

    override fun send(data: ByteArray) {
        connectedThread?.write(data)
    }

    override fun requestRssi() {
        // Not supported on Classic
    }

    override fun cleanup() {
        disconnect()
    }

    override fun getPacketStats(): NetworkStats {
        // No packet stats tracking in legacy classic threads currently, returning 0
        return NetworkStats(0, 0, 0)
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Legacy inferred from manifest
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null
        @Volatile private var cancelled = false

        override fun run() {
            try {
                if (!checkBluetoothPermission()) {
                    callback.onError("Connect failed: Missing permissions", LogType.ERROR)
                    callback.onStateChanged(ConnectionState.ERROR)
                    return
                }

                val deviceName = device.name ?: "Unknown"
                callback.onError("Connecting to $deviceName...", LogType.INFO) // Info log via error channel as generic log

                val manufacturer = Build.MANUFACTURER.uppercase()
                // Xiaomi/Redmi/Poco aggressive connection handling
                val aggressiveXiaomi = manufacturer.contains("XIAOMI") || 
                                     manufacturer.contains("REDMI") || 
                                     manufacturer.contains("POCO")

                if (aggressiveXiaomi) {
                    safeSleep(300)
                }

                var connected = false
                var reflectionAllowed = true // Should carry over from config if possible, defaulting true for now

                // Attempt 1: Standard Insecure SPP
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    socket?.connect()
                    connected = true
                    callback.onError("Standard SPP connection established.", LogType.SUCCESS)
                } catch (e: IOException) {
                    Log.w("BT", "Standard SPP failed: ${e.message}", e)
                    closeSocketSafely(socket)
                    socket = null
                    if (!cancelled) safeSleep(if (aggressiveXiaomi) 400 else 200)
                }

                // Attempt 2: Reflection (Port 1)
                if (!connected && !cancelled) {
                    try {
                        callback.onError("Trying fallback (Reflection Port 1)...", LogType.WARNING)
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                        socket?.connect()
                        connected = true
                        callback.onError("Reflection connection established.", LogType.SUCCESS)
                    } catch (e: Exception) {
                        Log.w("BT", "Reflection failed: ${e.message}", e)
                        closeSocketSafely(socket)
                        socket = null
                    }
                }

                if (connected && !cancelled) {
                    // Start Connected Thread
                    val s = socket
                    if (s != null) {
                        connectedThread = ConnectedThread(s)
                        connectedThread?.start()
                        callback.onStateChanged(ConnectionState.CONNECTED)
                    } else {
                        throw IOException("Socket is null after connect")
                    }
                } else if (!cancelled) {
                    callback.onError("All connection attempts failed", LogType.ERROR)
                    callback.onStateChanged(ConnectionState.ERROR)
                }

            } catch (e: Exception) {
                if (!cancelled) {
                    Log.e("BT", "Fatal connect error", e)
                    callback.onError("Connect error: ${e.message}", LogType.ERROR)
                    callback.onStateChanged(ConnectionState.ERROR)
                }
            } finally {
                isConnecting.set(false)
            }
        }

        fun cancel() {
            cancelled = true
            closeSocketSafely(socket)
        }

        private fun closeSocketSafely(socket: BluetoothSocket?) {
            try {
                socket?.close()
                safeSleep(100)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer = ByteArray(1024)
        @Volatile private var isRunning = true

        override fun run() {
            while (isRunning) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        callback.onDataReceived(data)
                    } else if (bytesRead == -1) {
                         // End of stream
                        break
                    }
                } catch (e: IOException) {
                    if (isRunning) {
                        Log.e("BT", "Read failed", e)
                        callback.onError("Connection lost", LogType.ERROR)
                        callback.onStateChanged(ConnectionState.DISCONNECTED)
                        break
                    }
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e("BT", "Write failed", e)
                callback.onError("Send failed", LogType.ERROR)
                // Let the read loop handle disconnect logic usually, but we can signal error too
            }
        }

        fun cancel() {
            isRunning = false
            try {
                socket.close()
            } catch (e: IOException) {
                // Ignore
            }
        }
    }

    private fun safeSleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
