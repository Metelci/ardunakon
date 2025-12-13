package com.metelci.ardunakon.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.security.EncryptionException
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comprehensive tests for encrypted WiFi send/receive operations.
 * Tests AES-GCM encryption, key management, and error handling.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedWifiTest {

    private lateinit var context: Context
    private lateinit var manager: WifiManager
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        manager = WifiManager(context, ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============== AES-GCM Encryption Tests ==============

    @Test
    fun `AES-GCM encryption produces valid ciphertext format`() {
        val key = ByteArray(32) { it.toByte() } // AES-256 key
        manager.setSessionKey(key)
        val plaintext = "Hello, Arduino!".toByteArray()

        val encrypted = manager.encryptIfNeeded(plaintext)

        // Format: IV (12 bytes) + Ciphertext (>=16 bytes due to GCM tag)
        assertTrue("Encrypted data should be larger than plaintext", encrypted.size > plaintext.size)
        assertTrue("Should include 12-byte IV", encrypted.size >= 12 + 16) // IV + at least GCM tag
    }

    @Test
    fun `AES-GCM encrypted data can be decrypted`() {
        val key = ByteArray(32) { it.toByte() }
        manager.setSessionKey(key)
        val plaintext = "Control packet data".toByteArray()

        val encrypted = manager.encryptIfNeeded(plaintext)

        // Decrypt to verify
        val iv = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(ciphertext)

        assertArrayEquals("Decrypted data should match original", plaintext, decrypted)
    }

    @Test
    fun `each encryption uses different IV`() {
        val key = ByteArray(32) { it.toByte() }
        manager.setSessionKey(key)
        val plaintext = "Test data".toByteArray()

        val encrypted1 = manager.encryptIfNeeded(plaintext)
        val encrypted2 = manager.encryptIfNeeded(plaintext)

        val iv1 = encrypted1.copyOfRange(0, 12)
        val iv2 = encrypted2.copyOfRange(0, 12)

        assertFalse("IVs should be different", iv1.contentEquals(iv2))
    }

    @Test
    fun `encryption with 16-byte key works (AES-128)`() {
        val key = ByteArray(16) { it.toByte() } // AES-128 key
        manager.setSessionKey(key)
        val plaintext = "Short key test".toByteArray()

        val encrypted = manager.encryptIfNeeded(plaintext)

        // Verify it can be decrypted
        val iv = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    // ============== Key Management Tests ==============

    @Test
    fun `no encryption without session key`() {
        manager.setSessionKey(null)
        manager.setRequireEncryption(false)
        val plaintext = "Unencrypted data".toByteArray()

        val result = manager.encryptIfNeeded(plaintext)

        assertArrayEquals("Data should pass through unchanged", plaintext, result)
    }

    @Test
    fun `session key can be updated`() {
        val key1 = ByteArray(32) { it.toByte() }
        val key2 = ByteArray(32) { (it + 100).toByte() }
        val plaintext = "Test".toByteArray()

        manager.setSessionKey(key1)
        val encrypted1 = manager.encryptIfNeeded(plaintext)

        manager.setSessionKey(key2)
        val encrypted2 = manager.encryptIfNeeded(plaintext)

        // Different keys should produce different ciphertext
        assertFalse("Different keys should produce different ciphertext",
            encrypted1.contentEquals(encrypted2))
    }

    @Test
    fun `session key can be cleared`() {
        val key = ByteArray(32) { it.toByte() }
        val plaintext = "Test".toByteArray()

        manager.setSessionKey(key)
        manager.setSessionKey(null)
        manager.setRequireEncryption(false)
        val result = manager.encryptIfNeeded(plaintext)

        assertArrayEquals("Data should pass through after key cleared", plaintext, result)
    }

    // ============== Encryption Enforcement Tests ==============

    @Test
    fun `throws NoSessionKeyException when required and no key`() {
        manager.setSessionKey(null)
        manager.setRequireEncryption(true)
        val plaintext = "Test".toByteArray()

        assertThrows(EncryptionException.NoSessionKeyException::class.java) {
            manager.encryptIfNeeded(plaintext)
        }
    }

    @Test
    fun `does not throw when not required and no key`() {
        manager.setSessionKey(null)
        manager.setRequireEncryption(false)
        val plaintext = "Test".toByteArray()

        val result = manager.encryptIfNeeded(plaintext)

        assertArrayEquals("Should pass through", plaintext, result)
    }

    @Test
    fun `require encryption flag persists`() {
        assertFalse("Default should be false", manager.isEncryptionRequired())

        manager.setRequireEncryption(true)
        assertTrue("Should be true after set", manager.isEncryptionRequired())

        manager.setRequireEncryption(false)
        assertFalse("Should be false after clear", manager.isEncryptionRequired())
    }

    // ============== Encryption Error Flow Tests ==============

    @Test
    fun `encryption error is exposed via StateFlow`() = runTest {
        // Manually trigger error via reflection (since we can't easily cause cipher failure)
        val errorField = WifiManager::class.java.getDeclaredField("_encryptionError").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val errorFlow = errorField.get(manager) as MutableStateFlow<EncryptionException?>

        val testError = EncryptionException.EncryptionFailedException("Test error")
        errorFlow.value = testError

        assertEquals("Error should be exposed", testError, manager.encryptionError.value)
    }

    @Test
    fun `clearEncryptionError clears the error state`() = runTest {
        val errorField = WifiManager::class.java.getDeclaredField("_encryptionError").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val errorFlow = errorField.get(manager) as MutableStateFlow<EncryptionException?>
        errorFlow.value = EncryptionException.NoSessionKeyException("Test")

        manager.clearEncryptionError()

        assertNull("Error should be cleared", manager.encryptionError.value)
    }

    // ============== Discovery Message Authentication Tests ==============

    @Test
    fun `discovery message without key uses legacy format`() {
        manager.setSessionKey(null)
        val (payload, nonce) = manager.buildDiscoveryMessage()

        assertNull("Nonce should be null without key", nonce)
        assertEquals("ARDUNAKON_DISCOVER", String(payload))
    }

    @Test
    fun `discovery message with key includes nonce and signature`() {
        val key = ByteArray(32) { it.toByte() }
        manager.setSessionKey(key)

        val (payload, nonce) = manager.buildDiscoveryMessage()

        assertNotNull("Nonce should be present with key", nonce)
        val message = String(payload)
        val parts = message.split("|")
        assertEquals("Should have 3 parts", 3, parts.size)
        assertEquals("ARDUNAKON_DISCOVER", parts[0])
    }

    @Test
    fun `discovery signature can be verified`() {
        val key = ByteArray(32) { it.toByte() }
        manager.setSessionKey(key)

        val (payload, nonce) = manager.buildDiscoveryMessage()
        val parts = String(payload).split("|")
        val generatedNonce = parts[1]
        val signature = parts[2]

        assertTrue("Signature should verify", manager.verifySignature(generatedNonce, signature, key))
    }

    @Test
    fun `invalid signature is rejected`() {
        val key = ByteArray(32) { it.toByte() }
        val wrongKey = ByteArray(32) { (it + 50).toByte() }
        manager.setSessionKey(key)

        val (payload, _) = manager.buildDiscoveryMessage()
        val parts = String(payload).split("|")
        val nonce = parts[1]

        // Create signature with wrong key
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(wrongKey, "HmacSHA256"))
        val wrongSig = android.util.Base64.encodeToString(
            mac.doFinal(android.util.Base64.decode(nonce, android.util.Base64.NO_WRAP)),
            android.util.Base64.NO_WRAP
        )

        assertFalse("Wrong signature should be rejected", manager.verifySignature(nonce, wrongSig, key))
    }

    // ============== Edge Cases ==============

    @Test
    fun `empty payload encrypts successfully`() {
        val key = ByteArray(32) { it.toByte() }
        manager.setSessionKey(key)
        val empty = ByteArray(0)

        val encrypted = manager.encryptIfNeeded(empty)

        // Should still have IV + GCM tag
        assertTrue("Empty payload should still produce ciphertext", encrypted.size >= 12 + 16)
    }

    @Test
    fun `large payload encrypts successfully`() {
        val key = ByteArray(32) { it.toByte() }
        manager.setSessionKey(key)
        val largePayload = ByteArray(10000) { (it % 256).toByte() }

        val encrypted = manager.encryptIfNeeded(largePayload)

        // Decrypt to verify
        val iv = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(ciphertext)

        assertArrayEquals("Large payload should round-trip", largePayload, decrypted)
    }
}
