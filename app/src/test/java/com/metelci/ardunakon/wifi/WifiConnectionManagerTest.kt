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
    fun `disconnect without connection is safe`() = runTest(testDispatcher) {
        manager.disconnect()
        assertFalse(manager.isConnected())
    }

    @Test
    fun `encryptIfNeeded returns original data when no session key`() = runTest(testDispatcher) {
        val data = byteArrayOf(1, 2, 3)
        val encryptMethod = manager.javaClass.getDeclaredMethod("encryptIfNeeded", ByteArray::class.java)
        encryptMethod.isAccessible = true
        
        val result = encryptMethod.invoke(manager, data) as ByteArray
        assertArrayEquals(data, result)
    }

    @Test
    fun `decryptIfNeeded throws when packet too short`() = runTest(testDispatcher) {
        val data = byteArrayOf(1, 2, 3)
        
        // Set sessionKey via reflection
        val sessionKeyField = manager.javaClass.getDeclaredField("sessionKey")
        sessionKeyField.isAccessible = true
        val sessionKey = sessionKeyField.get(manager) as java.util.concurrent.atomic.AtomicReference<ByteArray?>
        sessionKey.set(ByteArray(32))

        val decryptMethod = manager.javaClass.getDeclaredMethod("decryptIfNeeded", ByteArray::class.java)
        decryptMethod.isAccessible = true
        
        try {
            decryptMethod.invoke(manager, data)
            fail("Should have thrown EncryptionException")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.targetException
            assertTrue("Expected EncryptionException, got ${cause?.javaClass?.name}", cause is EncryptionException)
        }
    }

    private class FakeCryptoEngine : CryptoEngine {
        override fun encrypt(data: String): String = "ENC:$data"
        override fun decrypt(encryptedData: String): String = encryptedData.removePrefix("ENC:")
    }
}
