package com.metelci.ardunakon.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogParserTest {

    @Test
    fun parseLatestRelease_returns_fallback_when_version_not_found() {
        val changelog = """
            Version 0.2.0-alpha - January 1, 2026
            
            NEW FEATURES:
            ${'\u2022'} Something
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.3.0")

        assertEquals(listOf("Release notes for version 0.3.0 not found in CHANGELOG"), notes)
    }

    @Test
    fun parseLatestRelease_extracts_bullets_and_strips_categories() {
        val changelog = """
            Version 0.2.10-alpha - January 1, 2026
            
            NEW FEATURES:
            ${'\u2022'} New feature X
            ${'\u2022'} Second item
            
            ---
            
            Version 0.2.9-alpha - December 20, 2025
            ${'\u2022'} Older item
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        assertEquals(listOf("New feature X", "Second item"), notes)
    }

    @Test
    fun parseLatestRelease_limits_to_8_items() {
        val bullets = (1..12).joinToString("\n") { "${'\u2022'} Item $it" }
        val changelog = """
            Version 1.0.0 - January 1, 2026
            $bullets
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(8, notes.size)
        assertEquals("Item 1", notes.first())
        assertEquals("Item 8", notes.last())
    }

    @Test
    fun parseLatestRelease_handles_empty_changelog() {
        val changelog = ""

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(listOf("Release notes for version 1.0.0 not found in CHANGELOG"), notes)
    }

    @Test
    fun parseLatestRelease_handles_version_with_no_bullets() {
        val changelog = """
            Version 0.2.10-alpha - January 1, 2026
            
            ---
            
            Version 0.2.9-alpha - December 20, 2025
            ${'\u2022'} Old feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        assertEquals(listOf("No details found for this version."), notes)
    }

    @Test
    fun parseLatestRelease_handles_last_version_in_file() {
        val changelog = """
            Version 1.0.0 - January 1, 2026
            ${'\u2022'} Final version feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(listOf("Final version feature"), notes)
    }

    @Test
    fun parseLatestRelease_extracts_correct_version_from_multiple() {
        val changelog = """
            Version 0.3.0 - January 3, 2026
            ${'\u2022'} Newest
            
            ---
            
            Version 0.2.0 - January 2, 2026
            ${'\u2022'} Target version
            
            ---
            
            Version 0.1.0 - January 1, 2026
            ${'\u2022'} Oldest
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.0")

        assertEquals(listOf("Target version"), notes)
    }

    @Test
    fun parseLatestRelease_skips_category_headers() {
        val changelog = """
            Version 0.2.10-alpha - January 1, 2026
            
            NEW FEATURES:
            ${'\u2022'} Feature one
            
            FIXES:
            ${'\u2022'} Bug fix one
            
            IMPROVEMENTS:
            ${'\u2022'} Enhancement one
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        // Should not include the category headers (NEW FEATURES:, FIXES:, IMPROVEMENTS:)
        assertEquals(listOf("Feature one", "Bug fix one", "Enhancement one"), notes)
    }
}
