package com.metelci.ardunakon.security

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom

data class TrustedDeviceState(
    val address: String,
    val sharedKey: ByteArray,
    val trustedAt: Long,
    val revokedAt: Long? = null
) {
    val isTrusted: Boolean get() = revokedAt == null
}

/**
 * Stores per-device shared keys/trust state encrypted at rest using SecurityManager.
 * Acts as the single source of truth for MAC-based trust decisions.
 */
class TrustStore(context: Context) {
    private val securityManager = SecurityManager()
    private val file = File(context.filesDir, "trusted_devices.json")
    private val random = SecureRandom()

    suspend fun upsertTrustedDevice(address: String, sharedKey: ByteArray): TrustedDeviceState =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = loadAll().associateBy { it.address }
            val updated = existing.toMutableMap()
            updated[address] = TrustedDeviceState(address, sharedKey, trustedAt = now, revokedAt = null)
            saveAll(updated.values.toList())
            updated[address]!!
        }

    suspend fun revoke(address: String) = withContext(Dispatchers.IO) {
        val existing = loadAll().associateBy { it.address }
        val current = existing[address] ?: return@withContext
        val updated = existing.toMutableMap()
        updated[address] = current.copy(revokedAt = System.currentTimeMillis())
        saveAll(updated.values.toList())
    }

    suspend fun get(address: String): TrustedDeviceState? = withContext(Dispatchers.IO) {
        loadAll().firstOrNull { it.address == address }
    }

    suspend fun getOrCreateProvisional(address: String): TrustedDeviceState = withContext(Dispatchers.IO) {
        val existing = get(address)
        if (existing != null) return@withContext existing
        // Provision a random key; the device must prove knowledge during handshake.
        val key = ByteArray(32).also(random::nextBytes)
        upsertTrustedDevice(address, key)
    }

    private fun loadAll(): List<TrustedDeviceState> {
        if (!file.exists()) return emptyList()
        return try {
            val decrypted = securityManager.decrypt(file.readText())
            val arr = JSONArray(decrypted)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.getJSONObject(idx)
                val key = Base64.decode(obj.getString("sharedKey"), Base64.DEFAULT)
                TrustedDeviceState(
                    address = obj.getString("address"),
                    sharedKey = key,
                    trustedAt = obj.getLong("trustedAt"),
                    revokedAt = if (obj.has("revokedAt")) obj.optLong("revokedAt", 0).let { if (it == 0L) null else it } else null
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAll(devices: List<TrustedDeviceState>) {
        val arr = JSONArray()
        devices.forEach { device ->
            val obj = JSONObject()
            obj.put("address", device.address)
            obj.put("sharedKey", Base64.encodeToString(device.sharedKey, Base64.NO_WRAP))
            obj.put("trustedAt", device.trustedAt)
            device.revokedAt?.let { obj.put("revokedAt", it) }
            arr.put(obj)
        }
        file.writeText(securityManager.encrypt(arr.toString()))
    }
}
