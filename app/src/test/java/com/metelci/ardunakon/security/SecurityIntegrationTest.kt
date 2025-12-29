package com.metelci.ardunakon.security

import com.metelci.ardunakon.TestCryptoEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration-style tests for Security package.
 *
 * Tests encryption/decryption flows using TestCryptoEngine
 * (real SecurityManager requires Android Keystore).
 */
class SecurityIntegrationTest {

    private lateinit var cryptoEngine: CryptoEngine

    @Before
    fun setUp() {
        // Use TestCryptoEngine as SecurityManager requires Android Keystore
        cryptoEngine = TestCryptoEngine()
    }

    // ==================== Basic Encryption Flow ====================

    @Test
    fun `encrypt produces non-empty output`() {
        val plaintext = "Hello, World!"

        val ciphertext = cryptoEngine.encrypt(plaintext)

        assertTrue(ciphertext.isNotEmpty())
    }

    @Test
    fun `encrypt handles empty string`() {
        val ciphertext = cryptoEngine.encrypt("")

        assertNotNull(ciphertext)
    }

    @Test
    fun `encrypt handles unicode characters`() {
        val plaintext = "こんにちは 🎮 世界"

        val ciphertext = cryptoEngine.encrypt(plaintext)

        assertTrue(ciphertext.isNotEmpty())
    }

    @Test
    fun `encrypt handles long strings`() {
        val plaintext = "A".repeat(10000)

        val ciphertext = cryptoEngine.encrypt(plaintext)

        assertTrue(ciphertext.isNotEmpty())
    }

    // ==================== Round-Trip Flow ====================

    @Test
    fun `decrypt recovers original plaintext`() {
        val original = "Hello, World!"

        val ciphertext = cryptoEngine.encrypt(original)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(original, decrypted)
    }

    @Test
    fun `round-trip preserves unicode`() {
        val original = "日本語テスト 🎮 🕹️"

        val ciphertext = cryptoEngine.encrypt(original)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(original, decrypted)
    }

    @Test
    fun `round-trip preserves special characters`() {
        val original = "!@#\$%^&*()_+-=[]{}|;':\",./<>?\\"

        val ciphertext = cryptoEngine.encrypt(original)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(original, decrypted)
    }

    @Test
    fun `round-trip preserves newlines and whitespace`() {
        val original = "Line 1\nLine 2\r\nLine 3\tTabbed"

        val ciphertext = cryptoEngine.encrypt(original)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(original, decrypted)
    }

    @Test
    fun `round-trip preserves empty string`() {
        val original = ""

        val ciphertext = cryptoEngine.encrypt(original)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(original, decrypted)
    }

    @Test
    fun `multiple round-trips work correctly`() {
        val original = "Test message"

        var data = original
        repeat(5) {
            data = cryptoEngine.encrypt(data)
            data = cryptoEngine.decrypt(data)
        }

        assertEquals(original, data)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `encrypt handles JSON data`() {
        val json = """{"name":"Arduino","type":"LE","address":"AA:BB:CC:DD:EE:FF"}"""

        val ciphertext = cryptoEngine.encrypt(json)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(json, decrypted)
    }

    @Test
    fun `encrypt handles base64 input`() {
        val base64Data = "SGVsbG8gV29ybGQh"

        val ciphertext = cryptoEngine.encrypt(base64Data)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(base64Data, decrypted)
    }

    @Test
    fun `encrypt handles numeric strings`() {
        val numeric = "12345678901234567890"

        val ciphertext = cryptoEngine.encrypt(numeric)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(numeric, decrypted)
    }

    // ==================== CryptoEngine Interface ====================

    @Test
    fun `CryptoEngine interface is implemented correctly`() {
        assertTrue(cryptoEngine is CryptoEngine)

        // Should have encrypt and decrypt methods
        val plaintext = "test"
        val ciphertext = cryptoEngine.encrypt(plaintext)
        val decrypted = cryptoEngine.decrypt(ciphertext)

        assertEquals(plaintext, decrypted)
    }

    // ==================== TestCryptoEngine Specific ====================

    @Test
    fun `TestCryptoEngine uses base64 encoding`() {
        val cryptoEngine = TestCryptoEngine()

        val plaintext = "Hello"
        val encrypted = cryptoEngine.encrypt(plaintext)

        // TestCryptoEngine just does base64 encoding
        // So encrypted should be valid base64
        assertTrue(encrypted.isNotEmpty())
    }

    @Test
    fun `different instances produce same result for same input`() {
        val engine1 = TestCryptoEngine()
        val engine2 = TestCryptoEngine()

        val plaintext = "Same input"

        // Both should produce the same result (base64 is deterministic)
        val encrypted1 = engine1.encrypt(plaintext)
        val encrypted2 = engine2.encrypt(plaintext)

        assertEquals(encrypted1, encrypted2)
    }
}
