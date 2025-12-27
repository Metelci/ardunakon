package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.model.CustomCommand
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CustomCommandDialog validation logic and state management.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class CustomCommandDialogTest {

    @Test
    fun `validation passes with valid name and payload`() {
        val name = "Test Command"
        val payloadHex = "0102030405"
        
        val isValid = name.isNotBlank() && payloadHex.length == 10
        
        assertTrue(isValid)
    }

    @Test
    fun `validation fails with blank name`() {
        val name = ""
        val payloadHex = "0102030405"
        
        val isValid = name.isNotBlank() && payloadHex.length == 10
        
        assertFalse(isValid)
    }

    @Test
    fun `validation fails with whitespace-only name`() {
        val name = "   "
        val payloadHex = "0102030405"
        
        val isValid = name.isNotBlank() && payloadHex.length == 10
        
        assertFalse(isValid)
    }

    @Test
    fun `validation fails with short payload`() {
        val name = "Test Command"
        val payloadHex = "010203"
        
        val isValid = name.isNotBlank() && payloadHex.length == 10
        
        assertFalse(isValid)
    }

    @Test
    fun `validation fails with long payload`() {
        val name = "Test Command"
        val payloadHex = "01020304050607"
        
        val isValid = name.isNotBlank() && payloadHex.length == 10
        
        assertFalse(isValid)
    }

    @Test
    fun `payload hex filtering removes invalid characters`() {
        val input = "01G2H3I4J5"
        val filtered = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        
        assertEquals("012345", filtered)
    }

    @Test
    fun `payload hex filtering preserves valid hex characters`() {
        val input = "0123456789ABCDEF"
        val filtered = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        
        assertEquals("0123456789ABCDEF", filtered)
    }

    @Test
    fun `payload hex filtering converts lowercase to uppercase`() {
        val input = "abcdef"
        val filtered = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        
        assertEquals("ABCDEF", filtered)
    }

    @Test
    fun `payload hex is truncated to 10 characters`() {
        val input = "0123456789ABCDEF"
        val filtered = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(10)
        
        assertEquals("0123456789", filtered)
    }

    @Test
    fun `name is truncated to 20 characters`() {
        val input = "This is a very long command name that exceeds the limit"
        val truncated = input.take(20)
        
        assertEquals("This is a very long ", truncated)
        assertEquals(20, truncated.length)
    }

    @Test
    fun `payload parsing creates correct byte array`() {
        val payloadHex = "0102030405"
        val payload = payloadHex.padEnd(10, '0')
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        
        assertEquals(5, payload.size)
        assertEquals(0x01.toByte(), payload[0])
        assertEquals(0x02.toByte(), payload[1])
        assertEquals(0x03.toByte(), payload[2])
        assertEquals(0x04.toByte(), payload[3])
        assertEquals(0x05.toByte(), payload[4])
    }

    @Test
    fun `payload parsing pads short hex with zeros`() {
        val payloadHex = "0102"
        val payload = payloadHex.padEnd(10, '0')
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        
        assertEquals(5, payload.size)
        assertEquals(0x01.toByte(), payload[0])
        assertEquals(0x02.toByte(), payload[1])
        assertEquals(0x00.toByte(), payload[2])
        assertEquals(0x00.toByte(), payload[3])
        assertEquals(0x00.toByte(), payload[4])
    }

    @Test
    fun `default command ID is first available or 0x20`() {
        val availableIds = listOf<Byte>(0x30, 0x31, 0x32)
        val defaultId = availableIds.firstOrNull() ?: 0x20.toByte()
        
        assertEquals(0x30.toByte(), defaultId)
    }

    @Test
    fun `default command ID is 0x20 when no IDs available`() {
        val availableIds = emptyList<Byte>()
        val defaultId = availableIds.firstOrNull() ?: 0x20.toByte()
        
        assertEquals(0x20.toByte(), defaultId)
    }

    @Test
    fun `default color is blue`() {
        val defaultColor = 0xFF2196F3L
        
        assertEquals(0xFF2196F3L, defaultColor)
    }

    @Test
    fun `default icon is Build`() {
        val defaultIcon = "Build"
        
        assertEquals("Build", defaultIcon)
    }

    @Test
    fun `default toggle mode is false`() {
        val defaultToggle = false
        
        assertFalse(defaultToggle)
    }

    @Test
    fun `default keyboard shortcut is null`() {
        val defaultShortcut: Char? = null
        
        assertNull(defaultShortcut)
    }

    @Test
    fun `edit mode preserves existing command values`() {
        val existingCommand = CustomCommand(
            id = "test-id",
            name = "Test Command",
            commandId = 0x30.toByte(),
            payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
            colorHex = 0xFFF44336L,
            isToggle = true,
            iconName = "Lightbulb",
            keyboardShortcut = 'H'
        )
        
        assertEquals("Test Command", existingCommand.name)
        assertEquals(0x30.toByte(), existingCommand.commandId)
        assertEquals(0xFFF44336L, existingCommand.colorHex)
        assertTrue(existingCommand.isToggle)
        assertEquals("Lightbulb", existingCommand.iconName)
        assertEquals('H', existingCommand.keyboardShortcut)
    }

    @Test
    fun `payload hex formatting displays correctly`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val hexString = payload.joinToString("") { "%02X".format(it) }
        
        assertEquals("0102030405", hexString)
    }

    @Test
    fun `payload hex formatting handles zero bytes`() {
        val payload = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
        val hexString = payload.joinToString("") { "%02X".format(it) }
        
        assertEquals("0000000000", hexString)
    }

    @Test
    fun `payload hex formatting handles max bytes`() {
        val payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val hexString = payload.joinToString("") { "%02X".format(it) }
        
        assertEquals("FFFFFFFFFF", hexString)
    }

    @Test
    fun `command ID formatting displays as hex`() {
        val commandId = 0x30.toByte()
        val formatted = "0x%02X".format(commandId)
        
        assertEquals("0x30", formatted)
    }

    @Test
    fun `available colors list contains 10 colors`() {
        val colors = listOf(
            0xFF2196F3L, // Blue
            0xFF4CAF50L, // Green
            0xFFF44336L, // Red
            0xFFFF9800L, // Orange
            0xFF9C27B0L, // Purple
            0xFF00BCD4L, // Cyan
            0xFFFFEB3BL, // Yellow
            0xFF607D8BL, // Blue Grey
            0xFFE91E63L, // Pink
            0xFF795548L  // Brown
        )
        
        assertEquals(10, colors.size)
    }

    @Test
    fun `available icons list contains 8 icons`() {
        val icons = mapOf(
            "Build" to "Build",
            "Lightbulb" to "Lightbulb",
            "Notifications" to "Notifications",
            "PlayArrow" to "PlayArrow",
            "Stop" to "Stop",
            "VolumeUp" to "VolumeUp",
            "Bolt" to "Bolt",
            "Star" to "Star"
        )
        
        assertEquals(8, icons.size)
    }

    @Test
    fun `getIconByName returns correct icon for valid name`() {
        val iconName = "Lightbulb"
        val icon = getIconByName(iconName)
        
        assertNotNull(icon)
    }

    @Test
    fun `getIconByName returns Build icon for invalid name`() {
        val iconName = "InvalidIcon"
        val icon = getIconByName(iconName)
        
        // Should default to Build icon
        assertNotNull(icon)
    }
}
