package com.metelci.ardunakon.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SecurityConfig.
 *
 * Tests security configuration constants, warning messages,
 * and default values for production safety.
 */
class SecurityConfigTest {

    @Test
    fun requireWifiEncryptionDefault_isTrue() {
        assertTrue(
            "WiFi encryption should be required by default for security",
            SecurityConfig.REQUIRE_WIFI_ENCRYPTION_DEFAULT
        )
    }

    @Test
    fun allowPlaintextFallback_isFalse() {
        assertFalse(
            "Plaintext fallback should be disabled in production",
            SecurityConfig.ALLOW_PLAINTEXT_FALLBACK
        )
    }

    @Test
    fun logSecurityDetails_isFalse() {
        assertFalse(
            "Security details should not be logged in production",
            SecurityConfig.LOG_SECURITY_DETAILS
        )
    }

    @Test
    fun showDetailedErrors_isFalse() {
        assertFalse(
            "Detailed errors should not be shown in production to prevent info leakage",
            SecurityConfig.SHOW_DETAILED_ERRORS
        )
    }

    @Test
    fun handshakeTimeoutMs_isReasonable() {
        assertEquals(5000L, SecurityConfig.HANDSHAKE_TIMEOUT_MS)
        assertTrue(SecurityConfig.HANDSHAKE_TIMEOUT_MS > 0)
    }

    @Test
    fun maxHandshakeAttempts_isReasonable() {
        assertEquals(3, SecurityConfig.MAX_HANDSHAKE_ATTEMPTS)
        assertTrue(SecurityConfig.MAX_HANDSHAKE_ATTEMPTS > 0)
    }

    @Test
    fun enableSecurityHardening_isTrue() {
        assertTrue(
            "Security hardening should be enabled",
            SecurityConfig.ENABLE_SECURITY_HARDENING
        )
    }

    @Test
    fun enablePacketValidation_isTrue() {
        assertTrue(
            "Packet validation should be enabled for security",
            SecurityConfig.ENABLE_PACKET_VALIDATION
        )
    }

    @Test
    fun minSecurityApiLevel_isAndroid8OrHigher() {
        assertEquals(26, SecurityConfig.MIN_SECURITY_API_LEVEL)
        assertTrue(SecurityConfig.MIN_SECURITY_API_LEVEL >= 26)
    }

    @Test
    fun defaultR4WifiPsk_hasCorrectLength() {
        val psk = SecurityConfig.DEFAULT_R4_WIFI_PSK
        
        assertNotNull(psk)
        assertTrue("PSK should be at least 16 bytes", psk.size >= 16)
        assertEquals(32, psk.size) // AES-256 requires 32 bytes
    }

    @Test
    fun defaultR4WifiPsk_isNotEmpty() {
        val psk = SecurityConfig.DEFAULT_R4_WIFI_PSK
        
        assertTrue(psk.isNotEmpty())
        psk.forEach { byte ->
            assertNotEquals("PSK should not contain null bytes", 0.toByte(), byte)
        }
    }

    @Test
    fun allowDefaultPskForUnknownDevices_isFalse() {
        assertFalse(
            "Default PSK should not be used for unknown devices in production",
            SecurityConfig.ALLOW_DEFAULT_PSK_FOR_UNKNOWN_DEVICES
        )
    }

    @Test
    fun warnings_plaintextTransmission_isNotEmpty() {
        val warning = SecurityConfig.Warnings.PLAINTEXT_TRANSMISSION
        
        assertFalse(warning.isEmpty())
        assertTrue(warning.contains("Warning") || warning.contains("⚠️"))
    }

    @Test
    fun warnings_encryptionRequired_isNotEmpty() {
        val warning = SecurityConfig.Warnings.ENCRYPTION_REQUIRED
        
        assertFalse(warning.isEmpty())
        assertTrue(warning.contains("Security") || warning.contains("🔒"))
    }

    @Test
    fun warnings_handshakeFailed_isNotEmpty() {
        val warning = SecurityConfig.Warnings.HANDSHAKE_FAILED
        
        assertFalse(warning.isEmpty())
        assertTrue(warning.contains("Security") || warning.contains("🔒"))
    }

