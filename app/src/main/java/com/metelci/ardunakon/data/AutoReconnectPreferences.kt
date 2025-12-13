package com.metelci.ardunakon.data

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class AutoReconnectPreferences(
    private val context: Context,
    private val securityManager: CryptoEngine = SecurityManager()
) {
    private val fileName = "auto_reconnect_prefs.json"

    suspend fun saveAutoReconnectState(slot: Int, enabled: Boolean) = withContext(Dispatchers.IO) {
        try {
            val current = loadAll()
            current[slot] = enabled

            val jsonObj = JSONObject()
            jsonObj.put("slot0", current[0])
            jsonObj.put("slot1", current[1])

            val encrypted = securityManager.encrypt(jsonObj.toString())
            File(context.filesDir, fileName).writeText(encrypted)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("AutoReconnectPrefs", "Error saving state", e)
        }
    }

    suspend fun loadAutoReconnectState(): BooleanArray = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return@withContext booleanArrayOf(false, false)

            val encrypted = file.readText()
            val json = securityManager.decrypt(encrypted)
            val obj = JSONObject(json)

            booleanArrayOf(
                obj.optBoolean("slot0", false),
                obj.optBoolean("slot1", false)
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("AutoReconnectPrefs", "Error loading state", e)
            booleanArrayOf(false, false)
        }
    }

    private fun loadAll(): BooleanArray {
        // Synchronous helper for saveAutoReconnectState
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return booleanArrayOf(false, false)

        return try {
            val encrypted = file.readText()
            val json = securityManager.decrypt(encrypted)
            val obj = JSONObject(json)
            booleanArrayOf(
                obj.optBoolean("slot0", false),
                obj.optBoolean("slot1", false)
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            booleanArrayOf(false, false)
        }
    }
}
