package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.protocol.ProtocolManager
import kotlin.experimental.xor

object TelemetryParser {
    
    fun parse(packet: ByteArray): Telemetry? {
        // Expected: [START][DEV][CMD][D1][D2][D3][D4][D5][CHK][END]
        if (packet.size < 10) return null
        if (packet.first() != 0xAA.toByte() || packet.last() != 0x55.toByte()) return null

        // Checksum
        var xor: Byte = 0
        for (i in 1..7) xor = xor xor packet[i]
        if (xor != packet[8]) return null

        // Accept CMD_TELEMETRY (0x10) - Arduino sends telemetry with this command byte
        if (packet[2] != 0x10.toByte()) return null

        val batteryRaw = packet[3].toInt() and 0xFF
        val statusByte = packet[4].toInt() and 0xFF
        if (statusByte > 1) return null

        val battery = batteryRaw / 10f
        
        // Bounds check handled by consumer or here? 
        // Let's return raw data and let consumer decide if it's "valid" for logging, 
        // OR filtering here. 
        // Original code filtered invalid voltage (min/max).
        if (battery < BluetoothConfig.MIN_BATTERY_VOLTAGE || battery > BluetoothConfig.MAX_BATTERY_VOLTAGE) {
            return null
        }

        val status = if (statusByte == 1) "Safe Mode" else "Active"
        
        // WifiManager won't have packet stats from this packet content (it's internal to connection)
        // So we return a base Telemetry object. 
        // Consumers might need to fill in packet stats if they track it separately.
        
        return Telemetry(
            batteryVoltage = battery,
            status = status,
            packetsSent = 0,
            packetsDropped = 0,
            packetsFailed = 0
        )
    }
}
