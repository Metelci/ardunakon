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
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }

        composeTestRule.onNodeWithText("Help & Documentation").assertExists()
        composeTestRule.onNodeWithText("Setup").assertExists()
        composeTestRule.onNodeWithText("Compatibility").assertExists()
        composeTestRule.onNodeWithText("View Full Guide").assertExists()
        composeTestRule.onNodeWithText("Open Arduino Cloud").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun telemetryGraphDialog_exposes_icon_descriptions() {
        composeTestRule.setContent {
            TelemetryGraphDialog(
                telemetryHistoryManager = TelemetryHistoryManager(),
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Telemetry Graphs").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear History").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }
}
