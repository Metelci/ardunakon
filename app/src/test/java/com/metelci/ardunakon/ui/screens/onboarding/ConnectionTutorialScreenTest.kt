package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.model.ArduinoType
import com.metelci.ardunakon.model.ConnectionTutorialStep
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionTutorialScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun connectionTutorial_chooseArduino_requires_selection_to_continue() {
        var selected: ArduinoType? = null
        var nextTapped = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ConnectionTutorialScreen(
                step = ConnectionTutorialStep.CHOOSE_ARDUINO,
                selectedArduinoType = selected,
                onArduinoSelected = { selected = it },
                onNext = { nextTapped = true },
                onBack = {},
                onSkip = {},
                progress = 0.1f
            )
        }

        composeTestRule.onNodeWithText("Select Your Device", substring = true).assertExists()
        composeTestRule.onNodeWithText(ArduinoType.UNO_Q.displayName).performClick()
        composeTestRule.onNodeWithText("Continue").performClick()

        assertEquals(ArduinoType.UNO_Q, selected)
        assertTrue(nextTapped)
    }

    @Test
    fun connectionTutorial_connectionMode_toggles_and_shows_text() {
        var mode = ConnectionMode.BLUETOOTH
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ConnectionTutorialScreen(
                step = ConnectionTutorialStep.CONNECTION_MODE,
                selectedArduinoType = ArduinoType.R4_WIFI,
                onArduinoSelected = {},
                connectionMode = mode,
                onConnectionModeChanged = { mode = it },
                onNext = {},
                onBack = {},
                onSkip = {},
                progress = 0.5f
            )
        }

        composeTestRule.onNodeWithText("Connection Mode", substring = true).assertExists()
        composeTestRule.onNodeWithText("Bluetooth mode selected", substring = true).assertExists()
        composeTestRule.onNodeWithText("WiFi").performClick()

        assertEquals(ConnectionMode.WIFI, mode)
        
        // Advance slightly if needed for state change to reflect in UI
        composeTestRule.onNodeWithText("WiFi mode selected", substring = true).assertExists()
    }

    @Test
    fun connectionTutorial_setupFinal_shows_finish_action() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ConnectionTutorialScreen(
                step = ConnectionTutorialStep.SETUP_FINAL,
                selectedArduinoType = ArduinoType.UNO_OTHER,
                onArduinoSelected = {},
                onNext = { finished = true },
                onBack = {},
                onSkip = {},
                progress = 1.0f
            )
        }

        composeTestRule.onNodeWithText("Ready to Connect!", substring = true).assertExists()
        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.onNodeWithContentDescription("Finish tutorial").assertExists()

        assertTrue(finished)
    }
}
