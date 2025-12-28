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
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Help & Documentation").assertExists()
        composeTestRule.onNodeWithText("Setup").assertExists()
        composeTestRule.onNodeWithText("ARDUNAKON - ARDUINO SETUP GUIDE", substring = true).assertExists()
    }

    @Test
    fun helpDialog_switches_to_compatibility_tab() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Compatibility", substring = true).performClick()
        composeTestRule.onNodeWithText("COMPATIBILITY", substring = true).assertExists()
    }

    @Test
    fun helpDialog_takeTutorial_invokes_callbacks() {
        var dismissed = false
        var tutorialStarted = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            HelpDialog(
                onDismiss = { dismissed = true },
                onTakeTutorial = { tutorialStarted = true }
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Tutorial", substring = true).performClick()

        assertTrue(tutorialStarted)
        assertTrue(dismissed)
    }

    @Test
    fun helpDialog_openArduinoCloud_shows_webview_title() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Open Arduino Cloud", substring = true).performClick()
        composeTestRule.onNodeWithText("Arduino Cloud", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }
}
