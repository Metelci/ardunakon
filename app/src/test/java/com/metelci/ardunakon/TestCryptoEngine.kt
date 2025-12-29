package com.metelci.ardunakon

import com.metelci.ardunakon.security.CryptoEngine

/**
 * Lightweight symmetric codec for tests so we avoid touching Android Keystore.
 */
class TestCryptoEngine : CryptoEngine {
    override fun encrypt(data: String): String = "enc|$data"

    override fun decrypt(encryptedData: String): String {
        require(encryptedData.startsWith("enc|")) { "Invalid test cipher text" }
        return encryptedData.removePrefix("enc|")
    }
}
