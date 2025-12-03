package com.metelci.ardunakon.data

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.bluetooth.DeviceType
import com.metelci.ardunakon.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedDevice(
    val address: String,
    val name: String,
    val type: DeviceType,
    val lastSeen: Long
)

class DeviceNameCache(private val context: Context) {
    private val fileName = "device_names.json"
    private val securityManager = SecurityManager()
    private val maxCacheSize = 100
    private val maxAgeDays = 30L

    suspend fun saveName(address: String, name: String, type: DeviceType) = withContext(Dispatchers.IO) {
        try {
            val devices = loadAllDevices().toMutableList()

            // Remove existing entry for this address
            devices.removeAll { it.address == address }

            // Add new entry
            devices.add(CachedDevice(address, name, type, System.currentTimeMillis()))

            // Limit cache size (keep most recent)
            if (devices.size > maxCacheSize) {
                devices.sortByDescending { it.lastSeen }
                devices.subList(maxCacheSize, devices.size).clear()
            }

            saveAllDevices(devices)
        } catch (e: Exception) {
            Log.e("DeviceNameCache", "Error saving name", e)
        }
    }

    suspend fun getName(address: String): String? = withContext(Dispatchers.IO) {
        try {
            loadAllDevices().find { it.address == address }?.name
        } catch (e: Exception) {
            Log.e("DeviceNameCache", "Error loading name", e)
            null
        }
    }

    suspend fun cleanOldEntries() = withContext(Dispatchers.IO) {
        try {
            val maxAge = maxAgeDays * 24 * 60 * 60 * 1000L
            val cutoff = System.currentTimeMillis() - maxAge

            val devices = loadAllDevices().filter { it.lastSeen > cutoff }
            saveAllDevices(devices)
        } catch (e: Exception) {
            Log.e("DeviceNameCache", "Error cleaning cache", e)
        }
    }

    private fun loadAllDevices(): List<CachedDevice> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        try {
            val encrypted = file.readText()
            val json = securityManager.decrypt(encrypted)
            val jsonArray = JSONArray(json)

            return (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                CachedDevice(
                    address = obj.getString("address"),
                    name = obj.getString("name"),
                    type = DeviceType.valueOf(obj.getString("type")),
                    lastSeen = obj.getLong("lastSeen")
                )
            }
        } catch (e: Exception) {
            Log.e("DeviceNameCache", "Error loading cache", e)
            return emptyList()
        }
    }

    private fun saveAllDevices(devices: List<CachedDevice>) {
        try {
            val jsonArray = JSONArray()
            devices.forEach { device ->
                val obj = JSONObject()
                obj.put("address", device.address)
                obj.put("name", device.name)
                obj.put("type", device.type.name)
                obj.put("lastSeen", device.lastSeen)
                jsonArray.put(obj)
            }

            val json = jsonArray.toString()
            val encrypted = securityManager.encrypt(json)

            val file = File(context.filesDir, fileName)
            file.writeText(encrypted)
        } catch (e: Exception) {
            Log.e("DeviceNameCache", "Error saving cache", e)
        }
    }
}
