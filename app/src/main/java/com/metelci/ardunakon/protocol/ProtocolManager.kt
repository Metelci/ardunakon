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
    const val CMD_SERVO_Z: Byte = 0x06

    // Aux bits (mirrors Arduino ArdunakonProtocol.h)
    const val AUX_W: Byte = 0x01 // Used for A button -> servo Z +
    const val AUX_A: Byte = 0x02
    const val AUX_L: Byte = 0x04
    const val AUX_R: Byte = 0x08
    const val AUX_B: Byte = 0x02 // Used for Z button -> servo Z -

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

    fun formatServoZData(servoZ: Float): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = CMD_SERVO_Z
        packet[3] = mapJoystickValue(servoZ)
        packet[4] = 0
        packet[5] = 0
        packet[6] = 0
        packet[7] = 0
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

    /**
     * Format handshake request packet with app nonce.
     * Payload: NONCE_0..NONCE_15 in extended packet format (21 bytes total)
     */
    fun formatHandshakeRequest(nonce: ByteArray): ByteArray {
        require(nonce.size == 16) { "Nonce must be 16 bytes" }
        // Extended packet: START, DEV_ID, CMD, NONCE[16], CHECKSUM, END
        val packet = ByteArray(21) // 1+1+1+16+1+1
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = CMD_HANDSHAKE_REQUEST
        System.arraycopy(nonce, 0, packet, 3, 16)
        var xor: Byte = 0
        for (i in 1..18) xor = xor xor packet[i]
        packet[19] = xor
        packet[20] = END_BYTE
        return packet
    }

    /**
     * Parse handshake response packet.
     * Returns (deviceNonce, signature) or null if invalid.
     */
    fun parseHandshakeResponse(data: ByteArray): Pair<ByteArray, ByteArray>? {
        // Expected: START, DEV_ID, CMD, NONCE[16], SIG[32], CHECKSUM, END
        // Total: 1+1+1+16+32+1+1 = 53 bytes
        if (data.size < 53) return null
        if (data[0] != START_BYTE) return null
        if (data[2] != CMD_HANDSHAKE_RESPONSE) return null
        if (data[52] != END_BYTE) return null

        // Verify checksum
        var xor: Byte = 0
        for (i in 1..50) xor = xor xor data[i]
        if (data[51] != xor) return null

        val deviceNonce = data.copyOfRange(3, 19)
        val signature = data.copyOfRange(19, 51)
        return Pair(deviceNonce, signature)
    }

    /**
     * Format handshake complete acknowledgment.
     */
    fun formatHandshakeComplete(): ByteArray {
        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = CMD_HANDSHAKE_COMPLETE
        for (i in 3..7) packet[i] = 0
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
