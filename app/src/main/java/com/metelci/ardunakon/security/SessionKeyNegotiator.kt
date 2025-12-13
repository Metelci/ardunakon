package com.metelci.ardunakon.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Handles session key negotiation using a challenge-response handshake.
 *
 * Protocol:
 * 1. App generates a random nonce (challenge) and sends to device
 * 2. Device signs the nonce with its PSK and responds with its own nonce + signature
 * 3. App verifies device signature and derives session key from both nonces
 * 4. Session key is used for AES-GCM encryption of all subsequent packets
 *
 * Key Derivation uses HKDF with:
 * - IKM (Input Key Material): PSK
 * - Salt: appNonce || deviceNonce
 * - Info: "ARDUNAKON_SESSION_v1"
 */
class SessionKeyNegotiator(
    private val preSharedKey: ByteArray
) {
    private val secureRandom = SecureRandom()
    private var appNonce: ByteArray? = null

    companion object {
        private const val NONCE_SIZE = 16
        private const val SESSION_KEY_SIZE = 32 // AES-256
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private val HKDF_INFO = "ARDUNAKON_SESSION_v1".toByteArray()
    }

    /**
     * Starts the handshake by generating a challenge nonce.
     * @return The nonce bytes to send to the device
     */
    fun startHandshake(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)
        appNonce = nonce
        return nonce
    }

    /**
     * Completes the handshake by verifying the device's response and deriving the session key.
     *
     * @param deviceNonce The nonce received from the device
     * @param deviceSignature The device's HMAC signature of (appNonce || deviceNonce)
     * @return The derived session key (32 bytes for AES-256)
     * @throws EncryptionException.HandshakeFailedException if verification fails
     */
    fun completeHandshake(deviceNonce: ByteArray, deviceSignature: ByteArray): ByteArray {
        val storedNonce = appNonce
            ?: throw EncryptionException.HandshakeFailedException("Handshake not started")

        if (deviceNonce.size != NONCE_SIZE) {
            throw EncryptionException.HandshakeFailedException(
                "Invalid device nonce size: ${deviceNonce.size}"
            )
        }

        // Verify device signature: HMAC(PSK, appNonce || deviceNonce)
        val expectedSignature = computeHmac(storedNonce + deviceNonce, preSharedKey)
        if (!deviceSignature.contentEquals(expectedSignature)) {
            throw EncryptionException.HandshakeFailedException(
                "Device signature verification failed"
            )
        }

        // Derive session key using HKDF
        val sessionKey = deriveSessionKey(storedNonce, deviceNonce)

        // Clear the stored nonce
        appNonce = null

        return sessionKey
    }

    /**
     * Derives a session key using HKDF-Extract and HKDF-Expand.
     *
     * @param appNonce The app's challenge nonce
     * @param deviceNonce The device's response nonce
     * @return Derived session key (32 bytes)
     */
    private fun deriveSessionKey(appNonce: ByteArray, deviceNonce: ByteArray): ByteArray {
        // HKDF-Extract: PRK = HMAC(salt, IKM)
        val salt = appNonce + deviceNonce
        val prk = computeHmac(preSharedKey, salt)

        // HKDF-Expand: OKM = HMAC(PRK, info || 0x01)
        val expandInput = HKDF_INFO + byteArrayOf(0x01)
        val okm = computeHmac(expandInput, prk)

        // Return first SESSION_KEY_SIZE bytes
        return okm.copyOf(SESSION_KEY_SIZE)
    }

    private fun computeHmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(data)
    }

    /**
     * Encodes a nonce as Base64 for transmission in text-based protocols.
     */
    fun encodeNonce(nonce: ByteArray): String =
        Base64.encodeToString(nonce, Base64.NO_WRAP)

    /**
     * Decodes a Base64-encoded nonce.
     */
    fun decodeNonce(encoded: String): ByteArray =
        Base64.decode(encoded, Base64.NO_WRAP)
}
