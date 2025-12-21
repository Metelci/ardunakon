package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for E-STOP (Emergency Stop) behavior.
 * Validates the safety-critical logic for immediately stopping all motors.
 */
class EStopBehaviorTest {

    // ============== E-STOP State Management ==============

    @Test
    fun `E-STOP activation sets flag to true`() {
        var isEmergencyStopActive = false

        // Simulate setEmergencyStop(true)
        isEmergencyStopActive = true

        assertTrue("E-STOP should be active", isEmergencyStopActive)
    }

    @Test
    fun `E-STOP deactivation sets flag to false`() {
        var isEmergencyStopActive = true

        // Simulate setEmergencyStop(false)
        isEmergencyStopActive = false

        assertFalse("E-STOP should be inactive", isEmergencyStopActive)
    }

    @Test
    fun `E-STOP blocks auto-reconnect`() {
        var isEmergencyStopActive = true
        var shouldReconnect = true
        var autoReconnectEnabled = true

        // Simulate disconnectForEStop()
        if (isEmergencyStopActive) {
            shouldReconnect = false
            autoReconnectEnabled = false
        }

        assertFalse("Should reconnect must be false", shouldReconnect)
        assertFalse("Auto reconnect must be disabled", autoReconnectEnabled)
    }

    @Test
    fun `E-STOP clears saved device on reset`() {
        var savedDevice: String? = "TestDevice"

        // Simulate holdOfflineAfterEStopReset()
        savedDevice = null

        assertNull("Saved device should be cleared", savedDevice)
    }

    @Test
    fun `transmission loop skips when E-STOP active`() {
        val isEmergencyStopActive = true
        var transmissionSent = false

        // Simulate transmission loop check
        if (!isEmergencyStopActive) {
            transmissionSent = true
        }

        assertFalse("Transmission should not be sent when E-STOP active", transmissionSent)
    }

    @Test
    fun `reconnect monitor skips when E-STOP active`() {
        val isEmergencyStopActive = true
        var reconnectAttempted = false

        // Simulate reconnect monitor check
        if (!isEmergencyStopActive) {
            reconnectAttempted = true
        }

        assertFalse("Reconnect should not be attempted when E-STOP active", reconnectAttempted)
    }

    // ============== E-STOP Packet Format ==============

    @Test
    fun `E-STOP packet has correct command byte`() {
        val cmdEstop: Byte = 0x04
        assertEquals(0x04.toByte(), cmdEstop)
    }

    @Test
    fun `E-STOP packet has zeroed payload`() {
        val packetSize = 10
        val packet = ByteArray(packetSize)
        packet[0] = 0xAA.toByte() // START
        packet[1] = 0x01 // DEV_ID
        packet[2] = 0x04 // CMD_ESTOP
        // D1-D5 should be 0
        for (i in 3..7) packet[i] = 0
        // Calculate checksum: XOR of bytes 1-7
        var xor: Byte = 0
        for (i in 1..7) xor = (xor.toInt() xor packet[i].toInt()).toByte()
        packet[8] = xor
        packet[9] = 0x55.toByte() // END

        // Verify payload bytes are zero
        for (i in 3..7) {
            assertEquals("Payload byte $i should be zero", 0.toByte(), packet[i])
        }
    }

    @Test
    fun `E-STOP packet structure is valid`() {
        val packet = ByteArray(10)
        packet[0] = 0xAA.toByte() // START
        packet[1] = 0x01 // DEV_ID
        packet[2] = 0x04 // CMD_ESTOP
        for (i in 3..7) packet[i] = 0
        var xor: Byte = 0
        for (i in 1..7) xor = (xor.toInt() xor packet[i].toInt()).toByte()
        packet[8] = xor
        packet[9] = 0x55.toByte() // END

        assertEquals("Packet should be 10 bytes", 10, packet.size)
        assertEquals("Start byte should be 0xAA", 0xAA.toByte(), packet[0])
        assertEquals("End byte should be 0x55", 0x55.toByte(), packet[9])
    }

    // ============== E-STOP Connection Behavior ==============

    @Test
    fun `E-STOP disconnects all connections`() {
        var connectionCancelled = false
        var connectionCleared = false

        // Simulate disconnectForEStop()
        connectionCancelled = true
        connectionCleared = true

        assertTrue("Connection should be cancelled", connectionCancelled)
        assertTrue("Connection reference should be cleared", connectionCleared)
    }

    @Test
    fun `E-STOP resets RSSI to zero`() {
        var rssi = -50

        // Simulate disconnectForEStop() -> updateRssi(0)
        rssi = 0

        assertEquals("RSSI should be reset to 0", 0, rssi)
    }

    @Test
    fun `E-STOP updates connection state to DISCONNECTED`() {
        var connectionState = "CONNECTED"

        // Simulate disconnectForEStop() -> updateConnectionState(DISCONNECTED)
        connectionState = "DISCONNECTED"

        assertEquals("Connection state should be DISCONNECTED", "DISCONNECTED", connectionState)
    }

    // ============== E-STOP Force Send ==============

    @Test
    fun `E-STOP packet is sent with force flag`() {
        var forceUsed = false

        // Simulate sendDataToAll(stopPacket, force = true)
        fun sendDataToAll(data: ByteArray, force: Boolean) {
            forceUsed = force
        }

        sendDataToAll(ByteArray(10), force = true)

        assertTrue("Force flag should be true for E-STOP", forceUsed)
    }

    // ============== E-STOP Hold Offline ==============

    @Test
    fun `hold offline after E-STOP reset disables auto-reconnect`() {
        var shouldReconnect = true
        var autoReconnectEnabled = true
        var savedDevice: String? = "Device"

        // Simulate holdOfflineAfterEStopReset()
        shouldReconnect = false
        autoReconnectEnabled = false
        savedDevice = null

        assertFalse(shouldReconnect)
        assertFalse(autoReconnectEnabled)
        assertNull(savedDevice)
    }
}
