package com.metelci.ardunakon.protocol

import kotlin.experimental.xor
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtocolManagerTest {

    @Test
    fun testJoystickFormat() {
        // -1.0f -> 0, 0.0f -> 100, 1.0f -> 200
        val packet = ProtocolManager.formatJoystickData(
            leftX = 0f,
            leftY = 1f,
            rightX = -1f,
            rightY = 0.5f,
            auxBits = 0x01
        )

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0]) // START
        assertEquals(0x01.toByte(), packet[1]) // DEV_ID
        assertEquals(ProtocolManager.CMD_JOYSTICK, packet[2]) // CMD

        assertEquals(100.toByte(), packet[3]) // LeftX 0.0 -> 100
        assertEquals(200.toByte(), packet[4]) // LeftY 1.0 -> 200
        assertEquals(0.toByte(), packet[5]) // RightX -1.0 -> 0
        assertEquals(150.toByte(), packet[6]) // RightY 0.5 -> 150

        assertEquals(0x01.toByte(), packet[7]) // AuxBits

        // Manual Checksum: 01 ^ 01 ^ 64 ^ C8 ^ 00 ^ 96 ^ 01
        // 01^01 = 0
        // 0^64 (100) = 100
        // 100^200 (-56) = -84 (0xAC)
        // ... let the impl calculate it, we verify validity

        val checksum = calculateChecksum(packet)
        assertEquals(checksum, packet[8])
        assertEquals(0x55.toByte(), packet[9]) // END
    }

    @Test
    fun testEStopFormat() {
        val packet = ProtocolManager.formatEStopData()
        assertEquals(ProtocolManager.CMD_ESTOP, packet[2])
        assertEquals(0.toByte(), packet[3]) // Zeroed payload
        assertEquals(calculateChecksum(packet), packet[8])
    }

    @Test
    fun testHeartbeatFormat() {
        val seq = 0x1234
        val uptime = 0xABCDL
        val packet = ProtocolManager.formatHeartbeatData(seq, uptime)

        assertEquals(ProtocolManager.CMD_HEARTBEAT, packet[2])

        // Sequence (Big Endian logic in ProtocolManager: (seq shr 8), (seq and 0xFF))
        assertEquals(0x12.toByte(), packet[3])
        assertEquals(0x34.toByte(), packet[4])

        // Uptime (low 16 bits)
        assertEquals(0xCD.toByte(), packet[6]) // Low byte
        // 0xAB is high byte? Wait, let's check code logic:
        // packet[5] = (uptimeShort shr 8).toByte() -> 0xAB
        assertEquals(0xAB.toByte(), packet[5]) // High byte of low short

        assertEquals(calculateChecksum(packet), packet[8])
    }

    @Test
    fun testServoZFormat() {
        val packet = ProtocolManager.formatServoZData(servoZ = -1f)

        assertEquals(10, packet.size)
        assertEquals(0xAA.toByte(), packet[0]) // START
        assertEquals(0x01.toByte(), packet[1]) // DEV_ID
        assertEquals(ProtocolManager.CMD_SERVO_Z, packet[2]) // CMD
        assertEquals(0.toByte(), packet[3]) // -1.0 -> 0
        assertEquals(0.toByte(), packet[4])
        assertEquals(0.toByte(), packet[5])
        assertEquals(0.toByte(), packet[6])
        assertEquals(0.toByte(), packet[7])
        assertEquals(calculateChecksum(packet), packet[8])
        assertEquals(0x55.toByte(), packet[9]) // END
    }

    private fun calculateChecksum(packet: ByteArray): Byte {
        var xor: Byte = 0
        for (i in 1..7) {
            xor = xor xor packet[i]
        }
        return xor
    }
}
