package com.metelci.ardunakon.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.ArduinoType
import com.metelci.ardunakon.model.ConnectionTutorialStep
import com.metelci.ardunakon.model.FeatureType
import com.metelci.ardunakon.model.InterfaceElement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreensComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun welcomeScreen_start_and_skip_invoke_callbacks() {
        composeRule.setContent {
            var action by remember { mutableStateOf("none") }
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    WelcomeScreen(
                        onStart = { action = "start" },
                        onSkip = { action = "skip" }
                    )
                    androidx.compose.material3.Text("action=$action")
                }
            }
        }

        composeRule.onNodeWithText("Get Started", substring = true).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("action=start").assertIsDisplayed()

        composeRule.onNodeWithText("Skip Tour").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("action=skip").assertIsDisplayed()
    }

    @Test
    fun interfaceTourScreen_shows_progress_and_navigation_buttons() {
        composeRule.setContent {
            MaterialTheme {
                InterfaceTourScreen(
                    element = InterfaceElement.ESTOP,
                    onNext = {},
                    onBack = {},
                    onSkip = {},
                    progress = 0.25f
                )
            }
        }

        composeRule.onNodeWithText("25% complete").assertIsDisplayed()
        composeRule.onNodeWithText("Skip").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun connectionTutorial_choose_device_enables_next_after_selection() {
        composeRule.setContent {
            var selected: ArduinoType? by remember { mutableStateOf(null) }
            var nextClicks by remember { mutableStateOf(0) }
            MaterialTheme {
                ConnectionTutorialScreen(
                    step = ConnectionTutorialStep.CHOOSE_ARDUINO,
                    selectedArduinoType = selected,
                    onArduinoSelected = { selected = it },
                    onNext = { nextClicks++ },
                    onBack = {},
                    onSkip = {},
                    progress = 0.5f
                )
                androidx.compose.material3.Text("nextClicks=$nextClicks")
            }
        }

        composeRule.onNodeWithText("Select Your Device", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsNotEnabled()

        val firstType = ArduinoType.entries.first()
        composeRule.onNodeWithText(firstType.displayName).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Selected").assertIsDisplayed()

        composeRule.onNodeWithText("Next").assertIsEnabled().performClick()
        composeRule.onNodeWithText("nextClicks=1").assertIsDisplayed()
    }

    @Test
    fun completionScreen_shows_advanced_features_count_when_present() {
        composeRule.setContent {
            MaterialTheme {
                CompletionScreen(
                    onFinish = {},
                    exploredFeatures = setOf(FeatureType.TELEMETRY, FeatureType.DEBUG_CONSOLE)
                )
            }
        }

        composeRule.onNodeWithText("You're Ready!").assertIsDisplayed()
        composeRule.onNodeWithText("Advanced features explored (2)").assertIsDisplayed()
        composeRule.onNodeWithText("Start Controlling!", substring = true).assertIsDisplayed()
    }
}
