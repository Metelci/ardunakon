package com.metelci.ardunakon.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.security.CryptoEngine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for WifiEncryptionPreferences.
 * Tests PSK generation, storage, and retrieval with mock encryption.
 */
@RunWith(RobolectricTestRunner::class)
class WifiEncryptionPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: WifiEncryptionPreferences
    private lateinit var mockCrypto: FakeCryptoEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockCrypto = FakeCryptoEngine()
        prefs = WifiEncryptionPreferences(context, mockCrypto)
    }

    @After
    fun tearDown() {
        prefs.clearAll()
    }

    // ============== PSK Generation Tests ==============

    @Test
    fun `generatePsk creates 32-byte key`() {
        val psk = prefs.generatePsk()
        assertEquals("PSK should be 32 bytes", 32, psk.size)
    }

    @Test
    fun `generatePsk creates unique keys`() {
        val psk1 = prefs.generatePsk()
        val psk2 = prefs.generatePsk()
        assertFalse("Generated PSKs should be unique", psk1.contentEquals(psk2))
    }

    // ============== Storage Tests ==============

    @Test
    fun `savePsk stores encrypted key`() = runTest {
        val psk = ByteArray(32) { it.toByte() }
        prefs.savePsk("192.168.4.1", psk)

        assertTrue("Crypto engine should have encrypted", mockCrypto.encryptCalled)
        assertTrue("PSK should be marked as stored", prefs.hasPsk("192.168.4.1"))
    }

    @Test
    fun `loadPsk retrieves original key`() = runTest {
        val original = ByteArray(32) { it.toByte() }
        prefs.savePsk("192.168.4.1", original)

        val loaded = prefs.loadPsk("192.168.4.1")

        assertNotNull("Loaded PSK should not be null", loaded)
        assertArrayEquals("Loaded PSK should match original", original, loaded)
    }

    @Test
    fun `loadPsk returns null for unknown device`() = runTest {
        val result = prefs.loadPsk("10.0.0.1")
        assertNull("Should return null for unknown device", result)
    }

    @Test
    fun `hasPsk returns false for unknown device`() {
        assertFalse("Should return false for unknown device", prefs.hasPsk("10.0.0.1"))
    }

    // ============== Get Or Create Tests ==============

    @Test
    fun `getOrCreatePsk creates new PSK if not exists`() = runTest {
        val psk = prefs.getOrCreatePsk("192.168.1.100")

        assertEquals("PSK should be 32 bytes", 32, psk.size)
        assertTrue("PSK should be stored", prefs.hasPsk("192.168.1.100"))
    }

    @Test
    fun `getOrCreatePsk returns existing PSK`() = runTest {
        val original = ByteArray(32) { (it + 50).toByte() }
        prefs.savePsk("192.168.1.100", original)

        val retrieved = prefs.getOrCreatePsk("192.168.1.100")

        assertArrayEquals("Should return existing PSK", original, retrieved)
    }

    // ============== Clear Tests ==============

    @Test
    fun `clearPsk removes specific device`() = runTest {
        prefs.savePsk("192.168.4.1", ByteArray(32))
        prefs.savePsk("192.168.4.2", ByteArray(32))

        prefs.clearPsk("192.168.4.1")

        assertFalse("Cleared device should not exist", prefs.hasPsk("192.168.4.1"))
        assertTrue("Other device should still exist", prefs.hasPsk("192.168.4.2"))
    }

    @Test
    fun `clearAll removes all stored PSKs`() = runTest {
        prefs.savePsk("192.168.4.1", ByteArray(32))
        prefs.savePsk("192.168.4.2", ByteArray(32))

        prefs.clearAll()

        assertFalse("All PSKs should be cleared", prefs.hasPsk("192.168.4.1"))
        assertFalse("All PSKs should be cleared", prefs.hasPsk("192.168.4.2"))
    }

    // ============== Device List Tests ==============

    @Test
    fun `getStoredDevices returns all stored IPs`() = runTest {
        prefs.savePsk("192.168.4.1", ByteArray(32))
        prefs.savePsk("10.0.0.5", ByteArray(32))

        val devices = prefs.getStoredDevices()

        assertEquals("Should return 2 devices", 2, devices.size)
        assertTrue("Should contain first IP", devices.contains("192.168.4.1"))
        assertTrue("Should contain second IP", devices.contains("10.0.0.5"))
    }

    // ============== Preview Tests ==============

    @Test
    fun `getPskPreview returns truncated hex`() = runTest {
        val psk = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()) +
                ByteArray(28)
        prefs.savePsk("192.168.4.1", psk)

        val preview = prefs.getPskPreview("192.168.4.1")

        assertNotNull("Preview should not be null", preview)
        assertTrue("Preview should end with ...", preview!!.endsWith("..."))
        assertEquals("Preview should show first 4 bytes as hex", "deadbeef...", preview)
    }

    @Test
    fun `getPskPreview returns null for unknown device`() = runTest {
        val preview = prefs.getPskPreview("10.0.0.1")
        assertNull("Should return null for unknown device", preview)
    }

    // ============== Fake Crypto Engine ==============

    /**
     * Simple fake crypto engine that just Base64 encodes/decodes.
     * Used to test PSK storage without Android Keystore.
     */
    private class FakeCryptoEngine : CryptoEngine {
        var encryptCalled = false
        var decryptCalled = false

        override fun encrypt(data: String): String {
            encryptCalled = true
            // Simple "encryption" - just reverse the string
            return "ENCRYPTED:$data"
        }

        override fun decrypt(encryptedData: String): String {
            decryptCalled = true
            return encryptedData.removePrefix("ENCRYPTED:")
        }
    }
}
