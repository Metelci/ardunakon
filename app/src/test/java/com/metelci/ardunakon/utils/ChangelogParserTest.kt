package com.metelci.ardunakon.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogParserTest {

    @Test
    fun parseLatestRelease_returns_fallback_when_version_not_found() {
        val changelog = """
            # Changelog
            ## [0.2.0] - 2025-01-01
            ### Added
            - Something
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.3.0")

        assertEquals(listOf("Release notes for version 0.3.0 not found in CHANGELOG"), notes)
    }

    @Test
    fun parseLatestRelease_extracts_added_changed_fixed_and_strips_markdown() {
        val changelog = """
            # Changelog
            
            ## [0.2.10-alpha] - 2025-01-01
            ### Added
            - **New** feature `X`
            - Second item
            ### Removed
            - Should not be included
            ### Fixed
            - Fix `Y`
            
            ## [0.2.9-alpha] - 2024-12-01
            ### Added
            - Older item
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        assertEquals(listOf("New feature X", "Second item", "Fix Y"), notes)
    }

    @Test
    fun parseLatestRelease_limits_to_8_items() {
        val bullets = (1..12).joinToString("\n") { "- Item $it" }
        val changelog = """
            # Changelog
            ## [1.0.0]
            ### Added
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
    fun parseLatestRelease_handles_version_with_no_features() {
        val changelog = """
            # Changelog
            
            ## [0.2.10-alpha] - 2025-01-01
            
            ## [0.2.9-alpha] - 2024-12-01
            ### Added
            - Old feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        assertEquals(listOf("No detailed changes listed for version 0.2.10-alpha"), notes)
    }

    @Test
    fun parseLatestRelease_handles_Changed_section() {
        val changelog = """
            ## [1.0.0]
            ### Changed
            - Updated feature
            - Modified behavior
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(listOf("Updated feature", "Modified behavior"), notes)
    }

    @Test
    fun parseLatestRelease_parses_all_bullets() {
        val changelog = """
            ## [1.0.0]
            ### Added
            - Main feature
              - Sub detail
            - Another feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        // Parser includes all bullet lines
        assertTrue(notes.isNotEmpty())
        assertTrue(notes.size >= 2)
    }

    @Test
    fun parseLatestRelease_handles_last_version_in_file() {
        val changelog = """
            ## [1.0.0] - 2025-01-01
            ### Added
            - Final version feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(listOf("Final version feature"), notes)
    }

    @Test
    fun parseLatestRelease_extracts_correct_version_from_multiple() {
        val changelog = """
            ## [0.3.0] - 2025-02-01
            ### Added
            - Newest
            
            ## [0.2.0] - 2025-01-01
            ### Added
            - Target version
            
            ## [0.1.0] - 2024-12-01
            ### Added
            - Oldest
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.0")

        assertEquals(listOf("Target version"), notes)
    }

    @Test
    fun parseLatestRelease_handles_version_with_special_characters() {
        val changelog = """
            ## [0.2.10-alpha-hotfix3] - 2025-01-01
            ### Fixed
            - Critical bug
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha-hotfix3")

        assertEquals(listOf("Critical bug"), notes)
    }

    @Test
    fun parseLatestRelease_ignores_Removed_and_Deprecated_sections() {
        val changelog = """
            ## [1.0.0]
            ### Added
            - New thing
            ### Removed
            - Old thing
            ### Deprecated
            - Legacy API
            ### Fixed
            - Bug fix
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        // Only Added and Fixed should be included
        assertEquals(2, notes.size)
        assertEquals("New thing", notes[0])
        assertEquals("Bug fix", notes[1])
    }

    @Test
    fun parseLatestRelease_combines_all_relevant_sections() {
        val changelog = """
            ## [1.0.0]
            ### Added
            - Feature A
            - Feature B
            ### Changed
            - Change X
            ### Fixed
            - Fix Y
            - Fix Z
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(5, notes.size)
        assertEquals("Feature A", notes[0])
        assertEquals("Feature B", notes[1])
        assertEquals("Change X", notes[2])
        assertEquals("Fix Y", notes[3])
        assertEquals("Fix Z", notes[4])
    }
}
