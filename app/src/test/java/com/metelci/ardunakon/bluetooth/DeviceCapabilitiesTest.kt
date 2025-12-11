package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for DeviceCapabilities packet parsing and display logic.
 */
class DeviceCapabilitiesTest {

    @Test
    fun `parse empty packet returns default`() {
        val caps = DeviceCapabilities.fromPacket(byteArrayOf())
        assertEquals(DeviceCapabilities.DEFAULT, caps)
    }

    @Test
    fun `parse packet too short returns default`() {
        val caps = DeviceCapabilities.fromPacket(byteArrayOf(0x01, 0x02))
        assertEquals(DeviceCapabilities.DEFAULT, caps)
    }

    @Test
    fun `parse servo X capability`() {
        val packet = byteArrayOf(0x01, 0x00, 0x00) // CAP1 bit 0 = Servo X
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasServoX)
        assertFalse(caps.hasServoY)
    }

    @Test
    fun `parse servo Y capability`() {
        val packet = byteArrayOf(0x02, 0x00, 0x00) // CAP1 bit 1 = Servo Y
        val caps = DeviceCapabilities.fromPacket(packet)
        assertFalse(caps.hasServoX)
        assertTrue(caps.hasServoY)
    }

    @Test
    fun `parse motor capability`() {
        val packet = byteArrayOf(0x04, 0x00, 0x00) // CAP1 bit 2 = Motor
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasMotor)
    }

    @Test
    fun `parse LED matrix capability`() {
        val packet = byteArrayOf(0x08, 0x00, 0x00) // CAP1 bit 3 = LED Matrix
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasLedMatrix)
    }

    @Test
    fun `parse buzzer capability`() {
        val packet = byteArrayOf(0x10, 0x00, 0x00) // CAP1 bit 4 = Buzzer
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasBuzzer)
    }

    @Test
    fun `parse WiFi capability`() {
        val packet = byteArrayOf(0x20, 0x00, 0x00) // CAP1 bit 5 = WiFi
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasWiFi)
    }

    @Test
    fun `parse BLE capability`() {
        val packet = byteArrayOf(0x40, 0x00, 0x00) // CAP1 bit 6 = BLE
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasBLE)
    }

    @Test
    fun `parse multiple capabilities`() {
        // Servo X + Servo Y + Motor + Buzzer = 0x01 + 0x02 + 0x04 + 0x10 = 0x17
        val packet = byteArrayOf(0x17, 0x00, 0x01)
        val caps = DeviceCapabilities.fromPacket(packet)
        
        assertTrue(caps.hasServoX)
        assertTrue(caps.hasServoY)
        assertTrue(caps.hasMotor)
        assertFalse(caps.hasLedMatrix)
        assertTrue(caps.hasBuzzer)
        assertFalse(caps.hasWiFi)
        assertFalse(caps.hasBLE)
        assertEquals(BoardType.UNO, caps.boardType)
    }

    @Test
    fun `parse Modulino Pixels capability`() {
        val packet = byteArrayOf(0x00, 0x01, 0x00) // CAP2 bit 0 = Modulino Pixels
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasModulinoPixels)
    }

    @Test
    fun `parse Modulino Thermo capability`() {
        val packet = byteArrayOf(0x00, 0x02, 0x00) // CAP2 bit 1 = Modulino Thermo
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasModulinoThermo)
    }

    @Test
    fun `parse Modulino Distance capability`() {
        val packet = byteArrayOf(0x00, 0x04, 0x00) // CAP2 bit 2 = Modulino Distance
        val caps = DeviceCapabilities.fromPacket(packet)
        assertTrue(caps.hasModulinoDistance)
    }

    @Test
    fun `parse board type UNO`() {
        val packet = byteArrayOf(0x00, 0x00, 0x01)
        val caps = DeviceCapabilities.fromPacket(packet)
        assertEquals(BoardType.UNO, caps.boardType)
    }

    @Test
    fun `parse board type R4 WiFi`() {
        val packet = byteArrayOf(0x00, 0x00, 0x02)
        val caps = DeviceCapabilities.fromPacket(packet)
        assertEquals(BoardType.UNO_R4_WIFI, caps.boardType)
    }

    @Test
    fun `parse board type R4 Minima`() {
        val packet = byteArrayOf(0x00, 0x00, 0x03)
        val caps = DeviceCapabilities.fromPacket(packet)
        assertEquals(BoardType.UNO_R4_MINIMA, caps.boardType)
    }

    @Test
    fun `parse board type ESP32`() {
        val packet = byteArrayOf(0x00, 0x00, 0x04)
        val caps = DeviceCapabilities.fromPacket(packet)
        assertEquals(BoardType.ESP32, caps.boardType)
    }

    @Test
    fun `parse unknown board type`() {
        val packet = byteArrayOf(0x00, 0x00, 0xFF.toByte())
        val caps = DeviceCapabilities.fromPacket(packet)
        assertEquals(BoardType.UNKNOWN, caps.boardType)
    }

    @Test
    fun `parse with offset`() {
        val packet = byteArrayOf(0xAA.toByte(), 0x01, 0x01, 0x07, 0x00, 0x02) // Offset at index 3
        val caps = DeviceCapabilities.fromPacket(packet, 3)
        
        assertTrue(caps.hasServoX)
        assertTrue(caps.hasServoY)
        assertTrue(caps.hasMotor)
        assertEquals(BoardType.UNO_R4_WIFI, caps.boardType)
    }

    // ============== Display String Tests ==============

    @Test
    fun `display string shows Servo for servo capabilities`() {
        val caps = DeviceCapabilities(hasServoX = true, hasServoY = false)
        assertTrue(caps.toDisplayString().contains("Servo"))
    }

    @Test
    fun `display string shows Motor`() {
        val caps = DeviceCapabilities(hasServoX = false, hasServoY = false, hasMotor = true)
        assertTrue(caps.toDisplayString().contains("Motor"))
    }

    @Test
    fun `display string shows WiFi and BLE`() {
        val caps = DeviceCapabilities(
            hasServoX = false, hasServoY = false, hasMotor = false,
            hasWiFi = true, hasBLE = true
        )
        assertTrue(caps.toDisplayString().contains("WiFi"))
        assertTrue(caps.toDisplayString().contains("BLE"))
    }

    @Test
    fun `display string returns Basic for no capabilities`() {
        val caps = DeviceCapabilities(
            hasServoX = false, hasServoY = false, hasMotor = false,
            hasLedMatrix = false, hasBuzzer = false, hasWiFi = false, hasBLE = false
        )
        assertEquals("Basic", caps.toDisplayString())
    }

    // ============== BoardType Tests ==============

    @Test
    fun `BoardType fromByte handles all known types`() {
        assertEquals(BoardType.UNO, BoardType.fromByte(0x01))
        assertEquals(BoardType.UNO_R4_WIFI, BoardType.fromByte(0x02))
        assertEquals(BoardType.UNO_R4_MINIMA, BoardType.fromByte(0x03))
        assertEquals(BoardType.ESP32, BoardType.fromByte(0x04))
        assertEquals(BoardType.UNKNOWN, BoardType.fromByte(0x00))
        assertEquals(BoardType.UNKNOWN, BoardType.fromByte(0x99))
    }

    @Test
    fun `BoardType display names are correct`() {
        assertEquals("Arduino UNO", BoardType.UNO.displayName)
        assertEquals("Arduino UNO R4 WiFi", BoardType.UNO_R4_WIFI.displayName)
        assertEquals("Arduino UNO R4 Minima", BoardType.UNO_R4_MINIMA.displayName)
        assertEquals("ESP32", BoardType.ESP32.displayName)
        assertEquals("Unknown", BoardType.UNKNOWN.displayName)
    }
}
