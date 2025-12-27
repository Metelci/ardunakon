package com.metelci.ardunakon.ui.screens.control.dialogs

import android.view.View
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashLogDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createView(): View {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return View(context)
    }

    @Test
    fun crashLogDialog_shows_empty_state_and_actions() {
        var dismissed = false

        composeTestRule.setContent {
            CrashLogDialog(
                crashLog = "",
                view = createView(),
                onShare = {},
                onClear = {},
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Crash Log").assertExists()
        composeTestRule.onNodeWithText("No crash logs available").assertExists()
        composeTestRule.onNodeWithContentDescription("Share").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear").assertExists()
        composeTestRule.onNodeWithText("Close").performClick()

        composeTestRule.runOnIdle {
            assertTrue(dismissed)
        }
    }
}
