package com.metelci.ardunakon.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EncryptionException sealed class hierarchy.
 *
 * Tests exception types, messages, and inheritance structure
 * for proper error handling in security layer.
 */
class EncryptionExceptionTest {

    @Test
    fun handshakeFailedException_extendsEncryptionException() {
        val exception = EncryptionException.HandshakeFailedException("Test handshake failed")

        assertTrue(exception is EncryptionException)
        assertEquals("Test handshake failed", exception.message)
    }

    @Test
    fun encryptionFailedException_extendsEncryptionException() {
        val exception = EncryptionException.EncryptionFailedException("Encryption failed")

        assertTrue(exception is EncryptionException)
        assertEquals("Encryption failed", exception.message)
    }

    @Test
    fun encryptionFailedException_withCause() {
        val cause = IllegalStateException("Root cause")
        val exception = EncryptionException.EncryptionFailedException("Encryption failed", cause)

        assertEquals("Encryption failed", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun noSessionKeyException_extendsEncryptionException() {
        val exception = EncryptionException.NoSessionKeyException("No session key")

        assertTrue(exception is EncryptionException)
        assertEquals("No session key", exception.message)
    }

    @Test
    fun securityException_extendsEncryptionException() {
        val exception = EncryptionException.SecurityException("Security error")

        assertTrue(exception is EncryptionException)
        assertEquals("Security error", exception.message)
    }

    @Test
    fun allExceptions_areThrowable() {
        val handshake = EncryptionException.HandshakeFailedException("msg1")
        val encryption = EncryptionException.EncryptionFailedException("msg2")
        val noKey = EncryptionException.NoSessionKeyException("msg3")
        val security = EncryptionException.SecurityException("msg4")

        assertTrue(handshake is Throwable)
        assertTrue(encryption is Throwable)
        assertTrue(noKey is Throwable)
        assertTrue(security is Throwable)
    }

    @Test
    fun exceptions_canBeCaught() {
        try {
            throw EncryptionException.HandshakeFailedException("Test")
        } catch (e: EncryptionException) {
            assertTrue(e is EncryptionException.HandshakeFailedException)
            assertEquals("Test", e.message)
        }
    }

    @Test
    fun exceptions_retainStackTrace() {
        val exception = EncryptionException.SecurityException("Security issue")
        val stackTrace = exception.stackTrace

        assertNotNull(stackTrace)
        assertTrue(stackTrace.isNotEmpty())
    }

    @Test
    fun encryptionFailedException_nullCauseAllowed() {
        val exception = EncryptionException.EncryptionFailedException("Failed", null)

        assertEquals("Failed", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun handshakeFailedException_messageRequired() {
        val exception = EncryptionException.HandshakeFailedException("Handshake timeout")

        assertNotNull(exception.message)
        assertFalse(exception.message!!.isEmpty())
    }

    @Test
    fun noSessionKeyException_descriptiveMessage() {
        val exception = EncryptionException.NoSessionKeyException(
            "Session key not established"
        )

        assertTrue(exception.message!!.contains("Session") || exception.message!!.contains("key"))
    }

    @Test
    fun securityException_genericMessage() {
        val exception = EncryptionException.SecurityException("Generic security error")

        assertFalse(exception.message!!.contains("stack"))
        assertFalse(exception.message!!.contains("Exception"))
    }

    @Test
    fun allExceptionTypes_distinctClasses() {
        val handshake = EncryptionException.HandshakeFailedException("msg")
        val encryption = EncryptionException.EncryptionFailedException("msg")
        val noKey = EncryptionException.NoSessionKeyException("msg")
        val security = EncryptionException.SecurityException("msg")

        assertFalse(handshake::class == encryption::class)
        assertFalse(handshake::class == noKey::class)
        assertFalse(handshake::class == security::class)
        assertFalse(encryption::class == noKey::class)
    }

    @Test
    fun exceptions_whenPattern_matchesCorrectly() {
        val exception: EncryptionException = EncryptionException.HandshakeFailedException("Test")

        val result = when (exception) {
            is EncryptionException.HandshakeFailedException -> "handshake"
            is EncryptionException.EncryptionFailedException -> "encryption"
            is EncryptionException.NoSessionKeyException -> "no_key"
            is EncryptionException.SecurityException -> "security"
        }

        assertEquals("handshake", result)
    }
}
