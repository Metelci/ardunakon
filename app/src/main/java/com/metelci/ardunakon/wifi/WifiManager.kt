@file:Suppress("DEPRECATION")
package com.metelci.ardunakon.wifi

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class WifiManager(
    private val context: Context,
    private val onLog: (String) -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var targetIp: String = "192.168.4.1"
    private var targetPort: Int = 8888
    private val isConnected = AtomicBoolean(false)
    private val sessionKey = AtomicReference<ByteArray?>(null)
    private val discoveryNonce = AtomicReference<String?>(null)
    private val secureRandom = SecureRandom()

    // Connection State
    private val _connectionState = MutableStateFlow(WifiConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    // RSSI (Simulated for WiFi as direct RSSI from socket is hard, can get from Android WifiManager)
    private val _rssi = MutableStateFlow(0)
    val rssi = _rssi.asStateFlow()

    // RTT (Round Trip Time)
    private val _rtt = MutableStateFlow(0L)
    val rtt = _rtt.asStateFlow()

    private val _rttHistory = MutableStateFlow<List<Long>>(emptyList())
    val rttHistory = _rttHistory.asStateFlow()

    // Incoming Data
    private val _incomingData = MutableStateFlow<ByteArray?>(null)
    val incomingData = _incomingData.asStateFlow()

    // Scanned Devices
    private val _scannedDevices = MutableStateFlow<List<WifiDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()
    private var isScanning = AtomicBoolean(false)

    // NsdManager for mDNS (nullable - may not be available on all devices)
    private val nsdManager: android.net.nsd.NsdManager? by lazy { 
        try {
            context.getSystemService(Context.NSD_SERVICE) as? android.net.nsd.NsdManager
        } catch (e: Exception) {
            Log.e("WifiManager", "NsdManager not available", e)
            null
        }
    }
    private var discoveryListener: android.net.nsd.NsdManager.DiscoveryListener? = null
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    /**
     * Injects/updates the current session encryption key derived from the verification handshake.
     * When present, discovery replies are authenticated and control packets are AES-GCM encrypted.
     */
    fun setSessionKey(key: ByteArray?) {
        sessionKey.set(key)
    }

    fun startDiscovery() {
        if (isScanning.get()) return
        isScanning.set(true)
        _scannedDevices.value = emptyList()

        // Acquire Multicast Lock
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        multicastLock = wifi.createMulticastLock("ArdunakonMulticastLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        // Calculate Broadcast Address
        val broadcastAddress = getBroadcastAddress()
        onLog("Broadcast Address: ${broadcastAddress?.hostAddress ?: "Unknown"}")

        // 1. Start UDP Broadcast Scan (Existing + improved)
        scope.launch {
            var discoverySocket: DatagramSocket? = null
            try {
                discoverySocket = DatagramSocket()
                discoverySocket.broadcast = true
                discoverySocket.soTimeout = 2000 

                val key = sessionKey.get()
                val message = if (key != null) {
                    val nonceBytes = ByteArray(16).also(secureRandom::nextBytes)
                    val nonce = Base64.encodeToString(nonceBytes, Base64.NO_WRAP)
                    discoveryNonce.set(nonce)
                    val sig = Base64.encodeToString(hmac(nonceBytes, key), Base64.NO_WRAP)
                    "ARDUNAKON_DISCOVER|$nonce|$sig"
                } else {
                    "ARDUNAKON_DISCOVER"
                }
                val buffer = message.toByteArray()
                val packet = DatagramPacket(
                    buffer,
                    buffer.size,
                    InetAddress.getByName("255.255.255.255"),
                    8888
                )
                discoverySocket.send(packet)
                onLog("To 255.255.255.255: Sent")

                // Send to calculated broadcast address as well
                if (broadcastAddress != null) {
                    val subnetPacket = DatagramPacket(
                        buffer,
                        buffer.size,
                        broadcastAddress,
                        8888
                    )
                    discoverySocket.send(subnetPacket)
                    onLog("To ${broadcastAddress.hostAddress}: Sent")
                }

                val receiveBuffer = ByteArray(1024)
                val endTime = System.currentTimeMillis() + 5000
                
                while (System.currentTimeMillis() < endTime && isActive) {
                    try {
                        val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        discoverySocket.receive(receivePacket)
                        
                        val response = String(receivePacket.data, 0, receivePacket.length).trim()
                        if (response.startsWith("ARDUNAKON_DEVICE:")) {
                            val payload = response.substringAfter("ARDUNAKON_DEVICE:")
                            val parts = payload.split("|")
                            val name = parts.firstOrNull().orEmpty()
                            val ip = receivePacket.address.hostAddress ?: "Unknown"
                            onLog("Found Device: $name ($ip)")
                            val trusted = when {
                                key != null && parts.size >= 3 -> {
                                    val nonce = parts[1]
                                    val sig = parts[2]
                                    val expectedNonce = discoveryNonce.get()
                                    expectedNonce == nonce && verifySignature(nonce, sig, key)
                                }
                                key != null -> false // expected signature missing
                                else -> true // legacy un-authenticated response
                            }
                            addDevice(name, ip, 8888, trusted)
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // continue
                    }
                }
            } catch (e: Exception) {
                Log.e("WifiManager", "UDP Discovery failed", e)
                onLog("UDP Discovery failed: ${e.message}")
            } finally {
                discoverySocket?.close()
            }
        }

        // 2. Start mDNS Scan (Optional - may not work on all devices)
        try {
            stopDiscoveryListener() // Safety clear
            discoveryListener = object : android.net.nsd.NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("WifiManager", "mDNS Start Failed: $errorCode")
                    onLog("mDNS Start Failed: $errorCode")
                    try { stopDiscoveryListener() } catch (_: Exception) {}
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("WifiManager", "mDNS Stop Failed: $errorCode")
                    try { stopDiscoveryListener() } catch (_: Exception) {}
                }
                override fun onDiscoveryStarted(serviceType: String) {
                    Log.d("WifiManager", "mDNS Discovery Started")
                }
                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d("WifiManager", "mDNS Discovery Stopped")
                }
                override fun onServiceFound(serviceInfo: android.net.nsd.NsdServiceInfo) {
                    try {
                        Log.d("WifiManager", "mDNS Service Found: ${serviceInfo.serviceName}")
                        onLog("mDNS Found: ${serviceInfo.serviceName}")
                        resolveService(serviceInfo)
                    } catch (e: Exception) {
                        Log.e("WifiManager", "Error handling found service", e)
                    }
                }
                override fun onServiceLost(serviceInfo: android.net.nsd.NsdServiceInfo) {
                    Log.d("WifiManager", "mDNS Service Lost: ${serviceInfo.serviceName}")
                }
            }
            // Scan for common Arduino/IoT services
            nsdManager?.discoverServices("_http._tcp", android.net.nsd.NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            
        } catch (e: Exception) {
            Log.e("WifiManager", "mDNS Init Failed", e)
            onLog("mDNS not available: ${e.message}")
        }

        // Stop all scanning after 10 seconds
        scope.launch {
            delay(10000)
            stopDiscovery()
        }
    }

    private fun stopDiscovery() {
        isScanning.set(false)
        stopDiscoveryListener()
        
        multicastLock?.let {
            if (it.isHeld) it.release()
        }
        multicastLock = null
    }

    private fun stopDiscoveryListener() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                // Listener might not be started or already stopped
            }
            discoveryListener = null
        }
    }

    private fun resolveService(serviceInfo: android.net.nsd.NsdServiceInfo) {
        nsdManager?.resolveService(serviceInfo, object : android.net.nsd.NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: android.net.nsd.NsdServiceInfo, errorCode: Int) {
                Log.e("WifiManager", "mDNS Resolve Failed: $errorCode")
            }
            override fun onServiceResolved(serviceInfo: android.net.nsd.NsdServiceInfo) {
                val ip = serviceInfo.host.hostAddress
                val port = serviceInfo.port
                val name = serviceInfo.serviceName
                Log.d("WifiManager", "mDNS Resolved: $name @ $ip:$port")
                onLog("mDNS Resolved: $name @ $ip:$port")
                if (ip != null) {
                    addDevice(name, ip, port)
                }
            }
        })
    }

    private fun addDevice(name: String, ip: String, port: Int, trusted: Boolean = false) {
        scope.launch(Dispatchers.Main) {
            try {
                val currentList = _scannedDevices.value.toMutableList()
                // Avoid duplicates by IP
                if (currentList.none { it.ip == ip }) {
                    currentList.add(WifiDevice(name, ip, port, trusted))
                    _scannedDevices.value = currentList.toList()
                }
            } catch (e: Exception) {
                Log.e("WifiManager", "Error adding device: ${e.message}", e)
            }
        }
    }

    fun connect(ip: String, port: Int) {
        if (isConnected.get()) disconnect()

        targetIp = ip
        targetPort = port
        _connectionState.value = WifiConnectionState.CONNECTING
        onLog("WiFi: Connecting to $ip:$port...")

        scope.launch {
            try {
                socket = DatagramSocket()
                isConnected.set(true)
                _connectionState.value = WifiConnectionState.CONNECTED
                onLog("WiFi: Connected to $ip:$port (UDP)")
                lastRxTime = System.currentTimeMillis() // Initialize to prevent immediate timeout
                startReceiving()
                startRssiMonitor()
                startPing()
                startTimeoutMonitor()
            } catch (e: Exception) {
                Log.e("WifiManager", "Connection failed", e)
                onLog("WiFi: Connection failed - ${e.message}")
                _connectionState.value = WifiConnectionState.ERROR
                disconnect()
            }
        }
    }

    fun disconnect() {
        val wasConnected = isConnected.get()
        isConnected.set(false)
        receiveJob?.cancel()
        socket?.close()
        socket = null
        _connectionState.value = WifiConnectionState.DISCONNECTED
        _rssi.value = 0
        _rtt.value = 0L
        if (wasConnected) {
            onLog("WiFi: Disconnected")
        }
    }

    fun sendData(data: ByteArray) {
        if (!isConnected.get()) return
        scope.launch {
            try {
                val address = InetAddress.getByName(targetIp)
                val packetData = encryptIfNeeded(data)
                val packet = DatagramPacket(packetData, packetData.size, address, targetPort)
                socket?.send(packet)
            } catch (e: Exception) {
                Log.e("WifiManager", "Send failed", e)
                onLog("TX Error (UDP): ${e.message}")
            }
        }
    }

    private fun encryptIfNeeded(payload: ByteArray): ByteArray {
        val key = sessionKey.get() ?: return payload
        return try {
            val iv = ByteArray(12).also(secureRandom::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            val encrypted = cipher.doFinal(payload)
            iv + encrypted
        } catch (e: Exception) {
            Log.e("WifiManager", "Encryption failed, sending plaintext", e)
            payload
        }
    }

    private fun verifySignature(nonce: String, sig: String, key: ByteArray): Boolean {
        return try {
            val nonceBytes = Base64.decode(nonce, Base64.NO_WRAP)
            val sigBytes = Base64.decode(sig, Base64.NO_WRAP)
            val expected = hmac(nonceBytes, key)
            sigBytes.contentEquals(expected)
        } catch (e: Exception) {
            false
        }
    }

    private fun hmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun startReceiving() {
        receiveJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive && isConnected.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    delay(1) // Yield to avoid tight loop
                    socket?.receive(packet)
                    val data = packet.data.copyOf(packet.length)
                    _incomingData.value = data
                    onPacketReceived() // Update RTT measurement
                } catch (e: Exception) {
                    if (isActive && isConnected.get()) {
                         Log.e("WifiManager", "Receive error", e)
                    }
                }
            }
        }
    }

    // Monitor WiFi signal strength from system
    private fun startRssiMonitor() {
        scope.launch {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            while (isActive && isConnected.get()) {
                try {
                    val info = wifiManager.connectionInfo
                    // Only if connected to the target (or any wifi really)
                    _rssi.value = info.rssi
                } catch (e: SecurityException) {
                    Log.e("WifiManager", "Missing WiFi permission", e)
                } catch (e: Exception) {
                    Log.e("WifiManager", "Error reading RSSI", e)
                }
                delay(1000)
            }
        }
    }
    
    // Simple Ping for RTT - sends a packet and measures response time
    private var lastPingTime = 0L
    private var lastRxTime = 0L
    private var pingSequence = 0
    
    private fun startPing() {
        scope.launch {
            while (isActive && isConnected.get()) {
                try {
                    // Send standard Heartbeat packet
                    pingSequence++
                    val pingData = com.metelci.ardunakon.protocol.ProtocolManager.formatHeartbeatData(pingSequence)
                    val address = InetAddress.getByName(targetIp)
                    val packet = DatagramPacket(pingData, pingData.size, address, targetPort)
                    socket?.send(packet)
                    
                    delay(2000) // Heartbeat interval
                } catch (e: Exception) {
                    Log.e("WifiManager", "Ping failed", e)
                }
            }
        }
    }

    private fun startTimeoutMonitor() {
        scope.launch {
            while (isActive && isConnected.get()) {
                delay(1000)
                // Timeout if no data received for 10 seconds
                if (System.currentTimeMillis() - lastRxTime > 10000L) {
                     onLog("Connection timed out (no data for 10s)")
                     disconnect()
                }
            }
        }
    }
    
    // Called from receive loop when PONG or any data is received
    internal fun onPacketReceived() {
        lastRxTime = System.currentTimeMillis()
        if (lastPingTime > 0) {
            val rtt = System.currentTimeMillis() - lastPingTime
            if (rtt in 1..5000) { // Valid RTT range
                updateRtt(rtt)
            }
            lastPingTime = 0L
        }
    }
    
    // For manual RTT update from ProtocolManager
    fun updateRtt(newRtt: Long) {
        _rtt.value = newRtt
        val currentHistory = _rttHistory.value.toMutableList()
        currentHistory.add(0, newRtt)
        if (currentHistory.size > 40) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        _rttHistory.value = currentHistory
    }

    private fun getBroadcastAddress(): InetAddress? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val dhcp = wifi.dhcpInfo ?: return null
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = ((broadcast shr k * 8) and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }
}

enum class WifiConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class WifiDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val trusted: Boolean = false
)
