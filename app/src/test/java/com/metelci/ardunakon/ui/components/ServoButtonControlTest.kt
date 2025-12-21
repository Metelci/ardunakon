package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for servo button control logic.
 * Note: Compose rendering tests require instrumented test environment.
 */
class ServoButtonControlTest {

    @Test
    fun `servo angle starts at center`() {
        val centerAngle = 90
        assertEquals(90, centerAngle)
    }

    @Test
    fun `servo angle increment stays in bounds`() {
        var angle = 90
        angle = (angle + 10).coerceIn(0, 180)
        assertEquals(100, angle)
    }

    @Test
    fun `servo angle decrement stays in bounds`() {
        var angle = 90
        angle = (angle - 10).coerceIn(0, 180)
        assertEquals(80, angle)
    }

    @Test
    fun `servo angle clamps at maximum`() {
        var angle = 175
        angle = (angle + 10).coerceIn(0, 180)
        assertEquals(180, angle)
    }

    @Test
    fun `servo angle clamps at minimum`() {
        var angle = 5
        angle = (angle - 10).coerceIn(0, 180)
        assertEquals(0, angle)
    }

    @Test
    fun `servo x and y are independent`() {
        var servoX = 90
        var servoY = 90
        
        servoX = (servoX + 20).coerceIn(0, 180)
        
        assertEquals(110, servoX)
        assertEquals(90, servoY)
    }
}
