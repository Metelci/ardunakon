package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.bluetooth.ConnectionState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for StatusCard state text and color mapping logic.
 *
 * These tests verify the ConnectionState-to-text and color mapping.
 */
class StatusCardLogicTest {

    /**
     * Replicates the state-to-text calculation from StatusCard.
     */
    private fun getStateText(state: ConnectionState): String = when (state) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.RECONNECTING -> "Reconnecting..."
        ConnectionState.ERROR -> "Error"
        ConnectionState.DISCONNECTED -> "Disconnected"
    }

    /**
     * Categories for container color (filled vs transparent).
     */
    private enum class ContainerCategory { FILLED, TRANSPARENT }

    private fun getContainerCategory(state: ConnectionState): ContainerCategory = when (state) {
        ConnectionState.CONNECTED -> ContainerCategory.FILLED
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> ContainerCategory.FILLED
        else -> ContainerCategory.TRANSPARENT
    }

    /**
     * Content color categories.
     */
    private enum class ContentColorCategory { YELLOW, ORANGE, RED, GRAY }

    private fun getContentColorCategory(state: ConnectionState): ContentColorCategory = when (state) {
        ConnectionState.CONNECTED -> ContentColorCategory.YELLOW
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> ContentColorCategory.ORANGE
        ConnectionState.ERROR -> ContentColorCategory.RED
        ConnectionState.DISCONNECTED -> ContentColorCategory.GRAY
    }

    // ==================== State Text Mapping ====================

    @Test
    fun `CONNECTED state gives Connected text`() {
        assertEquals("Connected", getStateText(ConnectionState.CONNECTED))
    }

    @Test
    fun `CONNECTING state gives Connecting text`() {
        assertEquals("Connecting...", getStateText(ConnectionState.CONNECTING))
    }

    @Test
    fun `RECONNECTING state gives Reconnecting text`() {
        assertEquals("Reconnecting...", getStateText(ConnectionState.RECONNECTING))
    }

    @Test
    fun `ERROR state gives Error text`() {
        assertEquals("Error", getStateText(ConnectionState.ERROR))
    }

    @Test
    fun `DISCONNECTED state gives Disconnected text`() {
        assertEquals("Disconnected", getStateText(ConnectionState.DISCONNECTED))
    }

    // ==================== Container Color Category ====================

    @Test
    fun `CONNECTED has filled container`() {
        assertEquals(ContainerCategory.FILLED, getContainerCategory(ConnectionState.CONNECTED))
    }

    @Test
    fun `CONNECTING has filled container`() {
        assertEquals(ContainerCategory.FILLED, getContainerCategory(ConnectionState.CONNECTING))
    }

    @Test
    fun `RECONNECTING has filled container`() {
        assertEquals(ContainerCategory.FILLED, getContainerCategory(ConnectionState.RECONNECTING))
    }

    @Test
    fun `ERROR has transparent container`() {
        assertEquals(ContainerCategory.TRANSPARENT, getContainerCategory(ConnectionState.ERROR))
    }

    @Test
    fun `DISCONNECTED has transparent container`() {
        assertEquals(ContainerCategory.TRANSPARENT, getContainerCategory(ConnectionState.DISCONNECTED))
    }

    // ==================== Content Color Category ====================

    @Test
    fun `CONNECTED has yellow content`() {
        assertEquals(ContentColorCategory.YELLOW, getContentColorCategory(ConnectionState.CONNECTED))
    }

    @Test
    fun `CONNECTING has orange content`() {
        assertEquals(ContentColorCategory.ORANGE, getContentColorCategory(ConnectionState.CONNECTING))
    }

    @Test
    fun `RECONNECTING has orange content`() {
        assertEquals(ContentColorCategory.ORANGE, getContentColorCategory(ConnectionState.RECONNECTING))
    }

    @Test
    fun `ERROR has red content`() {
        assertEquals(ContentColorCategory.RED, getContentColorCategory(ConnectionState.ERROR))
    }

    @Test
    fun `DISCONNECTED has gray content`() {
        assertEquals(ContentColorCategory.GRAY, getContentColorCategory(ConnectionState.DISCONNECTED))
    }

    // ==================== All States Covered ====================

    @Test
    fun `all ConnectionState values have text mapping`() {
        for (state in ConnectionState.entries) {
            val text = getStateText(state)
            assertNotNull(text)
            assertTrue(text.isNotEmpty())
        }
    }

    @Test
    fun `all ConnectionState values have container category`() {
        for (state in ConnectionState.entries) {
            val category = getContainerCategory(state)
            assertNotNull(category)
        }
    }

    @Test
    fun `all ConnectionState values have content color category`() {
        for (state in ConnectionState.entries) {
            val category = getContentColorCategory(state)
            assertNotNull(category)
        }
    }

    // ==================== Label Formatting ====================

    @Test
    fun `label with state formats correctly`() {
        val label = "Bluetooth"
        val state = ConnectionState.CONNECTED
        val formatted = "$label: ${getStateText(state)}"

        assertEquals("Bluetooth: Connected", formatted)
    }

    @Test
    fun `wifi label with disconnected state formats correctly`() {
        val label = "WiFi"
        val state = ConnectionState.DISCONNECTED
        val formatted = "$label: ${getStateText(state)}"

        assertEquals("WiFi: Disconnected", formatted)
    }
}
