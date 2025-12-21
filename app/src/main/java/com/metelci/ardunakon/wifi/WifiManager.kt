@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.wifi

import android.content.Context
import android.util.Base64
import android.util.Log
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.crash.BreadcrumbManager
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import com.metelci.ardunakon.security.EncryptionException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WifiManager(
    private val context: Context,
    private val connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
    private val onLog: (String) -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val encryptionPreferences: WifiEncryptionPreferences = WifiEncryptionPreferences(context),
    private val scope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob()),
    private val startMonitors: Boolean = true
) : WifiConnectionCallback {
    private val secureRandom = SecureRandom()
    private val sessionKey = AtomicReference<ByteArray?>(null)
    private val discoveryNonce = AtomicReference<String?>(null)

    private val scanner = WifiScanner(
        context, scope, onLog,
        ::buildDiscoveryMessage, ::verifySignature,
        { sessionKey.get() }, { discoveryNonce.get() }
    )

    private val connectionManager = WifiConnectionManager(
        context, scope, this, encryptionPreferences, ioDispatcher
    )

    // Connection State (Binary compatible with UI)
    private val _connectionState = MutableStateFlow(WifiConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(false)
    val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()
    private var _shouldReconnect = false
    private var reconnectAttempts = 0
    private var nextReconnectAt = 0L

    private val _rssi = MutableStateFlow(0)
    val rssi = _rssi.asStateFlow()

    private val _rtt = MutableStateFlow(0L)
    val rtt = _rtt.asStateFlow()

    private val _rttHistory = MutableStateFlow<List<Long>>(emptyList())
    val rttHistory = _rttHistory.asStateFlow()

    private val _incomingData = MutableStateFlow<ByteArray?>(null)
    val incomingData = _incomingData.asStateFlow()

    val scannedDevices = scanner.scannedDevices
    val isScanning = scanner.isScanning

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry = _telemetry.asStateFlow()

    private val _encryptionError = MutableStateFlow<EncryptionException?>(null)
    val encryptionError = _encryptionError.asStateFlow()

    private val _isEncrypted = MutableStateFlow(false)
    val isEncrypted = _isEncrypted.asStateFlow()

    private var targetIp: String = "192.168.4.1"
    private var targetPort: Int = 8888

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
        if (startMonitors) {
            startReconnectMonitor()
        }
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
        _shouldReconnect = enabled
        scope.launch { connectionPreferences.saveLastConnection(autoReconnectWifi = enabled) }
        reconnectAttempts = 0
        onLog(if (enabled) "WiFi Auto-reconnect ARMED" else "WiFi Auto-reconnect DISABLED")
    }

    fun startDiscovery() = scanner.startDiscovery()
    fun stopDiscovery() = scanner.stopDiscovery()

    fun connect(ip: String, port: Int) {
        targetIp = ip
        targetPort = port
        BreadcrumbManager.leave("WiFi", "Connect: $ip:$port")
        connectionManager.connect(ip, port) { psk ->
            scope.launch {
                connectionPreferences.saveLastConnection(
                    type = "WIFI",
                    wifiIp = ip,
                    wifiPort = port,
                    wifiPsk = Base64.encodeToString(psk, Base64.NO_WRAP)
                )
            }
        }
    }

    fun disconnect() {
        _shouldReconnect = false
        BreadcrumbManager.leave("WiFi", "Manual disconnect")
        connectionManager.disconnect()
    }

    fun sendData(data: ByteArray) = connectionManager.sendData(data)

    fun cleanup() {
        scanner.stopDiscovery()
        connectionManager.disconnect()
        scope.cancel()
    }

    fun setRequireEncryption(required: Boolean) = connectionManager.setRequireEncryption(required)
    fun isEncryptionRequired(): Boolean = true // Now mandatory by default in Manager
    fun clearEncryptionError() { _encryptionError.value = null }

    // --- WifiConnectionCallback ---

    override fun onStateChanged(state: WifiConnectionState) {
        _connectionState.value = state
        if (state == WifiConnectionState.CONNECTED) {
            reconnectAttempts = 0
            _isEncrypted.value = connectionManager.isEncrypted()
            BreadcrumbManager.leave("WiFi", "Connected (encrypted: ${_isEncrypted.value})")
        } else if (state == WifiConnectionState.ERROR) {
            BreadcrumbManager.leave("WiFi", "Connection error")
        }
    }

    override fun onDataReceived(data: ByteArray) {
        _incomingData.value = data
    }

    override fun onRttUpdated(rtt: Long, history: List<Long>) {
        _rtt.value = rtt
        _rttHistory.value = history
    }

    override fun onTelemetryUpdated(telemetry: Telemetry) {
        _telemetry.value = telemetry
    }

    override fun onRssiUpdated(rssi: Int) {
        _rssi.value = rssi
    }

    override fun onLog(message: String) = onLog.invoke(message)

    override fun onEncryptionError(error: EncryptionException) {
        _encryptionError.value = error
        onLog("WiFi: Encryption error - ${error.message}")
    }

    // --- Private Helpers ---

    private fun buildDiscoveryMessage(): Pair<ByteArray, String?> {
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

    private fun verifySignature(nonce: String, sig: String, key: ByteArray): Boolean = try {
        val nonceBytes = Base64.decode(nonce, Base64.NO_WRAP)
        val sigBytes = Base64.decode(sig, Base64.NO_WRAP)
        sigBytes.contentEquals(hmac(nonceBytes, key))
    } catch (_: Exception) { false }

    private fun hmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun startReconnectMonitor() {
        scope.launch {
            while (isActive) {
                delay(2000)
                if (_shouldReconnect && (_connectionState.value == WifiConnectionState.DISCONNECTED || _connectionState.value == WifiConnectionState.ERROR)) {
                    if (System.currentTimeMillis() >= nextReconnectAt) {
                        if (reconnectAttempts >= 5) {
                            setAutoReconnectEnabled(false)
                        } else {
                            val backoff = (reconnectAttempts * 2000L).coerceAtMost(10000L)
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
