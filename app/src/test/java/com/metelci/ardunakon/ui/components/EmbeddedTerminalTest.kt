package com.metelci.ardunakon.ui.components

import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UI component data types.
 * Note: Compose rendering tests are skipped as they require instrumented test environment.
 */
class EmbeddedTerminalTest {

    @Test
    fun `LogEntry stores message correctly`() {
        val entry = LogEntry(
            timestamp = 1000L,
            type = LogType.INFO,
            message = "Test message"
        )
        assertEquals("Test message", entry.message)
        assertEquals(LogType.INFO, entry.type)
        assertEquals(1000L, entry.timestamp)
    }

    @Test
    fun `LogEntry with default timestamp uses system time`() {
        val before = System.currentTimeMillis()
        val entry = LogEntry(type = LogType.WARNING, message = "Warning")
        val after = System.currentTimeMillis()
        
        assertTrue("Timestamp should be in valid range", entry.timestamp in before..after)
    }

    @Test
    fun `different log types are distinct`() {
        val info = LogEntry(type = LogType.INFO, message = "info")
        val warning = LogEntry(type = LogType.WARNING, message = "warning")
        val error = LogEntry(type = LogType.ERROR, message = "error")
        val success = LogEntry(type = LogType.SUCCESS, message = "success")
        
        assertNotEquals(info.type, warning.type)
        assertNotEquals(warning.type, error.type)
        assertNotEquals(error.type, success.type)
    }
}
