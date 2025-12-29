package com.metelci.ardunakon.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Extended unit tests for CustomCommand data class.
 *
 * Adds comprehensive coverage for edge cases, boundary conditions,
 * and additional validation scenarios.
 */
class CustomCommandExtendedTest {

    // ==================== All Reserved Keys ====================

    @Test
    fun `all reserved keys are rejected as shortcuts`() {
        for (key in CustomCommand.RESERVED_KEYS) {
            assertFalse("Key $key should be rejected", CustomCommand.isValidShortcut(key))
        }
    }

    @Test
    fun `all reserved keys lowercase are also rejected`() {
        for (key in CustomCommand.RESERVED_KEYS) {
            val message = "Key ${key.lowercaseChar()} should be rejected"
            assertFalse(message, CustomCommand.isValidShortcut(key.lowercaseChar()))
        }
    }

    // ==================== All Available Keys ====================

    @Test
    fun `all available shortcut keys are accepted`() {
        for (key in CustomCommand.AVAILABLE_SHORTCUT_KEYS) {
            assertTrue("Key $key should be accepted", CustomCommand.isValidShortcut(key))
        }
    }

    @Test
    fun `available keys count is 18`() {
        // A-Z is 26, minus 8 reserved = 18
        assertEquals(18, CustomCommand.AVAILABLE_SHORTCUT_KEYS.size)
    }

    // ==================== Command ID Range ====================

    @Test
    fun `all command IDs in valid range are accepted`() {
        for (id in 0x20..0x3F) {
            assertTrue("ID $id should be valid", CustomCommand.isValidCommandId(id.toByte()))
        }
    }

    @Test
    fun `command ID just before range is rejected`() {
        assertFalse(CustomCommand.isValidCommandId(0x1F.toByte()))
    }

    @Test
    fun `command ID just after range is rejected`() {
        assertFalse(CustomCommand.isValidCommandId(0x40.toByte()))
    }

    @Test
    fun `valid command ID range has 32 values`() {
        // 0x20 to 0x3F inclusive = 32 values
        val validCount = (0x20..0x3F).count {
            CustomCommand.isValidCommandId(it.toByte())
        }
        assertEquals(32, validCount)
    }

    // ==================== Payload Validation ====================

    @Test
    fun `payload size 0 makes command invalid`() {
        val command = CustomCommand(
            name = "Test",
            commandId = 0x20.toByte(),
            payload = ByteArray(0)
        )
        assertFalse(command.isValid())
    }

    @Test
    fun `payload size 4 makes command invalid`() {
        val command = CustomCommand(
            name = "Test",
            commandId = 0x20.toByte(),
            payload = ByteArray(4)
        )
        assertFalse(command.isValid())
    }

    @Test
    fun `payload size 5 makes command valid`() {
        val command = CustomCommand(
            name = "Test",
            commandId = 0x20.toByte(),
            payload = ByteArray(5)
        )
        assertTrue(command.isValid())
    }

    @Test
    fun `payload size 6 makes command invalid`() {
        val command = CustomCommand(
            name = "Test",
            commandId = 0x20.toByte(),
            payload = ByteArray(6)
        )
        assertFalse(command.isValid())
    }

    // ==================== Copy Behavior ====================

    @Test
    fun `copy preserves original unchanged`() {
        val original = CustomCommand(
            id = "original",
            name = "Original",
            commandId = 0x20.toByte()
        )

        val copy = original.copy(name = "Modified")

        assertEquals("Original", original.name)
        assertEquals("Modified", copy.name)
        assertEquals("original", copy.id)
    }

    @Test
    fun `copy with new payload uses same reference unless cloned`() {
        val original = CustomCommand(
            name = "Test",
            commandId = 0x20.toByte(),
            payload = byteArrayOf(1, 2, 3, 4, 5)
        )

        // Create a copy of the payload before passing to copy()
        val newPayload = byteArrayOf(5, 4, 3, 2, 1)
        val copy = original.copy(payload = newPayload)

        // Verify copy has the new payload values
        assertEquals(5.toByte(), copy.payload[0])
        assertEquals(4.toByte(), copy.payload[1])

        // Note: data class copy() uses reference semantics for arrays
        // If we need independent copies, we should use payload.copyOf()
        val independentCopy = original.copy(payload = newPayload.copyOf())
        newPayload[0] = 99.toByte()

        // The independently copied version still has original values
        assertEquals(5.toByte(), independentCopy.payload[0])
    }

