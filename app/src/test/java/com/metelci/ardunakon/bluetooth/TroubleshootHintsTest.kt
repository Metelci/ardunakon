package com.metelci.ardunakon.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TroubleshootHintsTest {

    @Test
    fun getHintForError_matches_case_insensitive_substring_patterns() {
        val hint = TroubleshootHints.getHintForError("Socket timeout while connecting")

        assertEquals(
            Pair(
                "Connection socket creation failed",
                "Check Bluetooth pairing and restart the device"
            ),
            hint
        )
    }

    @Test
    fun getHintForError_matches_regex_patterns() {
        val hint = TroubleshootHints.getHintForError("Missed 3 ack packets from device")

        assertEquals(
            Pair(
                "ACK packets missing",
                "Check signal quality or move closer"
            ),
            hint
        )
    }

    @Test
    fun getHintForError_returns_null_when_no_match() {
        assertNull(TroubleshootHints.getHintForError("Some unrelated error"))
    }

    @Test
    fun formatHint_formats_display_string() {
        val formatted = TroubleshootHints.formatHint("Explanation", "Solution")
        assertEquals("→ Explanation. Solution", formatted)
    }
}

