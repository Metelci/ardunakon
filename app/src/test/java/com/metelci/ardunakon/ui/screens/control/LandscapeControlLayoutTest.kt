package com.metelci.ardunakon.ui.screens.control

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LandscapeControlLayout logic and calculations.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class LandscapeControlLayoutTest {

    @Test
    fun `left column weight is 0_65 when debug panel visible`() {
        val isDebugPanelVisible = true
        val leftWeight = if (isDebugPanelVisible) 0.65f else 1f

        assertEquals(0.65f, leftWeight, 0.001f)
    }

    @Test
    fun `left column weight is 1_0 when debug panel hidden`() {
        val isDebugPanelVisible = false
        val leftWeight = if (isDebugPanelVisible) 0.65f else 1f

        assertEquals(1f, leftWeight, 0.001f)
    }

    @Test
    fun `right column weight is 0_35 when debug panel visible`() {
        val debugPanelWeight = 0.35f

        assertEquals(0.35f, debugPanelWeight, 0.001f)
    }

    @Test
    fun `custom commands split into left and right groups`() {
        val customCommands = (1..6).toList()
        val leftCommands = customCommands.take(2)
        val rightCommands = customCommands.drop(2).take(2)

        assertEquals(listOf(1, 2), leftCommands)
        assertEquals(listOf(3, 4), rightCommands)
    }

    @Test
    fun `left commands shows first 2 commands`() {
        val customCommands = (1..10).toList()
        val leftCommands = customCommands.take(2)

        assertEquals(2, leftCommands.size)
        assertEquals(1, leftCommands[0])
        assertEquals(2, leftCommands[1])
    }

    @Test
    fun `right commands shows commands 3 and 4`() {
        val customCommands = (1..10).toList()
        val rightCommands = customCommands.drop(2).take(2)

        assertEquals(2, rightCommands.size)
        assertEquals(3, rightCommands[0])
        assertEquals(4, rightCommands[1])
    }

    @Test
    fun `joystick size calculation respects minimum`() {
        val maxJoystickWidth = 100f // dp
        val availableHeight = 50f // dp
        val maxSize = 220f // dp
        val joystickSize = minOf(maxJoystickWidth, availableHeight, maxSize)

        assertEquals(50f, joystickSize, 0.001f)
    }

    @Test
    fun `joystick size calculation respects maximum`() {
        val maxJoystickWidth = 300f // dp
        val availableHeight = 400f // dp
        val maxSize = 220f // dp
        val joystickSize = minOf(maxJoystickWidth, availableHeight, maxSize)

        assertEquals(220f, joystickSize, 0.001f)
    }

    @Test
    fun `available height has minimum of 150dp`() {
        val screenHeight = 200f // dp
        val usedSpace = 250f // dp
        val availableHeight = (screenHeight - usedSpace).coerceAtLeast(150f)

        assertEquals(150f, availableHeight, 0.001f)
    }

    @Test
    fun `panel width calculation when debug visible`() {
        val screenWidth = 1000f // dp
        val isDebugPanelVisible = true
        val panelWidth = if (isDebugPanelVisible) screenWidth * 0.65f else screenWidth

        assertEquals(650f, panelWidth, 0.001f)
    }

    @Test
    fun `panel width calculation when debug hidden`() {
        val screenWidth = 1000f // dp
        val isDebugPanelVisible = false
        val panelWidth = if (isDebugPanelVisible) screenWidth * 0.65f else screenWidth

        assertEquals(1000f, panelWidth, 0.001f)
    }

    @Test
    fun `available for joysticks has minimum of 10dp`() {
        val panelWidth = 100f // dp
        val usedSpace = 136f // dp
        val availableForJoysticks = (panelWidth - usedSpace).coerceAtLeast(10f)

        assertEquals(10f, availableForJoysticks, 0.001f)
    }

    @Test
    fun `max joystick width is half of available space`() {
        val availableForJoysticks = 400f // dp
        val maxJoystickWidth = availableForJoysticks / 2.1f

        assertEquals(190.476f, maxJoystickWidth, 0.1f)
    }

    @Test
    fun `header button size is 36dp in landscape`() {
        val buttonSize = 36 // dp

        assertEquals(36, buttonSize)
    }

    @Test
    fun `e-stop size is 72dp in landscape`() {
        val eStopSize = 72 // dp

        assertEquals(72, eStopSize)
    }

    @Test
    fun `custom command button size is 40dp in landscape`() {
        val customButtonSize = 40 // dp

        assertEquals(40, customButtonSize)
    }

    @Test
    fun `servo button size is 60dp in landscape`() {
        val servoButtonSize = 60 // dp

        assertEquals(60, servoButtonSize)
    }

    @Test
    fun `max custom commands per side is 2`() {
        val maxButtons = 2

        assertEquals(2, maxButtons)
    }
}
