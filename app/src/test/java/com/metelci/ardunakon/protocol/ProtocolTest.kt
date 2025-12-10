package com.metelci.ardunakon.protocol

import org.junit.Test
import org.junit.Assert.*

class ProtocolTest {

    @Test
    fun testJoystickMapping() {
        // Test Center (0.0) -> 100
        val center = ProtocolManager.formatJoystickData(0f, 0f, 0f, 0f, 0.toByte())
        assertEquals(100.toByte(), center[3]) // Left X
        assertEquals(100.toByte(), center[4]) // Left Y

        // Test Max (1.0) -> 200
        val max = ProtocolManager.formatJoystickData(1f, 1f, 1f, 1f, 0.toByte())
        assertEquals(200.toByte(), max[3])

        // Test Min (-1.0) -> 0
        val min = ProtocolManager.formatJoystickData(-1f, -1f, -1f, -1f, 0.toByte())
        assertEquals(0.toByte(), min[3])
    }

    @Test
    fun testSensitivityClamping() {
        // Sensitivity 2.0 with input 1.0 = 2.0 -> Should clamp to 1.0 (200)
        val overdriven = ProtocolManager.formatJoystickData(2.0f, 0f, 0f, 0f, 0.toByte())
        assertEquals(200.toByte(), overdriven[3])

        // Sensitivity 2.0 with input -1.0 = -2.0 -> Should clamp to -1.0 (0)
        val underdriven = ProtocolManager.formatJoystickData(-2.0f, 0f, 0f, 0f, 0.toByte())
        assertEquals(0.toByte(), underdriven[3])
    }

    @Test
    fun testEStopPacket() {
        val packet = ProtocolManager.formatEStopData()
        assertEquals(0x04.toByte(), packet[2]) // CMD_ESTOP
        assertEquals(0.toByte(), packet[3]) // Data should be 0
    }
}
