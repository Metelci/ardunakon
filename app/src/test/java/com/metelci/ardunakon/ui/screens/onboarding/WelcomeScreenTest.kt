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
@Config(sdk = [34])
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_shows_value_props_and_actions() {
        var started = false
        var skipped = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            WelcomeScreen(
                onStart = { started = true },
                onSkip = { skipped = true }
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Ardunakon", substring = true).assertExists()
        composeTestRule.onNodeWithText("Get Started", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Skip", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        assertTrue(started)
        assertTrue(skipped)
    }
}
