package com.metelci.ardunakon.bluetooth

import kotlin.experimental.xor

/**
 * Parses incoming telemetry packets from Arduino devices.
 * Handles battery voltage, status flags, and heartbeat/capability packets.
 *
 * Packet Structure: [START(0xAA), DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END(0x55)]
 */
object TelemetryParser {

    // Telemetry constants
    private const val MIN_BATTERY_VOLTAGE = 0.0f
    private const val MAX_BATTERY_VOLTAGE = 25.5f
    private const val CMD_HEARTBEAT: Byte = 0x03
    private const val CMD_CAPABILITIES: Byte = 0x05

    /**
     * Parsed telemetry result from Arduino heartbeat packet.
     */
    data class TelemetryResult(val batteryVoltage: Float, val status: String, val isEStop: Boolean = false)

    /**
     * Parses a telemetry packet and returns the result if valid.
     * Returns null if packet is invalid or cannot be parsed.
     */
    fun parse(packet: ByteArray): TelemetryResult? {
        // Validate packet structure
        if (packet.size < 10) return null
        if (packet.first() != 0xAA.toByte() || packet.last() != 0x55.toByte()) return null

        // Verify checksum
        if (!validateChecksum(packet)) return null

        // Only parse heartbeat packets for telemetry
        if (packet[2] != CMD_HEARTBEAT) return null

        val batteryRaw = packet[3].toInt() and 0xFF
        val statusByte = packet[4].toInt() and 0xFF
        if (statusByte > 1) return null

        val battery = batteryRaw / 10f

        // Validate battery voltage bounds
        if (battery < MIN_BATTERY_VOLTAGE || battery > MAX_BATTERY_VOLTAGE) {
            return null
        }

        val status = if (statusByte == 1) "Safe Mode" else "Active"
        val isEStop = statusByte == 1

        return TelemetryResult(battery, status, isEStop)
    }

    /**
     * Parses device capabilities from an announcement packet.
     */
    fun parseCapabilities(packet: ByteArray): DeviceCapabilities? {
        if (packet.size < 6) return null
        if (packet[2].toInt() and 0xFF != 0x05) return null

        return DeviceCapabilities.fromPacket(packet, 3)
    }

    /**
     * Validates the XOR checksum of a packet.
     * Checksum is calculated from bytes 1-7 (DEV_ID through D5).
     */
    fun validateChecksum(packet: ByteArray): Boolean {
        if (packet.size < 10) return false

        var xor: Byte = 0
        for (i in 1..7) {
            xor = xor xor packet[i]
        }
        return xor == packet[8]
    }

    /**
     * Extracts heartbeat sequence number from a heartbeat ACK packet.
     * Returns null if not a valid heartbeat packet.
     */
    fun extractHeartbeatSequence(packet: ByteArray): Int? {
        if (packet.size < 10) return null
        if (packet[2] != CMD_HEARTBEAT) return null
        if (!validateChecksum(packet)) return null

        val seqHigh = packet[3].toInt() and 0xFF
        val seqLow = packet[4].toInt() and 0xFF
        return (seqHigh shl 8) or seqLow
    }
}
