package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.protocol.ProtocolManager
import kotlin.experimental.xor

object TelemetryParser {

    private const val CMD_TELEMETRY: Int = 0x10
    private const val CUSTOM_FLAG: Int = 0x80
    private const val SAFE_MODE_FLAG: Int = 0x01

    fun parse(packet: ByteArray): Telemetry? {
        // Expected: [START][DEV][CMD][D1][D2][D3][D4][D5][CHK][END]
        // Min size 10 bytes.

        if (packet.size < 10) return null

        // Scan for start byte 0xAA
        for (i in 0..packet.size - 10) {
            if (packet[i] != 0xAA.toByte()) continue
            
            // Check potential frame
            // [i] = START (0xAA)
            // [i+1] = DEV (any)
            // [i+2] = CMD (0x10 for Telemetry)
            // [i+8] = CHK
            // [i+9] = END (0x55)

            if (packet[i + 9] != 0x55.toByte()) continue
            
            // CMD check
            if ((packet[i + 2].toInt() and 0xFF) != CMD_TELEMETRY) continue

            // Checksum
            var xor: Byte = 0
            for (j in (i + 1)..(i + 7)) {
                xor = xor xor packet[j]
            }
            if (xor != packet[i + 8]) continue

            // Valid Frame Found
            val batteryRaw = packet[i + 3].toInt() and 0xFF
            val statusByte = packet[i + 4].toInt() and 0xFF
            val isCustom = (statusByte and CUSTOM_FLAG) != 0
            val isSafeMode = (statusByte and SAFE_MODE_FLAG) != 0

            val battery = batteryRaw / 10f
            val baseStatus = if (isSafeMode) "Safe Mode" else "Active"
            val status = if (isCustom) {
                val counterA = packet[i + 5].toInt() and 0xFF
                val counterB = packet[i + 6].toInt() and 0xFF
                val counterC = packet[i + 7].toInt() and 0xFF
                "$baseStatus | A=$counterA B=$counterB C=$counterC"
            } else {
                baseStatus
            }

            return Telemetry(
                batteryVoltage = battery,
                status = status,
                packetsSent = 0,
                packetsDropped = 0,
                packetsFailed = 0
            ) 
        }
        return null
    }
}
