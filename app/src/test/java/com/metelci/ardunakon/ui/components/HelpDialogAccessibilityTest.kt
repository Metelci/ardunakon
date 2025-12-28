package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HelpDialogAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun helpDialog_exposes_tab_and_action_labels() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Help & Documentation").assertExists()
        composeTestRule.onNodeWithText("Setup", substring = true).assertExists()
        composeTestRule.onNodeWithText("Compatibility", substring = true).assertExists()
        composeTestRule.onNodeWithText("Full Guide", substring = true).assertExists()
        composeTestRule.onNodeWithText("Arduino Cloud", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun telemetryGraphDialog_exposes_icon_descriptions() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TelemetryGraphDialog(
                telemetryHistoryManager = TelemetryHistoryManager(),
                onDismiss = {}
            )
        }

        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Telemetry Graphs").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear History").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }
}
