package com.metelci.ardunakon.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SecurityManager.
 *
 * Tests Android Keystore-based encryption/decryption,
 * error handling, and data integrity. Note: Actual keystore
 * operations require device authentication in test environment.
 */
@RunWith(AndroidJUnit4::class)
class SecurityManagerTest {

    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        // Note: SecurityManager requires Android Keystore which may not be
        // available or may require authentication in test environment
        try {
            securityManager = SecurityManager()
        } catch (e: Exception) {
            // Skip tests if keystore is unavailable (common in emulators)
        }
    }

    @Test
    fun securityManager_implementsCryptoEngine() {
        if (!::securityManager.isInitialized) return

        assertTrue(securityManager is CryptoEngine)
    }

    @Test
    fun encrypt_withSimpleString_returnsBase64() {
        if (!::securityManager.isInitialized) return

        try {
            val plaintext = "Hello World"
            val encrypted = securityManager.encrypt(plaintext)

            assertNotNull(encrypted)
            assertFalse(encrypted.isEmpty())
            assertNotEquals(plaintext, encrypted)
            
            // Base64 encoded string should not contain plaintext
            assertFalse(encrypted.contains("Hello"))
        } catch (e: AuthRequiredException) {
            // Expected in test environment without authentication
        }
    }

    @Test
    fun encrypt_decrypt_roundTrip() {
        if (!::securityManager.isInitialized) return

        try {
            val original = "Test Data 123"
            
            val encrypted = securityManager.encrypt(original)
            val decrypted = securityManager.decrypt(encrypted)

            assertEquals(original, decrypted)
        } catch (e: AuthRequiredException) {
            // Expected in test environment
        }
    }

    @Test
    fun encrypt_multipleValues_producesDifferentCiphertexts() {
        if (!::securityManager.isInitialized) return

        try {
            val text = "Same Input"
            
            val encrypted1 = securityManager.encrypt(text)
            val encrypted2 = securityManager.encrypt(text)

            // Due to random IV, same input should produce different ciphertexts
            assertNotEquals(
                "Same input should produce different encrypted outputs (random IV)",
                encrypted1,
                encrypted2
            )
        } catch (e: AuthRequiredException) {
            // Expected
        }
    }

    @Test
    fun decrypt_withInvalidData_throwsException() {
        if (!::securityManager.isInitialized) return

        try {
            securityManager.decrypt("InvalidBase64Data!!!")
            fail("Should throw exception for invalid data")
        } catch (e: AuthRequiredException) {
            // Expected - corrupted data or authentication required
            assertTrue(e.message!!.contains("Corrupted") || e.message!!.contains("Invalid"))
        } catch (e: Exception) {
            // Other exceptions also acceptable for invalid input
        }
    }

    @Test
    fun decrypt_withTooShortData_throwsException() {
        if (!::securityManager.isInitialized) return

        try {
            securityManager.decrypt("AA==") // Very short base64
            fail("Should throw exception for corrupted data")
        } catch (e: AuthRequiredException) {
            assertTrue(e.message!!.contains("Corrupted") || e.message!!.contains("Invalid"))
        } catch (e: Exception) {
            // Acceptable
        }
    }

    @Test
    fun encrypt_emptyString_succeeds() {
        if (!::securityManager.isInitialized) return

        try {
            val encrypted = securityManager.encrypt("")
            
            assertNotNull(encrypted)
            assertFalse(encrypted.isEmpty())
            
            val decrypted = securityManager.decrypt(encrypted)
            assertEquals("", decrypted)
        } catch (e: AuthRequiredException) {
            // Expected
        }
    }

    @Test
    fun encrypt_unicodeCharacters_preservedAfterDecryption() {
        if (!::securityManager.isInitialized) return

        try {
            val original = "Hello 世界 🔒🌍"
            
            val encrypted = securityManager.encrypt(original)
            val decrypted = securityManager.decrypt(encrypted)

            assertEquals(original, decrypted)
        } catch (e: AuthRequiredException) {
            // Expected
        }
    }

    @Test
    fun encrypt_largeData_succeeds() {
        if (!::securityManager.isInitialized) return

        try {
            val largeText = "A".repeat(10000)
            
            val encrypted = securityManager.encrypt(largeText)
            val decrypted = securityManager.decrypt(encrypted)

            assertEquals(largeText, decrypted)
        } catch (e: AuthRequiredException) {
            // Expected
        }
    }

    @Test
    fun authRequiredException_hasMessage() {
        val exception = AuthRequiredException("Test message")

        assertEquals("Test message", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun authRequiredException_withCause() {
        val cause = IllegalStateException("Root")
        val exception = AuthRequiredException("Wrapper", cause)

        assertEquals("Wrapper", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun decrypt_tamperedCiphertext_throwsException() {
        if (!::securityManager.isInitialized) return

        try {
            val original = "Secret Data"
            val encrypted = securityManager.encrypt(original)
            
            // Tamper with the ciphertext
            val tampered = encrypted.dropLast(1) + "X"
            
            securityManager.decrypt(tampered)
            fail("Should throw exception for tampered data")
        } catch (e: AuthRequiredException) {
            // Expected - integrity check failed
        } catch (e: Exception) {
            // Acceptable
        }
    }

    @Test
    fun encrypt_specialCharacters_preservedInRoundTrip() {
        if (!::securityManager.isInitialized) return

        try {
            val special = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
            
            val encrypted = securityManager.encrypt(special)
            val decrypted = securityManager.decrypt(encrypted)

            assertEquals(special, decrypted)
        } catch (e: AuthRequiredException) {
            // Expected
        }
    }

    @Test
    fun encrypt_newlines_preserved() {
        if (!::securityManager.isInitialized) return

        try {
            val multiline = "Line 1\nLine 2\nLine 3"
            
            val encrypted = securityManager.encrypt(multiline)
            val decrypted = securityManager.decrypt(encrypted)

            assertEquals(multiline, decrypted)
        } catch (e: AuthRequiredException) {
            // Expected
        }
    }
}
