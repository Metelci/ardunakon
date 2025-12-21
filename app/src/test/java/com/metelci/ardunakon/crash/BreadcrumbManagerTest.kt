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
}
