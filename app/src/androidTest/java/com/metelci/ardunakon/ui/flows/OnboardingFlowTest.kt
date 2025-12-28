package com.metelci.ardunakon.ui.flows

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end instrumented tests for onboarding flow.
 * Tests the complete user journey through the tutorial screens.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        // Reset onboarding state before each test
        // This would require accessing OnboardingManager
    }

    @Test
    fun onboardingFlow_completeFullTutorial_success() {
        // Welcome screen
        composeTestRule.onNodeWithText("🚀 Ardunakon").assertExists()
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.waitForIdle()

        // Connection tutorial screen
        composeTestRule.onNodeWithText("Connection Tutorial").assertExists()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        // Interface tour screen
        composeTestRule.onNodeWithText("Interface Tour").assertExists()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        // Completion screen
        composeTestRule.onNodeWithText("You're All Set!").assertExists()
        composeTestRule.onNodeWithText("Start Controlling").performClick()
        composeTestRule.waitForIdle()

        // Verify we're on the main control screen
        composeTestRule.onNodeWithContentDescription("Joystick").assertExists()
    }

    @Test
    fun onboardingFlow_skipTutorial_goesToMainScreen() {
        // Welcome screen
        composeTestRule.onNodeWithText("🚀 Ardunakon").assertExists()
        composeTestRule.onNodeWithText("Skip Tour").performClick()
        composeTestRule.waitForIdle()

        // Verify we're on the main control screen
        composeTestRule.onNodeWithContentDescription("Joystick").assertExists()
    }

    @Test
    fun onboardingFlow_backNavigation_works() {
        // Start tutorial
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.waitForIdle()

        // Go to next screen
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        // Go back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // Verify we're back on connection tutorial
        composeTestRule.onNodeWithText("Connection Tutorial").assertExists()
    }

    @Test
    fun onboardingFlow_progressIndicator_updatesCorrectly() {
        // Welcome screen - no progress indicator
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.waitForIdle()

        // Connection tutorial - step 1 of 3
        composeTestRule.onNodeWithText("1 / 3").assertExists()

        // Next screen
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        // Interface tour - step 2 of 3
        composeTestRule.onNodeWithText("2 / 3").assertExists()

        // Next screen
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        // Completion - step 3 of 3
        composeTestRule.onNodeWithText("3 / 3").assertExists()
    }

    @Test
    fun onboardingFlow_retakeTutorial_fromHelp() {
        // Skip initial tutorial
        composeTestRule.onNodeWithText("Skip Tour").performClick()
        composeTestRule.waitForIdle()

        // Open help menu
        composeTestRule.onNodeWithContentDescription("Help").performClick()
        composeTestRule.waitForIdle()

        // Click take tutorial button
        composeTestRule.onNodeWithText("🎓 Take Tutorial").performClick()
        composeTestRule.waitForIdle()

        // Verify we're back at welcome screen
        composeTestRule.onNodeWithText("🚀 Ardunakon").assertExists()
    }
}
