package com.metelci.ardunakon.ui.screens.control

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ConnectionMode enum.
 */
class ConnectionModeTest {

    // ==================== Enum Values ====================

    @Test
    fun `ConnectionMode has 2 values`() {
        assertEquals(2, ConnectionMode.entries.size)
    }

    @Test
    fun `BLUETOOTH exists`() {
        assertNotNull(ConnectionMode.BLUETOOTH)
    }

    @Test
    fun `WIFI exists`() {
        assertNotNull(ConnectionMode.WIFI)
    }

    // ==================== Enum Names ====================

    @Test
    fun `BLUETOOTH name is correct`() {
        assertEquals("BLUETOOTH", ConnectionMode.BLUETOOTH.name)
    }

    @Test
    fun `WIFI name is correct`() {
        assertEquals("WIFI", ConnectionMode.WIFI.name)
    }

    // ==================== Enum Ordinal ====================

    @Test
    fun `BLUETOOTH ordinal is 0`() {
        assertEquals(0, ConnectionMode.BLUETOOTH.ordinal)
    }

    @Test
    fun `WIFI ordinal is 1`() {
        assertEquals(1, ConnectionMode.WIFI.ordinal)
    }

    // ==================== valueOf ====================

    @Test
    fun `valueOf BLUETOOTH`() {
        assertEquals(ConnectionMode.BLUETOOTH, ConnectionMode.valueOf("BLUETOOTH"))
    }

    @Test
    fun `valueOf WIFI`() {
        assertEquals(ConnectionMode.WIFI, ConnectionMode.valueOf("WIFI"))
    }

    // ==================== toString ====================

    @Test
    fun `toString returns name`() {
        assertEquals("BLUETOOTH", ConnectionMode.BLUETOOTH.toString())
        assertEquals("WIFI", ConnectionMode.WIFI.toString())
    }

    // ==================== Use Cases ====================

    @Test
    fun `connection mode can be toggled`() {
        var mode = ConnectionMode.BLUETOOTH

        mode = if (mode == ConnectionMode.BLUETOOTH) ConnectionMode.WIFI else ConnectionMode.BLUETOOTH
        assertEquals(ConnectionMode.WIFI, mode)

        mode = if (mode == ConnectionMode.BLUETOOTH) ConnectionMode.WIFI else ConnectionMode.BLUETOOTH
        assertEquals(ConnectionMode.BLUETOOTH, mode)
    }

    @Test
    fun `connection mode in when expression`() {
        val modes = listOf(ConnectionMode.BLUETOOTH, ConnectionMode.WIFI)

        for (mode in modes) {
            val label = when (mode) {
                ConnectionMode.BLUETOOTH -> "BT"
                ConnectionMode.WIFI -> "WiFi"
            }
            assertTrue(label.isNotEmpty())
        }
    }
}
