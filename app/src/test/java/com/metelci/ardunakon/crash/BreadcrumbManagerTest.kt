package com.metelci.ardunakon.crash

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreadcrumbManagerTest {

    @After
    fun tearDown() {
        BreadcrumbManager.clear()
    }

    @Test
    fun leave_capsBreadcrumbsAtMax() {
        repeat(60) { i ->
            BreadcrumbManager.leave(tag = "T", message = "m$i")
        }

        val lines = BreadcrumbManager.getBreadcrumbs()
            .split('\n')
            .filter { it.isNotBlank() }

        assertEquals(50, lines.size)
        assertTrue(lines.last().contains("m59"))
    }

    @Test
    fun getBreadcrumbs_returnsFormattedLines() {
        BreadcrumbManager.leave(tag = "UI", message = "Clicked")
        val text = BreadcrumbManager.getBreadcrumbs()

        assertTrue(text.contains("UI: Clicked"))
        assertTrue(text.trimStart().startsWith("["))
    }

    @Test
    fun clear_removesAllBreadcrumbs() {
        BreadcrumbManager.leave("TEST", "Action 1")
        BreadcrumbManager.leave("TEST", "Action 2")

        BreadcrumbManager.clear()

        assertEquals("", BreadcrumbManager.getBreadcrumbs())
    }

    @Test
    fun leave_handlesEmptyMessage() {
        BreadcrumbManager.leave("TEST", "")

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()

        assertTrue(breadcrumbs.contains("TEST: "))
    }

    @Test
    fun leave_handlesSpecialCharacters() {
        BreadcrumbManager.leave("TEST", "Message with \"quotes\"")
        BreadcrumbManager.leave("TEST", "Message\nwith\nnewlines")

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()

        assertTrue(breadcrumbs.contains("quotes"))
        assertTrue(breadcrumbs.contains("newlines"))
    }

    @Test
    fun leave_handlesLongMessages() {
        val longMessage = "A".repeat(500)
        BreadcrumbManager.leave("TEST", longMessage)

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()

        assertTrue(breadcrumbs.contains(longMessage))
    }

    @Test
    fun getBreadcrumbs_returnsEmptyStringWhenNoBreadcrumbs() {
        BreadcrumbManager.clear()

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()

        assertEquals("", breadcrumbs)
    }

    @Test
    fun leave_maintainsChronologicalOrder() {
        BreadcrumbManager.leave("FIRST", "Oldest")
        Thread.sleep(10)
        BreadcrumbManager.leave("SECOND", "Middle")
        Thread.sleep(10)
        BreadcrumbManager.leave("THIRD", "Newest")

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()
        val lines = breadcrumbs.lines()

        val firstIndex = lines.indexOfFirst { it.contains("FIRST") }
        val secondIndex = lines.indexOfFirst { it.contains("SECOND") }
        val thirdIndex = lines.indexOfFirst { it.contains("THIRD") }

        assertTrue(firstIndex < secondIndex)
        assertTrue(secondIndex < thirdIndex)
    }

    @Test
    fun leave_dropsOldestWhenCapacityExceeded() {
        BreadcrumbManager.clear()

        // Add more than MAX_BREADCRUMBS (50)
        repeat(55) { i ->
            BreadcrumbManager.leave("TAG", "Message $i")
        }

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()
        val lines = breadcrumbs.lines().filter { it.isNotBlank() }

        // Should have max 50 breadcrumbs
        assertTrue(lines.size <= 50)
        // Latest message should be present
        assertTrue(breadcrumbs.contains("Message 54"))
    }

    @Test
    fun leave_threadSafeConcurrentOperations() {
        val threadCount = 10
        val breadcrumbsPerThread = 5

        val threads = (0 until threadCount).map { threadId ->
            Thread {
                repeat(breadcrumbsPerThread) { msgId ->
                    BreadcrumbManager.leave("T$threadId", "Msg $msgId")
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()
        val lines = breadcrumbs.lines().filter { it.isNotBlank() }

        assertEquals(50, lines.size)
    }

    @Test
    fun leave_formatIncludesTimestamp() {
        BreadcrumbManager.leave("UI", "Button clicked")

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()

        // Format: [HH:mm:ss.SSS] TAG: message
        assertTrue(breadcrumbs.matches(Regex("\\[\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\] UI: Button clicked")))
    }

    @Test
    fun leave_multipleCallsCreateSeparateLines() {
        BreadcrumbManager.leave("TAG1", "Message 1")
        BreadcrumbManager.leave("TAG2", "Message 2")
        BreadcrumbManager.leave("TAG3", "Message 3")

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()
        val lines = breadcrumbs.split("\n")

        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("TAG1"))
        assertTrue(lines[1].contains("TAG2"))
        assertTrue(lines[2].contains("TAG3"))
    }

    @Test
    fun leave_handlesRapidSequentialCalls() {
        repeat(100) { i ->
            BreadcrumbManager.leave("RAPID", "Action $i")
        }

        val breadcrumbs = BreadcrumbManager.getBreadcrumbs()
        val lines = breadcrumbs.lines().filter { it.isNotBlank() }

        // Should keep only last 50
        assertEquals(50, lines.size)
        assertTrue(breadcrumbs.contains("Action 99"))
        assertTrue(breadcrumbs.contains("Action 50"))
    }
}
