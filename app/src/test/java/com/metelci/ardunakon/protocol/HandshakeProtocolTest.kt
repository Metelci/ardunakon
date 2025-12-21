package com.metelci.ardunakon.protocol

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for encryption handshake packet formatting in ProtocolManager.
 */
class HandshakeProtocolTest {

    // ============== Handshake Request Tests ==============

    @Test
    fun `formatHandshakeRequest creates valid packet`() {
        val nonce = ByteArray(16) { it.toByte() }

        val packet = ProtocolManager.formatHandshakeRequest(nonce)

        assertEquals("Packet should be 21 bytes", 21, packet.size)
        assertEquals("Start byte", 0xAA.toByte(), packet[0])
        assertEquals("Device ID", 0x01.toByte(), packet[1])
        assertEquals("Command", 0x10.toByte(), packet[2])
        assertEquals("End byte", 0x55.toByte(), packet[20])
    }

    @Test
    fun `formatHandshakeRequest includes nonce`() {
        val nonce = ByteArray(16) { (it + 100).toByte() }

        val packet = ProtocolManager.formatHandshakeRequest(nonce)

        // Nonce should be at bytes 3-18
        for (i in 0 until 16) {
            assertEquals("Nonce byte $i", nonce[i], packet[3 + i])
        }
    }

    @Test
    fun `formatHandshakeRequest calculates checksum`() {
        val nonce = ByteArray(16) { it.toByte() }

        val packet = ProtocolManager.formatHandshakeRequest(nonce)

        // Verify checksum (XOR of bytes 1-18)
        var expectedXor: Byte = 0
        for (i in 1..18) {
            expectedXor = (expectedXor.toInt() xor packet[i].toInt()).toByte()
        }
        assertEquals("Checksum should match", expectedXor, packet[19])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatHandshakeRequest rejects wrong nonce size`() {
        val shortNonce = ByteArray(8)
        ProtocolManager.formatHandshakeRequest(shortNonce)
    }

    // ============== Handshake Response Parsing Tests ==============

    @Test
    fun `parseHandshakeResponse parses valid packet`() {
        // Build a valid response packet
        val deviceNonce = ByteArray(16) { (it + 10).toByte() }
        val signature = ByteArray(32) { (it + 50).toByte() }
        val packet = buildValidHandshakeResponse(deviceNonce, signature)

        val result = ProtocolManager.parseHandshakeResponse(packet)

        assertNotNull("Should parse successfully", result)
        val (parsedNonce, parsedSig) = result!!
        assertArrayEquals("Device nonce should match", deviceNonce, parsedNonce)
        assertArrayEquals("Signature should match", signature, parsedSig)
    }

    @Test
    fun `parseHandshakeResponse returns null for short packet`() {
        val shortPacket = ByteArray(20)

        val result = ProtocolManager.parseHandshakeResponse(shortPacket)

        assertNull("Should return null for short packet", result)
    }

    @Test
    fun `parseHandshakeResponse returns null for wrong start byte`() {
        val packet = buildValidHandshakeResponse(ByteArray(16), ByteArray(32))
        packet[0] = 0x00 // Wrong start byte

        val result = ProtocolManager.parseHandshakeResponse(packet)

        assertNull("Should return null for wrong start byte", result)
    }

    @Test
    fun `parseHandshakeResponse returns null for wrong command`() {
        val packet = buildValidHandshakeResponse(ByteArray(16), ByteArray(32))
        packet[2] = 0x01 // Wrong command

        val result = ProtocolManager.parseHandshakeResponse(packet)

        assertNull("Should return null for wrong command", result)
    }

    @Test
    fun `parseHandshakeResponse returns null for wrong end byte`() {
        val packet = buildValidHandshakeResponse(ByteArray(16), ByteArray(32))
        packet[52] = 0x00 // Wrong end byte

        val result = ProtocolManager.parseHandshakeResponse(packet)

        assertNull("Should return null for wrong end byte", result)
    }

    @Test
    fun `parseHandshakeResponse returns null for bad checksum`() {
        val packet = buildValidHandshakeResponse(ByteArray(16), ByteArray(32))
        packet[51] = (packet[51].toInt() xor 0xFF).toByte() // Corrupt checksum

        val result = ProtocolManager.parseHandshakeResponse(packet)

        assertNull("Should return null for bad checksum", result)
    }

    // ============== Handshake Complete Tests ==============

    @Test
    fun `formatHandshakeComplete creates valid packet`() {
        val packet = ProtocolManager.formatHandshakeComplete()

        assertEquals("Packet should be 10 bytes", 10, packet.size)
        assertEquals("Start byte", 0xAA.toByte(), packet[0])
        assertEquals("Device ID", 0x01.toByte(), packet[1])
        assertEquals("Command", 0x12.toByte(), packet[2])
        assertEquals("End byte", 0x55.toByte(), packet[9])
    }

    @Test
    fun `formatHandshakeComplete has zeroed payload`() {
        val packet = ProtocolManager.formatHandshakeComplete()

        for (i in 3..7) {
            assertEquals("Payload byte $i should be zero", 0.toByte(), packet[i])
        }
    }

    // ============== Helper Functions ==============

    private fun buildValidHandshakeResponse(deviceNonce: ByteArray, signature: ByteArray): ByteArray {
        // Packet: START, DEV_ID, CMD, NONCE[16], SIG[32], CHECKSUM, END
        // Total: 53 bytes
        val packet = ByteArray(53)
        packet[0] = 0xAA.toByte() // START
        packet[1] = 0x01 // Device ID
        packet[2] = 0x11 // CMD_HANDSHAKE_RESPONSE
        System.arraycopy(deviceNonce, 0, packet, 3, 16)
        System.arraycopy(signature, 0, packet, 19, 32)

        // Calculate checksum (XOR of bytes 1-50)
        var xor: Byte = 0
        for (i in 1..50) {
            xor = (xor.toInt() xor packet[i].toInt()).toByte()
        }
        packet[51] = xor
        packet[52] = 0x55.toByte() // END

        return packet
    }
}
