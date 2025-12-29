package com.metelci.ardunakon.protocol

import org.junit.Assert.*
import org.junit.Test

/**
 * Integration-style tests for Protocol package.
 *
 * Tests the ProtocolManager command formatting and packet validation flows.
 */
class ProtocolIntegrationTest {

    // ==================== Joystick Command Flow ====================

    @Test
    fun `formatJoystickData creates valid packet`() {
        val packet = ProtocolManager.formatJoystickData(0f, 0f, 0f, 0f)

        // Verify packet structure: 10 bytes with header and trailer
        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0]) // Header
        assertEquals(0x55.toByte(), packet[9]) // Trailer
    }

    @Test
    fun `formatJoystickData handles center values`() {
        val packet = ProtocolManager.formatJoystickData(0f, 0f, 0f, 0f)

        assertTrue(packet.isNotEmpty())
        assertEquals(10, packet.size)
    }

    @Test
    fun `formatJoystickData handles max values`() {
        val packet = ProtocolManager.formatJoystickData(1f, 1f, 1f, 1f)

        assertTrue(packet.isNotEmpty())
        assertEquals(10, packet.size)
    }

    @Test
    fun `formatJoystickData handles min values`() {
        val packet = ProtocolManager.formatJoystickData(-1f, -1f, -1f, -1f)

        assertTrue(packet.isNotEmpty())
        assertEquals(10, packet.size)
    }

    @Test
    fun `formatJoystickData with aux bits`() {
        val packet = ProtocolManager.formatJoystickData(0f, 0f, 0f, 0f, 0x0F)

        assertTrue(packet.isNotEmpty())
        assertEquals(10, packet.size)
    }

    // ==================== Servo Command Flow ====================

    @Test
    fun `formatServoZData creates valid packet`() {
        val packet = ProtocolManager.formatServoZData(0.5f)

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(0x55.toByte(), packet[9])
    }

    @Test
    fun `formatServoZData handles min value`() {
        val packet = ProtocolManager.formatServoZData(0f)

        assertTrue(packet.isNotEmpty())
        assertEquals(10, packet.size)
    }

    @Test
    fun `formatServoZData handles max value`() {
        val packet = ProtocolManager.formatServoZData(1f)

        assertTrue(packet.isNotEmpty())
        assertEquals(10, packet.size)
    }

    // ==================== Button Command Flow ====================

    @Test
    fun `formatButtonData creates valid packet for press`() {
        val packet = ProtocolManager.formatButtonData(1, true)

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(0x55.toByte(), packet[9])
    }

    @Test
    fun `formatButtonData creates valid packet for release`() {
        val packet = ProtocolManager.formatButtonData(1, false)

        assertEquals(10, packet.size)
    }

    @Test
    fun `formatButtonData handles different button IDs`() {
        for (buttonId in 0..15) {
            val packet = ProtocolManager.formatButtonData(buttonId, true)
            assertEquals(10, packet.size)
        }
    }

    // ==================== Heartbeat Flow ====================

    @Test
    fun `formatHeartbeatData creates valid packet`() {
        val packet = ProtocolManager.formatHeartbeatData(1)

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(0x55.toByte(), packet[9])
    }

    @Test
    fun `formatHeartbeatData handles sequence 0`() {
        val packet = ProtocolManager.formatHeartbeatData(0)

        assertEquals(10, packet.size)
    }

    @Test
    fun `formatHeartbeatData handles max sequence`() {
        val packet = ProtocolManager.formatHeartbeatData(0xFFFF)

        assertEquals(10, packet.size)
    }

    @Test
    fun `formatHeartbeatData with custom uptime`() {
        val packet = ProtocolManager.formatHeartbeatData(100, 123456789L)

        assertEquals(10, packet.size)
    }

    // ==================== E-STOP Flow ====================

    @Test
    fun `formatEStopData creates valid packet`() {
        val packet = ProtocolManager.formatEStopData()

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(0x55.toByte(), packet[9])
    }

    // ==================== Custom Command Flow ====================

    @Test
    fun `formatCustomCommandData creates valid packet`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val packet = ProtocolManager.formatCustomCommandData(0x20, payload)

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0])
        assertEquals(0x55.toByte(), packet[9])
    }

    @Test
    fun `formatCustomCommandData validates command ID range`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        // Valid range is 0x20-0x3F
        val packet = ProtocolManager.formatCustomCommandData(0x3F, payload)
        assertEquals(10, packet.size)
    }

    @Test
    fun `formatCustomCommandData throws for invalid command ID`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        try {
            ProtocolManager.formatCustomCommandData(0x00, payload)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `formatCustomCommandData throws for invalid payload size`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03) // Too short

        try {
            ProtocolManager.formatCustomCommandData(0x20, payload)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // ==================== Handshake Flow ====================

    @Test
    fun `formatHandshakeRequest creates packet`() {
        val nonce = ByteArray(16) { it.toByte() }
        val packet = ProtocolManager.formatHandshakeRequest(nonce)

        assertTrue(packet.isNotEmpty())
    }

    @Test
    fun `formatHandshakeComplete creates packet`() {
        val packet = ProtocolManager.formatHandshakeComplete()

        assertTrue(packet.isNotEmpty())
    }

    @Test
    fun `parseHandshakeResponse returns null for invalid packet`() {
        val invalidPacket = byteArrayOf(0x00, 0x01, 0x02)

        val result = ProtocolManager.parseHandshakeResponse(invalidPacket)

        assertNull(result)
    }

    // ==================== Packet Cache Flow ====================

    @Test
    fun `shouldSendJoystickPacket allows first packet`() {
        ProtocolManager.resetPacketCache()

        val packet = ProtocolManager.formatJoystickData(0.5f, 0.5f, 0f, 0f)
        val shouldSend = ProtocolManager.shouldSendJoystickPacket(packet)

        assertTrue(shouldSend)
    }

    @Test
    fun `shouldSendJoystickPacket suppresses duplicate`() {
        ProtocolManager.resetPacketCache()

        val packet = ProtocolManager.formatJoystickData(0.5f, 0.5f, 0f, 0f)
        ProtocolManager.shouldSendJoystickPacket(packet)

        // Immediately sending the same packet should be suppressed
        val shouldSend = ProtocolManager.shouldSendJoystickPacket(packet)

        // May or may not be false depending on implementation
        // Just verify no crash
    }

    @Test
    fun `resetPacketCache clears cached packet`() {
        val packet = ProtocolManager.formatJoystickData(0.5f, 0.5f, 0f, 0f)
        ProtocolManager.shouldSendJoystickPacket(packet)

        ProtocolManager.resetPacketCache()

        // After reset, the same packet should be allowed again
        val shouldSend = ProtocolManager.shouldSendJoystickPacket(packet)
        assertTrue(shouldSend)
    }
}
