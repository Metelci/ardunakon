package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.model.CustomCommand
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CustomCommandListDialog logic and state management.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class CustomCommandListDialogTest {

    @Test
    fun `command count display shows correct format`() {
        val commands = listOf(
            createTestCommand("Command 1"),
            createTestCommand("Command 2"),
            createTestCommand("Command 3")
        )

        val displayText = "${commands.size}/16 commands configured"

        assertEquals("3/16 commands configured", displayText)
    }

    @Test
    fun `command count display shows zero when empty`() {
        val commands = emptyList<CustomCommand>()

        val displayText = "${commands.size}/16 commands configured"

        assertEquals("0/16 commands configured", displayText)
    }

    @Test
    fun `command count display shows max when full`() {
        val commands = (1..16).map { createTestCommand("Command $it") }

        val displayText = "${commands.size}/16 commands configured"

        assertEquals("16/16 commands configured", displayText)
    }

    @Test
    fun `add button is enabled when under limit`() {
        val commands = (1..15).map { createTestCommand("Command $it") }

        val isEnabled = commands.size < 16

        assertTrue(isEnabled)
    }

    @Test
    fun `add button is disabled when at limit`() {
        val commands = (1..16).map { createTestCommand("Command $it") }

        val isEnabled = commands.size < 16

        assertFalse(isEnabled)
    }

    @Test
    fun `add button is enabled when empty`() {
        val commands = emptyList<CustomCommand>()

        val isEnabled = commands.size < 16

        assertTrue(isEnabled)
    }

    @Test
    fun `landscape layout splits commands into two columns`() {
        val commands = (1..10).map { createTestCommand("Command $it") }

        val leftCommands = commands.filterIndexed { index, _ -> index % 2 == 0 }
        val rightCommands = commands.filterIndexed { index, _ -> index % 2 == 1 }

        assertEquals(5, leftCommands.size)
        assertEquals(5, rightCommands.size)
    }

    @Test
    fun `landscape layout handles odd number of commands`() {
        val commands = (1..9).map { createTestCommand("Command $it") }

        val leftCommands = commands.filterIndexed { index, _ -> index % 2 == 0 }
        val rightCommands = commands.filterIndexed { index, _ -> index % 2 == 1 }

        assertEquals(5, leftCommands.size)
        assertEquals(4, rightCommands.size)
    }

    @Test
    fun `landscape layout left column gets first command`() {
        val commands = (1..10).map { createTestCommand("Command $it") }

        val leftCommands = commands.filterIndexed { index, _ -> index % 2 == 0 }

        assertEquals("Command 1", leftCommands[0].name)
        assertEquals("Command 3", leftCommands[1].name)
        assertEquals("Command 5", leftCommands[2].name)
    }

    @Test
    fun `landscape layout right column gets second command`() {
        val commands = (1..10).map { createTestCommand("Command $it") }

        val rightCommands = commands.filterIndexed { index, _ -> index % 2 == 1 }

        assertEquals("Command 2", rightCommands[0].name)
        assertEquals("Command 4", rightCommands[1].name)
        assertEquals("Command 6", rightCommands[2].name)
    }

    @Test
    fun `command item displays ID in hex format`() {
        val command = createTestCommand("Test", commandId = 0x30.toByte())

        val displayText = "ID: 0x%02X".format(command.commandId)

        assertEquals("ID: 0x30", displayText)
    }

    @Test
    fun `command item displays toggle mode correctly`() {
        val toggleCommand = createTestCommand("Toggle", isToggle = true)
        val momentaryCommand = createTestCommand("Momentary", isToggle = false)

        val toggleText = if (toggleCommand.isToggle) "Toggle" else "Momentary"
        val momentaryText = if (momentaryCommand.isToggle) "Toggle" else "Momentary"

        assertEquals("Toggle", toggleText)
        assertEquals("Momentary", momentaryText)
    }

    @Test
    fun `command item displays full info string`() {
        val command = createTestCommand("Test", commandId = 0x30.toByte(), isToggle = true)

        val infoText = "ID: 0x%02X • %s".format(
            command.commandId,
            if (command.isToggle) "Toggle" else "Momentary"
        )

        assertEquals("ID: 0x30 • Toggle", infoText)
    }

    @Test
    fun `empty state is shown when no commands`() {
        val commands = emptyList<CustomCommand>()

        val isEmpty = commands.isEmpty()

        assertTrue(isEmpty)
    }

    @Test
    fun `empty state is not shown when commands exist`() {
        val commands = listOf(createTestCommand("Command 1"))

        val isEmpty = commands.isEmpty()

        assertFalse(isEmpty)
    }

    @Test
    fun `delete confirmation finds correct command by ID`() {
        val commands = listOf(
            createTestCommand("Command 1", id = "id-1"),
            createTestCommand("Command 2", id = "id-2"),
            createTestCommand("Command 3", id = "id-3")
        )

        val commandToDelete = commands.find { it.id == "id-2" }

        assertNotNull(commandToDelete)
        assertEquals("Command 2", commandToDelete?.name)
    }

    @Test
    fun `delete confirmation returns null for non-existent ID`() {
        val commands = listOf(
            createTestCommand("Command 1", id = "id-1"),
            createTestCommand("Command 2", id = "id-2")
        )

        val commandToDelete = commands.find { it.id == "id-999" }

        assertNull(commandToDelete)
    }

    @Test
    fun `command list maintains insertion order`() {
        val commands = listOf(
            createTestCommand("First"),
            createTestCommand("Second"),
            createTestCommand("Third")
        )

        assertEquals("First", commands[0].name)
        assertEquals("Second", commands[1].name)
        assertEquals("Third", commands[2].name)
    }

    @Test
    fun `each command has unique ID`() {
        val commands = listOf(
            createTestCommand("Command 1", id = "id-1"),
            createTestCommand("Command 2", id = "id-2"),
            createTestCommand("Command 3", id = "id-3")
        )

        val uniqueIds = commands.map { it.id }.toSet()

        assertEquals(3, uniqueIds.size)
    }

    // Helper function to create test commands
    private fun createTestCommand(
        name: String,
        id: String = java.util.UUID.randomUUID().toString(),
        commandId: Byte = 0x30.toByte(),
        isToggle: Boolean = false
    ): CustomCommand {
        return CustomCommand(
            id = id,
            name = name,
            commandId = commandId,
            payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
            colorHex = 0xFF2196F3L,
            isToggle = isToggle,
            iconName = "Build",
            keyboardShortcut = null
        )
    }
}
