package com.metelci.ardunakon.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class HandshakeChallenge(
    val nonce: ByteArray,
    val expiresAt: Long
)

data class HandshakeResult(
    val trusted: Boolean,
    val sessionKey: ByteArray?,
    val error: String? = null
)

/**
    * Issues MAC-based challenges and verifies responses using per-device shared keys from [TrustStore].
    * Session keys are derived from the challenge nonce to encrypt subsequent control traffic.
    */
class HandshakeManager(context: Context) {
    private val trustStore = TrustStore(context)
    private val random = SecureRandom()
    private val inFlight = ConcurrentHashMap<String, HandshakeChallenge>()

    suspend fun issueChallenge(address: String): HandshakeChallenge {
        val nonce = ByteArray(16).also(random::nextBytes)
        val challenge = HandshakeChallenge(nonce, expiresAt = System.currentTimeMillis() + 10_000)
        inFlight[address] = challenge
        return challenge
    }

    suspend fun verifyResponse(address: String, macBase64: String): HandshakeResult {
        val challenge = inFlight[address] ?: return HandshakeResult(false, null, "No challenge in flight")
        if (System.currentTimeMillis() > challenge.expiresAt) {
            inFlight.remove(address)
            return HandshakeResult(false, null, "Challenge expired")
        }

        val trustedDevice = trustStore.getOrCreateProvisional(address)
        val macBytes = try {
            Base64.decode(macBase64, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            return HandshakeResult(false, null, "Invalid MAC encoding")
        }

        val expected = hmac(challenge.nonce, trustedDevice.sharedKey)
        val trusted = macBytes.contentEquals(expected)
        if (!trusted) {
            trustStore.revoke(address)
            inFlight.remove(address)
            return HandshakeResult(false, null, "MAC mismatch")
        }

        // Derive a session key tied to the nonce
        val sessionKey = hmac(challenge.nonce + byteArrayOf(0x01), trustedDevice.sharedKey)
        trustStore.upsertTrustedDevice(address, trustedDevice.sharedKey) // ensure non-revoked record
        inFlight.remove(address)
        return HandshakeResult(true, sessionKey)
    }

    fun clear(address: String) {
        inFlight.remove(address)
    }

    private fun hmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
