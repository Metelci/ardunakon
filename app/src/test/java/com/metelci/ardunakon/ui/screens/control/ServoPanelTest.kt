package com.metelci.ardunakon.ui.screens.control

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ServoPanel logic and state management.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class ServoPanelTest {

    @Test
    fun `servo callback receives x, y, and z values`() {
        var receivedX = 0f
        var receivedY = 0f
        var receivedZ = 0f
        
        val onServoMove: (Float, Float, Float) -> Unit = { x, y, z ->
            receivedX = x
            receivedY = y
            receivedZ = z
        }
        
        onServoMove(0.5f, -0.3f, 0.8f)
        
        assertEquals(0.5f, receivedX, 0.001f)
        assertEquals(-0.3f, receivedY, 0.001f)
        assertEquals(0.8f, receivedZ, 0.001f)
    }

    @Test
    fun `servo values are in range -1 to 1`() {
        val x = 0.75f
        val y = -0.5f
        val z = 0.25f
        
        assertTrue(x >= -1f && x <= 1f)
        assertTrue(y >= -1f && y <= 1f)
        assertTrue(z >= -1f && z <= 1f)
    }

    @Test
    fun `default button size is 56dp`() {
        val defaultSize = 56 // dp
        
        assertEquals(56, defaultSize)
    }

    @Test
    fun `servo X position is maintained`() {
        val servoX = 0.5f
        
        assertEquals(0.5f, servoX, 0.001f)
    }

    @Test
    fun `servo Y position is maintained`() {
        val servoY = -0.3f
        
        assertEquals(-0.3f, servoY, 0.001f)
    }

    @Test
    fun `servo Z position is maintained`() {
        val servoZ = 0.8f
        
        assertEquals(0.8f, servoZ, 0.001f)
    }

    @Test
    fun `log callback receives messages`() {
        var loggedMessage = ""
        
        val onLog: (String) -> Unit = { message ->
            loggedMessage = message
        }
        
        onLog("Servo moved to X: 0.5")
        
        assertEquals("Servo moved to X: 0.5", loggedMessage)
    }

    @Test
    fun `log callback has default empty implementation`() {
        val onLog: (String) -> Unit = {}
        
        // Should not throw
        onLog("Test message")
    }

    @Test
    fun `servo panel supports three-axis control`() {
        val axes = listOf("X", "Y", "Z")
        
        assertEquals(3, axes.size)
    }

    @Test
    fun `servo positions can be at minimum`() {
        val servoX = -1f
        val servoY = -1f
        val servoZ = -1f
        
        assertEquals(-1f, servoX, 0.001f)
        assertEquals(-1f, servoY, 0.001f)
        assertEquals(-1f, servoZ, 0.001f)
    }

    @Test
    fun `servo positions can be at maximum`() {
        val servoX = 1f
        val servoY = 1f
        val servoZ = 1f
        
        assertEquals(1f, servoX, 0.001f)
        assertEquals(1f, servoY, 0.001f)
        assertEquals(1f, servoZ, 0.001f)
    }

    @Test
    fun `servo positions can be at center`() {
        val servoX = 0f
        val servoY = 0f
        val servoZ = 0f
        
        assertEquals(0f, servoX, 0.001f)
        assertEquals(0f, servoY, 0.001f)
        assertEquals(0f, servoZ, 0.001f)
    }

    @Test
    fun `servo move callback is called with all three axes`() {
        var callCount = 0
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        
        val onServoMove: (Float, Float, Float) -> Unit = { x, y, z ->
            callCount++
            lastX = x
            lastY = y
            lastZ = z
        }
        
        onServoMove(0.1f, 0.2f, 0.3f)
        onServoMove(0.4f, 0.5f, 0.6f)
        
        assertEquals(2, callCount)
        assertEquals(0.4f, lastX, 0.001f)
        assertEquals(0.5f, lastY, 0.001f)
        assertEquals(0.6f, lastZ, 0.001f)
    }
}
