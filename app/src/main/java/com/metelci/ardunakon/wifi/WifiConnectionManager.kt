package com.metelci.ardunakon.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.util.Log
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.bluetooth.TelemetryParser
import com.metelci.ardunakon.protocol.ProtocolManager
import com.metelci.ardunakon.security.EncryptionException
import com.metelci.ardunakon.security.SessionKeyNegotiator
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.*

interface WifiConnectionCallback {
    fun onStateChanged(state: WifiConnectionState)
    fun onDataReceived(data: ByteArray)
    fun onRttUpdated(rtt: Long, history: List<Long>)
    fun onTelemetryUpdated(telemetry: Telemetry)
    fun onLog(message: String)
    fun onEncryptionError(error: EncryptionException)
    fun onRssiUpdated(rssi: Int)
}

class WifiConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val callback: WifiConnectionCallback,
    private val encryptionPreferences: com.metelci.ardunakon.data.WifiEncryptionPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var shouldReconnect = false
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var targetIp: String = "192.168.4.1"
    private var targetPort: Int = 8888
    private val isConnected = AtomicBoolean(false)
    private val sessionKey = AtomicReference<ByteArray?>(null)
    private val isEncrypted = AtomicBoolean(false)
    private val secureRandom = SecureRandom()
    private val requireEncryption = AtomicBoolean(true)

    private val rttHistory = mutableListOf<Long>()
    private var packetsSent = 0L
    private var packetsFailed = 0L

    private var lastPingTime = 0L
    private var lastRxTime = 0L
    private var pingSequence = 0

    private val heartbeatBaseIntervalMs = 3000L
    private val heartbeatMinIntervalMs = 1500L
    private val heartbeatMaxIntervalMs = 5000L

    private val handshakeTimeoutMs = 5000L
    private val defaultR4WifiPsk = "ArdunakonSecretKey1234567890ABCD".toByteArray()

    fun setRequireEncryption(required: Boolean) {
        requireEncryption.set(required)
    }

    fun connect(ip: String, port: Int, onPskSuccess: (ByteArray) -> Unit = {}) {
        if (isConnected.get()) disconnect()

        targetIp = ip
        targetPort = port
        callback.onStateChanged(WifiConnectionState.CONNECTING)
        callback.onLog("WiFi: Connecting to $ip:$port...")

        scope.launch(ioDispatcher) {
            try {
                socket = DatagramSocket()
                isConnected.set(true)
                lastRxTime = System.currentTimeMillis()

                val encryptionReady = establishEncryptionIfRequired(onPskSuccess)
                if (!encryptionReady) {
                    disconnect()
                    return@launch
                }

                callback.onStateChanged(WifiConnectionState.CONNECTED)
                callback.onLog("WiFi: Connected to $ip:$port (UDP${if (isEncrypted.get()) ", encrypted" else ""})")

                startReceiving()
                startPing()
                startTimeoutMonitor()
                startRssiMonitor()
            } catch (e: Exception) {
                Log.e("WifiConn", "Connection failed", e)
                callback.onLog("WiFi: Connection failed - ${e.message}")
                callback.onStateChanged(WifiConnectionState.ERROR)
                disconnect()
            }
        }
    }

    fun disconnect() {
        if (!isConnected.get()) return
        isConnected.set(false)
        receiveJob?.cancel()
        socket?.close()
        socket = null
        isEncrypted.set(false)
        sessionKey.set(null)
        callback.onStateChanged(WifiConnectionState.DISCONNECTED)
        callback.onLog("WiFi: Disconnected")
    }

    private suspend fun establishEncryptionIfRequired(onPskSuccess: (ByteArray) -> Unit): Boolean {
        val required = requireEncryption.get()
        return try {
            val storedPsk = encryptionPreferences.loadPsk(targetIp)
            val candidates = listOfNotNull(storedPsk, defaultR4WifiPsk).distinctBy { it.contentHashCode() }

            var handshakeSuccess = false
            for (psk in candidates) {
                if (performEncryptionHandshake(psk)) {
                    handshakeSuccess = true
                    onPskSuccess(psk)
                    if (storedPsk == null || !storedPsk.contentEquals(psk)) {
                        encryptionPreferences.savePsk(targetIp, psk)
                    }
                    break
                }
            }

            if (handshakeSuccess) {
                true
            } else if (required) {
                callback.onEncryptionError(
                    EncryptionException.HandshakeFailedException("Encryption required but handshake failed")
                )
                false
            } else {
                isEncrypted.set(false)
                sessionKey.set(null)
                true
            }
        } catch (e: Exception) {
            if (required) {
                callback.onEncryptionError(EncryptionException.HandshakeFailedException("Security error: ${e.message}"))
                false
            } else {
                isEncrypted.set(false)
                sessionKey.set(null)
                true
            }
        }
    }

    private suspend fun performEncryptionHandshake(psk: ByteArray): Boolean = withContext(ioDispatcher) {
        try {
            val negotiator = SessionKeyNegotiator(psk)
            val appNonce = negotiator.startHandshake()
            val requestPacket = ProtocolManager.formatHandshakeRequest(appNonce)
            val address = InetAddress.getByName(targetIp)
            socket?.send(DatagramPacket(requestPacket, requestPacket.size, address, targetPort))

            val responseBuffer = ByteArray(64)
            val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket?.soTimeout = handshakeTimeoutMs.toInt()
            socket?.receive(receivePacket)

            val responseData = receivePacket.data.copyOf(receivePacket.length)
            val (deviceNonce, signature) = ProtocolManager.parseHandshakeResponse(responseData)
                ?: return@withContext false

            sessionKey.set(negotiator.completeHandshake(deviceNonce, signature))
            isEncrypted.set(true)

            val ackPacket = ProtocolManager.formatHandshakeComplete()
            socket?.send(DatagramPacket(ackPacket, ackPacket.size, address, targetPort))
            socket?.soTimeout = 0
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendData(data: ByteArray) {
        if (!isConnected.get()) return
        scope.launch(ioDispatcher) {
            try {
                val address = InetAddress.getByName(targetIp)
                val packetData = encryptIfNeeded(data)
                socket?.send(DatagramPacket(packetData, packetData.size, address, targetPort))
                packetsSent++
            } catch (e: EncryptionException) {
                if (requireEncryption.get()) {
                    callback.onEncryptionError(e)
                    packetsFailed++
                }
            } catch (e: Exception) {
                packetsFailed++
            }
        }
    }

    private fun encryptIfNeeded(payload: ByteArray): ByteArray {
        val key = sessionKey.get() ?: return payload
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(payload)
    }

    private fun decryptIfNeeded(payload: ByteArray): ByteArray {
        val key = sessionKey.get() ?: return payload
        if (payload.size <= 12) throw EncryptionException.EncryptionFailedException("Packet too short")
        val iv = payload.copyOfRange(0, 12)
        val cipherText = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    private fun startReceiving() {
        receiveJob = scope.launch(ioDispatcher) {
            val buffer = ByteArray(1024)
            while (isActive && isConnected.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val data = try {
                        decryptIfNeeded(packet.data.copyOf(packet.length))
                    } catch (
                        e: Exception
                    ) {
                        continue
                    }

                    callback.onDataReceived(data)
                    lastRxTime = System.currentTimeMillis()

                    TelemetryParser.parse(data)?.let { result ->
                        callback.onTelemetryUpdated(
                            Telemetry(
                                batteryVoltage = result.batteryVoltage,
                                status = result.status,
                                packetsSent = packetsSent,
                                packetsDropped = 0,
                                packetsFailed = packetsFailed
                            )
                        )
                    }

                    if (lastPingTime > 0) {
                        val rtt = System.currentTimeMillis() - lastPingTime
                        if (rtt in 1..5000) {
                            rttHistory.add(0, rtt)
                            if (rttHistory.size > 40) rttHistory.removeAt(rttHistory.lastIndex)
                            callback.onRttUpdated(rtt, rttHistory.toList())
                        }
                        lastPingTime = 0L
                    }
                } catch (e: Exception) { /* continue */ }
            }
        }
    }

    private fun getOptimalHeartbeatInterval(): Long {
        val lastRtt = rttHistory.firstOrNull()
        if (lastRtt == null || lastRtt <= 0L) return heartbeatBaseIntervalMs

        // Adaptive keep-alive tuning (1.5s - 5s). Base is 3s until RTT is known.
        return when {
            lastRtt <= 30L -> heartbeatMaxIntervalMs // Excellent link: relax keep-alives
            lastRtt <= 100L -> 4000L
            lastRtt <= 250L -> heartbeatBaseIntervalMs
            lastRtt <= 800L -> 2000L
            else -> heartbeatMinIntervalMs // Very poor link: keep the session warm
        }
    }

    private fun startPing() {
        scope.launch(ioDispatcher) {
            while (isActive && isConnected.get()) {
                try {
                    pingSequence++
                    lastPingTime = System.currentTimeMillis()
                    val pingData = ProtocolManager.formatHeartbeatData(pingSequence)
                    sendData(pingData)

                    val delayMs = getOptimalHeartbeatInterval()
                    delay(delayMs)
                } catch (e: Exception) {
                    delay(2000)
                }
            }
        }
    }

    private fun startTimeoutMonitor() {
        scope.launch {
            while (isActive && isConnected.get()) {
                delay(1000)
                if (System.currentTimeMillis() - lastRxTime > 10000L) {
                    callback.onLog("WiFi: Connection timed out")
                    disconnect()
                }
            }
        }
    }

    private fun startRssiMonitor() {
        scope.launch(ioDispatcher) {
            val appContext = context.applicationContext
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            while (isActive && isConnected.get()) {
                try {
                    getCurrentWifiRssi(connectivityManager, wifiManager)?.let { callback.onRssiUpdated(it) }
                } catch (_: Exception) {}
                delay(2000)
            }
        }
    }

    private fun getCurrentWifiRssi(
        connectivityManager: ConnectivityManager?,
        wifiManager: android.net.wifi.WifiManager?
    ): Int? {
        // Preferred: ConnectivityManager + NetworkCapabilities TransportInfo (avoids deprecated WifiManager.connectionInfo).
        try {
            val network = connectivityManager?.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
            val transportInfo = capabilities.transportInfo
            val wifiInfo = transportInfo as? WifiInfo
            if (wifiInfo != null) {
                val rssi = wifiInfo.rssi
                // Typical RSSI range is about -127..0. Treat other values as unknown.
                if (rssi in -127..0) return rssi
            }
        } catch (_: Exception) {
            // Fall back below.
        }

        // Fallback: reflection (legacy devices / stacks).
        return try {
            val wm = wifiManager ?: return null
            val getConnectionInfo = wm.javaClass.getMethod("getConnectionInfo")
            val info = getConnectionInfo.invoke(wm)
            val getRssi = info?.javaClass?.getMethod("getRssi") ?: return null
            (getRssi.invoke(info) as? Int)
        } catch (_: Exception) {
            null
        }
    }

    fun isConnected() = isConnected.get()
    fun isEncrypted() = isEncrypted.get()
}
