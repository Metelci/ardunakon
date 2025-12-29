package com.metelci.ardunakon.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Thrown when encrypted storage requires user authentication or secure hardware is unavailable.
 *
 * @param message Human-readable message suitable for user-facing flows.
 * @param cause Optional underlying exception.
 */
class AuthRequiredException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal interface to allow injecting a fake crypto engine in tests without
 * touching the Android keystore.
 */
interface CryptoEngine {
    /**
     * Encrypts plaintext into a Base64 payload.
     *
     * @param data Plaintext string to encrypt.
     * @return Base64-encoded payload containing IV and ciphertext.
     * @throws AuthRequiredException when secure storage is unavailable or authentication is required.
     */
    fun encrypt(data: String): String

    /**
     * Decrypts a Base64 payload back into plaintext.
     *
     * @param encryptedData Base64 payload produced by [encrypt].
     * @return Decrypted plaintext string.
     * @throws AuthRequiredException when secure storage is unavailable or payload is invalid.
     */
    fun decrypt(encryptedData: String): String
}

/**
 * Android Keystore-backed implementation of [CryptoEngine] using AES/GCM.
 */
class SecurityManager : CryptoEngine {

    private val provider = "AndroidKeyStore"
    private val alias = "ArdunakonDataKey"
    private val transformation = "AES/GCM/NoPadding"

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null)
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            setUserAuthenticationParameters(
                                300,
                                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                            )
                        } else {
                            setUserAuthenticationRequired(true)
                            @Suppress("DEPRECATION")
                            setUserAuthenticationValidityDurationSeconds(300)
                        }
                    }
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(provider)
        keyStore.load(null)
        return keyStore.getKey(alias, null) as SecretKey
    }

    /**
     * Encrypts data using AES/GCM and stores the IV with the ciphertext.
     *
     * @param data Plaintext string to encrypt.
     * @return Base64-encoded IV + ciphertext payload.
     * @throws AuthRequiredException when keystore access fails.
     */
    override fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Combine IV and Encrypted Data for storage
            // Format: IV_LENGTH (1 byte) + IV + ENCRYPTED_DATA
            val combined = ByteArray(1 + iv.size + encryptedBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: UserNotAuthenticatedException) {
            // SECURITY FIX: Provide generic error message
            throw AuthRequiredException("Device authentication required for secure storage.", e)
        } catch (e: GeneralSecurityException) {
            // SECURITY FIX: Provide generic error message
            throw AuthRequiredException("Security system unavailable. Check device unlock status.", e)
        }
    }

    /**
     * Decrypts data produced by [encrypt].
     *
     * @param encryptedData Base64 payload containing IV + ciphertext.
     * @return Decrypted plaintext string.
     * @throws AuthRequiredException when keystore access fails or payload is invalid.
     */
    override fun decrypt(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)

            // SECURITY FIX: Validate input without exposing specific implementation details
            if (combined.size < 3) {
                throw AuthRequiredException("Corrupted secure data detected")
            }

            // Extract IV size - ensure it's positive and within valid range
            val ivSize = combined[0].toInt() and 0xFF // Ensure positive value (0-255)

            // SECURITY FIX: Generic validation without exposing specific crypto details
            if (ivSize < 8 || ivSize > 16 || combined.size < 1 + ivSize + 1) {
                throw AuthRequiredException("Invalid secure data format")
            }

            // Extract IV
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)

            // Extract Encrypted Content
            val encryptedSize = combined.size - 1 - ivSize
            if (encryptedSize < 0) {
                throw AuthRequiredException("Invalid secure data structure")
            }

            val encryptedBytes = ByteArray(encryptedSize)
            System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedSize)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

            val decoded = cipher.doFinal(encryptedBytes)
            return String(decoded, Charsets.UTF_8)
        } catch (e: UserNotAuthenticatedException) {
            // SECURITY FIX: Provide generic error message
            throw AuthRequiredException("Device authentication required to access secure data.", e)
        } catch (e: GeneralSecurityException) {
            // SECURITY FIX: Provide generic error message
            throw AuthRequiredException("Security system unavailable. Check device unlock status.", e)
        }
    }
}
