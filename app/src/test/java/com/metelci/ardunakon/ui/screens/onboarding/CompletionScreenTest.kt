package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1280dp")
class CompletionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completionScreen_finishAction_invokes_callback() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CompletionScreen(
                onFinish = { finished = true },
                exploredFeatures = emptySet()
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNode(hasText("Controlling", substring = true), useUnmergedTree = true)
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        assertTrue(finished)
    }

    @Test
    fun completionScreen_hides_feature_count_when_none() {
        composeTestRule.setContent {
            CompletionScreen(
                onFinish = {},
                exploredFeatures = emptySet()
            )
        }

        composeTestRule.onNodeWithText("Advanced features explored", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Rotate screen").assertExists()
    }
}
