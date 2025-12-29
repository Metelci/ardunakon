package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BluetoothModels data classes.
 */
class BluetoothModelsTest {

    // ==================== ConnectionState Enum ====================

    @Test
    fun `ConnectionState has 5 values`() {
        assertEquals(5, ConnectionState.entries.size)
    }

    @Test
    fun `ConnectionState values exist`() {
        assertNotNull(ConnectionState.DISCONNECTED)
        assertNotNull(ConnectionState.CONNECTING)
        assertNotNull(ConnectionState.CONNECTED)
        assertNotNull(ConnectionState.ERROR)
        assertNotNull(ConnectionState.RECONNECTING)
    }

    @Test
    fun `ConnectionState names are correct`() {
        assertEquals("DISCONNECTED", ConnectionState.DISCONNECTED.name)
        assertEquals("CONNECTING", ConnectionState.CONNECTING.name)
        assertEquals("CONNECTED", ConnectionState.CONNECTED.name)
        assertEquals("ERROR", ConnectionState.ERROR.name)
        assertEquals("RECONNECTING", ConnectionState.RECONNECTING.name)
    }

    // ==================== DeviceType Enum ====================

    @Test
    fun `DeviceType has 2 values`() {
        assertEquals(2, DeviceType.entries.size)
    }

    @Test
    fun `DeviceType values exist`() {
        assertNotNull(DeviceType.CLASSIC)
        assertNotNull(DeviceType.LE)
    }

    // ==================== BluetoothDeviceModel ====================

    @Test
    fun `BluetoothDeviceModel creation with minimal params`() {
        val device = BluetoothDeviceModel(
            name = "Test Device",
            address = "AA:BB:CC:DD:EE:FF",
            type = DeviceType.LE
        )

        assertEquals("Test Device", device.name)
        assertEquals("AA:BB:CC:DD:EE:FF", device.address)
        assertEquals(DeviceType.LE, device.type)
        assertEquals(0, device.rssi)
    }

    @Test
    fun `BluetoothDeviceModel creation with rssi`() {
        val device = BluetoothDeviceModel(
            name = "Test Device",
            address = "AA:BB:CC:DD:EE:FF",
            type = DeviceType.CLASSIC,
            rssi = -65
        )

        assertEquals(-65, device.rssi)
    }

    @Test
    fun `BluetoothDeviceModel equality`() {
        val device1 = BluetoothDeviceModel("Test", "AA:BB:CC:DD:EE:FF", DeviceType.LE, -50)
        val device2 = BluetoothDeviceModel("Test", "AA:BB:CC:DD:EE:FF", DeviceType.LE, -50)

        assertEquals(device1, device2)
    }

    @Test
    fun `BluetoothDeviceModel inequality by address`() {
        val device1 = BluetoothDeviceModel("Test", "AA:BB:CC:DD:EE:FF", DeviceType.LE)
        val device2 = BluetoothDeviceModel("Test", "11:22:33:44:55:66", DeviceType.LE)

        assertNotEquals(device1, device2)
    }

    @Test
    fun `BluetoothDeviceModel inequality by type`() {
        val device1 = BluetoothDeviceModel("Test", "AA:BB:CC:DD:EE:FF", DeviceType.LE)
        val device2 = BluetoothDeviceModel("Test", "AA:BB:CC:DD:EE:FF", DeviceType.CLASSIC)

        assertNotEquals(device1, device2)
    }

    @Test
    fun `BluetoothDeviceModel copy`() {
        val original = BluetoothDeviceModel("Test", "AA:BB:CC:DD:EE:FF", DeviceType.LE, -50)
        val copy = original.copy(rssi = -30)

        assertEquals(-50, original.rssi)
        assertEquals(-30, copy.rssi)
        assertEquals(original.address, copy.address)
    }

    // ==================== ConnectionHealth ====================

    @Test
    fun `ConnectionHealth default values`() {
        val health = ConnectionHealth()

        assertEquals(0L, health.lastPacketAt)
        assertEquals(0, health.rssiFailureCount)
        assertEquals(0, health.lastHeartbeatSeq)
        assertEquals(0L, health.lastHeartbeatAt)
        assertEquals(0L, health.lastRttMs)
    }

    @Test
    fun `ConnectionHealth with values`() {
        val health = ConnectionHealth(
            lastPacketAt = 12345L,
            rssiFailureCount = 3,
            lastHeartbeatSeq = 100,
            lastHeartbeatAt = 12340L,
            lastRttMs = 25L
        )

        assertEquals(12345L, health.lastPacketAt)
        assertEquals(3, health.rssiFailureCount)
        assertEquals(100, health.lastHeartbeatSeq)
        assertEquals(12340L, health.lastHeartbeatAt)
        assertEquals(25L, health.lastRttMs)
    }

    @Test
    fun `ConnectionHealth equality`() {
        val health1 = ConnectionHealth(100L, 1, 50, 90L, 10L)
        val health2 = ConnectionHealth(100L, 1, 50, 90L, 10L)

        assertEquals(health1, health2)
    }

    @Test
    fun `ConnectionHealth copy`() {
        val original = ConnectionHealth(100L, 1, 50, 90L, 10L)
        val copy = original.copy(lastRttMs = 20L)

        assertEquals(10L, original.lastRttMs)
        assertEquals(20L, copy.lastRttMs)
    }
}
