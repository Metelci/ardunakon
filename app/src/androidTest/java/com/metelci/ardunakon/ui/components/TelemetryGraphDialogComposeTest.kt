package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import org.junit.Rule
import org.junit.Test

class TelemetryGraphDialogComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun telemetryGraphDialog_shows_empty_state_and_close_dismisses() {
        val history = TelemetryHistoryManager()
        composeRule.setContent {
            var dismissed by remember { mutableStateOf(false) }
            MaterialTheme {
                Column {
                    TelemetryGraphDialog(
                        telemetryHistoryManager = history,
                        onDismiss = { dismissed = true }
                    )
                    Text("dismissed=$dismissed")
                }
            }
        }

        composeRule.onNodeWithText("Telemetry Graphs").assertIsDisplayed()
        composeRule.onNodeWithText("No battery data available").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.onNodeWithText("dismissed=true").assertIsDisplayed()
    }

    @Test
    fun telemetryGraphDialog_tab_switches_and_clear_history_restores_empty_state() {
        val history = TelemetryHistoryManager()
        composeRule.setContent {
            MaterialTheme {
                TelemetryGraphDialog(
                    telemetryHistoryManager = history,
                    onDismiss = {}
                )
            }
        }

        // Battery is the default tab.
        composeRule.onNodeWithText("No battery data available").assertIsDisplayed()

        // Populate and verify empty state disappears.
        composeRule.runOnUiThread { history.recordBattery(12.4f) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No battery data available").assertDoesNotExist()

        // Switch to RTT tab and confirm empty state.
        composeRule.onNodeWithText("RTT").performClick()
        composeRule.onNodeWithText("No RTT data available").assertIsDisplayed()

        // Clear history should restore battery empty state when returning to Battery tab.
        composeRule.onNodeWithContentDescription("Clear History").performClick()
        composeRule.onNodeWithText("Battery").performClick()
        composeRule.onNodeWithText("No battery data available").assertIsDisplayed()
    }
}
