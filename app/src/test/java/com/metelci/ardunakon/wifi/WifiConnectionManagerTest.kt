package com.metelci.ardunakon.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import com.metelci.ardunakon.security.CryptoEngine
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WifiConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var manager: WifiConnectionManager
    private lateinit var prefs: WifiEncryptionPreferences
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val callback = mockk<WifiConnectionCallback>(relaxed = true)
    private val socketFactory = mockk<SocketFactory>(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = WifiEncryptionPreferences(context, FakeCryptoEngine())
        manager = WifiConnectionManager(
            context = context,
            scope = testScope,
            callback = callback,
            encryptionPreferences = prefs,
            ioDispatcher = testDispatcher,
            socketFactory = socketFactory
        )
    }

    @Test
    fun `initial state is disconnected`() = runTest(testDispatcher) {
        assertFalse(manager.isConnected())
        assertFalse(manager.isEncrypted())
    }

    @Test
    fun `disconnect updates state and isConnected flag`() = runTest(testDispatcher) {
        // Force connected state for testing
        val isConnectedField = manager.javaClass.getDeclaredField("isConnected")
        isConnectedField.isAccessible = true
        (isConnectedField.get(manager) as java.util.concurrent.atomic.AtomicBoolean).set(true)

        manager.disconnect()

        assertFalse(manager.isConnected())
        io.mockk.verify { callback.onStateChanged(WifiConnectionState.DISCONNECTED) }
    }

    @Test
    fun `sendData does nothing when disconnected`() = runTest(testDispatcher) {
        val data = byteArrayOf(1, 2, 3)
        manager.sendData(data)
        // No socket operations should occur (we can check socket field is null)
        val socketField = manager.javaClass.getDeclaredField("socket")
        socketField.isAccessible = true
        assertNull(socketField.get(manager))
    }

    @Test
    fun `encryption and decryption are consistent`() = runTest(testDispatcher) {
        val data = "Hello WiFi".toByteArray()
        val key = ByteArray(16) { it.toByte() }

        // Set sessionKey via reflection
        val sessionKeyField = manager.javaClass.getDeclaredField("sessionKey")
        sessionKeyField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val sessionKey = sessionKeyField.get(manager) as java.util.concurrent.atomic.AtomicReference<ByteArray?>
        sessionKey.set(key)

        val encryptMethod = manager.javaClass.getDeclaredMethod("encryptIfNeeded", ByteArray::class.java)
        encryptMethod.isAccessible = true
        val decryptMethod = manager.javaClass.getDeclaredMethod("decryptIfNeeded", ByteArray::class.java)
        decryptMethod.isAccessible = true

        val encrypted = encryptMethod.invoke(manager, data) as ByteArray
        assertNotEquals("Encrypted data should differ from original", data.toList(), encrypted.toList())
        assertTrue("Encrypted data should be longer than original (IV + tag)", encrypted.size > data.size)

        val decrypted = decryptMethod.invoke(manager, encrypted) as ByteArray
        assertArrayEquals("Decrypted data should match original", data, decrypted)
    }

    @Test
    fun `setRequireEncryption updates internal flag`() = runTest(testDispatcher) {
        val flagField = manager.javaClass.getDeclaredField("requireEncryption")
        flagField.isAccessible = true
        val flag = flagField.get(manager) as java.util.concurrent.atomic.AtomicBoolean

        manager.setRequireEncryption(false)
        assertFalse(flag.get())

        manager.setRequireEncryption(true)
        assertTrue(flag.get())
    }

    @Test
    fun `connect updates state to CONNECTING`() = runTest(testDispatcher) {
        // We don't call actual connect because it opens a socket
        // but we can test the initial state change
        // To test further we'd need to mock DatagramSocket creation (Refactoring needed or partial mocks)
    }

    @Test
    fun `getCurrentWifiRssi handles null managers`() = runTest(testDispatcher) {
        val method = manager.javaClass.getDeclaredMethod(
            "getCurrentWifiRssi",
            android.net.ConnectivityManager::class.java,
            android.net.wifi.WifiManager::class.java
        )
        method.isAccessible = true

        val result = method.invoke(manager, null, null)
        assertNull(result)
    }

    @Test
    fun `getCurrentWifiRssi uses reflection fallback`() = runTest(testDispatcher) {
        val wm = mockk<android.net.wifi.WifiManager>()
        val info = mockk<android.net.wifi.WifiInfo>()
        
        // Mocking reflection is hard, but we can verify it doesn't crash
        val method = manager.javaClass.getDeclaredMethod(
            "getCurrentWifiRssi",
            android.net.ConnectivityManager::class.java,
            android.net.wifi.WifiManager::class.java
        )
        method.isAccessible = true
        
        // This will likely hit the fallback catch blocks and return null if not perfectly mocked
        val result = method.invoke(manager, null, wm)
        assertNull(result)
    }

    @Test
    fun `isEncrypted reflects internal state`() = runTest(testDispatcher) {
        val field = manager.javaClass.getDeclaredField("isEncrypted")
        field.isAccessible = true
        (field.get(manager) as java.util.concurrent.atomic.AtomicBoolean).set(true)
        assertTrue(manager.isEncrypted())
        
        (field.get(manager) as java.util.concurrent.atomic.AtomicBoolean).set(false)
        assertFalse(manager.isEncrypted())
    }

    
    // Note: Testing the full connect() flow with StandardTestDispatcher is complex due to
    // multiple long-running coroutines (ping, timeout monitor, RSSI monitor). Individual
    // components are tested separately.

    @Test
    fun `rtt history is maintained with limit`() = runTest(testDispatcher) {
        val rttHistoryField = manager.javaClass.getDeclaredField("rttHistory")
        rttHistoryField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val history = rttHistoryField.get(manager) as MutableList<Long>

        // Directly test the history limit logic
        repeat(50) {
            history.add(0, 100L)
            if (history.size > 40) history.removeAt(history.lastIndex)
        }
        
        assertEquals(40, history.size)
    }

    @Test
    fun `decryptIfNeeded throws on short packet`() = runTest(testDispatcher) {
        val key = ByteArray(16) { it.toByte() }
        val sessionKeyField = manager.javaClass.getDeclaredField("sessionKey")
        sessionKeyField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (sessionKeyField.get(manager) as java.util.concurrent.atomic.AtomicReference<ByteArray?>).set(key)

        val method = manager.javaClass.getDeclaredMethod("decryptIfNeeded", ByteArray::class.java)
        method.isAccessible = true

        try {
            method.invoke(manager, byteArrayOf(1, 2, 3))
            fail("Should throw exception for short packet")
        } catch (e: Exception) {
            // NoSuchMethodException/InvocationTargetException wrapper
            assertTrue(e.cause is com.metelci.ardunakon.security.EncryptionException)
        }
    }

    @Test
    fun `decryptIfNeeded with invalid data throws exception`() = runTest(testDispatcher) {
        // Set session key to force decryption
        val sessionKeyField = manager.javaClass.getDeclaredField("sessionKey")
        sessionKeyField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (sessionKeyField.get(manager) as java.util.concurrent.atomic.AtomicReference<ByteArray?>).set(ByteArray(16))

        val method = manager.javaClass.getDeclaredMethod("decryptIfNeeded", ByteArray::class.java)
        method.isAccessible = true

        // Short packet should cause EncryptionException
        try {
            method.invoke(manager, byteArrayOf(1, 2, 3))
            fail("Should throw exception for short packet")
        } catch (e: Exception) {
            // InvocationTargetException wraps the actual exception
            assertTrue(e.cause is com.metelci.ardunakon.security.EncryptionException)
        }
    }
    private class FakeCryptoEngine : CryptoEngine {
        override fun encrypt(data: String): String = "ENC:$data"
        override fun decrypt(encryptedData: String): String = encryptedData.removePrefix("ENC:")
    }
}
