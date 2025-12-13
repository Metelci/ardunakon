package com.metelci.ardunakon.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/**
 * Manages Pre-Shared Keys (PSKs) for WiFi encryption.
 *
 * PSKs are:
 * - Auto-generated using SecureRandom when not present
 * - Encrypted using Android Keystore before storage
 * - Stored per-device IP address
 *
 * This enables encrypted WiFi connections while maintaining usability.
 */
class WifiEncryptionPreferences(
    private val context: Context,
    private val cryptoEngine: CryptoEngine = SecurityManager()
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val secureRandom = SecureRandom()

    companion object {
        private const val TAG = "WifiEncryption"
        private const val PREFS_NAME = "wifi_encryption_prefs"
        private const val KEY_PREFIX = "psk_"
        const val PSK_SIZE = 32 // 256-bit key for AES-256
    }

    /**
     * Get or create a PSK for the given device IP.
     * If no PSK exists, a new one is generated and stored.
     *
     * @param deviceIp The IP address of the WiFi device
     * @return The PSK for this device (32 bytes)
     */
    suspend fun getOrCreatePsk(deviceIp: String): ByteArray = withContext(Dispatchers.IO) {
        val existing = loadPsk(deviceIp)
        if (existing != null) {
            Log.d(TAG, "Loaded existing PSK for $deviceIp")
            return@withContext existing
        }

        val newPsk = generatePsk()
        savePsk(deviceIp, newPsk)
        Log.d(TAG, "Generated new PSK for $deviceIp")
        newPsk
    }

    /**
     * Generate a new cryptographically secure PSK.
     */
    fun generatePsk(): ByteArray {
        val psk = ByteArray(PSK_SIZE)
        secureRandom.nextBytes(psk)
        return psk
    }

    /**
     * Save a PSK for a device, encrypted with Keystore.
     */
    suspend fun savePsk(deviceIp: String, psk: ByteArray) = withContext(Dispatchers.IO) {
        try {
            // Convert PSK to Base64 string before encryption (CryptoEngine uses String)
            val pskBase64 = Base64.encodeToString(psk, Base64.NO_WRAP)
            val encrypted = cryptoEngine.encrypt(pskBase64)
            prefs.edit().putString(KEY_PREFIX + sanitizeIp(deviceIp), encrypted).apply()
            Log.d(TAG, "Saved PSK for $deviceIp")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save PSK for $deviceIp", e)
            throw e
        }
    }

    /**
     * Load a PSK for a device, decrypting from Keystore.
     * Returns null if no PSK exists.
     */
    suspend fun loadPsk(deviceIp: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val encrypted = prefs.getString(KEY_PREFIX + sanitizeIp(deviceIp), null)
                ?: return@withContext null
            // Decrypt to get Base64-encoded PSK, then decode to ByteArray
            val pskBase64 = cryptoEngine.decrypt(encrypted)
            Base64.decode(pskBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PSK for $deviceIp", e)
            null
        }
    }

    /**
     * Check if a PSK exists for a device.
     */
    fun hasPsk(deviceIp: String): Boolean {
        return prefs.contains(KEY_PREFIX + sanitizeIp(deviceIp))
    }

    /**
     * Clear the PSK for a device.
     */
    fun clearPsk(deviceIp: String) {
        prefs.edit().remove(KEY_PREFIX + sanitizeIp(deviceIp)).apply()
        Log.d(TAG, "Cleared PSK for $deviceIp")
    }

    /**
     * Clear all stored PSKs.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all PSKs")
    }

    /**
     * Get all device IPs that have stored PSKs.
     */
    fun getStoredDevices(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX).replace("_", ".") }
    }

    /**
     * Get the PSK as a hex string for display (first 8 chars only for security).
     */
    suspend fun getPskPreview(deviceIp: String): String? {
        val psk = loadPsk(deviceIp) ?: return null
        return psk.take(4).joinToString("") { "%02x".format(it) } + "..."
    }

    /**
     * Sanitize IP address for use as SharedPreferences key.
     */
    private fun sanitizeIp(ip: String): String {
        return ip.replace(".", "_")
    }
}
