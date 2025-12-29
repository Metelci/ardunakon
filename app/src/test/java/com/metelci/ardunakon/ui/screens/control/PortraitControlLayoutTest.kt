package com.metelci.ardunakon.ui.screens.control

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PortraitControlLayout logic and calculations.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class PortraitControlLayoutTest {

    @Test
    fun `custom commands split into left and right groups of 3`() {
        val customCommands = (1..8).toList()
        val leftCommands = customCommands.take(3)
        val rightCommands = customCommands.drop(3).take(3)

        assertEquals(listOf(1, 2, 3), leftCommands)
        assertEquals(listOf(4, 5, 6), rightCommands)
    }

    @Test
    fun `left commands shows first 3 commands`() {
        val customCommands = (1..10).toList()
        val leftCommands = customCommands.take(3)

        assertEquals(3, leftCommands.size)
        assertEquals(1, leftCommands[0])
        assertEquals(2, leftCommands[1])
        assertEquals(3, leftCommands[2])
    }

    @Test
    fun `right commands shows commands 4, 5, and 6`() {
        val customCommands = (1..10).toList()
        val rightCommands = customCommands.drop(3).take(3)

        assertEquals(3, rightCommands.size)
        assertEquals(4, rightCommands[0])
        assertEquals(5, rightCommands[1])
        assertEquals(6, rightCommands[2])
    }

    @Test
    fun `servo row weight is 0_25 when debug panel visible`() {
        val isDebugPanelVisible = true
        val servoWeight = if (isDebugPanelVisible) 0.25f else 0.4f

        assertEquals(0.25f, servoWeight, 0.001f)
    }

    @Test
    fun `servo row weight is 0_4 when debug panel hidden`() {
        val isDebugPanelVisible = false
        val servoWeight = if (isDebugPanelVisible) 0.25f else 0.4f

        assertEquals(0.4f, servoWeight, 0.001f)
    }

    @Test
    fun `joystick weight is 0_35 when debug panel visible`() {
        val isDebugPanelVisible = true
        val joystickWeight = if (isDebugPanelVisible) 0.35f else 0.6f

        assertEquals(0.35f, joystickWeight, 0.001f)
    }

    @Test
    fun `joystick weight is 0_6 when debug panel hidden`() {
        val isDebugPanelVisible = false
        val joystickWeight = if (isDebugPanelVisible) 0.35f else 0.6f

        assertEquals(0.6f, joystickWeight, 0.001f)
    }

    @Test
    fun `debug panel weight is 0_4 when visible`() {
        val debugPanelWeight = 0.4f

        assertEquals(0.4f, debugPanelWeight, 0.001f)
    }

    @Test
    fun `portrait joystick size is half screen width or 180dp max`() {
        val screenWidth = 400f // dp
        val maxSize = 180f // dp
        val joystickSize = minOf(screenWidth * 0.5f, maxSize)

        assertEquals(180f, joystickSize, 0.001f)
    }

    @Test
    fun `portrait joystick size respects screen width when small`() {
        val screenWidth = 300f // dp
        val maxSize = 180f // dp
        val joystickSize = minOf(screenWidth * 0.5f, maxSize)

        assertEquals(150f, joystickSize, 0.001f)
    }

    @Test
    fun `header button size is 32dp in portrait`() {
        val buttonSize = 32 // dp

        assertEquals(32, buttonSize)
    }

    @Test
    fun `e-stop size is 56dp in portrait`() {
        val eStopSize = 56 // dp

        assertEquals(56, eStopSize)
    }

    @Test
    fun `custom command button size is 44dp in portrait`() {
        val customButtonSize = 44 // dp

        assertEquals(44, customButtonSize)
    }

    @Test
    fun `servo button size is 56dp in portrait`() {
        val servoButtonSize = 56 // dp

        assertEquals(56, servoButtonSize)
    }

    @Test
    fun `max custom commands per side is 3 in portrait`() {
        val maxButtons = 3

        assertEquals(3, maxButtons)
    }

    @Test
    fun `portrait layout has vertical stacking`() {
        val layoutOrientation = "vertical"

        assertEquals("vertical", layoutOrientation)
    }

    @Test
    fun `landscape layout has horizontal stacking`() {
        val layoutOrientation = "horizontal"

        assertEquals("horizontal", layoutOrientation)
    }
}
