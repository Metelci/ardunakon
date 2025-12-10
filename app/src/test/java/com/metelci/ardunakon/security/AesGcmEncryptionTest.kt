package com.metelci.ardunakon.security

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Security tests for AES-256-GCM encryption used in WiFi communication.
 * Tests encryption/decryption correctness, IV uniqueness, and data integrity.
 */
class AesGcmEncryptionTest {

    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagLength = 128 // bits
    private val ivLength = 12 // bytes (96 bits, recommended for GCM)
    private val keyLength = 256 // bits

    private fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(keyLength)
        return keyGen.generateKey()
    }

    private fun generateIv(): ByteArray {
        val iv = ByteArray(ivLength)
        SecureRandom().nextBytes(iv)
        return iv
    }

    @Test
    fun `encrypt and decrypt returns original plaintext`() {
        val key = generateKey()
        val plaintext = "Hello, Ardunakon!".toByteArray()

        // Encrypt
        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Decrypt
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val decrypted = decryptCipher.doFinal(ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `ciphertext differs from plaintext`() {
        val key = generateKey()
        val plaintext = "Secret data for servo control".toByteArray()

        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        assertFalse("Ciphertext should differ from plaintext", plaintext.contentEquals(ciphertext))
    }

    @Test
    fun `same plaintext with different IVs produces different ciphertext`() {
        val key = generateKey()
        val plaintext = "Same message".toByteArray()

        // First encryption
        val iv1 = generateIv()
        val cipher1 = Cipher.getInstance(transformation)
        cipher1.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv1))
        val ciphertext1 = cipher1.doFinal(plaintext)

        // Second encryption with different IV
        val iv2 = generateIv()
        val cipher2 = Cipher.getInstance(transformation)
        cipher2.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv2))
        val ciphertext2 = cipher2.doFinal(plaintext)

        assertFalse("Same plaintext with different IVs should produce different ciphertext",
            ciphertext1.contentEquals(ciphertext2))
    }

    @Test
    fun `wrong key fails decryption`() {
        val key1 = generateKey()
        val key2 = generateKey()
        val plaintext = "Secret message".toByteArray()

        // Encrypt with key1
        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key1, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Try to decrypt with key2
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key2, GCMParameterSpec(gcmTagLength, iv))

        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            decryptCipher.doFinal(ciphertext)
        }
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val key = generateKey()
        val plaintext = "Authentic message".toByteArray()

        // Encrypt
        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Tamper with ciphertext (flip a bit)
        val tamperedCiphertext = ciphertext.copyOf()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0x01).toByte()

        // Try to decrypt tampered data
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))

        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            decryptCipher.doFinal(tamperedCiphertext)
        }
    }

    @Test
    fun `wrong IV fails decryption`() {
        val key = generateKey()
        val plaintext = "Message with specific IV".toByteArray()

        // Encrypt
        val iv1 = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv1))
        val ciphertext = cipher.doFinal(plaintext)

        // Try to decrypt with different IV
        val iv2 = generateIv()
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv2))

        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            decryptCipher.doFinal(ciphertext)
        }
    }

    @Test
    fun `encrypt empty data succeeds`() {
        val key = generateKey()
        val plaintext = ByteArray(0)

        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // GCM produces authentication tag even for empty plaintext
        assertTrue("Ciphertext should contain at least the auth tag", ciphertext.isNotEmpty())

        // Decrypt
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val decrypted = decryptCipher.doFinal(ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt large data succeeds`() {
        val key = generateKey()
        val plaintext = ByteArray(10000) { it.toByte() } // 10KB of data

        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Decrypt
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val decrypted = decryptCipher.doFinal(ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `key from byte array works correctly`() {
        // Simulate session key from handshake (32 bytes for AES-256)
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val key = SecretKeySpec(keyBytes, "AES")

        val plaintext = "Session encrypted message".toByteArray()

        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val decrypted = decryptCipher.doFinal(ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `iv prepended format works for wifi packets`() {
        // Test the format used in WifiManager: IV + Ciphertext
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val key = SecretKeySpec(keyBytes, "AES")

        val plaintext = "Joystick command packet".toByteArray()

        // Encrypt (simulating WifiManager.encryptIfNeeded)
        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val encrypted = cipher.doFinal(plaintext)
        val packet = iv + encrypted // IV prepended

        // Verify packet structure
        assertEquals("Packet should be IV + ciphertext", ivLength + encrypted.size, packet.size)

        // Decrypt (extract IV from packet)
        val extractedIv = packet.copyOfRange(0, ivLength)
        val extractedCiphertext = packet.copyOfRange(ivLength, packet.size)

        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, extractedIv))
        val decrypted = decryptCipher.doFinal(extractedCiphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `multiple sequential encryptions with unique IVs`() {
        // Key generation validates the crypto provider is available
        generateKey()
        val secureRandom = SecureRandom()
        val ivSet = mutableSetOf<String>()

        // Simulate 100 packet encryptions
        repeat(100) {
            val iv = ByteArray(ivLength)
            secureRandom.nextBytes(iv)
            val ivHex = iv.joinToString("") { "%02x".format(it) }

            assertFalse("IV should be unique for each encryption", ivSet.contains(ivHex))
            ivSet.add(ivHex)
        }

        assertEquals("All 100 IVs should be unique", 100, ivSet.size)
    }

    @Test
    fun `gcm authentication tag length is 128 bits`() {
        val key = generateKey()
        val plaintext = "Test message".toByteArray()

        val iv = generateIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // GCM output = ciphertext (same length as plaintext) + auth tag (16 bytes for 128-bit)
        val expectedLength = plaintext.size + 16 // 128 bits = 16 bytes
        assertEquals("Ciphertext should include 128-bit auth tag", expectedLength, ciphertext.size)
    }
}
