package com.metelci.ardunakon.security

/**
 * Custom exceptions for encryption-related errors during connection.
 * These exceptions enable proper error handling and user-facing notifications.
 */
sealed class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Thrown when the handshake with the device fails.
     * This can happen if the device doesn't respond, returns an invalid signature,
     * or the pre-shared key doesn't match.
     */
    class HandshakeFailedException(message: String) : EncryptionException(message)

    /**
     * Thrown when encryption of a packet fails.
     * This typically indicates a cipher initialization error or corrupted key.
     */
    class EncryptionFailedException(message: String, cause: Throwable? = null) : EncryptionException(message, cause)

    /**
     * Thrown when encryption is required but no session key has been established.
     * The user should be prompted to either establish a session or disable encryption.
     */
    class NoSessionKeyException(message: String) : EncryptionException(message)
}
