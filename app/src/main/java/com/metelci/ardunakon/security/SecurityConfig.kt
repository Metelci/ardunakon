package com.metelci.ardunakon.security

/**
 * Security configuration for Ardunakon application.
 *
 * This class centralizes all security settings and provides a single point
 * of configuration for security-related behavior throughout the application.
 */
object SecurityConfig {

    /**
     * Whether encryption is required by default for all WiFi communications.
     *
     * SECURITY FIX: Changed from false to true to prevent plaintext transmission.
     * Only set to false for development/testing purposes.
     */
    const val REQUIRE_WIFI_ENCRYPTION_DEFAULT = true

    /**
     * Whether to allow plaintext fallback for development/testing.
     *
     * SECURITY: Should be false in production builds.
     */
    const val ALLOW_PLAINTEXT_FALLBACK = false

    /**
     * Whether to log detailed security information.
     *
     * SECURITY: Should be false in production to prevent information leakage.
     */
    const val LOG_SECURITY_DETAILS = false

    /**
     * Whether to show detailed error messages to users.
     *
     * SECURITY: Should be false to prevent information leakage.
     */
    const val SHOW_DETAILED_ERRORS = false

    /**
     * Timeout for security handshake operations (milliseconds).
     */
    const val HANDSHAKE_TIMEOUT_MS = 5000L

    /**
     * Maximum number of failed handshake attempts before blocking.
     */
    const val MAX_HANDSHAKE_ATTEMPTS = 3

    /**
     * Whether to enable additional security hardening features.
     *
     * SECURITY: Enables additional checks and validations.
     */
    const val ENABLE_SECURITY_HARDENING = true

    /**
     * Whether to validate packet integrity beyond basic checksum.
     *
     * SECURITY: Enables additional packet validation.
     */
    const val ENABLE_PACKET_VALIDATION = true

    /**
     * Minimum required Android API level for security features.
     */
    const val MIN_SECURITY_API_LEVEL = 26 // Android 8.0

    /**
     * Default PSK for Arduino R4 WiFi devices (for compatibility).
     *
     * SECURITY: This should be changed in production or device-specific PSKs should be used.
     */
    val DEFAULT_R4_WIFI_PSK: ByteArray =
        "ArdunakonSecretKey1234567890ABCD".toByteArray(Charsets.UTF_8)

    /**
     * Whether to use the default PSK for unknown devices.
     *
     * SECURITY: Should be false in production to prevent MITM attacks.
     */
    const val ALLOW_DEFAULT_PSK_FOR_UNKNOWN_DEVICES = false

    /**
     * Security warning messages for user-facing interfaces.
     */
    object Warnings {
        const val PLAINTEXT_TRANSMISSION =
            "⚠️ Warning: Unencrypted data transmission detected. Use only on trusted networks."

        const val ENCRYPTION_REQUIRED =
            "🔒 Security: Encrypted connection required. Unsupported devices will be blocked."

        const val HANDSHAKE_FAILED =
            "🔒 Security: Device verification failed. Connection blocked for security."

        const val DEVELOPMENT_MODE =
            "⚠️ Development Mode: Security features reduced for testing."
    }

    /**
     * Error messages that don't leak implementation details.
     */
    object SecureErrorMessages {
        const val GENERIC_SECURITY_ERROR = "Security protocol error occurred"
        const val DEVICE_VERIFICATION_FAILED = "Device security verification failed"
        const val ENCRYPTION_UNAVAILABLE = "Encryption service unavailable"
        const val HANDSHAKE_TIMEOUT = "Security handshake timeout"
        const val INVALID_SECURITY_RESPONSE = "Invalid security response from device"
        const val CORRUPTED_SECURE_DATA = "Corrupted secure data detected"
        const val AUTHENTICATION_REQUIRED = "Device authentication required"
        const val SECURITY_SYSTEM_UNAVAILABLE = "Security system unavailable"
    }
}
