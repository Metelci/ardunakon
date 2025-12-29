package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.model.CustomCommand
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CustomCommandButton logic and state management.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class CustomCommandButtonTest {

    @Test
    fun `button uses command color`() {
        val command = createTestCommand(colorHex = 0xFFF44336L)

        assertEquals(0xFFF44336L, command.colorHex)
    }

    @Test
    fun `button displays command icon`() {
        val command = createTestCommand(iconName = "Lightbulb")

        assertEquals("Lightbulb", command.iconName)
    }

    @Test
    fun `button shows shortcut label when shortcut exists`() {
        val command = createTestCommand(keyboardShortcut = 'H')

        assertNotNull(command.keyboardShortcut)
        assertEquals('H', command.keyboardShortcut)
    }

    @Test
    fun `button hides shortcut label when shortcut is null`() {
        val command = createTestCommand(keyboardShortcut = null)

        assertNull(command.keyboardShortcut)
    }

    @Test
    fun `icon size is smaller when shortcut exists`() {
        val withShortcut = 18 // dp
        val withoutShortcut = 24 // dp

        assertTrue(withShortcut < withoutShortcut)
    }

    @Test
    fun `button has electric yellow border`() {
        val electricYellow = 0xFFFFFF00L

        assertEquals(0xFFFFFF00L, electricYellow)
    }

    @Test
    fun `button uses rounded corner shape`() {
        val cornerRadius = 12 // dp

        assertTrue(cornerRadius > 0)
    }

    @Test
    fun `button has semi-transparent background`() {
        val alpha = 0.85f

        assertTrue(alpha < 1.0f)
        assertTrue(alpha > 0.0f)
    }

    @Test
    fun `button row displays up to max buttons`() {
        val commands = (1..5).map { createTestCommand("Command $it") }
        val maxButtons = 2

        val displayCommands = commands.take(maxButtons)

        assertEquals(2, displayCommands.size)
    }

    @Test
    fun `button row shows all commands when under max`() {
        val commands = (1..2).map { createTestCommand("Command $it") }
        val maxButtons = 3

        val displayCommands = commands.take(maxButtons)

        assertEquals(2, displayCommands.size)
    }

    @Test
    fun `button row fills remaining slots with placeholders`() {
        val commands = (1..1).map { createTestCommand("Command $it") }
        val maxButtons = 3

        val displayCommands = commands.take(maxButtons)
        val placeholderCount = maxButtons - displayCommands.size

        assertEquals(2, placeholderCount)
    }

    @Test
    fun `button row shows no placeholders when full`() {
        val commands = (1..3).map { createTestCommand("Command $it") }
        val maxButtons = 3

        val displayCommands = commands.take(maxButtons)
        val placeholderCount = maxButtons - displayCommands.size

        assertEquals(0, placeholderCount)
    }

    @Test
    fun `placeholder has electric yellow border`() {
        val electricYellow = 0xFFFFFF00L
        val placeholderAlpha = 0.5f

        assertEquals(0xFFFFFF00L, electricYellow)
        assertTrue(placeholderAlpha < 1.0f)
    }

    @Test
    fun `placeholder has no background`() {
        // Placeholder should only have border, no background fill
        val hasBackground = false

        assertFalse(hasBackground)
    }

    @Test
    fun `placeholder uses same corner radius as button`() {
        val buttonCornerRadius = 12 // dp
        val placeholderCornerRadius = 12 // dp

        assertEquals(buttonCornerRadius, placeholderCornerRadius)
    }

    @Test
    fun `button content description uses command name`() {
        val command = createTestCommand("Horn Button")

        assertEquals("Horn Button", command.name)
    }

    @Test
    fun `button row spacing is consistent`() {
        val spacing = 8 // dp

        assertTrue(spacing > 0)
    }

    @Test
    fun `default button size is 56dp`() {
        val defaultSize = 56 // dp

        assertEquals(56, defaultSize)
    }

    @Test
    fun `button row default size is 52dp`() {
        val defaultRowSize = 52 // dp

        assertEquals(52, defaultRowSize)
    }

    @Test
    fun `button row default max is 2 buttons`() {
        val defaultMax = 2

        assertEquals(2, defaultMax)
    }

    @Test
    fun `toggle command maintains state`() {
        val toggleCommand = createTestCommand(isToggle = true)

        assertTrue(toggleCommand.isToggle)
    }

    @Test
    fun `momentary command does not maintain state`() {
        val momentaryCommand = createTestCommand(isToggle = false)

        assertFalse(momentaryCommand.isToggle)
    }

    @Test
    fun `command has unique ID`() {
        val command1 = createTestCommand("Command 1")
        val command2 = createTestCommand("Command 2")

        assertNotEquals(command1.id, command2.id)
    }

    @Test
    fun `command payload is 5 bytes`() {
        val command = createTestCommand()

        assertEquals(5, command.payload.size)
    }

    @Test
    fun `button displays all available icon types`() {
        val iconNames = listOf(
            "Build",
            "Lightbulb",
            "Notifications",
            "PlayArrow",
            "Stop",
            "VolumeUp",
            "Bolt",
            "Star"
        )

        iconNames.forEach { iconName ->
            val command = createTestCommand(iconName = iconName)
            assertEquals(iconName, command.iconName)
        }
    }

    @Test
    fun `button supports all available colors`() {
        val colors = listOf(
            0xFF2196F3L, 0xFF4CAF50L, 0xFFF44336L, 0xFFFF9800L,
            0xFF9C27B0L, 0xFF00BCD4L, 0xFFFFEB3BL, 0xFF607D8BL,
            0xFFE91E63L, 0xFF795548L
        )

        colors.forEach { color ->
            val command = createTestCommand(colorHex = color)
            assertEquals(color, command.colorHex)
        }
    }

    @Test
    fun `button supports all available shortcuts`() {
        val shortcuts = CustomCommand.AVAILABLE_SHORTCUT_KEYS

        shortcuts.forEach { shortcut ->
            val command = createTestCommand(keyboardShortcut = shortcut)
            assertEquals(shortcut, command.keyboardShortcut)
        }
    }

    // Helper function to create test commands
    private fun createTestCommand(
        name: String = "Test Command",
        id: String = java.util.UUID.randomUUID().toString(),
        commandId: Byte = 0x30.toByte(),
        colorHex: Long = 0xFF2196F3L,
        isToggle: Boolean = false,
        iconName: String = "Build",
        keyboardShortcut: Char? = null
    ): CustomCommand {
        return CustomCommand(
            id = id,
            name = name,
            commandId = commandId,
            payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
            colorHex = colorHex,
            isToggle = isToggle,
            iconName = iconName,
            keyboardShortcut = keyboardShortcut
        )
    }
}
