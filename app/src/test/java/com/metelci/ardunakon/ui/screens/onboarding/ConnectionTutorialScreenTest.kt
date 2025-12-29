package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
@Config(sdk = [34], qualifiers = "w800dp-h1280dp")
class ConnectionTutorialScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun connectionTutorial_chooseArduino_requires_selection_to_continue() {
        var selected by mutableStateOf<ArduinoType?>(null)
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
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Select Your Device", substring = true).assertExists()
        composeTestRule.onNodeWithText(ArduinoType.UNO_Q.displayName).performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        assertEquals(ArduinoType.UNO_Q, selected)
        assertTrue(nextTapped)
    }

    @Test
    fun connectionTutorial_connectionMode_toggles_and_shows_text() {
        var mode by mutableStateOf(ConnectionMode.BLUETOOTH)
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
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Connection Mode", substring = true).assertExists()
        composeTestRule.onNodeWithText("Bluetooth mode selected", substring = true).assertExists()
        composeTestRule.onNodeWithText("WiFi").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

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
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Ready to Connect!", substring = true).assertExists()
        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithContentDescription("Finish tutorial").assertExists()

        assertTrue(finished)
    }
}
