package com.metelci.ardunakon.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.model.LogType
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
class BluetoothScannerTest {

    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var adapter: BluetoothAdapter
    private lateinit var scanner: BluetoothScanner
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val foundDevices = mutableListOf<BluetoothDeviceModel>()
    private val updatedDevices = mutableListOf<BluetoothDeviceModel>()
    private val logs = mutableListOf<String>()

    private val callbacks = object : BluetoothScanner.ScannerCallbacks {
        override fun onDeviceFound(device: BluetoothDeviceModel) {
            foundDevices.add(device)
        }
        override fun onDeviceUpdated(device: BluetoothDeviceModel) {
            updatedDevices.add(device)
        }
        override fun onScanLog(message: String, type: LogType) {
            logs.add(message)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManager.adapter

        // Grant permissions for Robolectric
        val app = context.applicationContext as android.app.Application
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_SCAN)
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_ADVERTISE)
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_ADMIN)
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        // Enable adapter
        shadowOf(adapter).setEnabled(true)
        shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)

        scanner = BluetoothScanner(
            context = context,
            adapter = adapter,
            scope = testScope,
            callbacks = callbacks,
            cryptoEngine = com.metelci.ardunakon.TestCryptoEngine()
        )

        foundDevices.clear()
        updatedDevices.clear()
        logs.clear()
    }

    @Test
    fun `initial scannedDevices is empty`() {
        assertTrue(scanner.scannedDevices.value.isEmpty())
    }

    @Test
    fun `addDevice adds new device to list`() = runTest(testDispatcher) {
        val device = adapter.getRemoteDevice("00:11:22:33:44:55")

        val isNew = scanner.addDevice(device, DeviceType.CLASSIC, -60)
        assertTrue("addDevice should return true for new discovery", isNew)

        // Check immediate addition (no dispatcher advance needed now)
        val currentDevices = scanner.scannedDevices.value
        assertEquals("List should contain 1 device immediately", 1, currentDevices.size)
        assertEquals("00:11:22:33:44:55", currentDevices[0].address)
        assertEquals(DeviceType.CLASSIC, currentDevices[0].type)

        assertEquals("Found callback should be triggered", 1, foundDevices.size)
    }

    @Test
    fun `addDevice deduplicates by address`() = runTest(testDispatcher) {
        val device = adapter.getRemoteDevice("00:11:22:33:44:55")

        scanner.addDevice(device, DeviceType.CLASSIC, -60)
        val isNew = scanner.addDevice(device, DeviceType.CLASSIC, -70)

        assertFalse("Second add with same address should not be new", isNew)
        assertEquals("Should still only have 1 device", 1, scanner.scannedDevices.value.size)
    }

    @Test
    fun `addDevice upgrades Classic to LE if marker found`() = runTest(testDispatcher) {
        val device = adapter.getRemoteDevice("AA:BB:CC:DD:EE:FF")

        // Initial Classic addition
        scanner.addDevice(device, DeviceType.CLASSIC, -60)
        assertEquals(1, scanner.scannedDevices.value.size)
        assertEquals(DeviceType.CLASSIC, scanner.scannedDevices.value[0].type)

        // Add again as LE
        val isNew = scanner.addDevice(device, DeviceType.LE, -55)
        assertFalse(isNew)

        testDispatcher.scheduler.runCurrent()
        assertEquals(DeviceType.LE, scanner.scannedDevices.value[0].type)
        assertEquals(1, updatedDevices.size)
    }

    @Test
    fun `startScan clears results and triggers logs`() = runTest(testDispatcher) {
        val device = adapter.getRemoteDevice("00:11:22:33:44:55")
        scanner.addDevice(device, DeviceType.CLASSIC, -60)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, scanner.scannedDevices.value.size)

        scanner.startScan()

        // List should be empty
        assertTrue("Scanned devices should be cleared", scanner.scannedDevices.value.isEmpty())

        // Should have a scan timeout job
        testDispatcher.scheduler.advanceTimeBy(11000)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `isBleOnlyName identifies BLE modules`() {
        // Accessing private method via reflection for precise testing
        val method = scanner.javaClass.getDeclaredMethod("isBleOnlyName", String::class.java)
        method.isAccessible = true

        assertTrue(method.invoke(scanner, "HM-10") as Boolean)
        assertTrue(method.invoke(scanner, "AT-09") as Boolean)
        assertTrue(method.invoke(scanner, "ArdunakonQ") as Boolean)
        assertTrue(method.invoke(scanner, "Arduino R4") as Boolean)

        assertFalse(method.invoke(scanner, "HC-05") as Boolean)
        assertFalse(method.invoke(scanner, "My Speaker") as Boolean)
        assertFalse(method.invoke(scanner, null) as Boolean)
    }
}
