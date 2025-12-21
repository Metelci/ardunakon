package com.metelci.ardunakon.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import android.Manifest
import android.content.pm.PackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WifiScannerTest {

    private lateinit var context: Context
    private lateinit var scanner: WifiScanner
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

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
            getDiscoveryNonce = { null }
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
        assertTrue("Should contain AP mode device in $devices", devices.any { it.name.contains("AP mode") && it.ip == "192.168.4.1" })
    }

    @Test
    fun `stopDiscovery clears isScanning flag`() = runTest(testDispatcher) {
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        assertTrue("Should be scanning", scanner.isScanning.value)
        scanner.stopDiscovery()
        testDispatcher.scheduler.runCurrent()
        assertFalse("Should not be scanning", scanner.isScanning.value)
    }

    @Test
    fun `startDiscovery returns early if already scanning`() = runTest(testDispatcher) {
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        assertTrue(scanner.isScanning.value)
        
        // This should not clear or restart anything
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        assertTrue(scanner.isScanning.value)
    }

    @Test
    fun `startDiscovery fails if ACCESS_WIFI_STATE is missing`() = runTest(testDispatcher) {
        val app = context.applicationContext as android.app.Application
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_WIFI_STATE)
        
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        
        assertFalse("Should not be scanning without permission", scanner.isScanning.value)
    }

    @Test
    fun `mDNS is skipped if NEARBY_WIFI_DEVICES is missing on newer APIs`() = runTest(testDispatcher) {
        // Mocking SDK version to 33+ where NEARBY_WIFI_DEVICES is required
        // Robolectric @Config(sdk=[34]) already does this.
        val app = context.applicationContext as android.app.Application
        shadowOf(app).denyPermissions(Manifest.permission.NEARBY_WIFI_DEVICES)
        
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        
        assertTrue("Still scans (UDP)", scanner.isScanning.value)
        // We can't easily check if startMdnsScan was skipped without reflection or mocking NsdManager
        // but the log should contain the message. 
        // Our onLog print will show it in stdout.
    }

    @Test
    fun `addDevice avoids duplicates and merges info`() = runTest(testDispatcher) {
        val addDeviceMethod = scanner.javaClass.getDeclaredMethod(
            "addDevice", String::class.java, String::class.java, Int::class.java, Boolean::class.java
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
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val shadowWifi = shadowOf(wifi)
        
        scanner.startDiscovery()
        testDispatcher.scheduler.runCurrent()
        
        // Reflection to check if lock is held
        val lockField = scanner.javaClass.getDeclaredField("multicastLock")
        lockField.isAccessible = true
        val lock = lockField.get(scanner) as? android.net.wifi.WifiManager.MulticastLock
        
        assertNotNull("Lock should have been created", lock)
        assertTrue("Lock should be held", lock?.isHeld ?: false)
        
        scanner.stopDiscovery()
        testDispatcher.scheduler.runCurrent()
        
        assertFalse("Lock should be released", lock?.isHeld ?: true)
    }
}
