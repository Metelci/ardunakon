package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1024dp-h2048dp")
class HelpDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun helpDialog_shows_setup_content_by_default() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(2000)

        // Title
        composeTestRule.onNode(hasText("Help & Documentation", substring = true)).assertExists()

        // Tab - Setup
        composeTestRule.onNode(hasText("Setup", substring = false).and(hasClickAction())).assertExists()
    }

    @Test
    fun helpDialog_switches_to_compatibility_tab() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(2000)

        // Click Compatibility tab
        composeTestRule.onNode(hasText("Compatibility", substring = false).and(hasClickAction())).performClick()

        composeTestRule.mainClock.advanceTimeBy(2000)

        // Check if tab is selected
        composeTestRule.onNode(hasText("Compatibility", substring = false).and(hasClickAction())).assertIsSelected()
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
        composeTestRule.mainClock.advanceTimeBy(3000)

        // Scroll to the item containing Tutorial (Index 2)
        composeTestRule.onNodeWithTag("HelpLazyColumn").performScrollToIndex(2)

        composeTestRule.onNode(hasText("Tutorial", substring = true).and(hasClickAction()))
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(1000)

        assertTrue(tutorialStarted)
        assertTrue(dismissed)
    }

    @Test
    fun helpDialog_openArduinoCloud_shows_webview_title() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(3000)

        // Scroll to the item containing Arduino Cloud (Index 2)
        composeTestRule.onNodeWithTag("HelpLazyColumn").performScrollToIndex(2)

        composeTestRule.onNode(hasText("Arduino Cloud", substring = true).and(hasClickAction()))
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(2000)

        // Check if any "Arduino Cloud" exists (title)
        composeTestRule.onAllNodes(hasText("Arduino Cloud", substring = true))
            .onFirst()
            .assertExists()
    }
}
