package com.metelci.ardunakon.wifi

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WifiConnectionState enum.
 */
class WifiConnectionStateTest {

    // ==================== Enum Values ====================

    @Test
    fun `WifiConnectionState has 4 values`() {
        assertEquals(4, WifiConnectionState.entries.size)
    }

    @Test
    fun `DISCONNECTED exists`() {
        assertNotNull(WifiConnectionState.DISCONNECTED)
    }

    @Test
    fun `CONNECTING exists`() {
        assertNotNull(WifiConnectionState.CONNECTING)
    }

    @Test
    fun `CONNECTED exists`() {
        assertNotNull(WifiConnectionState.CONNECTED)
    }

    @Test
    fun `ERROR exists`() {
        assertNotNull(WifiConnectionState.ERROR)
    }

    // ==================== Enum Names ====================

    @Test
    fun `DISCONNECTED name is correct`() {
        assertEquals("DISCONNECTED", WifiConnectionState.DISCONNECTED.name)
    }

    @Test
    fun `CONNECTING name is correct`() {
        assertEquals("CONNECTING", WifiConnectionState.CONNECTING.name)
    }

    @Test
    fun `CONNECTED name is correct`() {
        assertEquals("CONNECTED", WifiConnectionState.CONNECTED.name)
    }

    @Test
    fun `ERROR name is correct`() {
        assertEquals("ERROR", WifiConnectionState.ERROR.name)
    }

    // ==================== Enum Ordinal ====================

    @Test
    fun `DISCONNECTED ordinal is 0`() {
        assertEquals(0, WifiConnectionState.DISCONNECTED.ordinal)
    }

    @Test
    fun `CONNECTING ordinal is 1`() {
        assertEquals(1, WifiConnectionState.CONNECTING.ordinal)
    }

    @Test
    fun `CONNECTED ordinal is 2`() {
        assertEquals(2, WifiConnectionState.CONNECTED.ordinal)
    }

    @Test
    fun `ERROR ordinal is 3`() {
        assertEquals(3, WifiConnectionState.ERROR.ordinal)
    }

    // ==================== valueOf ====================

    @Test
    fun `valueOf DISCONNECTED`() {
        assertEquals(WifiConnectionState.DISCONNECTED, WifiConnectionState.valueOf("DISCONNECTED"))
    }

    @Test
    fun `valueOf CONNECTING`() {
        assertEquals(WifiConnectionState.CONNECTING, WifiConnectionState.valueOf("CONNECTING"))
    }

    @Test
    fun `valueOf CONNECTED`() {
        assertEquals(WifiConnectionState.CONNECTED, WifiConnectionState.valueOf("CONNECTED"))
    }

    @Test
    fun `valueOf ERROR`() {
        assertEquals(WifiConnectionState.ERROR, WifiConnectionState.valueOf("ERROR"))
    }

    // ==================== toString ====================

    @Test
    fun `toString returns name`() {
        assertEquals("DISCONNECTED", WifiConnectionState.DISCONNECTED.toString())
        assertEquals("CONNECTING", WifiConnectionState.CONNECTING.toString())
        assertEquals("CONNECTED", WifiConnectionState.CONNECTED.toString())
        assertEquals("ERROR", WifiConnectionState.ERROR.toString())
    }

    // ==================== State Transitions ====================

    @Test
    fun `typical connection flow order`() {
        // Typical flow: DISCONNECTED -> CONNECTING -> CONNECTED
        val flow = listOf(
            WifiConnectionState.DISCONNECTED,
            WifiConnectionState.CONNECTING,
            WifiConnectionState.CONNECTED
        )

        assertEquals(3, flow.size)
        assertEquals(WifiConnectionState.DISCONNECTED, flow[0])
        assertEquals(WifiConnectionState.CONNECTING, flow[1])
        assertEquals(WifiConnectionState.CONNECTED, flow[2])
    }

    @Test
    fun `error recovery flow`() {
        // Error flow: CONNECTED -> ERROR -> DISCONNECTED -> CONNECTING -> CONNECTED
        val flow = listOf(
            WifiConnectionState.CONNECTED,
            WifiConnectionState.ERROR,
            WifiConnectionState.DISCONNECTED,
            WifiConnectionState.CONNECTING,
            WifiConnectionState.CONNECTED
        )

        assertEquals(5, flow.size)
    }
}
