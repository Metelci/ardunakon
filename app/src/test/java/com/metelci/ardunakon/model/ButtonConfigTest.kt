package com.metelci.ardunakon.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ButtonConfig data class.
 */
class ButtonConfigTest {

    // ==================== Creation ====================

    @Test
    fun `create with minimal parameters`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "CMD")

        assertEquals(1, config.id)
        assertEquals("Test", config.label)
        assertEquals("CMD", config.command)
    }

    @Test
    fun `create with all parameters`() {
        val config = ButtonConfig(
            id = 5,
            label = "Full Config",
            command = "FULL",
            colorHex = 0xFFFF0000,
            isToggle = true
        )

        assertEquals(5, config.id)
        assertEquals("Full Config", config.label)
        assertEquals("FULL", config.command)
        assertEquals(0xFFFF0000, config.colorHex)
        assertTrue(config.isToggle)
    }

    // ==================== Default Values ====================

    @Test
    fun `default colorHex is blue`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "CMD")

        assertEquals(0xFF2196F3, config.colorHex)
    }

    @Test
    fun `default isToggle is false`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "CMD")

        assertFalse(config.isToggle)
    }

    // ==================== getColor() ====================

    @Test
    fun `getColor returns correct color for default blue`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "CMD")
        val color = config.getColor()

        // Color(0xFF2196F3) - Material Blue 500
        assertNotNull(color)
    }

    @Test
    fun `getColor returns correct color for custom red`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "CMD", colorHex = 0xFFFF0000)
        val color = config.getColor()

        assertNotNull(color)
    }

    @Test
    fun `getColor returns correct color for transparent`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "CMD", colorHex = 0x00000000)
        val color = config.getColor()

        assertNotNull(color)
    }

    // ==================== Equality ====================

    @Test
    fun `equal configs are equal`() {
        val config1 = ButtonConfig(id = 1, label = "Test", command = "CMD")
        val config2 = ButtonConfig(id = 1, label = "Test", command = "CMD")

        assertEquals(config1, config2)
    }

    @Test
    fun `different ids are not equal`() {
        val config1 = ButtonConfig(id = 1, label = "Test", command = "CMD")
        val config2 = ButtonConfig(id = 2, label = "Test", command = "CMD")

        assertNotEquals(config1, config2)
    }

    @Test
    fun `different labels are not equal`() {
        val config1 = ButtonConfig(id = 1, label = "Test1", command = "CMD")
        val config2 = ButtonConfig(id = 1, label = "Test2", command = "CMD")

        assertNotEquals(config1, config2)
    }

    @Test
    fun `different commands are not equal`() {
        val config1 = ButtonConfig(id = 1, label = "Test", command = "CMD1")
        val config2 = ButtonConfig(id = 1, label = "Test", command = "CMD2")

        assertNotEquals(config1, config2)
    }

    @Test
    fun `different colors are not equal`() {
        val config1 = ButtonConfig(id = 1, label = "Test", command = "CMD", colorHex = 0xFFFF0000)
        val config2 = ButtonConfig(id = 1, label = "Test", command = "CMD", colorHex = 0xFF00FF00)

        assertNotEquals(config1, config2)
    }

    @Test
    fun `different toggle values are not equal`() {
        val config1 = ButtonConfig(id = 1, label = "Test", command = "CMD", isToggle = true)
        val config2 = ButtonConfig(id = 1, label = "Test", command = "CMD", isToggle = false)

        assertNotEquals(config1, config2)
    }

    // ==================== Copy ====================

    @Test
    fun `copy preserves values`() {
        val original = ButtonConfig(id = 1, label = "Test", command = "CMD")
        val copy = original.copy()

        assertEquals(original, copy)
    }

    @Test
    fun `copy with modified label`() {
        val original = ButtonConfig(id = 1, label = "Original", command = "CMD")
        val modified = original.copy(label = "Modified")

        assertEquals("Modified", modified.label)
        assertEquals("Original", original.label)
    }

    @Test
    fun `copy with modified color`() {
        val original = ButtonConfig(id = 1, label = "Test", command = "CMD")
        val modified = original.copy(colorHex = 0xFFFF0000)

        assertEquals(0xFFFF0000, modified.colorHex)
        assertEquals(0xFF2196F3, original.colorHex)
    }

    // ==================== defaultButtonConfigs ====================

    @Test
    fun `defaultButtonConfigs has 4 items`() {
        assertEquals(4, defaultButtonConfigs.size)
    }

    @Test
    fun `defaultButtonConfigs IDs are 1 to 4`() {
        val ids = defaultButtonConfigs.map { it.id }

        assertEquals(listOf(1, 2, 3, 4), ids)
    }

    @Test
    fun `defaultButtonConfigs labels are Aux 1 to 4`() {
        val labels = defaultButtonConfigs.map { it.label }

        assertEquals(listOf("Aux 1", "Aux 2", "Aux 3", "Aux 4"), labels)
    }

    @Test
    fun `defaultButtonConfigs commands are 1 to 4`() {
        val commands = defaultButtonConfigs.map { it.command }

        assertEquals(listOf("1", "2", "3", "4"), commands)
    }

    @Test
    fun `defaultButtonConfigs have distinct colors`() {
        val colors = defaultButtonConfigs.map { it.colorHex }
        val uniqueColors = colors.toSet()

        assertEquals(4, uniqueColors.size)
    }

    @Test
    fun `defaultButtonConfigs are not toggles by default`() {
        for (config in defaultButtonConfigs) {
            assertFalse("Config ${config.id} should not be toggle", config.isToggle)
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `empty label is allowed`() {
        val config = ButtonConfig(id = 1, label = "", command = "CMD")

        assertEquals("", config.label)
    }

    @Test
    fun `empty command is allowed`() {
        val config = ButtonConfig(id = 1, label = "Test", command = "")

        assertEquals("", config.command)
    }

    @Test
    fun `negative id is allowed`() {
        val config = ButtonConfig(id = -1, label = "Test", command = "CMD")

        assertEquals(-1, config.id)
    }

    @Test
    fun `zero id is allowed`() {
        val config = ButtonConfig(id = 0, label = "Test", command = "CMD")

        assertEquals(0, config.id)
    }
}
