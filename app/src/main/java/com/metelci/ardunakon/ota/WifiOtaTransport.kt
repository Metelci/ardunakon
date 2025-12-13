@file:Suppress("DEPRECATION")
package com.metelci.ardunakon.ota

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * WiFi OTA Transport - Connects to Arduino AP and uploads via HTTP
 */
class WifiOtaTransport(
    private val context: Context,
    private val connectionFactory: (URL, Network?) -> HttpURLConnection = { url, network ->
        (network?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
    }
) : OtaTransport {
    
    companion object {
        const val TAG = "WifiOtaTransport"
        const val ARDUINO_SSID_PREFIX = "Ardunakon_OTA"
        const val ARDUINO_PASSWORD = "ardunakon123"
        const val ARDUINO_IP = "192.168.4.1"
        const val ARDUINO_PORT = 80
        const val CONNECT_TIMEOUT_MS = 30000L
    }
    
    private var connectedNetwork: Network? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses NetworkSpecifier
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsidPattern(android.os.PatternMatcher(ARDUINO_SSID_PREFIX, android.os.PatternMatcher.PATTERN_PREFIX))
                    .setWpa2Passphrase(ARDUINO_PASSWORD)
                    .build()
                
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build()
                
                val connected = suspendCancellableCoroutine<Boolean> { cont ->
                    val callback = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            Log.d(TAG, "Connected to Arduino AP")
                            connectedNetwork = network
                            connectivityManager.bindProcessToNetwork(network)
                            if (cont.isActive) cont.resume(true)
                        }
                        
                        override fun onUnavailable() {
                            Log.e(TAG, "Arduino AP not available")
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                    networkCallback = callback
                    connectivityManager.requestNetwork(request, callback)
                    
                    cont.invokeOnCancellation {
                        connectivityManager.unregisterNetworkCallback(callback)
                    }
                }
                
                return@withContext withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connected } ?: false
                
            } else {
                // Legacy WiFi connection
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                // Note: Legacy connection would need different implementation
                Log.w(TAG, "Legacy WiFi connection not fully implemented")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "WiFi connection error: ${e.message}")
            return@withContext false
        }
    }
    
    override suspend fun sendChunk(data: ByteArray, offset: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ARDUINO_IP:$ARDUINO_PORT/update")
            val connection = connectionFactory(url, connectedNetwork)
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/octet-stream")
                setRequestProperty("X-Offset", offset.toString())
                setRequestProperty("X-Size", data.size.toString())
                connectTimeout = 5000
                readTimeout = 5000
            }
            
            connection.outputStream.use { it.write(data) }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            return@withContext responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Send chunk error: ${e.message}")
            return@withContext false
        }
    }
    
    override suspend fun complete(crc: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ARDUINO_IP:$ARDUINO_PORT/complete")
            val connection = connectionFactory(url, connectedNetwork)
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }
            
            OutputStreamWriter(connection.outputStream).use {
                it.write("""{"crc": $crc}""")
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            return@withContext responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Complete error: ${e.message}")
            return@withContext false
        }
    }
    
    override suspend fun abort() {
        try {
            val url = URL("http://$ARDUINO_IP:$ARDUINO_PORT/abort")
            val connection = connectionFactory(url, connectedNetwork)
            connection.requestMethod = "POST"
            connection.responseCode
            connection.disconnect()
        } catch (_: Exception) {}
    }
    
    override fun disconnect() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(null)
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        connectedNetwork = null
        networkCallback = null
    }
}
