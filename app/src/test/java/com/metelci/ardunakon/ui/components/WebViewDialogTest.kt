package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebViewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun webViewDialog_shows_title_and_closes() {
        var dismissed = false

        composeTestRule.setContent {
            WebViewDialog(
                htmlContent = "<html><body>Doc</body></html>",
                title = "Docs",
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Docs").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        composeTestRule.runOnIdle {
            assertTrue(dismissed)
        }
    }
}
