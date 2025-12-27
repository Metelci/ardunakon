package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import com.metelci.ardunakon.model.ArduinoType
import com.metelci.ardunakon.model.ConnectionTutorialStep
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingKeyboardNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_getStarted_responds_to_enter_key() {
        var started = false

        composeTestRule.setContent {
            WelcomeScreen(
                onStart = { started = true },
                onSkip = {}
            )
        }

        val startButton = composeTestRule.onNodeWithText("Get Started", substring = true)
        startButton.performSemanticsAction(SemanticsActions.RequestFocus)
        startButton.assertIsFocused()
        startButton.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.runOnIdle {
            assertTrue(started)
        }
    }

    @Test
    fun connectionTutorial_finish_responds_to_enter_key() {
        var finished = false

        composeTestRule.setContent {
            ConnectionTutorialScreen(
                step = ConnectionTutorialStep.SETUP_FINAL,
                selectedArduinoType = ArduinoType.UNO_Q,
                onArduinoSelected = {},
                onNext = { finished = true },
                onBack = {},
                onSkip = {},
                progress = 1.0f
            )
        }

        val finishButton = composeTestRule.onNodeWithText("Finish")
        finishButton.performSemanticsAction(SemanticsActions.RequestFocus)
        finishButton.assertIsFocused()
        finishButton.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.runOnIdle {
            assertTrue(finished)
        }
    }
}
