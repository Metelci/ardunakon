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

    private class FakeCryptoEngine : CryptoEngine {
        override fun encrypt(data: String): String = "ENC:$data"
        override fun decrypt(encryptedData: String): String = encryptedData.removePrefix("ENC:")
    }
}
