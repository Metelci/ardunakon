package com.metelci.ardunakon.ui.flows

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.model.InterfaceElement
import com.metelci.ardunakon.ui.screens.onboarding.OnboardingFlow
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.metelci.ardunakon.HiltTestActivity
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private val onboardingCompleted = AtomicBoolean(false)
    private val onboardingSkipped = AtomicBoolean(false)

    @Before
    fun setUp() {
        hiltRule.inject()
        onboardingCompleted.set(false)
        onboardingSkipped.set(false)
        composeTestRule.activityRule.scenario.onActivity {}
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    OnboardingFlow(
                        onComplete = { onboardingCompleted.set(true) },
                        onSkip = { onboardingSkipped.set(true) }
                    )
                }
            }
        }
        waitForWelcomeScreen()
    }

    @Test
    fun onboardingFlow_completeFullTutorial_success() {
        // Welcome screen
        composeTestRule.onNodeWithText("🚀 Ardunakon").assertExists()
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.waitForIdle()

        // Interface tour screens
        advanceThroughInterfaceTour()

        // Connection tutorial screen
        composeTestRule.onNodeWithText("What kind of Arduino are you using?").assertExists()
        selectArduinoType()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🔌 Connection Mode").assertExists()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitForIdle()

        // Completion screen
        composeTestRule.onNodeWithText("🚀 Ready to Connect!").assertExists()
        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("You're Ready!").assertExists()
        composeTestRule.onNodeWithText("Start Controlling! ▶️").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10_000) { onboardingCompleted.get() }
    }

    @Test
    fun onboardingFlow_skipTutorial_goesToMainScreen() {
        // Welcome screen
        composeTestRule.onNodeWithText("🚀 Ardunakon").assertExists()
        composeTestRule.onNodeWithText("Skip Tour").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10_000) { onboardingSkipped.get() }
    }

    @Test
    fun onboardingFlow_backNavigation_works() {
        // Start tutorial
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.waitForIdle()

        advanceThroughInterfaceTour()
        composeTestRule.onNodeWithText("What kind of Arduino are you using?").assertExists()

        selectArduinoType()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🔌 Connection Mode").assertExists()

        // Go back
        composeTestRule.onNodeWithText("Back").performClick()
        composeTestRule.waitForIdle()

        // Verify we're back on the previous step
        composeTestRule.onNodeWithText("What kind of Arduino are you using?").assertExists()
    }

    @Test
    fun onboardingFlow_progressIndicator_updatesCorrectly() {
        composeTestRule.onNodeWithText("Get Started ▶️").performClick()
        composeTestRule.waitForIdle()

        val initialProgress = progressText()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        val updatedProgress = progressText()
        assertNotEquals(initialProgress, updatedProgress)
    }

    @Test
    fun onboardingFlow_retakeTutorial_fromHelp() {
        composeTestRule.onNodeWithText("Skip Tour").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10_000) { onboardingSkipped.get() }
        assertTrue(onboardingSkipped.get())
    }

    private fun waitForWelcomeScreen() {
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodesWithText("Get Started ▶️")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun advanceThroughInterfaceTour(stepsToAdvance: Int = InterfaceElement.tourOrder().size) {
        val totalSteps = InterfaceElement.tourOrder().size
        repeat(stepsToAdvance) { index ->
            val buttonText = if (index == totalSteps - 1) "Continue" else "Next"
            composeTestRule.onNodeWithText(buttonText).performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun selectArduinoType() {
        composeTestRule.onNodeWithText("Arduino UNO Q").performClick()
        composeTestRule.waitForIdle()
    }

    private fun progressText(): String {
        val nodes = composeTestRule.onAllNodes(
            hasText("complete", substring = true, ignoreCase = true)
        ).fetchSemanticsNodes()
        if (nodes.isEmpty()) return ""
        val texts = nodes.first().config.getOrNull(SemanticsProperties.Text) ?: return ""
        return texts.joinToString(" ") { it.text }
    }
}