    @Test
    fun warnings_developmentMode_isNotEmpty() {
        val warning = SecurityConfig.Warnings.DEVELOPMENT_MODE
        
        assertFalse(warning.isEmpty())
        assertTrue(warning.contains("Development") || warning.contains("⚠️"))
    }

    @Test
    fun secureErrorMessages_genericSecurityError_doesNotLeakInfo() {
        val message = SecurityConfig.SecureErrorMessages.GENERIC_SECURITY_ERROR
        
        assertFalse(message.isEmpty())
        assertFalse("Should not leak implementation details", message.contains("stack"))
        assertFalse("Should not leak implementation details", message.contains("exception"))
    }

    @Test
    fun secureErrorMessages_deviceVerificationFailed_doesNotLeakInfo() {
        val message = SecurityConfig.SecureErrorMessages.DEVICE_VERIFICATION_FAILED
        
        assertFalse(message.isEmpty())
        assertTrue(message.contains("verification") || message.contains("failed"))
    }

    @Test
    fun secureErrorMessages_encryptionUnavailable_doesNotLeakInfo() {
        val message = SecurityConfig.SecureErrorMessages.ENCRYPTION_UNAVAILABLE
        
        assertFalse(message.isEmpty())
        assertFalse("Should not leak crypto details", message.contains("AES"))
        assertFalse("Should not leak crypto details", message.contains("key"))
    }

    @Test
    fun secureErrorMessages_handshakeTimeout_isGeneric() {
        val message = SecurityConfig.SecureErrorMessages.HANDSHAKE_TIMEOUT
        
        assertFalse(message.isEmpty())
        assertTrue(message.contains("timeout") || message.contains("handshake"))
    }

    @Test
    fun secureErrorMessages_allMessagesAreGeneric() {
        val messages = listOf(
            SecurityConfig.SecureErrorMessages.GENERIC_SECURITY_ERROR,
            SecurityConfig.SecureErrorMessages.DEVICE_VERIFICATION_FAILED,
            SecurityConfig.SecureErrorMessages.ENCRYPTION_UNAVAILABLE,
            SecurityConfig.SecureErrorMessages.HANDSHAKE_TIMEOUT,
            SecurityConfig.SecureErrorMessages.INVALID_SECURITY_RESPONSE,
            SecurityConfig.SecureErrorMessages.CORRUPTED_SECURE_DATA,
            SecurityConfig.SecureErrorMessages.AUTHENTICATION_REQUIRED,
            SecurityConfig.SecureErrorMessages.SECURITY_SYSTEM_UNAVAILABLE
        )

        messages.forEach { message ->
            assertFalse("Message should not be empty", message.isEmpty())
            assertFalse("Should not leak stack traces", message.contains("stack"))
            assertFalse("Should not leak exception names", message.contains("Exception"))
        }
    }

    @Test
    fun productionSecuritySettings_areSecure() {
        // Verify all production security flags are properly set
        assertTrue("WiFi encryption required", SecurityConfig.REQUIRE_WIFI_ENCRYPTION_DEFAULT)
        assertFalse("No plaintext fallback", SecurityConfig.ALLOW_PLAINTEXT_FALLBACK)
        assertFalse("No security detail logging", SecurityConfig.LOG_SECURITY_DETAILS)
        assertFalse("No detailed error messages", SecurityConfig.SHOW_DETAILED_ERRORS)
        assertFalse("No default PSK for unknown", SecurityConfig.ALLOW_DEFAULT_PSK_FOR_UNKNOWN_DEVICES)
        assertTrue("Security hardening enabled", SecurityConfig.ENABLE_SECURITY_HARDENING)
        assertTrue("Packet validation enabled", SecurityConfig.ENABLE_PACKET_VALIDATION)
    }

    @Test
    fun timeouts_arePositiveValues() {
        assertTrue("Handshake timeout must be positive", SecurityConfig.HANDSHAKE_TIMEOUT_MS > 0)
        assertTrue("Max attempts must be positive", SecurityConfig.MAX_HANDSHAKE_ATTEMPTS > 0)
    }
}
