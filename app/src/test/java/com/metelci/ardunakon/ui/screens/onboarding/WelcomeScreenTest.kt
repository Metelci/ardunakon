package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
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
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_shows_value_props_and_actions() {
        var started = false
        var skipped = false

        composeTestRule.setContent {
            WelcomeScreen(
                onStart = { started = true },
                onSkip = { skipped = true }
            )
        }

        composeTestRule.onNodeWithText("🚀 Ardunakon").assertExists()
        composeTestRule.onNodeWithText("Arduino Controller App").assertExists()
        composeTestRule.onNodeWithText("Control with precision").assertExists()
        composeTestRule.onNodeWithText("Bluetooth + WiFi support").assertExists()
        composeTestRule.onNodeWithText("Debug & Telemetry").assertExists()
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.onNodeWithText("Skip Tour").performClick()

        composeTestRule.runOnIdle {
            assertTrue(started)
            assertTrue(skipped)
        }
    }
}