    // ==================== Color Values ====================

    @Test
    fun `colorHex accepts full range of colors`() {
        val command1 = CustomCommand(name = "Red", commandId = 0x20.toByte(), colorHex = 0xFFFF0000)
        val command2 = CustomCommand(name = "Blue", commandId = 0x20.toByte(), colorHex = 0xFF0000FF)
        val command3 = CustomCommand(name = "Transparent", commandId = 0x20.toByte(), colorHex = 0x00000000)

        assertEquals(0xFFFF0000, command1.colorHex)
        assertEquals(0xFF0000FF, command2.colorHex)
        assertEquals(0x00000000, command3.colorHex)
    }

    // ==================== Toggle Mode ====================

    @Test
    fun `toggle and momentary commands are distinct`() {
        val toggle = CustomCommand(name = "Toggle", commandId = 0x20.toByte(), isToggle = true)
        val momentary = CustomCommand(name = "Momentary", commandId = 0x20.toByte(), isToggle = false)

        assertTrue(toggle.isToggle)
        assertFalse(momentary.isToggle)
    }

    // ==================== Icon Names ====================

    @Test
    fun `iconName is stored correctly`() {
        val command = CustomCommand(name = "Test", commandId = 0x20.toByte(), iconName = "Star")
        assertEquals("Star", command.iconName)
    }

    @Test
    fun `empty iconName is allowed`() {
        val command = CustomCommand(name = "Test", commandId = 0x20.toByte(), iconName = "")
        assertEquals("", command.iconName)
    }

    // ==================== ID Generation ====================

    @Test
    fun `auto generated IDs are unique`() {
        val command1 = CustomCommand(name = "Test1", commandId = 0x20.toByte())
        val command2 = CustomCommand(name = "Test2", commandId = 0x20.toByte())

        assertNotEquals(command1.id, command2.id)
    }

    @Test
    fun `explicit ID is preserved`() {
        val command = CustomCommand(id = "my-custom-id", name = "Test", commandId = 0x20.toByte())
        assertEquals("my-custom-id", command.id)
    }

    // ==================== Edge Case Shortcuts ====================

    @Test
    fun `digit shortcuts are rejected`() {
        assertFalse(CustomCommand.isValidShortcut('0'))
        assertFalse(CustomCommand.isValidShortcut('5'))
        assertFalse(CustomCommand.isValidShortcut('9'))
    }

    @Test
    fun `special character shortcuts are rejected`() {
        assertFalse(CustomCommand.isValidShortcut('!'))
        assertFalse(CustomCommand.isValidShortcut('@'))
        assertFalse(CustomCommand.isValidShortcut(' '))
        assertFalse(CustomCommand.isValidShortcut('-'))
    }

    // ==================== Equality Edge Cases ====================

    @Test
    fun `equals with null returns false`() {
        val command = CustomCommand(name = "Test", commandId = 0x20.toByte())
        assertFalse(command == null)
    }

    @Test
    fun `equals with different type returns false`() {
        val command = CustomCommand(name = "Test", commandId = 0x20.toByte())
        assertFalse(command.equals("not a command"))
    }

    @Test
    fun `equals with same instance returns true`() {
        val command = CustomCommand(name = "Test", commandId = 0x20.toByte())
        assertTrue(command.equals(command))
    }

    // ==================== Keyboard Shortcut Case Sensitivity ====================

    @Test
    fun `uppercase and lowercase shortcuts for non-reserved are both valid`() {
        assertTrue(CustomCommand.isValidShortcut('C'))
        assertTrue(CustomCommand.isValidShortcut('c'))
        assertTrue(CustomCommand.isValidShortcut('T'))
        assertTrue(CustomCommand.isValidShortcut('t'))
    }
}
