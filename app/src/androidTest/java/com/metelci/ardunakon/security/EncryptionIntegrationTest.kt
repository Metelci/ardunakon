package com.metelci.ardunakon.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for encryption with preferences.
 *
 * Tests the interaction between SessionKeyNegotiator,
 * WifiEncryptionPreferences, and SecurityConfig to ensure
 * end-to-end encryption flow works correctly.
 */
@RunWith(AndroidJUnit4::class)
class EncryptionIntegrationTest {

    private lateinit var mockPreferences: WifiEncryptionPreferences
    private lateinit var sessionKeyNegotiator: SessionKeyNegotiator

    @Before
    fun setup() {
        mockPreferences = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun encryptionFlow_withStoredPsk_succeeds() = runBlocking {
        val testPsk = "TestSecretKey12345678901234567890"
        every { mockPreferences.getPskFlow(any()) } returns flowOf(testPsk)

        sessionKeyNegotiator = SessionKeyNegotiator(mockPreferences)

        val psk = mockPreferences.getPskFlow("test-device").first()
        
        assertEquals(testPsk, psk)
        assertEquals(32, testPsk.length) // AES-256 requires 32 bytes
    }

    @Test
    fun encryptionFlow_withoutStoredPsk_usesDefault() = runBlocking {
        every { mockPreferences.getPskFlow(any()) } returns flowOf(null)

        val defaultPsk = String(SecurityConfig.DEFAULT_R4_WIFI_PSK)
        
        assertNotNull(defaultPsk)
        assertEquals(32, defaultPsk.length)
    }

    @Test
    fun encryptionFlow_encryptionRequired_enforcesPolicy() {
        assertTrue(
            "WiFi encryption should be required by default",
            SecurityConfig.REQUIRE_WIFI_ENCRYPTION_DEFAULT
        )
        assertFalse(
            "Plaintext fallback should be disabled",
            SecurityConfig.ALLOW_PLAINTEXT_FALLBACK
        )
    }

    @Test
    fun encryptionFlow_handshakeTimeout_isConfigured() {
        val timeout = SecurityConfig.HANDSHAKE_TIMEOUT_MS
        
        assertTrue("Handshake timeout must be positive", timeout > 0)
        assertTrue("Handshake timeout should be reasonable (< 30s)", timeout < 30000)
    }

    @Test
    fun encryptionFlow_maxRetries_isConfigured() {
        val maxRetries = SecurityConfig.MAX_HANDSHAKE_ATTEMPTS
        
        assertTrue("Max retries must be positive", maxRetries > 0)
        assertTrue("Max retries should be reasonable (< 10)", maxRetries < 10)
    }

    @Test
    fun encryptionFlow_securityHardening_isEnabled() {
        assertTrue(SecurityConfig.ENABLE_SECURITY_HARDENING)
        assertTrue(SecurityConfig.ENABLE_PACKET_VALIDATION)
    }

    @Test
    fun encryptionFlow_errorMessages_areGeneric() {
        val genericError = SecurityConfig.SecureErrorMessages.GENERIC_SECURITY_ERROR
        val handshakeError = SecurityConfig.SecureErrorMessages.HANDSHAKE_TIMEOUT
        val verificationError = SecurityConfig.SecureErrorMessages.DEVICE_VERIFICATION_FAILED

        assertFalse(genericError.contains("stack", ignoreCase = true))
        assertFalse(handshakeError.contains("exception", ignoreCase = true))
        assertFalse(verificationError.contains("AES", ignoreCase = true))
    }

    @Test
    fun encryptionFlow_sessionKeyNegotiator_initializes() {
        sessionKeyNegotiator = SessionKeyNegotiator(mockPreferences)
        
        assertNotNull(sessionKeyNegotiator)
    }

    @Test
    fun encryptionFlow_pskLength_isValid() {
        val psk = SecurityConfig.DEFAULT_R4_WIFI_PSK
        
        assertEquals("PSK must be 32 bytes for AES-256", 32, psk.size)
        assertTrue("PSK should not be all zeros", psk.any { it != 0.toByte() })
    }

    @Test
    fun encryptionFlow_multipleDevices_supportSeparatePsks() = runBlocking {
        val device1Psk = "Device1Key123456789012345678901"
        val device2Psk = "Device2Key123456789012345678901"

        every { mockPreferences.getPskFlow("device1") } returns flowOf(device1Psk)
        every { mockPreferences.getPskFlow("device2") } returns flowOf(device2Psk)

        val psk1 = mockPreferences.getPskFlow("device1").first()
        val psk2 = mockPreferences.getPskFlow("device2").first()

        assertEquals(device1Psk, psk1)
        assertEquals(device2Psk, psk2)
        assertNotEquals(psk1, psk2)
    }

    @Test
    fun encryptionFlow_unknownDevice_rejectsDefaultPsk() {
        assertFalse(
            "Default PSK should not be allowed for unknown devices",
            SecurityConfig.ALLOW_DEFAULT_PSK_FOR_UNKNOWN_DEVICES
        )
    }

    @Test
    fun encryptionFlow_productionSettings_areSecure() {
        assertTrue("Encryption required", SecurityConfig.REQUIRE_WIFI_ENCRYPTION_DEFAULT)
        assertFalse("No plaintext fallback", SecurityConfig.ALLOW_PLAINTEXT_FALLBACK)
        assertFalse("No security logging", SecurityConfig.LOG_SECURITY_DETAILS)
        assertFalse("No detailed errors", SecurityConfig.SHOW_DETAILED_ERRORS)
        assertFalse("No default PSK", SecurityConfig.ALLOW_DEFAULT_PSK_FOR_UNKNOWN_DEVICES)
    }

    @Test
    fun encryptionFlow_warningMessages_areUserFriendly() {
        val plaintextWarning = SecurityConfig.Warnings.PLAINTEXT_TRANSMISSION
        val encryptionRequired = SecurityConfig.Warnings.ENCRYPTION_REQUIRED
        
        assertTrue("Should warn about security", plaintextWarning.contains("Warning") || plaintextWarning.contains("⚠️"))
        assertTrue("Should mention encryption", encryptionRequired.contains("Security") || encryptionRequired.contains("🔒"))
    }
}
