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
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_startAction_invokes_callback() {
        var started = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            WelcomeScreen(onStart = { started = true }, onSkip = {})
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Get Started", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        assertTrue(started)
    }

    @Test
    fun welcomeScreen_skipAction_invokes_callback() {
        var skipped = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            WelcomeScreen(onStart = {}, onSkip = { skipped = true })
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Skip", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        assertTrue(skipped)
    }
}
