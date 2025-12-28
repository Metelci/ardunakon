package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.metelci.ardunakon.model.FeatureType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompletionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completionScreen_shows_checklist_and_finish_action() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CompletionScreen(
                onFinish = { finished = true },
                exploredFeatures = setOf(FeatureType.DEBUG_CONSOLE)
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Ready", substring = true).assertExists()
        composeTestRule.onNodeWithText("Controlling", substring = true)
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
