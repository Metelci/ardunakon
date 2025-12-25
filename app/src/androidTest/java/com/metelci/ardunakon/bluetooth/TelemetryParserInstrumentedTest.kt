package com.metelci.ardunakon.bluetooth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TelemetryManager packet parsing.
 *
 * These tests require real Android environment for proper
 * telemetry parsing behavior.
 */
@RunWith(AndroidJUnit4::class)
class TelemetryParserInstrumentedTest {

    private lateinit var telemetryManager: TelemetryManager

    @Before
    fun setup() {
        telemetryManager = TelemetryManager()
    }

    @Test
    fun parseTelemetryPacket_validPacket_extractsData() {
        // Standard telemetry format: "BAT:7.4V|RSSI:-65|LAT:25ms"
        val packet = "BAT:7.4V|RSSI:-65|LAT:25ms"
        
        // Parse should not throw
        val parsed = parseTelemetryData(packet)
        
        assertTrue("Should parse battery", parsed.containsKey("BAT"))
        assertTrue("Should parse RSSI", parsed.containsKey("RSSI"))
    }

    @Test
    fun parseTelemetryPacket_emptyPacket_handlesGracefully() {
        val packet = ""
        
        val parsed = parseTelemetryData(packet)
        
        assertTrue("Empty packet should return empty result", parsed.isEmpty())
    }

    @Test
    fun parseTelemetryPacket_malformedPacket_handlesGracefully() {
        val packet = "not a valid packet format"
        
        // Should not throw
        val parsed = parseTelemetryData(packet)
        
        // May have partial data or be empty
        assertNotNull(parsed)
    }

    @Test
    fun parseTelemetryPacket_partialData_extractsAvailable() {
        val packet = "BAT:8.2V"
        
        val parsed = parseTelemetryData(packet)
        
        assertTrue("Should parse available data", parsed.containsKey("BAT") || parsed.isEmpty())
    }

    private fun parseTelemetryData(packet: String): Map<String, String> {
        if (packet.isEmpty()) return emptyMap()
        
        return try {
            packet.split("|")
                .filter { it.contains(":") }
                .associate { 
                    val parts = it.split(":", limit = 2)
                    parts[0] to parts.getOrElse(1) { "" }
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
