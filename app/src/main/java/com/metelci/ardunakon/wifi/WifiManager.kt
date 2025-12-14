@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.bluetooth.TelemetryParser
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import com.metelci.ardunakon.security.EncryptionException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class WifiManager(
    private val context: Context,
    private val connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
    private val onLog: (String) -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val encryptionPreferences: WifiEncryptionPreferences = WifiEncryptionPreferences(context)
) {
    companion object {
        private val DEFAULT_R4_WIFI_PSK: ByteArray =
            "ArdunakonSecretKey1234567890ABCD".toByteArray(Charsets.UTF_8)
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        // Filter out expected socket exceptions (timeouts and socket closed during disconnect)
        val isExpectedException = throwable is java.net.SocketTimeoutException || 
                                   throwable is java.net.SocketException
        
        if (!isExpectedException) {
            Log.e("WifiManager", "Uncaught exception in scope", throwable)
            onLog("Critical Error: ${throwable.message}")
            com.metelci.ardunakon.crash.CrashHandler.logException(context, throwable, "Uncaught WiFi Exception")
        }
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob() + exceptionHandler)
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var targetIp: String = "192.168.4.1"
    private var targetPort: Int = 8888
    private val isConnected = AtomicBoolean(false)
    private val sessionKey = AtomicReference<ByteArray?>(null)
    private val discoveryNonce = AtomicReference<String?>(null)
    private val secureRandom = SecureRandom()
    // Encryption is optional: if negotiated it's a plus; otherwise we operate in plaintext.
    // This flag controls whether we *attempt* to negotiate and whether failures are surfaced.
    private val requireEncryption = AtomicBoolean(false)

    // Connection State
    private val _connectionState = MutableStateFlow(WifiConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    // Auto-Reconnect
    private val _autoReconnectEnabled = MutableStateFlow(false)
    val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()
    private var _shouldReconnect = false
    private var reconnectAttempts = 0
    private var nextReconnectAt = 0L

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

    // DoS Protection: Rate limit discovery responses per IP
    private val discoveryRateLimiter = mutableMapOf<String, Long>()
    private val discoveryRateLimitMs = 1000L // Limit to 1 response per second per IP

    // Telemetry State (Matches BluetoothManager for consistency)
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry = _telemetry.asStateFlow()

    // Encryption Error State - exposed for UI to show blocking dialogs
    private val _encryptionError = MutableStateFlow<EncryptionException?>(null)
    val encryptionError = _encryptionError.asStateFlow()

    // Encryption Status - true when session key is established via handshake
    private val _isEncrypted = MutableStateFlow(false)
    val isEncrypted = _isEncrypted.asStateFlow()

    // Handshake constants
    private val handshakeTimeoutMs = 5000L

    // Packet stats for WiFi
    private var packetsSent = 0L
    private var packetsFailed = 0L
    // UDP doesn't really have "dropped" in the same queue-sense as BLE, so we can ignore or count send errors


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
     * Injects/updates the current session encryption key when an external flow establishes one.
     * When present, discovery replies are authenticated and control packets are AES-GCM encrypted.
     */
    fun setSessionKey(key: ByteArray?) {
        sessionKey.set(key)
    }

    /**
     * Sets whether encryption is required for all outgoing packets.
     * When true, sendData() will fail and notify the user if encryption cannot be performed.
     */
    fun setRequireEncryption(required: Boolean) {
        requireEncryption.set(required)
    }

    /**
     * Returns whether encryption is currently required.
     */
    fun isEncryptionRequired(): Boolean = requireEncryption.get()

    /**
     * Clears any pending encryption error.
     */
    fun clearEncryptionError() {
        _encryptionError.value = null
    }

    init {
        scope.launch {
            val lastConn = connectionPreferences.loadLastConnection()
            _shouldReconnect = lastConn.autoReconnectWifi
            _autoReconnectEnabled.value = lastConn.autoReconnectWifi
            
            if (!lastConn.wifiIp.isNullOrEmpty()) {
                targetIp = lastConn.wifiIp
                targetPort = lastConn.wifiPort
                if (_shouldReconnect) onLog("Restored WiFi target: $targetIp and armed auto-reconnect")
            }
        }
        startReconnectMonitor()
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
        _shouldReconnect = enabled
        scope.launch {
            connectionPreferences.saveLastConnection(autoReconnectWifi = enabled)
        }
        if (enabled) {
            reconnectAttempts = 0
            onLog("WiFi Auto-reconnect ARMED")
        } else {
            onLog("WiFi Auto-reconnect DISABLED")
        }
    }

    /**
     * Builds the discovery message that will be broadcast when scanning.
     * Returns the raw bytes and the nonce (when present) for verification.
     */
    internal fun buildDiscoveryMessage(): Pair<ByteArray, String?> {
        val key = sessionKey.get()
        return if (key != null) {
            val nonceBytes = ByteArray(16).also(secureRandom::nextBytes)
            val nonce = Base64.encodeToString(nonceBytes, Base64.NO_WRAP)
            val sig = Base64.encodeToString(hmac(nonceBytes, key), Base64.NO_WRAP)
            discoveryNonce.set(nonce)
            "ARDUNAKON_DISCOVER|$nonce|$sig".toByteArray() to nonce
        } else {
            "ARDUNAKON_DISCOVER".toByteArray() to null
        }
    }

    fun startDiscovery() {
        if (isScanning.get()) return
        isScanning.set(true)
        _scannedDevices.value = emptyList()

        // Ensure Arduino R4 WiFi AP is discoverable even if mDNS/UDP broadcast is blocked
        addDevice(
            name = "Arduino R4 WiFi (AP mode)",
            ip = "192.168.4.1",
            port = 8888,
            trusted = false
        )

        val canUseMdns =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNearbyWifiPermission()
        if (!canUseMdns) {
            onLog("WiFi discovery: NEARBY_WIFI_DEVICES not granted, skipping mDNS scan (UDP still runs)")
        }

        // Acquire Multicast Lock
        if (!hasWifiStatePermission()) {
            onLog("WiFi discovery requires ACCESS_WIFI_STATE permission")
            isScanning.set(false)
            return
        }
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        multicastLock = try {
            wifi.createMulticastLock("ArdunakonMulticastLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: SecurityException) {
            onLog("WiFi discovery failed: Missing multicast permission")
            Log.e("WifiManager", "Missing WiFi permission", e)
            isScanning.set(false)
            return
        }

        // Calculate Broadcast Address
        val broadcastAddress = getBroadcastAddress()
        onLog("Broadcast Address: ${broadcastAddress?.hostAddress ?: "Unknown"}")

        // 1. Start UDP Broadcast Scan (Existing + improved)
        scope.launch {
            var discoverySocket: DatagramSocket? = null
            try {
                discoverySocket = try {
                    DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        soTimeout = 2000
                        // Bind to the same port Arduino broadcasts to, so we can receive unsolicited beacons.
                        bind(InetSocketAddress(8888))
                    }
                } catch (e: Exception) {
                    onLog("UDP discovery: couldn't bind to port 8888 (${e.message}); using ephemeral port")
                    DatagramSocket().apply {
                        broadcast = true
                        soTimeout = 2000
                    }
                }

                val (buffer, _) = buildDiscoveryMessage()
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
                            val senderIp = receivePacket.address.hostAddress ?: "Unknown"
                            
                            // DoS Protection: Rate limit responses from same IP
                            val now = System.currentTimeMillis()
                            val lastResponse = discoveryRateLimiter[senderIp] ?: 0L
                            if (now - lastResponse < discoveryRateLimitMs) {
                                onLog("Rate-limiting discovery from $senderIp (flood protection)")
                                continue
                            }
                            discoveryRateLimiter[senderIp] = now
                            
                            val payload = response.substringAfter("ARDUNAKON_DEVICE:")
                            val parts = payload.split("|")
                            val name = parts.firstOrNull().orEmpty()
                            onLog("Found Device: $name ($senderIp)")
                            val key = sessionKey.get()
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
                            addDevice(name, senderIp, 8888, trusted)
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
        if (canUseMdns) {
            try {
                stopDiscoveryListener() // Safety clear
                discoveryListener = object : android.net.nsd.NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e("WifiManager", "mDNS Start Failed: $errorCode")
                        onLog("mDNS Start Failed: $errorCode")
                        try {
                            stopDiscoveryListener()
                        } catch (_: Exception) {}
                    }
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e("WifiManager", "mDNS Stop Failed: $errorCode")
                        try {
                            stopDiscoveryListener()
                        } catch (_: Exception) {}
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
        
        // Clear rate limiter to prevent memory leak
        discoveryRateLimiter.clear()
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
        nsdManager?.resolveService(
            serviceInfo,
            object : android.net.nsd.NsdManager.ResolveListener {
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
            }
        )
    }

    // Internal for test visibility
    internal fun addDevice(name: String, ip: String, port: Int, trusted: Boolean = false) {
        scope.launch {
            try {
                val currentList = _scannedDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.ip == ip }
                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    val resolvedName =
                        if ((existing.name.isBlank() || existing.name == "Arduino R4 WiFi (AP mode)") &&
                            name.isNotBlank()
                        ) {
                            name
                        } else {
                            existing.name
                        }
                    val resolvedPort = when {
                        existing.port == 8888 || port == 8888 -> 8888
                        else -> existing.port
                    }
                    val updated = existing.copy(
                        name = resolvedName,
                        port = resolvedPort,
                        trusted = existing.trusted || trusted
                    )
                    if (updated != existing) {
                        currentList[existingIndex] = updated
                        _scannedDevices.value = currentList.toList()
                    }
                } else {
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
                lastRxTime = System.currentTimeMillis() // Initialize to prevent immediate timeout

                val encryptionReady = establishEncryptionIfRequired()
                if (!encryptionReady) return@launch

                _connectionState.value = WifiConnectionState.CONNECTED
                onLog("WiFi: Connected to $ip:$port (UDP${if (_isEncrypted.value) ", encrypted" else ""})")
                
                // Save connection details
                scope.launch {
                    connectionPreferences.saveLastConnection(
                        type = "WIFI",
                        wifiIp = ip,
                        wifiPort = port
                    )
                }
                
                startReceiving()
                startRssiMonitor()
                startPing()
                startTimeoutMonitor()
                
                reconnectAttempts = 0 // Reset circuit breaker
            } catch (e: Exception) {
                Log.e("WifiManager", "Connection failed", e)
                onLog("WiFi: Connection failed - ${e.message}")
                com.metelci.ardunakon.crash.CrashHandler.logException(context, e, "WiFi Connection Failed")
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
        _isEncrypted.value = false
        sessionKey.set(null)
        if (wasConnected) {
            onLog("WiFi: Disconnected")
        }
    }

    /**
     * Cancels background coroutines and closes sockets/listeners.
     * Intended for app shutdown and JVM unit tests (Robolectric) to avoid hanging non-daemon work.
     */
    fun cleanup() {
        try {
            stopDiscovery()
        } catch (_: Exception) {}
        try {
            stopDiscoveryListener()
        } catch (_: Exception) {}
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        multicastLock = null

        try {
            disconnect()
        } catch (_: Exception) {}

        try {
            scope.cancel()
        } catch (_: Exception) {}
    }

    private suspend fun establishEncryptionIfRequired(): Boolean {
        val requested = requireEncryption.get()
        return try {
            val storedPsk = encryptionPreferences.loadPsk(targetIp)

            // Encryption is optional: don't block first-time connections unless explicitly requested.
            if (!requested && storedPsk == null) return true

            val candidates = listOfNotNull(
                storedPsk,
                // Only try the built-in default when encryption was explicitly requested.
                DEFAULT_R4_WIFI_PSK.takeIf { requested }
            ).distinctBy { it.contentHashCode() }

            for (psk in candidates) {
                val success = performEncryptionHandshake(psk, reportErrors = requested)
                if (success) {
                    if (storedPsk == null || !storedPsk.contentEquals(psk)) {
                        try {
                            encryptionPreferences.savePsk(targetIp, psk)
                        } catch (e: Exception) {
                            onLog("WiFi: Encryption established but PSK could not be saved: ${e.message}")
                        }
                    }
                    return true
                }
            }

            onLog("WiFi: Encryption not available, continuing without encryption")
            _isEncrypted.value = false
            sessionKey.set(null)
            if (!requested) _encryptionError.value = null
            true
        } catch (e: Exception) {
            Log.e("WifiManager", "Encryption negotiation failed", e)
            onLog("WiFi: Encryption negotiation failed, continuing without encryption")
            _isEncrypted.value = false
            sessionKey.set(null)
            if (!requested) _encryptionError.value = null
            true
        }
    }

    private fun cleanupAfterHandshakeFailure() {
        isConnected.set(false)
        receiveJob?.cancel()
        socket?.close()
        socket = null
        _isEncrypted.value = false
        sessionKey.set(null)
    }

    /**
     * Perform encryption handshake with device using PSK.
     * Returns true if handshake succeeded and session key is established.
     *
     * @param psk Pre-shared key (32 bytes) for this device
     */
    suspend fun performEncryptionHandshake(psk: ByteArray, reportErrors: Boolean = true): Boolean =
        withContext(ioDispatcher) {
        if (!isConnected.get()) {
            onLog("WiFi: Cannot handshake - not connected")
            return@withContext false
        }

        try {
            val negotiator = com.metelci.ardunakon.security.SessionKeyNegotiator(psk)
            val appNonce = negotiator.startHandshake()

            // Send handshake request
            val requestPacket = com.metelci.ardunakon.protocol.ProtocolManager.formatHandshakeRequest(appNonce)
            val address = InetAddress.getByName(targetIp)
            val sendPacket = DatagramPacket(requestPacket, requestPacket.size, address, targetPort)
            socket?.send(sendPacket)
            onLog("WiFi: Handshake request sent")

            // Wait for response
            val responseBuffer = ByteArray(64)
            val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket?.soTimeout = handshakeTimeoutMs.toInt()

            try {
                socket?.receive(receivePacket)
            } catch (e: java.net.SocketTimeoutException) {
                onLog("WiFi: Handshake timeout - device may not support encryption")
                if (reportErrors) {
                    _encryptionError.value = EncryptionException.HandshakeFailedException(
                        "Device did not respond to handshake request"
                    )
                }
                return@withContext false
            }

            // Parse response
            val responseData = receivePacket.data.copyOf(receivePacket.length)
            val parsed = com.metelci.ardunakon.protocol.ProtocolManager.parseHandshakeResponse(responseData)
            if (parsed == null) {
                onLog("WiFi: Invalid handshake response")
                if (reportErrors) {
                    _encryptionError.value = EncryptionException.HandshakeFailedException(
                        "Invalid handshake response from device"
                    )
                }
                return@withContext false
            }

            val (deviceNonce, signature) = parsed

            // Complete handshake
            val newSessionKey = negotiator.completeHandshake(deviceNonce, signature)
            setSessionKey(newSessionKey)
            _isEncrypted.value = true

            // Send acknowledgment
            val ackPacket = com.metelci.ardunakon.protocol.ProtocolManager.formatHandshakeComplete()
            val ackSendPacket = DatagramPacket(ackPacket, ackPacket.size, address, targetPort)
            socket?.send(ackSendPacket)

            // Reset socket timeout
            socket?.soTimeout = 0

            onLog("WiFi: Encryption established ✓")
            true

        } catch (e: EncryptionException.HandshakeFailedException) {
            Log.e("WifiManager", "Handshake failed", e)
            if (reportErrors) _encryptionError.value = e
            onLog("WiFi: Handshake failed - ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("WifiManager", "Handshake error", e)
            if (reportErrors) {
                _encryptionError.value = EncryptionException.HandshakeFailedException(
                    "Unexpected error during handshake: ${e.message}"
                )
            }
            onLog("WiFi: Handshake error - ${e.message}")
            if (reportErrors) {
                com.metelci.ardunakon.crash.CrashHandler.logException(context, e, "WiFi Handshake Error")
            }
            false
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
                packetsSent++
            } catch (e: EncryptionException) {
                Log.e("WifiManager", "Encryption error", e)
                val requested = requireEncryption.get()
                if (requested) {
                    _encryptionError.value = e
                    onLog("Encryption Error: ${e.message}")
                } else {
                    onLog("Encryption unavailable, falling back to plaintext: ${e.message}")
                }

                // Best-effort downgrade to plaintext so safe-network users aren't blocked.
                _isEncrypted.value = false
                sessionKey.set(null)

                try {
                    val address = InetAddress.getByName(targetIp)
                    val packet = DatagramPacket(data, data.size, address, targetPort)
                    socket?.send(packet)
                    packetsSent++
                } catch (_: Exception) {
                    // ignore: original send already failed
                }
            } catch (e: Exception) {
                Log.e("WifiManager", "Send failed", e)
                onLog("TX Error (UDP): ${e.message}")
                packetsFailed++
            }
        }
    }

    // Internal for test visibility
    @Throws(EncryptionException::class)
    internal fun encryptIfNeeded(payload: ByteArray): ByteArray {
        val key = sessionKey.get()
        if (key == null) return payload
        return try {
            val iv = ByteArray(12).also(secureRandom::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            val encrypted = cipher.doFinal(payload)
            iv + encrypted
        } catch (e: Exception) {
            Log.e("WifiManager", "Encryption failed", e)
            throw EncryptionException.EncryptionFailedException(
                "Failed to encrypt packet: ${e.message}", e
            )
        }
    }

    // Internal for test visibility
    @Throws(EncryptionException::class)
    internal fun decryptIfNeeded(payload: ByteArray): ByteArray {
        val key = sessionKey.get() ?: return payload
        if (payload.size <= 12) {
            throw EncryptionException.EncryptionFailedException(
                "Encrypted packet too short to decrypt",
                IllegalArgumentException("payload length=${payload.size}")
            )
        }
        return try {
            val iv = payload.copyOfRange(0, 12)
            val cipherText = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            Log.e("WifiManager", "Decryption failed", e)
            throw EncryptionException.EncryptionFailedException(
                "Failed to decrypt packet: ${e.message}", e
            )
        }
    }

    // Internal for test visibility
    internal fun verifySignature(nonce: String, sig: String, key: ByteArray): Boolean = try {
        val nonceBytes = Base64.decode(nonce, Base64.NO_WRAP)
        val sigBytes = Base64.decode(sig, Base64.NO_WRAP)
        val expected = hmac(nonceBytes, key)
        sigBytes.contentEquals(expected)
    } catch (e: Exception) {
        false
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
                    val rawData = packet.data.copyOf(packet.length)
                    val data = try {
                        decryptIfNeeded(rawData)
                    } catch (e: EncryptionException) {
                        val requested = requireEncryption.get()
                        if (requested) {
                            _encryptionError.value = e
                            onLog("Encryption Error: ${e.message}")
                        } else {
                            onLog("Encryption unavailable, falling back to plaintext: ${e.message}")
                            _encryptionError.value = null
                        }

                        // Downgrade to plaintext for subsequent packets.
                        _isEncrypted.value = false
                        sessionKey.set(null)
                        rawData
                    }
                    _incomingData.value = data
                    
                    // Parse Telemetry using centralized parser
                    android.util.Log.d("WifiTelemetry", "Received packet, size: ${data.size}")
                    val result = TelemetryParser.parse(data)
                    if (result != null) {
                        android.util.Log.d("WifiTelemetry", "Telemetry parsed: Battery=${result.batteryVoltage}V, Status=${result.status}")
                        _telemetry.value = Telemetry(
                            batteryVoltage = result.batteryVoltage,
                            status = result.status,
                            packetsSent = packetsSent,
                            packetsDropped = 0, // Not applicable for UDP (fire and forget)
                            packetsFailed = packetsFailed
                        )
                    } else {
                        android.util.Log.d("WifiTelemetry", "TelemetryParser returned NULL - not a telemetry packet")
                    }

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
            if (!hasWifiStatePermission()) {
                onLog("RSSI monitor requires ACCESS_WIFI_STATE permission")
                return@launch
            }
            val wifiManager = context.applicationContext.getSystemService(
                Context.WIFI_SERVICE
            ) as android.net.wifi.WifiManager
            while (isActive && isConnected.get()) {
                try {
                    val info = wifiManager.connectionInfo
                    // Only if connected to the target (or any wifi really)
                    _rssi.value = info.rssi
                } catch (e: SecurityException) {
                    Log.e("WifiManager", "Missing WiFi permission", e)
                    onLog("RSSI monitor failed: Missing WiFi permission")
                    return@launch
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
                    lastPingTime = System.currentTimeMillis() // Capture time before sending
                    val pingData = com.metelci.ardunakon.protocol.ProtocolManager.formatHeartbeatData(pingSequence)
                    val address = InetAddress.getByName(targetIp)
                    val payload = encryptIfNeeded(pingData)
                    val packet = DatagramPacket(payload, payload.size, address, targetPort)
                    socket?.send(packet)

                    delay(2000) // Heartbeat interval
                } catch (e: EncryptionException) {
                    _encryptionError.value = e
                    onLog("Encryption Error: ${e.message}")
                    if (requireEncryption.get()) {
                        disconnect()
                        return@launch
                    }
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

    private fun startReconnectMonitor() {
        scope.launch {
            while (isActive) {
                delay(2000)
                if (_shouldReconnect) {
                    val state = _connectionState.value
                    if (state == WifiConnectionState.DISCONNECTED || state == WifiConnectionState.ERROR) {
                        if (System.currentTimeMillis() >= nextReconnectAt) {
                            if (reconnectAttempts >= 5) {
                                onLog("WiFi: Too many failed reconnect attempts. Disabling.")
                                setAutoReconnectEnabled(false) // Disable on too many failures
                            } else {
                                val backoff = (reconnectAttempts * 2000L).coerceAtMost(10000L) // Simple backoff
                                onLog("WiFi: Auto-reconnecting to $targetIp... (attempt ${reconnectAttempts + 1})")
                                reconnectAttempts++
                                nextReconnectAt = System.currentTimeMillis() + backoff
                                connect(targetIp, targetPort)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        if (!hasWifiStatePermission()) return null
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val dhcp = wifi.dhcpInfo ?: return null
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) quads[k] = ((broadcast shr k * 8) and 0xFF).toByte()
            InetAddress.getByAddress(quads)
        } catch (e: SecurityException) {
            Log.e("WifiManager", "Missing WiFi permission", e)
            null
        }
    }

    private fun hasWifiStatePermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED

    private fun hasNearbyWifiPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
}

enum class WifiConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class WifiDevice(val name: String, val ip: String, val port: Int, val trusted: Boolean = false)
