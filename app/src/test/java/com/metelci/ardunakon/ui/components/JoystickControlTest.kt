package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JoystickState data class.
 * Note: Compose rendering tests require instrumented test environment.
 */
class JoystickControlTest {

    @Test
    fun `joystick state stores x and y correctly`() {
        val state = JoystickState(x = 0.5f, y = -0.5f)
        assertEquals(0.5f, state.x, 0.0001f)
        assertEquals(-0.5f, state.y, 0.0001f)
    }

    @Test
    fun `joystick state at center is zero`() {
        val state = JoystickState(x = 0f, y = 0f)
        assertEquals(0f, state.x, 0.0001f)
        assertEquals(0f, state.y, 0.0001f)
    }

    @Test
    fun `joystick state at max positive`() {
        val state = JoystickState(x = 1f, y = 1f)
        assertEquals(1f, state.x, 0.0001f)
        assertEquals(1f, state.y, 0.0001f)
    }

    @Test
    fun `joystick state at max negative`() {
        val state = JoystickState(x = -1f, y = -1f)
        assertEquals(-1f, state.x, 0.0001f)
        assertEquals(-1f, state.y, 0.0001f)
    }

    @Test
    fun `joystick states with same values are equal`() {
        val state1 = JoystickState(x = 0.5f, y = 0.3f)
        val state2 = JoystickState(x = 0.5f, y = 0.3f)
        assertEquals(state1, state2)
    }

    @Test
    fun `joystick states with different values are not equal`() {
        val state1 = JoystickState(x = 0.5f, y = 0.3f)
        val state2 = JoystickState(x = 0.5f, y = 0.4f)
        assertNotEquals(state1, state2)
    }
}
