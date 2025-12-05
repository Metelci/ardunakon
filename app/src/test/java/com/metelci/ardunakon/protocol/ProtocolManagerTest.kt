package com.metelci.ardunakon.protocol

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.experimental.xor

class ProtocolManagerTest {

    @Test
    fun `checksum is xor of payload bytes`() {
        val packet = ProtocolManager.formatButtonData(buttonId = 3, pressed = true)
        // checksum is XOR of bytes 1..7
        var xor: Byte = 0
        for (i in 1..7) xor = xor.xor(packet[i])
        assertEquals(xor, packet[8])
    }

    @Test
    fun `joystick mapping clamps values`() {
        val packet = ProtocolManager.formatJoystickData(-1f, 0f, 1f, 2f)
        assertEquals(0.toByte(), packet[3])
        assertEquals(100.toByte(), packet[4])
        assertEquals(200.toByte(), packet[5])
        // rightY clamped to 1.0 so maps to 200
        assertEquals(200.toByte(), packet[6])
    }

    @Test
    fun `heartbeat encodes sequence and uptime`() {
        val seq = 0xBEEF
        val packet = ProtocolManager.formatHeartbeatData(sequence = seq, uptime = 0x1234)
        assertEquals((seq shr 8).toByte(), packet[3])
        assertEquals((seq and 0xFF).toByte(), packet[4])
        assertEquals(0x12.toByte(), packet[5])
        assertEquals(0x34.toByte(), packet[6])
        assertEquals(ProtocolManager.CMD_HEARTBEAT, packet[2])
    }
}
