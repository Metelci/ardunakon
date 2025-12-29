package com.metelci.ardunakon.protocol

import kotlin.experimental.xor

/**
 * Formats and parses protocol packets exchanged with Arduino firmware.
 *
 * Packet layout: [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END].
 */
object ProtocolManager {

    // Packet Structure: [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
    private const val START_BYTE: Byte = 0xAA.toByte()
    private const val END_BYTE: Byte = 0x55.toByte()
    private const val PACKET_SIZE = 10

    /** Command ID for joystick payload packets. */
    const val CMD_JOYSTICK: Byte = 0x01

    /** Command ID for button press/release packets. */
    const val CMD_BUTTON: Byte = 0x02

    /** Command ID for heartbeat packets. */
    const val CMD_HEARTBEAT: Byte = 0x03

    /** Command ID for emergency stop packets. */
    const val CMD_ESTOP: Byte = 0x04

    /** Command ID for capability announcement packets. */
    const val CMD_ANNOUNCE_CAPABILITIES: Byte = 0x05

    /** Command ID for servo Z control packets. */
    const val CMD_SERVO_Z: Byte = 0x06

    /** First command ID reserved for custom, user-defined commands. */
    const val CMD_CUSTOM_RANGE_START: Byte = 0x20

    /** Last command ID reserved for custom, user-defined commands. */
    const val CMD_CUSTOM_RANGE_END: Byte = 0x3F

    /** Aux bit used for servo Z positive direction. */
    const val AUX_W: Byte = 0x01 // Used for A button -> servo Z +

    /** Aux bit used for A button state. */
    const val AUX_A: Byte = 0x02

    /** Aux bit used for L button state. */
    const val AUX_L: Byte = 0x04

    /** Aux bit used for R button state. */
    const val AUX_R: Byte = 0x08

    /** Aux bit used for servo Z negative direction. */
    const val AUX_B: Byte = 0x02 // Used for Z button -> servo Z -

    /** Command ID for encryption handshake request. */
    const val CMD_HANDSHAKE_REQUEST: Byte = 0x10

    /** Command ID for encryption handshake response. */
    const val CMD_HANDSHAKE_RESPONSE: Byte = 0x11

    /** Command ID for encryption handshake completion acknowledgement. */
    const val CMD_HANDSHAKE_COMPLETE: Byte = 0x12

    /** Command ID for encryption handshake failure notification. */
    const val CMD_HANDSHAKE_FAILED: Byte = 0x13

    /** Default device ID used when a per-device ID is not specified. */
    const val DEFAULT_DEVICE_ID: Byte = 0x01

    // ========== Network Efficiency: Duplicate Suppression & Rate Limiting ==========

    // Last sent joystick packet for duplicate suppression
    @Volatile private var lastJoystickPacket: ByteArray? = null

    // Rate limiting: cap at ~60 packets per second (16ms minimum interval)
    private const val MIN_SEND_INTERVAL_MS = 16L

    @Volatile private var lastSendTime = 0L

    /**
     * Determines whether a joystick packet should be transmitted.
     *
     * Returns false when the payload is identical to the last sent packet (duplicate suppression)
     * or when the send interval is below the configured minimum (rate limiting).
     *
     * @param packet Full joystick packet payload.
     * @return True when the packet should be sent, false when it should be skipped.
     */
    fun shouldSendJoystickPacket(packet: ByteArray): Boolean {
        val now = System.currentTimeMillis()

        // Rate limiting: ensure minimum interval between sends
        if (now - lastSendTime < MIN_SEND_INTERVAL_MS) {
            return false
        }

        // Duplicate suppression: skip if packet is identical (except checksum position 8)
        val last = lastJoystickPacket
        if (last != null && last.size == packet.size) {
            // Compare data bytes (skip checksum at position 8)
            var identical = true
            for (i in 0 until packet.size) {
                if (i != 8 && last[i] != packet[i]) {
                    identical = false
                    break
                }
            }
            if (identical) return false
        }

        // Packet is new - update cache
        lastJoystickPacket = packet.copyOf()
        lastSendTime = now
        return true
    }

    /**
     * Resets the duplicate suppression cache and rate-limiter timer.
     */
    fun resetPacketCache() {
        lastJoystickPacket = null
        lastSendTime = 0L
    }

    // 0 = Min (-1.0), 100 = Center (0.0), 200 = Max (1.0)
    private fun mapJoystickValue(value: Float): Byte {
        val clamped = value.coerceIn(-1f, 1f)
        val mapped = ((clamped + 1f) * 100).toInt()
        return mapped.toByte()
    }

    /**
     * Formats a joystick packet payload.
     *
     * @param leftX Left joystick X axis (-1.0 to 1.0).
     * @param leftY Left joystick Y axis (-1.0 to 1.0).
     * @param rightX Right joystick X axis (-1.0 to 1.0).
     * @param rightY Right joystick Y axis (-1.0 to 1.0).
     * @param auxBits Auxiliary bitfield for buttons/servo Z.
     * @return Protocol packet ready to send.
     */
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

    /**
     * Formats a servo Z control packet.
     *
     * @param servoZ Servo Z position (-1.0 to 1.0).
     * @return Protocol packet ready to send.
     */
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

    /**
     * Formats a button press/release packet.
     *
     * @param buttonId Logical button ID.
     * @param pressed True if pressed, false if released.
     * @return Protocol packet ready to send.
     */
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

    /**
     * Formats an emergency stop packet.
     *
     * @return Protocol packet ready to send.
     */
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

    /**
     * Formats a heartbeat packet with sequence and uptime information.
     *
     * @param sequence Sequence counter used for staleness detection.
     * @param uptime Timestamp or uptime value in milliseconds.
     * @return Protocol packet ready to send.
     */
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
     * Formats a handshake request packet with an app nonce.
     *
     * Payload: NONCE_0..NONCE_15 in extended packet format (21 bytes total).
     *
     * @param nonce 16-byte nonce.
     * @return Extended handshake request packet.
     * @throws IllegalArgumentException when nonce length is not 16 bytes.
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
     * Parses a handshake response packet.
     *
     * @param data Raw packet data.
     * @return Pair of (deviceNonce, signature) or null if invalid.
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
     * Formats a handshake completion acknowledgement packet.
     *
     * @return Protocol packet ready to send.
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

    /**
     * Format a custom command packet.
     *
     * @param commandId Command ID in range 0x20-0x3F
     * @param payload 5-byte payload data
     * @return 10-byte protocol packet
     * @throws IllegalArgumentException if commandId is out of range or payload size is wrong
     */
    fun formatCustomCommandData(commandId: Byte, payload: ByteArray): ByteArray {
        require(commandId in CMD_CUSTOM_RANGE_START..CMD_CUSTOM_RANGE_END) {
            "Command ID must be in custom range 0x20-0x3F, got: 0x${commandId.toInt().and(0xFF).toString(16)}"
        }
        require(payload.size == 5) {
            "Payload must be exactly 5 bytes, got: ${payload.size}"
        }

        val packet = ByteArray(PACKET_SIZE)
        packet[0] = START_BYTE
        packet[1] = DEFAULT_DEVICE_ID
        packet[2] = commandId
        System.arraycopy(payload, 0, packet, 3, 5)
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
