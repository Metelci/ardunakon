package com.metelci.ardunakon.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DeviceVerificationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DeviceVerificationManager(private val context: Context) {

    private val provider = "AndroidKeyStore"
    private val deviceKeyAliasPrefix = "ArdunakonDeviceKey_"
    private val verificationChallengeSize = 16 // 16 bytes = 128 bits
    private val verificationTimeoutMs = 5000L // 5 seconds for challenge-response

    // Generate a cryptographic challenge for device verification
    fun generateVerificationChallenge(deviceAddress: String): String {
        try {
            // Generate a random challenge
            val random = SecureRandom()
            val challengeBytes = ByteArray(verificationChallengeSize)
            random.nextBytes(challengeBytes)

            // Encrypt the challenge with device-specific key
            val encryptedChallenge = encryptChallengeForDevice(deviceAddress, challengeBytes)

            // Return as Base64 string for easy transmission
            return Base64.encodeToString(encryptedChallenge, Base64.DEFAULT)
        } catch (e: Exception) {
            throw DeviceVerificationException("Failed to generate verification challenge", e)
        }
    }

    // Verify the device's response to our challenge
    fun verifyDeviceResponse(deviceAddress: String, challenge: String, response: String): Boolean {
        try {
            // Decrypt the original challenge
            val challengeBytes = decryptChallengeForDevice(deviceAddress, challenge)

            // The device should have encrypted the challenge with its own key
            // and returned it. We decrypt it and compare.
            val responseBytes = decryptChallengeForDevice(deviceAddress, response)

            // Verify the response matches the original challenge
            return challengeBytes.contentEquals(responseBytes)
        } catch (e: Exception) {
            throw DeviceVerificationException("Device verification failed", e)
        }
    }

    // Generate a device-specific encryption key if it doesn't exist
    private fun getOrCreateDeviceKey(deviceAddress: String): SecretKey {
        val keyAlias = "$deviceKeyAliasPrefix${deviceAddress.replace(":", "_")}"

        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null)

        // Check if we already have a key for this device
        if (!keyStore.containsAlias(keyAlias)) {
            // Generate a new AES key for this specific device
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256) // 256-bit AES
                    .build()
            )
            return keyGenerator.generateKey()
        }

        // Return existing key
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    private fun encryptChallengeForDevice(deviceAddress: String, challengeBytes: ByteArray): ByteArray {
        val key = getOrCreateDeviceKey(deviceAddress)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(challengeBytes)

        // Combine IV and encrypted data: IV_LENGTH(1) + IV + ENCRYPTED_DATA
        val combined = ByteArray(1 + iv.size + encryptedBytes.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

        return combined
    }

    private fun decryptChallengeForDevice(deviceAddress: String, encryptedData: String): ByteArray {
        val key = getOrCreateDeviceKey(deviceAddress)
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        // Validate minimum data size
        if (combined.size < 3) {
            throw DeviceVerificationException("Invalid encrypted data size")
        }

        // Extract IV size
        val ivSize = combined[0].toInt() and 0xFF
        if (ivSize < 8 || ivSize > 16) {
            throw DeviceVerificationException("Invalid IV size: $ivSize")
        }

        // Extract IV
        val iv = ByteArray(ivSize)
        System.arraycopy(combined, 1, iv, 0, ivSize)

        // Extract encrypted content
        val encryptedSize = combined.size - 1 - ivSize
        if (encryptedSize < 1) {
            throw DeviceVerificationException("Invalid encrypted data size")
        }

        val encryptedBytes = ByteArray(encryptedSize)
        System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedSize)

        // Decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(encryptedBytes)
    }

    // Generate a shared secret for secure communication
    fun generateSharedSecret(deviceAddress: String): String {
        try {
            val random = SecureRandom()
            val secretBytes = ByteArray(32) // 256-bit secret
            random.nextBytes(secretBytes)

            val encryptedSecret = encryptChallengeForDevice(deviceAddress, secretBytes)
            return Base64.encodeToString(encryptedSecret, Base64.DEFAULT)
        } catch (e: Exception) {
            throw DeviceVerificationException("Failed to generate shared secret", e)
        }
    }

    // Verify a shared secret response
    fun verifySharedSecret(deviceAddress: String, secret: String, response: String): Boolean {
        try {
            val secretBytes = decryptChallengeForDevice(deviceAddress, secret)
            val responseBytes = decryptChallengeForDevice(deviceAddress, response)

            return secretBytes.contentEquals(responseBytes)
        } catch (e: Exception) {
            throw DeviceVerificationException("Shared secret verification failed", e)
        }
    }

    // Clean up device-specific keys when device is removed
    fun removeDeviceKeys(deviceAddress: String) {
        try {
            val keyAlias = "$deviceKeyAliasPrefix${deviceAddress.replace(":", "_")}"
            val keyStore = KeyStore.getInstance(provider)
            keyStore.load(null)

            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (e: Exception) {
            // Ignore cleanup failures
        }
    }
}
