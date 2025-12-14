package com.metelci.ardunakon.wifi

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.data.ConnectionPreferences
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.metelci.ardunakon.security.EncryptionException
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.data.WifiEncryptionPreferences

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WifiManagerTest {

    private lateinit var context: Context
    private lateinit var manager: WifiManager
    private lateinit var prefs: WifiEncryptionPreferences
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        prefs = WifiEncryptionPreferences(context, FakeCryptoEngine())
        manager = WifiManager(
            context = context,
            connectionPreferences = ConnectionPreferences(context, TestCryptoEngine()),
            ioDispatcher = mainDispatcher,
            encryptionPreferences = prefs
        )
    }

    @After
    fun tearDown() {
        manager.cleanup()
        Dispatchers.resetMain()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPrivateField(instance: Any, name: String): T {
        val field = WifiManager::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(instance) as T
    }

    @Test
    fun encryptIfNeededPassesThroughWithoutKey() {
        val payload = "hello".toByteArray()

        manager.setRequireEncryption(false)
        val encrypted = manager.encryptIfNeeded(payload)

        assertArrayEquals(payload, encrypted)
    }

    @Test
    fun encryptIfNeededEncryptsWhenSessionKeyPresent() {
        val key = ByteArray(16) { it.toByte() }
        manager.setSessionKey(key)
        val payload = "secret".toByteArray()

        val encrypted = manager.encryptIfNeeded(payload)

        assertFalse(encrypted.contentEquals(payload))

        val iv = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(ciphertext)
        assertArrayEquals(payload, decrypted)
    }

    @Test
    fun verifySignatureValidatesNonce() {
        val key = ByteArray(16) { (it + 1).toByte() }
        val nonceBytes = ByteArray(16) { 7 }
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(key, "HmacSHA256"))
        }
        val sigBytes = mac.doFinal(nonceBytes)
        val nonce = Base64.encodeToString(nonceBytes, Base64.NO_WRAP)
        val sig = Base64.encodeToString(sigBytes, Base64.NO_WRAP)

        assertTrue(manager.verifySignature(nonce, sig, key))
        assertFalse(manager.verifySignature(nonce, "invalid", key))
    }

    @Test
    fun updateRttMaintainsRecentHistory() {
        (1..45).forEach { manager.updateRtt(it.toLong()) }

        val history = manager.rttHistory.value
        assertEquals(40, history.size)
        assertEquals(45L, manager.rtt.value)
        assertEquals(45L, history.first())
    }

    @Test
    fun buildDiscoveryMessageWithoutKeyUsesLegacyPayload() {
        val (payload, nonce) = manager.buildDiscoveryMessage()

        assertNull(nonce)
        assertEquals("ARDUNAKON_DISCOVER", String(payload))
    }

    @Test
    fun buildDiscoveryMessageWithKeyIncludesNonceAndSignature() {
        val key = ByteArray(16) { (it + 2).toByte() }
        manager.setSessionKey(key)

        val (payload, nonce) = manager.buildDiscoveryMessage()

        assertNotNull(nonce)
        val message = String(payload)
        val parts = message.split("|")
        assertEquals("ARDUNAKON_DISCOVER", parts[0])
        assertEquals(3, parts.size)
        val generatedNonce = parts[1]
        val sig = parts[2]
        assertEquals(generatedNonce, nonce)
        assertTrue(manager.verifySignature(generatedNonce, sig, key))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun disconnectResetsStateAndMetrics() {
        // Set internal flags to simulate active connection
        getPrivateField<AtomicBoolean>(manager, "isConnected").set(true)
        getPrivateField<MutableStateFlow<Int>>(manager, "_rssi").value = -42
        getPrivateField<MutableStateFlow<Long>>(manager, "_rtt").value = 123L

        manager.disconnect()

        assertEquals(WifiConnectionState.DISCONNECTED, manager.connectionState.value)
        assertEquals(0, manager.rssi.value)
        assertEquals(0L, manager.rtt.value)
    }

    @Test
    fun addDeviceAvoidsDuplicatesAndPreservesTrustFlag() {
        manager.addDevice("First", "10.0.0.1", 8888, trusted = true)
        manager.addDevice("Second", "10.0.0.1", 8888, trusted = false) // duplicate IP ignored
        manager.addDevice("Third", "10.0.0.2", 8888, trusted = false)

        val devices = manager.scannedDevices.value
        assertEquals(2, devices.size)
        assertEquals("First", devices.first().name)
        assertTrue(devices.first().trusted)
        assertEquals("Third", devices.last().name)
    }

    @Test
    fun onPacketReceivedUpdatesRttWhenPingOutstanding() {
        val field = WifiManager::class.java.getDeclaredField("lastPingTime").apply { isAccessible = true }
        val now = System.currentTimeMillis() - 50
        field.setLong(manager, now)

        manager.onPacketReceived()

        assertTrue(manager.rtt.value in 1..5000)
        assertTrue(manager.rttHistory.value.first() >= 1)
    }

    @Test
    fun sendDataWritesToSocketWhenConnected() {
        val receiver = DatagramSocket(0)
        val port = receiver.localPort
        receiver.soTimeout = 2000

        WifiManager::class.java.getDeclaredField("targetIp").apply {
            isAccessible = true
            set(manager, "127.0.0.1")
        }
        WifiManager::class.java.getDeclaredField("targetPort").apply {
            isAccessible = true
            setInt(manager, port)
        }
        getPrivateField<AtomicBoolean>(manager, "isConnected").set(true)
        val senderSocket = DatagramSocket()
        WifiManager::class.java.getDeclaredField("socket").apply {
            isAccessible = true
            set(manager, senderSocket)
        }

        val payload = "ping".toByteArray()
        manager.setRequireEncryption(false)
        manager.sendData(payload)

        val buffer = ByteArray(32)
        val packet = DatagramPacket(buffer, buffer.size)
        receiver.receive(packet)

        assertEquals("ping", String(packet.data, 0, packet.length))

        receiver.close()
        senderSocket.close()
    }

    @Test
    fun timeoutMonitorDisconnectsAfterIdlePeriod() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val localPrefs = WifiEncryptionPreferences(context, FakeCryptoEngine())
        val localManager = WifiManager(
            context = context,
            connectionPreferences = ConnectionPreferences(context, TestCryptoEngine()),
            ioDispatcher = dispatcher,
            encryptionPreferences = localPrefs
        )
        getPrivateField<AtomicBoolean>(localManager, "isConnected").set(true)
        WifiManager::class.java.getDeclaredField("lastRxTime").apply {
            isAccessible = true
            setLong(localManager, System.currentTimeMillis() - 20_000)
        }

        WifiManager::class.java.getDeclaredMethod("startTimeoutMonitor").apply {
            isAccessible = true
            invoke(localManager)
        }

        scheduler.advanceTimeBy(11_000)
        scheduler.runCurrent()

        assertEquals(WifiConnectionState.DISCONNECTED, localManager.connectionState.value)
        localManager.cleanup()
    }

    @Test
    fun stopDiscoveryClearsScanningFlagAndListener() {
        WifiManager::class.java.getDeclaredField("isScanning").apply {
            isAccessible = true
            (get(manager) as AtomicBoolean).set(true)
        }
        val listener = object : android.net.nsd.NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onServiceFound(serviceInfo: android.net.nsd.NsdServiceInfo?) {}
            override fun onServiceLost(serviceInfo: android.net.nsd.NsdServiceInfo?) {}
        }
        WifiManager::class.java.getDeclaredField("discoveryListener").apply {
            isAccessible = true
            set(manager, listener)
        }

        WifiManager::class.java.getDeclaredMethod("stopDiscovery").apply {
            isAccessible = true
            invoke(manager)
        }

        val scanning = getPrivateField<AtomicBoolean>(manager, "isScanning")
        val listenerField = WifiManager::class.java.getDeclaredField("discoveryListener").apply { isAccessible = true }
        assertFalse(scanning.get())
        assertNull(listenerField.get(manager))
    }

    // ========== Encryption Enforcement Tests ==========

    @Test
    fun encryptIfNeededThrowsWhenRequiredAndNoKey() {
        manager.setRequireEncryption(true)
        val payload = "test".toByteArray()

        assertThrows(EncryptionException.NoSessionKeyException::class.java) {
            manager.encryptIfNeeded(payload)
        }
    }

    @Test
    fun encryptIfNeededSucceedsWhenRequiredAndKeyPresent() {
        val key = ByteArray(16) { it.toByte() }
        manager.setSessionKey(key)
        manager.setRequireEncryption(true)
        val payload = "secret".toByteArray()

        val encrypted = manager.encryptIfNeeded(payload)

        assertFalse("Should encrypt when key present", encrypted.contentEquals(payload))
        assertTrue("Encrypted data should include IV (12 bytes) + ciphertext", encrypted.size > payload.size)
    }

    @Test
    fun setRequireEncryptionUpdatesState() {
        assertFalse("Default should be false", manager.isEncryptionRequired())

        manager.setRequireEncryption(true)
        assertTrue(manager.isEncryptionRequired())

        manager.setRequireEncryption(false)
        assertFalse(manager.isEncryptionRequired())
    }

    @Test
    fun clearEncryptionErrorResetsState() {
        // Manually set an error
        val errorField = WifiManager::class.java.getDeclaredField("_encryptionError").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val errorFlow = errorField.get(manager) as MutableStateFlow<EncryptionException?>
        errorFlow.value = EncryptionException.NoSessionKeyException("test")

        assertNotNull(manager.encryptionError.value)

        manager.clearEncryptionError()

        assertNull(manager.encryptionError.value)
    }
    @Test
    fun stopDiscoveryClearsRateLimiter() {
        val limiterField = WifiManager::class.java.getDeclaredField("discoveryRateLimiter").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val limiter = limiterField.get(manager) as MutableMap<String, Long>
        limiter["1.2.3.4"] = 123456789L

        WifiManager::class.java.getDeclaredMethod("stopDiscovery").apply {
            isAccessible = true
            invoke(manager)
        }

        assertTrue("Rate limiter map should be empty after stopping discovery", limiter.isEmpty())
    }

    private class FakeCryptoEngine : CryptoEngine {
        override fun encrypt(data: String): String = "ENCRYPTED:$data"
        override fun decrypt(encryptedData: String): String = encryptedData.removePrefix("ENCRYPTED:")
    }
}
