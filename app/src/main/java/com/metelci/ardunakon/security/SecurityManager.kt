package com.metelci.ardunakon.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

class SecurityManager {

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

    fun encrypt(data: String): String {
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
    }

    fun decrypt(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)
        
        // Extract IV
        val ivSize = combined[0].toInt()
        val iv = ByteArray(ivSize)
        System.arraycopy(combined, 1, iv, 0, ivSize)
        
        // Extract Encrypted Content
        val encryptedSize = combined.size - 1 - ivSize
        val encryptedBytes = ByteArray(encryptedSize)
        System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedSize)
        
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        
        val decoded = cipher.doFinal(encryptedBytes)
        return String(decoded, Charsets.UTF_8)
    }
}
