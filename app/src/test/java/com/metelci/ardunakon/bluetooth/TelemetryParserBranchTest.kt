package com.metelci.ardunakon.bluetooth

import kotlin.experimental.xor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryParserBranchTest {

    private fun buildPacket(
        cmd: Byte = 0x10,
        batteryTenths: Int = 120,
        status: Int = 0,
        corruptChecksum: Boolean = false,
        start: Byte = 0xAA.toByte(),
        end: Byte = 0x55.toByte()
    ): ByteArray {
        val packet = ByteArray(10)
        packet[0] = start
        packet[1] = 0x01
        packet[2] = cmd
        packet[3] = batteryTenths.toByte()
        packet[4] = status.toByte()
        packet[5] = 0x00
        packet[6] = 0x00
        packet[7] = 0x00

        var checksum: Byte = 0
        for (i in 1..7) checksum = checksum xor packet[i]
        packet[8] = if (corruptChecksum) checksum.xor(0x01) else checksum
        packet[9] = end
        return packet
    }

    @Test
    fun parse_returns_null_for_short_packet() {
        assertNull(TelemetryParser.parse(byteArrayOf(0xAA.toByte(), 0x55.toByte())))
    }

    @Test
    fun parse_returns_null_for_bad_start_or_end() {
        assertNull(TelemetryParser.parse(buildPacket(start = 0x00)))
        assertNull(TelemetryParser.parse(buildPacket(end = 0x00)))
    }

    @Test
    fun parse_returns_null_for_bad_checksum() {
        assertNull(TelemetryParser.parse(buildPacket(corruptChecksum = true)))
    }

    @Test
    fun parse_returns_null_for_non_telemetry_cmd() {
        assertNull(TelemetryParser.parse(buildPacket(cmd = 0x42)))
    }

    @Test
    fun parse_accepts_unknown_status_byte_as_active() {
        val parsed = TelemetryParser.parse(buildPacket(status = 2))
        assertNotNull(parsed)
        assertEquals("Active", parsed?.status)
    }

    @Test
    fun parse_accepts_battery_at_byte_limits() {
        val low = TelemetryParser.parse(buildPacket(batteryTenths = 0, status = 0))
        assertNotNull(low)
        assertEquals(0.0f, low?.batteryVoltage)

        val high = TelemetryParser.parse(buildPacket(batteryTenths = 255, status = 0))
        assertNotNull(high)
        assertEquals(25.5f, high?.batteryVoltage)
    }

    @Test
    fun parse_returns_telemetry_for_active_and_safe_mode() {
        val active = TelemetryParser.parse(buildPacket(batteryTenths = 120, status = 0))
        assertNotNull(active)
        assertEquals("Active", active?.status)
        assertEquals(12.0f, active?.batteryVoltage)

        val safe = TelemetryParser.parse(buildPacket(batteryTenths = 120, status = 1))
        assertNotNull(safe)
        assertEquals("Safe Mode", safe?.status)
    }

    @Test
    fun parse_includes_custom_counters_when_custom_flag_set() {
        val packet = buildPacket(status = 0x80).apply {
            this[5] = 10
            this[6] = 20
            this[7] = 30
            var checksum: Byte = 0
            for (i in 1..7) checksum = checksum xor this[i]
            this[8] = checksum
        }
        val parsed = TelemetryParser.parse(packet)
        assertNotNull(parsed)
        assertEquals("Active | A=10 B=20 C=30", parsed?.status)
    }
}
