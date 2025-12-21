package com.metelci.ardunakon.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager as AndroidWifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

data class WifiDevice(val name: String, val ip: String, val port: Int, val trusted: Boolean = false)

class WifiScanner(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onLog: (String) -> Unit,
    private val buildDiscoveryMessage: () -> Pair<ByteArray, String?>,
    private val verifySignature: (String, String, ByteArray) -> Boolean,
    private val getSessionKey: () -> ByteArray?,
    private val getDiscoveryNonce: () -> String?
) {
    private val _scannedDevices = MutableStateFlow<List<WifiDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val discoveryRateLimiter = mutableMapOf<String, Long>()
    private val discoveryRateLimitMs = 1000L
    private val discoveryTimeoutMs = 4000L

    private val nsdManager: NsdManager? by lazy {
        try {
            context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        } catch (e: Exception) {
            Log.e("WifiScanner", "NsdManager not available", e)
            null
        }
    }
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: AndroidWifiManager.MulticastLock? = null

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scannedDevices.value = emptyList()

        addDevice("Arduino R4 WiFi (AP mode)", "192.168.4.1", 8888, false)

        val canUseMdns = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNearbyWifiPermission()
        if (!canUseMdns) {
            onLog("WiFi discovery: NEARBY_WIFI_DEVICES not granted, skipping mDNS scan")
        }

        if (!hasWifiStatePermission()) {
            onLog("WiFi discovery requires ACCESS_WIFI_STATE permission")
            _isScanning.value = false
            return
        }

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as AndroidWifiManager
        multicastLock = try {
            wifi.createMulticastLock("ArdunakonMulticastLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: SecurityException) {
            onLog("WiFi discovery failed: Missing multicast permission")
            _isScanning.value = false
            return
        }

        val broadcastAddress = getBroadcastAddress()
        
        scope.launch {
            var discoverySocket: DatagramSocket? = null
            try {
                discoverySocket = try {
                    DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        soTimeout = discoveryTimeoutMs.toInt()
                        bind(InetSocketAddress(8888))
                    }
                } catch (e: Exception) {
                    onLog("UDP discovery: couldn't bind to port 8888; using ephemeral port")
                    DatagramSocket().apply {
                        broadcast = true
                        soTimeout = discoveryTimeoutMs.toInt()
                    }
                }

                val (buffer, _) = buildDiscoveryMessage()
                val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName("255.255.255.255"), 8888)
                discoverySocket.send(packet)

                broadcastAddress?.let {
                    val subnetPacket = DatagramPacket(buffer, buffer.size, it, 8888)
                    discoverySocket.send(subnetPacket)
                }

                val receiveBuffer = ByteArray(1024)
                val endTime = System.currentTimeMillis() + discoveryTimeoutMs

                while (_isScanning.value && System.currentTimeMillis() < endTime && isActive) {
                    try {
                        val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        discoverySocket.receive(receivePacket)

                        val response = String(receivePacket.data, 0, receivePacket.length).trim()
                        if (response.startsWith("ARDUNAKON_DEVICE:")) {
                            val senderIp = receivePacket.address.hostAddress ?: "Unknown"
                            val now = System.currentTimeMillis()
                            val lastResponse = discoveryRateLimiter[senderIp] ?: 0L
                            if (now - lastResponse < discoveryRateLimitMs) continue
                            discoveryRateLimiter[senderIp] = now
                            
                            val payload = response.substringAfter("ARDUNAKON_DEVICE:")
                            val parts = payload.split("|")
                            val name = parts.firstOrNull().orEmpty()
                            
                            val key = getSessionKey()
                            val trusted = when {
                                key != null && parts.size >= 3 -> {
                                    val nonce = parts[1]
                                    val sig = parts[2]
                                    val expectedNonce = getDiscoveryNonce()
                                    expectedNonce == nonce && verifySignature(nonce, sig, key)
                                }
                                key != null -> false
                                else -> true
                            }
                            addDevice(name, senderIp, 8888, trusted)
                        }
                    } catch (e: java.net.SocketTimeoutException) { /* continue */ }
                }
            } catch (e: Exception) {
                Log.e("WifiScanner", "UDP Discovery failed", e)
            } finally {
                discoverySocket?.close()
            }
        }

        if (canUseMdns) {
            startMdnsScan()
        }

        scope.launch {
            delay(discoveryTimeoutMs)
            stopDiscovery()
        }
    }

    private fun startMdnsScan() {
        try {
            stopDiscoveryListener()
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = stopDiscoveryListener()
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = stopDiscoveryListener()
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    resolveService(serviceInfo)
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            }
            nsdManager?.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("WifiScanner", "mDNS Init Failed", e)
        }
    }

    fun stopDiscovery() {
        _isScanning.value = false
        stopDiscoveryListener()
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
        discoveryRateLimiter.clear()
    }

    private fun stopDiscoveryListener() {
        discoveryListener?.let {
            try { nsdManager?.stopServiceDiscovery(it) } catch (_: Exception) {}
            discoveryListener = null
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val manager = nsdManager ?: return
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val ip = getResolvedHostAddress(serviceInfo)
                if (ip != null) addDevice(serviceInfo.serviceName, ip, serviceInfo.port)
            }
        }

        // Resolve via reflection to avoid compile-time references to deprecated overloads on newer SDKs.
        val executor = ContextCompat.getMainExecutor(context)
        try {
            val modern = manager.javaClass.getMethod(
                "resolveService",
                NsdServiceInfo::class.java,
                java.util.concurrent.Executor::class.java,
                NsdManager.ResolveListener::class.java
            )
            modern.invoke(manager, serviceInfo, executor, listener)
            return
        } catch (_: Exception) {
            // Fall back below.
        }

        try {
            val legacy = manager.javaClass.getMethod(
                "resolveService",
                NsdServiceInfo::class.java,
                NsdManager.ResolveListener::class.java
            )
            legacy.invoke(manager, serviceInfo, listener)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun getResolvedHostAddress(serviceInfo: NsdServiceInfo): String? {
        // API 34+ provides hostAddresses; older versions have host. Avoid deprecated references by reflection.
        val host: InetAddress? =
            try {
                val method = serviceInfo.javaClass.getMethod("getHostAddresses")
                val value = method.invoke(serviceInfo)
                when (value) {
                    is Array<*> -> value.firstOrNull() as? InetAddress
                    is List<*> -> value.firstOrNull() as? InetAddress
                    else -> null
                }
            } catch (_: Exception) {
                null
            } ?: try {
                val method = serviceInfo.javaClass.getMethod("getHost")
                method.invoke(serviceInfo) as? InetAddress
            } catch (_: Exception) {
                null
            }

        return host?.hostAddress
    }

    private fun addDevice(name: String, ip: String, port: Int, trusted: Boolean = false) {
        scope.launch {
            val currentList = _scannedDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.ip == ip }
            if (existingIndex >= 0) {
                val existing = currentList[existingIndex]
                val updated = existing.copy(
                    name = if (existing.name.isBlank() || existing.name.contains("AP mode")) name else existing.name,
                    port = if (port == 8888) 8888 else existing.port,
                    trusted = existing.trusted || trusted
                )
                if (updated != existing) {
                    currentList[existingIndex] = updated
                    _scannedDevices.value = currentList
                }
            } else {
                currentList.add(WifiDevice(name, ip, port, trusted))
                _scannedDevices.value = currentList
            }
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        // Use ConnectivityManager link properties to avoid deprecated WifiManager.dhcpInfo.
        return try {
            val appContext = context.applicationContext
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val network = cm.activeNetwork ?: return null
            val linkProperties = cm.getLinkProperties(network) ?: return null

            val ipv4 = linkProperties.linkAddresses
                .firstOrNull { it.address is Inet4Address }
                ?.let { it.address as Inet4Address to it.prefixLength }
                ?: return null

            val addrInt = ByteBuffer.wrap(ipv4.first.address).order(ByteOrder.BIG_ENDIAN).int
            val prefixLen = ipv4.second.coerceIn(0, 32)
            val mask = if (prefixLen == 0) 0 else (-1 shl (32 - prefixLen))
            val broadcastInt = addrInt or mask.inv()
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(broadcastInt).array()
            InetAddress.getByAddress(bytes)
        } catch (_: Exception) {
            null
        }
    }

    private fun hasWifiStatePermission() = context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
    private fun hasNearbyWifiPermission() = context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
}
