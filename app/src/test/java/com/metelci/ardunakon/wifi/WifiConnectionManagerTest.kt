package com.metelci.ardunakon.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.EncryptionException
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val callback = mockk<WifiConnectionCallback>(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = WifiEncryptionPreferences(context, FakeCryptoEngine())
        manager = WifiConnectionManager(
            context = context,
            scope = testScope,
            callback = callback,
            encryptionPreferences = prefs,
            ioDispatcher = testDispatcher
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

    private class FakeCryptoEngine : CryptoEngine {
        override fun encrypt(data: String): String = "ENC:$data"
        override fun decrypt(encryptedData: String): String = encryptedData.removePrefix("ENC:")
    }
}
