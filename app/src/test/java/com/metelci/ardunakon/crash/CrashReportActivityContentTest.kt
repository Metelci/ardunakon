package com.metelci.ardunakon.crash

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashReportActivityContentTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun crashReportContent_showsMessageStackAndInvokesActions() {
        val stack = "Line 1\nLine 2\nLine 3"

        var restarted = false
        var shared = false
        var copied = false

        composeTestRule.setContent {
            CrashReportActivity_Content(
                message = "Something went wrong",
                stackTrace = stack,
                onRestart = { restarted = true },
                onShare = { shared = true },
                onCopy = { copied = true }
            )
        }

        composeTestRule.onNodeWithText("Application Crashed").assertExists()
        composeTestRule.onNodeWithText("Error: Something went wrong").assertExists()
        composeTestRule.onNodeWithText("Line 2", substring = true).assertExists()

        composeTestRule.onNodeWithText("Copy Log").performClick()
        composeTestRule.onNodeWithText("Share").performClick()
        composeTestRule.onNodeWithText("Restart Application").performClick()

        composeTestRule.runOnIdle {
            assertTrue(copied)
            assertTrue(shared)
            assertTrue(restarted)
        }
    }
}

