package com.metelci.ardunakon.data

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.SecurityManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ConnectionPreferences(
    private val context: Context,
    private val securityManager: CryptoEngine = SecurityManager()
) {
    private val fileName = "connection_prefs.json"

    data class LastConnection(
        val type: String?, // "BLUETOOTH" or "WIFI"
        val btAddress: String?,
        val wifiIp: String?,
        val wifiPort: Int,
        val autoReconnectWifi: Boolean
    )

    suspend fun saveLastConnection(
        type: String? = null,
        btAddress: String? = null,
        wifiIp: String? = null,
        wifiPort: Int? = null,
        autoReconnectWifi: Boolean? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val current = loadLastConnection()
            val newType = type ?: current.type
            val newBtAddress = btAddress ?: current.btAddress
            val newWifiIp = wifiIp ?: current.wifiIp
            val newWifiPort = wifiPort ?: current.wifiPort
            val newAutoReconnectWifi = autoReconnectWifi ?: current.autoReconnectWifi

            val jsonObj = JSONObject()
            jsonObj.put("type", newType)
            jsonObj.put("btAddress", newBtAddress)
            jsonObj.put("wifiIp", newWifiIp)
            jsonObj.put("wifiPort", newWifiPort)
            jsonObj.put("autoReconnectWifi", newAutoReconnectWifi)

            val encrypted = securityManager.encrypt(jsonObj.toString())
            File(context.filesDir, fileName).writeText(encrypted)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("ConnectionPrefs", "Error saving prefs", e)
        }
    }

    suspend fun loadLastConnection(): LastConnection = withContext(Dispatchers.IO) {
        val default = LastConnection(null, null, null, 8888, false)
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return@withContext default

            val encrypted = file.readText()
            val json = securityManager.decrypt(encrypted)
            val obj = JSONObject(json)

            LastConnection(
                type = obj.optString("type").takeIf { it.isNotEmpty() },
                btAddress = obj.optString("btAddress").takeIf { it.isNotEmpty() },
                wifiIp = obj.optString("wifiIp").takeIf { it.isNotEmpty() },
                wifiPort = obj.optInt("wifiPort", 8888),
                autoReconnectWifi = obj.optBoolean("autoReconnectWifi", false)
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("ConnectionPrefs", "Error loading prefs", e)
            default
        }
    }
}
