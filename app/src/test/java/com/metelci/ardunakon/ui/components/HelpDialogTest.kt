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
class HelpDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun helpDialog_shows_setup_content_by_default() {
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }

        composeTestRule.onNodeWithText("Help & Documentation").assertExists()
        composeTestRule.onNodeWithText("Setup").assertExists()
        composeTestRule.onNodeWithText("ARDUNAKON - ARDUINO SETUP GUIDE", substring = true).assertExists()
    }

    @Test
    fun helpDialog_switches_to_compatibility_tab() {
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }

        composeTestRule.onNodeWithText("Compatibility").performClick()
        composeTestRule.onNodeWithText("MAXIMUM BLUETOOTH COMPATIBILITY REPORT", substring = true).assertExists()
    }

    @Test
    fun helpDialog_takeTutorial_invokes_callbacks() {
        var dismissed = false
        var tutorialStarted = false

        composeTestRule.setContent {
            HelpDialog(
                onDismiss = { dismissed = true },
                onTakeTutorial = { tutorialStarted = true }
            )
        }

        composeTestRule.onNodeWithText("Take Tutorial", substring = true).performClick()

        composeTestRule.runOnIdle {
            assertTrue(tutorialStarted)
            assertTrue(dismissed)
        }
    }

    @Test
    fun helpDialog_openArduinoCloud_shows_webview_title() {
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }

        composeTestRule.onNodeWithText("Open Arduino Cloud").performClick()
        composeTestRule.onNodeWithText("Arduino Cloud").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }
}
