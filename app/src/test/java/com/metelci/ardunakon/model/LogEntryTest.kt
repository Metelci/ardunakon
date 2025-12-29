package com.metelci.ardunakon.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LogEntry and LogType.
 */
class LogEntryTest {

    // ==================== LogType Enum ====================

    @Test
    fun `LogType has 4 values`() {
        assertEquals(4, LogType.entries.size)
    }

    @Test
    fun `LogType INFO exists`() {
        assertNotNull(LogType.INFO)
    }

    @Test
    fun `LogType SUCCESS exists`() {
        assertNotNull(LogType.SUCCESS)
    }

    @Test
    fun `LogType ERROR exists`() {
        assertNotNull(LogType.ERROR)
    }

    @Test
    fun `LogType WARNING exists`() {
        assertNotNull(LogType.WARNING)
    }

    // ==================== LogEntry Creation ====================

    @Test
    fun `LogEntry with explicit timestamp`() {
        val entry = LogEntry(timestamp = 12345L, type = LogType.INFO, message = "Test")

        assertEquals(12345L, entry.timestamp)
        assertEquals(LogType.INFO, entry.type)
        assertEquals("Test", entry.message)
    }

    @Test
    fun `LogEntry with default timestamp is non-zero`() {
        val entry = LogEntry(type = LogType.INFO, message = "Test")

        assertTrue(entry.timestamp > 0)
    }

    @Test
    fun `LogEntry with empty message`() {
        val entry = LogEntry(type = LogType.WARNING, message = "")

        assertEquals("", entry.message)
    }

    @Test
    fun `LogEntry with long message`() {
        val longMessage = "A".repeat(1000)
        val entry = LogEntry(type = LogType.ERROR, message = longMessage)

        assertEquals(1000, entry.message.length)
    }

    // ==================== LogEntry Equality ====================

    @Test
    fun `equal LogEntries are equal`() {
        val entry1 = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test")
        val entry2 = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test")

        assertEquals(entry1, entry2)
    }

    @Test
    fun `different timestamps are not equal`() {
        val entry1 = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test")
        val entry2 = LogEntry(timestamp = 200L, type = LogType.INFO, message = "Test")

        assertNotEquals(entry1, entry2)
    }

    @Test
    fun `different types are not equal`() {
        val entry1 = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test")
        val entry2 = LogEntry(timestamp = 100L, type = LogType.ERROR, message = "Test")

        assertNotEquals(entry1, entry2)
    }

    @Test
    fun `different messages are not equal`() {
        val entry1 = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test1")
        val entry2 = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test2")

        assertNotEquals(entry1, entry2)
    }

    // ==================== LogEntry Copy ====================

    @Test
    fun `copy preserves values`() {
        val original = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test")
        val copy = original.copy()

        assertEquals(original, copy)
    }

    @Test
    fun `copy with modified message`() {
        val original = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Original")
        val modified = original.copy(message = "Modified")

        assertEquals("Modified", modified.message)
        assertEquals("Original", original.message)
    }

    @Test
    fun `copy with modified type`() {
        val original = LogEntry(timestamp = 100L, type = LogType.INFO, message = "Test")
        val modified = original.copy(type = LogType.ERROR)

        assertEquals(LogType.ERROR, modified.type)
        assertEquals(LogType.INFO, original.type)
    }

    // ==================== LogType name() ====================

    @Test
    fun `LogType name is correct`() {
        assertEquals("INFO", LogType.INFO.name)
        assertEquals("SUCCESS", LogType.SUCCESS.name)
        assertEquals("ERROR", LogType.ERROR.name)
        assertEquals("WARNING", LogType.WARNING.name)
    }

    // ==================== LogEntry toString ====================

    @Test
    fun `LogEntry toString contains message`() {
        val entry = LogEntry(type = LogType.INFO, message = "Test message")

        val str = entry.toString()
        assertTrue(str.contains("Test message"))
    }

    @Test
    fun `LogEntry toString contains type`() {
        val entry = LogEntry(type = LogType.ERROR, message = "Test")

        val str = entry.toString()
        assertTrue(str.contains("ERROR"))
    }
}
