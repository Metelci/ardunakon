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
        val btType: String?, // "CLASSIC" or "LE"
        val wifiIp: String?,
        val wifiPort: Int,
        val wifiPsk: String?, // Encrypted PSK
        val autoReconnectWifi: Boolean,
        val joystickSensitivity: Float
    )

    suspend fun saveLastConnection(
        type: String? = null,
        btAddress: String? = null,
        btType: String? = null,
        wifiIp: String? = null,
        wifiPort: Int? = null,
        wifiPsk: String? = null,
        autoReconnectWifi: Boolean? = null,
        joystickSensitivity: Float? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val current = loadLastConnection()
            val newType = type ?: current.type
            val newBtAddress = btAddress ?: current.btAddress
            val newBtType = btType ?: current.btType
            val newWifiIp = wifiIp ?: current.wifiIp
            val newWifiPort = wifiPort ?: current.wifiPort
            val newWifiPsk = wifiPsk ?: current.wifiPsk
            val newAutoReconnectWifi = autoReconnectWifi ?: current.autoReconnectWifi
            val newJoystickSensitivity = joystickSensitivity ?: current.joystickSensitivity

            val jsonObj = JSONObject()
            jsonObj.put("type", newType)
            jsonObj.put("btAddress", newBtAddress)
            jsonObj.put("btType", newBtType)
            jsonObj.put("wifiIp", newWifiIp)
            jsonObj.put("wifiPort", newWifiPort)
            jsonObj.put("wifiPsk", newWifiPsk)
            jsonObj.put("autoReconnectWifi", newAutoReconnectWifi)
            jsonObj.put("joystickSensitivity", newJoystickSensitivity.toDouble())

            val encrypted = securityManager.encrypt(jsonObj.toString())
            File(context.filesDir, fileName).writeText(encrypted)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("ConnectionPrefs", "Error saving prefs", e)
        }
    }

    suspend fun loadLastConnection(): LastConnection = withContext(Dispatchers.IO) {
        val default = LastConnection(null, null, null, null, 8888, null, false, 1.0f)
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return@withContext default

            val encrypted = file.readText()
            val json = securityManager.decrypt(encrypted)
            val obj = JSONObject(json)

            LastConnection(
                type = obj.optString("type").takeIf { it.isNotEmpty() },
                btAddress = obj.optString("btAddress").takeIf { it.isNotEmpty() },
                btType = obj.optString("btType").takeIf { it.isNotEmpty() },
                wifiIp = obj.optString("wifiIp").takeIf { it.isNotEmpty() },
                wifiPort = obj.optInt("wifiPort", 8888),
                wifiPsk = obj.optString("wifiPsk").takeIf { it.isNotEmpty() },
                autoReconnectWifi = obj.optBoolean("autoReconnectWifi", false),
                joystickSensitivity = obj.optDouble("joystickSensitivity", 1.0).toFloat()
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("ConnectionPrefs", "Error loading prefs", e)
            default
        }
    }
}
