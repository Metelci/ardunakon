package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.model.InterfaceElement
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealScreenTourTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun realScreenTour_firstElement_shows_progress_and_next_only() {
        var nextTapped = false
        var skipTapped = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            RealScreenTour(
                currentElement = InterfaceElement.tourOrder().first(),
                onNext = { nextTapped = true },
                onBack = {},
                onSkip = { skipTapped = true },
                progress = 0.2f
            )
        }

        composeTestRule.onNodeWithText("Interface Tour - 1/5").assertExists()
        composeTestRule.onNodeWithText("Emergency Stop").assertExists()
        composeTestRule.onNodeWithText("Instantly stops all motors - the most important button!").assertExists()
        composeTestRule.onNodeWithText("Tip:", substring = true).assertExists()

        composeTestRule.onNodeWithText("Skip Tour").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()

        assertTrue(skipTapped)
        assertTrue(nextTapped)
    }

    @Test
    fun realScreenTour_lastElement_shows_continue_and_back() {
        var backTapped = false
        var nextTapped = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            RealScreenTour(
                currentElement = InterfaceElement.tourOrder().last(),
                onNext = { nextTapped = true },
                onBack = { backTapped = true },
                onSkip = {},
                progress = 1.0f
            )
        }

        composeTestRule.onNodeWithText("Interface Tour - 5/5").assertExists()
        composeTestRule.onNodeWithText("Connection Mode").assertExists()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Next").assertDoesNotExist()

        assertTrue(nextTapped)
        assertTrue(backTapped)
    }
}
