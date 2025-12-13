package com.metelci.ardunakon

import com.metelci.ardunakon.security.CryptoEngine

/**
 * Lightweight symmetric codec for tests so we avoid touching Android Keystore.
 */
class TestCryptoEngine : CryptoEngine {
    override fun encrypt(data: String): String = "enc|$data"

    override fun decrypt(encryptedData: String): String {
        if (!encryptedData.startsWith("enc|")) {
            throw IllegalArgumentException("Invalid test cipher text")
        }
        return encryptedData.removePrefix("enc|")
    }
}
