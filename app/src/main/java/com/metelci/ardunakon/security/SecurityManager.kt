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

class AuthRequiredException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal interface to allow injecting a fake crypto engine in tests without
 * touching the Android keystore.
 */
interface CryptoEngine {
    fun encrypt(data: String): String
    fun decrypt(encryptedData: String): String
}

class SecurityManager : CryptoEngine {

    private val provider = "AndroidKeyStore"
    private val alias = "ArdunakonProfileKey"
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
            throw AuthRequiredException("Unlock your device to encrypt profiles.", e)
        } catch (e: GeneralSecurityException) {
            throw AuthRequiredException("Profile encryption unavailable until device is unlocked.", e)
        }
    }

    override fun decrypt(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)

            // Validate minimum data size (at least 1 byte for IV size + some IV + some data)
            if (combined.size < 3) {
                throw IllegalArgumentException("Encrypted data is too short to be valid")
            }

            // Extract IV size - ensure it's positive and within valid range
            val ivSize = combined[0].toInt() and 0xFF // Ensure positive value (0-255)

            // Validate IV size - GCM typically uses 12 bytes, but allow 8-16 as valid range
            if (ivSize < 8 || ivSize > 16) {
                throw IllegalArgumentException("Invalid IV size: $ivSize. Expected 8-16 bytes.")
            }

            // Validate total size - must have room for IV size byte + IV + at least some encrypted data
            if (combined.size < 1 + ivSize + 1) {
                throw IllegalArgumentException("Encrypted data is too short for declared IV size")
            }

            // Extract IV
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)

            // Extract Encrypted Content
            val encryptedSize = combined.size - 1 - ivSize
            if (encryptedSize < 0) {
                throw IllegalArgumentException("Invalid encrypted data size")
            }

            val encryptedBytes = ByteArray(encryptedSize)
            System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedSize)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

            val decoded = cipher.doFinal(encryptedBytes)
            return String(decoded, Charsets.UTF_8)
        } catch (e: UserNotAuthenticatedException) {
            throw AuthRequiredException("Unlock your device to access saved profiles.", e)
        } catch (e: GeneralSecurityException) {
            throw AuthRequiredException("Profile decryption unavailable until device is unlocked.", e)
        }
    }
}
