package com.metelci.ardunakon.protocol

import kotlin.experimental.xor

object ProtocolManager {

    // Packet Structure: [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
    private const val START_BYTE: Byte = 0xAA.toByte()
    private const val END_BYTE: Byte = 0x55.toByte()
    private const val PACKET_SIZE = 10

    // Commands
    const val CMD_JOYSTICK: Byte = 0x01
    const val CMD_BUTTON: Byte = 0x02
    const val CMD_HEARTBEAT: Byte = 0x03
    const val CMD_ESTOP: Byte = 0x04
    const val CMD_ANNOUNCE_CAPABILITIES: Byte = 0x05

    // Encryption Handshake Commands
    const val CMD_HANDSHAKE_REQUEST: Byte = 0x10
    const val CMD_HANDSHAKE_RESPONSE: Byte = 0x11
    const val CMD_HANDSHAKE_COMPLETE: Byte = 0x12
    const val CMD_HANDSHAKE_FAILED: Byte = 0x13

    const val DEFAULT_DEVICE_ID: Byte = 0x01

    // 0 = Min (-1.0), 100 = Center (0.0), 200 = Max (1.0)
    private fun mapJoystickValue(value: Float): Byte {
        val clamped = value.coerceIn(-1f, 1f)
        val mapped = ((clamped + 1f) * 100).toInt()
        return mapped.toByte()
    }

    fun formatJoystickData(leftX: Float, leftY: Float, rightX: Float, rightY: Float, auxBits: Byte = 0): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID // Default Device ID
        packet[2] = CMD_JOYSTICK
        packet[3] = mapJoystickValue(leftX)
        packet[4] = mapJoystickValue(leftY)
        packet[5] = mapJoystickValue(rightX)
        packet[6] = mapJoystickValue(rightY)

        packet[7] = auxBits
        packet[8] = calculateChecksum(packet)
        packet[9] = END_BYTE
        return packet
    }

    fun formatButtonData(buttonId: Int, pressed: Boolean): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = CMD_BUTTON
        packet[3] = buttonId.toByte()
        packet[4] = if (pressed) 1 else 0
        packet[5] = 0
        packet[6] = 0
        packet[7] = 0
        packet[8] = calculateChecksum(packet)
        packet[9] = END_BYTE
        return packet
    }

    fun formatEStopData(): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = CMD_ESTOP
        // Fill rest with 0
        for (i in 3..7) packet[i] = 0
        packet[8] = calculateChecksum(packet)
        packet[9] = END_BYTE
        return packet
    }

    fun formatHeartbeatData(sequence: Int, uptime: Long = System.currentTimeMillis()): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = CMD_HEARTBEAT

        // Sequence (low 16 bits) for staleness detection
        val seq = sequence and 0xFFFF
        packet[3] = (seq shr 8).toByte()
        packet[4] = (seq and 0xFF).toByte()

        // Uptime (low 16 bits) for optional diagnostics
        val uptimeShort = (uptime and 0xFFFF).toInt()
        packet[5] = (uptimeShort shr 8).toByte()
        packet[6] = (uptimeShort and 0xFF).toByte()

        // Remaining payload bytes unused for now
        packet[7] = 0
        packet[8] = calculateChecksum(packet)
        packet[9] = END_BYTE
        return packet
    }

    private fun calculateChecksum(packet: ByteArray): Byte {
        var xor: Byte = 0
        // XOR from DEV_ID (1) to D5 (7)
        for (i in 1..7) {
            xor = xor xor packet[i]
        }
        return xor
    }
}
