package com.metelci.ardunakon.security

/**
 * Custom exceptions for encryption-related errors during connection.
 * These exceptions enable proper error handling and user-facing notifications.
 */
sealed class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * SECURITY FIX: Thrown when the handshake with the device fails.
     * Provides generic error messages to prevent information leakage.
     */
    class HandshakeFailedException(message: String) : EncryptionException(message)

    /**
     * SECURITY FIX: Thrown when encryption of a packet fails.
     * Provides generic error messages to prevent information leakage.
     */
    class EncryptionFailedException(message: String, cause: Throwable? = null) : EncryptionException(message, cause)

    /**
     * SECURITY FIX: Thrown when encryption is required but no session key has been established.
     * Provides generic error messages to prevent information leakage.
     */
    class NoSessionKeyException(message: String) : EncryptionException(message)

    /**
     * SECURITY FIX: Generic security exception for any security-related errors.
     * Used to prevent specific error details from being exposed to users.
     */
    class SecurityException(message: String) : EncryptionException(message)
}
