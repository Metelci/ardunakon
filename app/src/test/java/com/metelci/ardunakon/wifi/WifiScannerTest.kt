package com.metelci.ardunakon.wifi

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WifiScannerTest {

    private lateinit var context: Context
    private lateinit var scanner: WifiScanner
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val callback = mockk<WifiConnectionCallback>(relaxed = true)
    private val socketFactory = mockk<SocketFactory>(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Grant permissions
        val app = context.applicationContext as android.app.Application
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_WIFI_STATE)
        shadowOf(app).grantPermissions(Manifest.permission.NEARBY_WIFI_DEVICES)

        scanner = WifiScanner(
            context = context,
            scope = testScope,
            onLog = { println("WIFI_SCANNER_LOG: $it") },
            buildDiscoveryMessage = { "DISCOVER".toByteArray() to null },
            verifySignature = { _, _, _ -> true },
            getSessionKey = { null },
            getDiscoveryNonce = { null },
            socketFactory = socketFactory
        )
    }

    @org.junit.After
    fun tearDown() {
        scanner.stopDiscovery()
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `initial devices list contains AP mode device`() = runTest(testDispatcher) {
        // startDiscovery adds AP mode device immediately
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        val devices = scanner.scannedDevices.value
        assertTrue(
            "Should contain AP mode device in $devices",
            devices.any {
                it.name.contains("AP mode") && it.ip == "192.168.4.1"
            }
        )
    }


    // Note: Testing isScanning state with StandardTestDispatcher is problematic because
    // the discovery loop runs synchronously when runCurrent() is called and completes
    // immediately, setting isScanning back to false. The important discovery functionality
    // (UDP responses, signatures, trusted devices) is tested in other tests.

    @Test
    fun `startDiscovery fails if ACCESS_WIFI_STATE is missing`() = runTest(testDispatcher) {
        val app = context.applicationContext as android.app.Application
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_WIFI_STATE)

        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()

        assertFalse("Should not be scanning without permission", scanner.isScanning.value)
    }


    @Test
    fun `addDevice avoids duplicates and merges info`() = runTest(testDispatcher) {
        val addDeviceMethod = scanner.javaClass.getDeclaredMethod(
            "addDevice",
            String::class.java,
            String::class.java,
            Int::class.java,
            Boolean::class.java
        )
        addDeviceMethod.isAccessible = true

        // Use name that will be overwritten
        addDeviceMethod.invoke(scanner, "Arduino R4 WiFi (AP mode)", "192.168.1.10", 8888, false)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, scanner.scannedDevices.value.size)

        addDeviceMethod.invoke(scanner, "Better Name", "192.168.1.10", 8888, true)
        testDispatcher.scheduler.runCurrent()
        val devices = scanner.scannedDevices.value
        assertEquals(1, devices.size)
        assertEquals("Better Name", devices[0].name)
        assertTrue(devices[0].trusted)
    }

    @Test
    fun `stopDiscovery releases multicast lock`() = runTest(testDispatcher) {
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()

        val lockField = scanner.javaClass.getDeclaredField("multicastLock")
        lockField.isAccessible = true
        val lock = lockField.get(scanner) as? android.net.wifi.WifiManager.MulticastLock

        // if lock is null, maybe startDiscovery returned early due to missing permission in shadow
        // but we grant it in setUp.
        
        scanner.stopDiscovery()
        testDispatcher.scheduler.runCurrent()

        assertTrue("Lock should be null or released", lock == null || !lock.isHeld)
    }

    @Test
    fun `getBroadcastAddress handles exceptions`() = runTest(testDispatcher) {
        val method = scanner.javaClass.getDeclaredMethod("getBroadcastAddress")
        method.isAccessible = true
        
        // This will likely return null in Robolectric unless connectivity is mocked
        val result = method.invoke(scanner)
        // We just want to ensure it doesn't crash
    }

    @Test
    fun `UDP discovery loop handles timeout and continues`() = runTest(testDispatcher) {
        val mockSocket = mockk<java.net.DatagramSocket>(relaxed = true)
        io.mockk.every { socketFactory.createDiscoverySocket() } returns mockSocket
        
        // Throws timeout then succeeds
        io.mockk.every { mockSocket.receive(any()) } throws java.net.SocketTimeoutException() andThen {
            val responseData = "ARDUNAKON_DEVICE:Delayed|192.168.1.51|8888".toByteArray()
            val packet = it.invocation.args[0] as java.net.DatagramPacket
            System.arraycopy(responseData, 0, packet.data, 0, responseData.size)
            packet.length = responseData.size
            packet.address = java.net.InetAddress.getByName("192.168.1.51")
        } andThenThrows java.net.SocketException("Closed")
        
        scanner.startDiscovery()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        val devices = scanner.scannedDevices.value
        assertTrue("Should discover device after a timeout", devices.any { it.name == "Delayed" })
    }

    @Test
    fun `UDP discovery loop ignores malformed responses`() = runTest(testDispatcher) {
        val mockSocket = mockk<java.net.DatagramSocket>(relaxed = true)
        io.mockk.every { socketFactory.createDiscoverySocket() } returns mockSocket
        
        val responseData = "INVALID_PREFIX:Device|1.2.3.4|8888".toByteArray()
        io.mockk.every { mockSocket.receive(any()) } answers {
            val packet = firstArg<java.net.DatagramPacket>()
            System.arraycopy(responseData, 0, packet.data, 0, responseData.size)
            packet.length = responseData.size
            packet.address = java.net.InetAddress.getByName("1.2.3.4")
        } andThenThrows java.net.SocketException("Done")
        
        scanner.startDiscovery()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Should only contain the default AP mode device
        assertEquals(1, scanner.scannedDevices.value.size)
    }

    @Test
    fun `UDP discovery loop handles trusted devices with signature`() = runTest(testDispatcher) {
        val mockSocket = mockk<java.net.DatagramSocket>(relaxed = true)
        io.mockk.every { socketFactory.createDiscoverySocket() } returns mockSocket
        
        // Mock scanner with key and nonce
        val scannerWithKey = WifiScanner(
            context = context,
            scope = testScope,
            onLog = {},
            buildDiscoveryMessage = { "DISCOVER".toByteArray() to "NONCE" },
            verifySignature = { nonce, _, _ -> nonce == "NONCE" },
            getSessionKey = { byteArrayOf(1) },
            getDiscoveryNonce = { "NONCE" },
            socketFactory = socketFactory
        )

        val responseData = "ARDUNAKON_DEVICE:SecureDevice|NONCE|SIG".toByteArray()
        io.mockk.every { mockSocket.receive(any()) } answers {
            val packet = firstArg<java.net.DatagramPacket>()
            System.arraycopy(responseData, 0, packet.data, 0, responseData.size)
            packet.length = responseData.size
            packet.address = java.net.InetAddress.getByName("192.168.1.100")
        } andThenThrows java.net.SocketException("Closed")
        
        scannerWithKey.startDiscovery()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        val devices = scannerWithKey.scannedDevices.value
        val device = devices.find { it.name == "SecureDevice" }
        assertNotNull(device)
        assertTrue("Device should be trusted", device?.trusted ?: false)
    }

    @Test
    fun `UDP discovery loop marks as untrusted if signature fails`() = runTest(testDispatcher) {
        val mockSocket = mockk<java.net.DatagramSocket>(relaxed = true)
        io.mockk.every { socketFactory.createDiscoverySocket() } returns mockSocket
        
        val scannerWithKey = WifiScanner(
            context = context,
            scope = testScope,
            onLog = {},
            buildDiscoveryMessage = { "DISCOVER".toByteArray() to "NONCE" },
            verifySignature = { _, _, _ -> false }, // Fail signature
            getSessionKey = { byteArrayOf(1) },
            getDiscoveryNonce = { "NONCE" },
            socketFactory = socketFactory
        )

        val responseData = "ARDUNAKON_DEVICE:InsecureDevice|WRONG_NONCE|SIG".toByteArray()
        io.mockk.every { mockSocket.receive(any()) } answers {
            val packet = firstArg<java.net.DatagramPacket>()
            System.arraycopy(responseData, 0, packet.data, 0, responseData.size)
            packet.length = responseData.size
            packet.address = java.net.InetAddress.getByName("192.168.1.101")
        } andThenThrows java.net.SocketException("Closed")
        
        scannerWithKey.startDiscovery()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        val devices = scannerWithKey.scannedDevices.value
        val device = devices.find { it.name == "InsecureDevice" }
        assertNotNull(device)
        assertFalse("Device should not be trusted", device?.trusted ?: true)
    }
}
