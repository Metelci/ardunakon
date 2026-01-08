package com.metelci.ardunakon.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogParserTest {

    @Test
    fun parseLatestRelease_returns_fallback_when_version_not_found() {
        val changelog = """
            # Changelog
            ## 0.2.0 - 2025-01-01
            - Something
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.3.0")

        assertEquals(listOf("Release notes for version 0.3.0 not found in CHANGELOG"), notes)
    }

    @Test
    fun parseLatestRelease_extracts_bullets_and_strips_markdown() {
        val changelog = """
            # Changelog
            
            ## 0.2.10-alpha (Build 20)
            - **New** feature `X`
            - Second item
            
            ## 0.2.9-alpha
            - Older item
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        assertEquals(listOf("New feature X", "Second item"), notes)
    }

    @Test
    fun parseLatestRelease_limits_to_8_items() {
        val bullets = (1..12).joinToString("\n") { "- Item $it" }
        val changelog = """
            # Changelog
            ## 1.0.0
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
            # Changelog
            
            ## 0.2.10-alpha
            
            ## 0.2.9-alpha
            - Old feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.10-alpha")

        assertEquals(listOf("No details found for this version."), notes)
    }

    @Test
    fun parseLatestRelease_handles_last_version_in_file() {
        val changelog = """
            ## 1.0.0
            - Final version feature
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "1.0.0")

        assertEquals(listOf("Final version feature"), notes)
    }

    @Test
    fun parseLatestRelease_extracts_correct_version_from_multiple() {
        val changelog = """
            ## 0.3.0
            - Newest
            
            ## 0.2.0
            - Target version
            
            ## 0.1.0
            - Oldest
        """.trimIndent()

        val notes = ChangelogParser.parseLatestRelease(changelog, currentVersion = "0.2.0")

        assertEquals(listOf("Target version"), notes)
    }
}
