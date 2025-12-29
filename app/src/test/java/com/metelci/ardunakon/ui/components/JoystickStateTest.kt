package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JoystickState data class.
 *
 * The JoystickControl composable itself requires Compose UI testing,
 * but we can test the JoystickState data class behavior.
 */
class JoystickStateTest {

    // ==================== State Creation Flow ====================

    @Test
    fun `JoystickState with zero values`() {
        val state = JoystickState(x = 0f, y = 0f)

        assertEquals(0f, state.x, 0.001f)
        assertEquals(0f, state.y, 0.001f)
    }

    @Test
    fun `JoystickState with positive values`() {
        val state = JoystickState(x = 0.5f, y = 0.75f)

        assertEquals(0.5f, state.x, 0.001f)
        assertEquals(0.75f, state.y, 0.001f)
    }

    @Test
    fun `JoystickState with negative values`() {
        val state = JoystickState(x = -0.5f, y = -0.75f)

        assertEquals(-0.5f, state.x, 0.001f)
        assertEquals(-0.75f, state.y, 0.001f)
    }

    @Test
    fun `JoystickState with max values`() {
        val state = JoystickState(x = 1f, y = 1f)

        assertEquals(1f, state.x, 0.001f)
        assertEquals(1f, state.y, 0.001f)
    }

    @Test
    fun `JoystickState with min values`() {
        val state = JoystickState(x = -1f, y = -1f)

        assertEquals(-1f, state.x, 0.001f)
        assertEquals(-1f, state.y, 0.001f)
    }

    // ==================== Equality Flow ====================

    @Test
    fun `JoystickState equality`() {
        val state1 = JoystickState(x = 0.5f, y = 0.5f)
        val state2 = JoystickState(x = 0.5f, y = 0.5f)

        assertEquals(state1, state2)
    }

    @Test
    fun `JoystickState inequality`() {
        val state1 = JoystickState(x = 0.5f, y = 0.5f)
        val state2 = JoystickState(x = 0.6f, y = 0.5f)

        assertNotEquals(state1, state2)
    }

    // ==================== Copy Flow ====================

    @Test
    fun `JoystickState copy with modified x`() {
        val original = JoystickState(x = 0.5f, y = 0.5f)
        val copied = original.copy(x = 0.8f)

        assertEquals(0.8f, copied.x, 0.001f)
        assertEquals(0.5f, copied.y, 0.001f)
        // Original unchanged
        assertEquals(0.5f, original.x, 0.001f)
    }

    @Test
    fun `JoystickState copy with modified y`() {
        val original = JoystickState(x = 0.5f, y = 0.5f)
        val copied = original.copy(y = 0.8f)

        assertEquals(0.5f, copied.x, 0.001f)
        assertEquals(0.8f, copied.y, 0.001f)
    }

    // ==================== Hash Code Flow ====================

    @Test
    fun `equal JoystickStates have same hashCode`() {
        val state1 = JoystickState(x = 0.5f, y = 0.5f)
        val state2 = JoystickState(x = 0.5f, y = 0.5f)

        assertEquals(state1.hashCode(), state2.hashCode())
    }

    // ==================== ToString Flow ====================

    @Test
    fun `JoystickState toString contains values`() {
        val state = JoystickState(x = 0.5f, y = 0.75f)

        val str = state.toString()
        assertTrue(str.contains("0.5"))
        assertTrue(str.contains("0.75"))
    }

    // ==================== Destructuring Flow ====================

    @Test
    fun `JoystickState destructuring`() {
        val state = JoystickState(x = 0.3f, y = 0.4f)

        val (x, y) = state

        assertEquals(0.3f, x, 0.001f)
        assertEquals(0.4f, y, 0.001f)
    }

    // ==================== Magnitude Calculation ====================

    @Test
    fun `center position has zero magnitude`() {
        val state = JoystickState(x = 0f, y = 0f)

        val magnitude = kotlin.math.sqrt(state.x * state.x + state.y * state.y)

        assertEquals(0f, magnitude, 0.001f)
    }

    @Test
    fun `edge position has magnitude of 1`() {
        val state = JoystickState(x = 1f, y = 0f)

        val magnitude = kotlin.math.sqrt(state.x * state.x + state.y * state.y)

        assertEquals(1f, magnitude, 0.001f)
    }

    @Test
    fun `diagonal position has correct magnitude`() {
        // At 45 degrees with magnitude 1, x and y should both be ~0.707
        val value = 1f / kotlin.math.sqrt(2f)
        val state = JoystickState(x = value, y = value)

        val magnitude = kotlin.math.sqrt(state.x * state.x + state.y * state.y)

        assertEquals(1f, magnitude, 0.01f)
    }

    // ==================== Angle Calculation ====================

    @Test
    fun `right direction has 0 degree angle`() {
        val state = JoystickState(x = 1f, y = 0f)

        val angle = kotlin.math.atan2(state.y.toDouble(), state.x.toDouble())

        assertEquals(0.0, angle, 0.01)
    }

    @Test
    fun `up direction has 90 degree angle`() {
        val state = JoystickState(x = 0f, y = -1f) // Negative Y is up in screen coords

        val angle = kotlin.math.atan2(state.y.toDouble(), state.x.toDouble())

        assertEquals(-Math.PI / 2, angle, 0.01)
    }

    @Test
    fun `left direction has 180 degree angle`() {
        val state = JoystickState(x = -1f, y = 0f)

        val angle = kotlin.math.atan2(state.y.toDouble(), state.x.toDouble())

        assertEquals(Math.PI, kotlin.math.abs(angle), 0.01)
    }
}
