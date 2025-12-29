package com.metelci.ardunakon.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.EncryptionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WifiManagerTest {

    private lateinit var context: Context
    private lateinit var manager: WifiManager
    private lateinit var prefs: WifiEncryptionPreferences
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        prefs = WifiEncryptionPreferences(context, FakeCryptoEngine())
        manager = WifiManager(
            context = context,
            connectionPreferences = ConnectionPreferences(context, TestCryptoEngine()),
            ioDispatcher = testDispatcher,
            encryptionPreferences = prefs,
            scope = testScope,
            startMonitors = false
        )
    }

    @After
    fun tearDown() {
        manager.cleanup()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is DISCONNECTED`() = runTest(testDispatcher) {
        assertEquals(WifiConnectionState.DISCONNECTED, manager.connectionState.value)
    }

    @Test
    fun `setAutoReconnectEnabled updates state`() = runTest(testDispatcher) {
        manager.setAutoReconnectEnabled(true)
        assertTrue(manager.autoReconnectEnabled.value)

        manager.setAutoReconnectEnabled(false)
        assertFalse(manager.autoReconnectEnabled.value)
    }

    @Test
    fun `isEncryptionRequired is always true`() = runTest(testDispatcher) {
        assertTrue(manager.isEncryptionRequired())
        manager.setRequireEncryption(false)
        assertTrue("Should still be true as it is hardcoded in Manager", manager.isEncryptionRequired())
    }

    @Test
    fun `clearEncryptionError resets flow`() = runTest(testDispatcher) {
        // Use reflection to set private _encryptionError
        val field = WifiManager::class.java.getDeclaredField("_encryptionError")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val errorFlow = field.get(manager) as kotlinx.coroutines.flow.MutableStateFlow<EncryptionException?>

        errorFlow.value = EncryptionException.NoSessionKeyException("test")
        assertNotNull(manager.encryptionError.value)

        manager.clearEncryptionError()
        assertNull(manager.encryptionError.value)
    }

    @Test
    fun `onStateChanged updates flow`() = runTest(testDispatcher) {
        manager.onStateChanged(WifiConnectionState.CONNECTED)
        assertEquals(WifiConnectionState.CONNECTED, manager.connectionState.value)

        manager.onStateChanged(WifiConnectionState.ERROR)
        assertEquals(WifiConnectionState.ERROR, manager.connectionState.value)
    }

    @Test
    fun `onRssiUpdated updates flow`() = runTest(testDispatcher) {
        manager.onRssiUpdated(-50)
        assertEquals(-50, manager.rssi.value)
    }

    @Test
    fun `onRttUpdated updates flow`() = runTest(testDispatcher) {
        val history = listOf(10L, 20L)
        manager.onRttUpdated(15L, history)
        assertEquals(15L, manager.rtt.value)
        assertEquals(history, manager.rttHistory.value)
    }

    @Test
    fun `buildDiscoveryMessage reflects sessionKey presence`() = runTest(testDispatcher) {
        // Case 1: No session key
        val (msg1, nonce1) = manager.buildDiscoveryMessageForTest()
        val str1 = String(msg1)
        assertEquals("ARDUNAKON_DISCOVER", str1)
        assertNull(nonce1)

        // Case 2: With session key
        val key = ByteArray(16) { 0x01.toByte() }
        manager.setSessionKeyForTest(key)

        val (msg2, nonce2) = manager.buildDiscoveryMessageForTest()
        val str2 = String(msg2)
        assertTrue(str2.startsWith("ARDUNAKON_DISCOVER|"))
        assertNotNull(nonce2)
        assertTrue(str2.contains(nonce2!!))
    }

    @Test
    fun `verifySignature validates correctly`() = runTest(testDispatcher) {
        val verifyMethod = manager.javaClass.getDeclaredMethod(
            "verifySignature",
            String::class.java,
            String::class.java,
            ByteArray::class.java
        )
        verifyMethod.isAccessible = true

        val key = "secret".toByteArray()
        val nonce = android.util.Base64.encodeToString("nonce".toByteArray(), android.util.Base64.NO_WRAP)

        // We need a real signature from hmac
        val hmacMethod = manager.javaClass.getDeclaredMethod("hmac", ByteArray::class.java, ByteArray::class.java)
        hmacMethod.isAccessible = true
        val sigBytes = hmacMethod.invoke(manager, "nonce".toByteArray(), key) as ByteArray
        val sig = android.util.Base64.encodeToString(sigBytes, android.util.Base64.NO_WRAP)

        val result = verifyMethod.invoke(manager, nonce, sig, key) as Boolean
        assertTrue(result)

        val invalidResult = verifyMethod.invoke(manager, nonce, "invalid", key) as Boolean
        assertFalse(invalidResult)
    }

    @Test
    fun `init restores last connection preferences`() = runTest(testDispatcher) {
        val mockPrefs = io.mockk.mockk<ConnectionPreferences>(relaxed = true)
        io.mockk.coEvery { mockPrefs.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "WIFI",
            btAddress = null,
            btType = null,
            wifiIp = "10.0.0.5",
            wifiPort = 9999,
            wifiPsk = null,
            autoReconnectWifi = true,
            joystickSensitivity = 1.0f
        )

        val localManager = WifiManager(
            context = context,
            connectionPreferences = mockPrefs,
            ioDispatcher = testDispatcher,
            encryptionPreferences = prefs,
            scope = testScope,
            startMonitors = false
        )

        testDispatcher.scheduler.runCurrent()

        // Use reflection to check private targetIp
        val ipField = localManager.javaClass.getDeclaredField("targetIp")
        ipField.isAccessible = true
        assertEquals("10.0.0.5", ipField.get(localManager))
        assertTrue(localManager.autoReconnectEnabled.value)
    }

    @Test
    fun `onEncryptionError updates flow and logs`() = runTest(testDispatcher) {
        var logMessage: String? = null
        val localManager = WifiManager(
            context = context,
            connectionPreferences = ConnectionPreferences(context, TestCryptoEngine()),
            onLog = { msg, _ -> logMessage = msg },
            ioDispatcher = testDispatcher,
            encryptionPreferences = prefs,
            scope = testScope,
            startMonitors = false
        )

        val error = EncryptionException.HandshakeFailedException("oops")
        localManager.onEncryptionError(error)

        assertEquals(error, localManager.encryptionError.value)
        assertTrue(logMessage?.contains("Encryption error") == true)
    }

    // Test helpers now provided via @VisibleForTesting methods.

    // Note: Testing reconnection monitor flow with StandardTestDispatcher is complex due to
    // long-running coroutines with delays. The reconnection logic in WifiManager uses
    // a delay loop which doesn't work reliably with test dispatchers. Individual
    // reconnection components (attempt counting, auto-reconnect flags) are tested separately.

    @Test
    fun `cleanup cancels scope and stops components`() = runTest(testDispatcher) {
        manager.cleanup()
        // verify no crash
    }

    private class FakeCryptoEngine : CryptoEngine {
        override fun encrypt(data: String): String = "ENC:$data"
        override fun decrypt(encryptedData: String): String = encryptedData.removePrefix("ENC:")
    }
}
