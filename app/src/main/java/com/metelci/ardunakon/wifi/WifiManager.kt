@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.wifi

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.crash.BreadcrumbManager
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import com.metelci.ardunakon.model.LogType
import com.metelci.ardunakon.security.EncryptionException
import com.metelci.ardunakon.util.RecoveryManager
import com.metelci.ardunakon.util.RetryPolicy
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages WiFi discovery, connection, encryption, and telemetry state.
 *
 * @param context Application context.
 * @param connectionPreferences Persistent connection settings.
 * @param onLog Callback for log messages.
 * @param ioDispatcher Dispatcher for IO-bound work.
 * @param encryptionPreferences Storage for encryption state.
 * @param scope Coroutine scope for background work.
 * @param startMonitors When true, starts reconnect monitoring.
 */
class WifiManager(
    private val context: Context,
    private val connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
    private val recoveryManager: RecoveryManager? = null,
    private val onLog: (String, LogType) -> Unit = { _, _ -> },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val encryptionPreferences: WifiEncryptionPreferences = WifiEncryptionPreferences(context),
    private val scope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob()),
    private val startMonitors: Boolean = true
) : WifiConnectionCallback, IWifiManager {
    private val secureRandom = SecureRandom()
    private val sessionKey = AtomicReference<ByteArray?>(null)
    private val discoveryNonce = AtomicReference<String?>(null)

    private val scanner = WifiScanner(
        context,
        scope,
        { msg -> onLog(msg, LogType.INFO) },
        ::buildDiscoveryMessage,
        ::verifySignature,
        { sessionKey.get() },
        { discoveryNonce.get() }
    )

    private val connectionManager = WifiConnectionManager(
        context,
        scope,
        this,
        encryptionPreferences,
        ioDispatcher
    )

    // Connection State (Binary compatible with UI)
    private val _connectionState = MutableStateFlow(WifiConnectionState.DISCONNECTED)

    /**
     * Current WiFi connection state.
     */
    override val connectionState = _connectionState.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(false)

    /**
     * True when auto-reconnect is enabled.
     */
    override val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()
    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private var nextReconnectAt = 0L

    private val _rssi = MutableStateFlow(0)

    /**
     * Latest RSSI reading.
     */
    override val rssi = _rssi.asStateFlow()

    private val _rtt = MutableStateFlow(0L)

    /**
     * Latest RTT reading.
     */
    override val rtt = _rtt.asStateFlow()

    private val _rttHistory = MutableStateFlow<List<Long>>(emptyList())

    /**
     * Rolling RTT history.
     */
    override val rttHistory = _rttHistory.asStateFlow()

    private val _incomingData = MutableStateFlow<ByteArray?>(null)

    /**
     * Last received raw packet, if any.
     */
    override val incomingData = _incomingData.asStateFlow()

    /**
     * Discovered WiFi devices broadcast results.
     */
    override val scannedDevices: StateFlow<List<WifiDevice>> = scanner.scannedDevices

    /**
     * True while discovery is active.
     */
    override val isScanning: StateFlow<Boolean> = scanner.isScanning

    private val _connectedDeviceInfo = MutableStateFlow<String?>(null)

    override val connectedDeviceInfo: StateFlow<String?> = _connectedDeviceInfo.asStateFlow()

    private val _telemetry = MutableStateFlow<Telemetry?>(null)

    /**
     * Latest telemetry sample received from the device.
     */
    override val telemetry = _telemetry.asStateFlow()

    private val _encryptionError = MutableStateFlow<EncryptionException?>(null)

    /**
     * Latest encryption error, if any.
     */
    override val encryptionError = _encryptionError.asStateFlow()

    private val _isEncrypted = MutableStateFlow(false)

    /**
     * True when the current connection is encrypted.
     */
    override val isEncrypted = _isEncrypted.asStateFlow()

    private var targetIp: String = "192.168.4.1"
    private var targetPort: Int = 8888

    @VisibleForTesting
    fun buildDiscoveryMessageForTest(): Pair<ByteArray, String?> = buildDiscoveryMessage()

    @VisibleForTesting
    fun setSessionKeyForTest(key: ByteArray?) {
        sessionKey.set(key)
    }

    init {
        scope.launch {
            val lastConn = connectionPreferences.loadLastConnection()
            shouldReconnect = lastConn.autoReconnectWifi
            _autoReconnectEnabled.value = lastConn.autoReconnectWifi

            if (!lastConn.wifiIp.isNullOrEmpty()) {
                targetIp = lastConn.wifiIp
                targetPort = lastConn.wifiPort
                if (shouldReconnect) onLog("Restored WiFi target: $targetIp and armed auto-reconnect")
            }
        }
        if (startMonitors) {
            startReconnectMonitor()
        }
    }

    /**
     * Enables or disables auto-reconnect for WiFi.
     *
     * @param enabled True to enable auto-reconnect.
     */
    override fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
        shouldReconnect = enabled
        scope.launch { connectionPreferences.saveLastConnection(autoReconnectWifi = enabled) }
        reconnectAttempts = 0
        log(if (enabled) "WiFi Auto-reconnect ARMED" else "WiFi Auto-reconnect DISABLED")
    }

    /**
     * Starts WiFi discovery broadcasts.
     */
    override fun startDiscovery() = scanner.startDiscovery()

    /**
     * Stops WiFi discovery.
     */
    override fun stopDiscovery() = scanner.stopDiscovery()

    /**
     * Connects to a WiFi device and persists the target.
     *
     * @param ip Target IP address.
     * @param port Target port.
     */
    override fun connect(ip: String, port: Int) {
        targetIp = ip
        targetPort = port
        _connectedDeviceInfo.value = buildConnectedDeviceInfo(ip, port)
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

    /**
     * Disconnects the current WiFi connection and stops auto-reconnect.
     */
    override fun disconnect() {
        shouldReconnect = false
        _connectedDeviceInfo.value = null
        BreadcrumbManager.leave("WiFi", "Manual disconnect")
        connectionManager.disconnect()
    }

    /**
     * Sends raw data over the active WiFi connection.
     *
     * @param data Payload to transmit.
     */
    override fun sendData(data: ByteArray) = connectionManager.sendData(data)

    /**
     * Stops discovery, disconnects, and cancels background work.
     */
    override fun cleanup() {
        scanner.stopDiscovery()
        connectionManager.disconnect()
        scope.cancel()
    }

    /**
     * Toggles whether encryption is required by the connection manager.
     *
     * @param required True when encryption is required.
     */
    override fun setRequireEncryption(required: Boolean) = connectionManager.setRequireEncryption(required)

    /**
     * Indicates whether encryption is required (mandatory by default).
     *
     * @return True if encryption is required.
     */
    override fun isEncryptionRequired(): Boolean = true // Now mandatory by default in Manager

    /**
     * Clears the latest encryption error.
     */
    override fun clearEncryptionError() {
        _encryptionError.value = null
    }

    // --- WifiConnectionCallback ---

    /**
     * Connection state callback from [WifiConnectionCallback].
     *
     * @param state New connection state.
     */
    override fun onStateChanged(state: WifiConnectionState) {
        _connectionState.value = state
        if (state == WifiConnectionState.CONNECTED) {
            reconnectAttempts = 0
            _isEncrypted.value = connectionManager.isEncrypted()
            _connectedDeviceInfo.value = buildConnectedDeviceInfo(targetIp, targetPort)
            recoveryManager?.recordSuccess(RecoveryManager.OP_WIFI_CONNECT)
            BreadcrumbManager.leave("WiFi", "Connected (encrypted: ${_isEncrypted.value})")
        } else if (state == WifiConnectionState.CONNECTING) {
            _connectedDeviceInfo.value = buildConnectedDeviceInfo(targetIp, targetPort)
        } else if (state == WifiConnectionState.ERROR) {
            recoveryManager?.recordFailure(RecoveryManager.OP_WIFI_CONNECT)
            BreadcrumbManager.leave("WiFi", "Connection error")
            _connectedDeviceInfo.value = null
        } else if (state == WifiConnectionState.DISCONNECTED) {
            _connectedDeviceInfo.value = null
        }
    }

    /**
     * Raw data callback from [WifiConnectionCallback].
     *
     * @param data Received payload.
     */
    override fun onDataReceived(data: ByteArray) {
        _incomingData.value = data
    }

    /**
     * RTT update callback from [WifiConnectionCallback].
     *
     * @param rtt Latest RTT in milliseconds.
     * @param history Updated RTT history list.
     */
    override fun onRttUpdated(rtt: Long, history: List<Long>) {
        _rtt.value = rtt
        _rttHistory.value = history
    }

    /**
     * Telemetry update callback from [WifiConnectionCallback].
     *
     * @param telemetry Latest telemetry payload.
     */
    override fun onTelemetryUpdated(telemetry: Telemetry) {
        _telemetry.value = telemetry
    }

    /**
     * RSSI update callback from [WifiConnectionCallback].
     *
     * @param rssi Latest RSSI in dBm.
     */
    override fun onRssiUpdated(rssi: Int) {
        _rssi.value = rssi
    }

    /**
     * Log callback from [WifiConnectionCallback].
     *
     * @param message Log message.
     */
    override fun onLog(message: String) = onLog.invoke(message, LogType.INFO)

    /**
     * Encryption error callback from [WifiConnectionCallback].
     *
     * @param error Encryption error.
     */
    override fun onEncryptionError(error: EncryptionException) {
        _encryptionError.value = error
        log("WiFi: Encryption error - ${error.message}", LogType.ERROR)
    }

    override fun log(message: String, type: LogType) {
        onLog.invoke(message, type)
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
    } catch (_: Exception) {
        false
    }

    private fun hmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun startReconnectMonitor() {
        val retryPolicy = RetryPolicy.forNetwork()
        scope.launch {
            while (isActive) {
                delay(2000)
                if (shouldReconnect && (
                        _connectionState.value == WifiConnectionState.DISCONNECTED ||
                            _connectionState.value == WifiConnectionState.ERROR
                        )
                ) {
                    if (System.currentTimeMillis() >= nextReconnectAt) {
                        // Check circuit breaker
                        if (recoveryManager?.shouldAllowOperation(RecoveryManager.OP_WIFI_CONNECT) == false) {
                            log("WiFi: Connection circuit breaker TRIPPED, delaying reconnect", LogType.WARNING)
                            nextReconnectAt = System.currentTimeMillis() + 10000
                            continue
                        }

                        if (reconnectAttempts >= 5) { // Increased from 3
                            log("WiFi: Max reconnect attempts reached, disarming auto-reconnect", LogType.ERROR)
                            setAutoReconnectEnabled(false)
                        } else {
                            val backoff = retryPolicy.calculateDelay(reconnectAttempts)
                            val attemptNumber = reconnectAttempts + 1
                            log(
                                "WiFi: Auto-reconnecting to $targetIp:$targetPort... (attempt $attemptNumber)",
                                LogType.INFO
                            )
                            reconnectAttempts++
                            nextReconnectAt = System.currentTimeMillis() + backoff

                            try {
                                connect(targetIp, targetPort)
                                // We don't record success here as connect() is fire-and-forget,
                                // success is recorded in onStateChanged
                            } catch (e: Exception) {
                                recoveryManager?.recordFailure(RecoveryManager.OP_WIFI_CONNECT)
                                log("WiFi: Reconnect attempt failed: ${e.message}", LogType.ERROR)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildConnectedDeviceInfo(ip: String, port: Int): String {
        val name = scannedDevices.value.firstOrNull { it.ip == ip && it.port == port }?.name?.takeIf { it.isNotBlank() }
        val endpoint = if (port == 8888) ip else "$ip:$port"
        return name?.let { "$it • $endpoint" } ?: endpoint
    }
}
