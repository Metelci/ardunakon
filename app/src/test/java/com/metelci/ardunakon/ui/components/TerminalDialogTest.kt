package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TerminalDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun terminalDialog_shows_logs_and_actions() {
        var cleared = false
        var exported = false
        var dismissed = false

        val logs = listOf(LogEntry(type = LogType.INFO, message = "Hello"))
        val telemetry = Telemetry(batteryVoltage = 12.5f, status = "OK")

        composeTestRule.setContent {
            TerminalDialog(
                logs = logs,
                telemetry = telemetry,
                onDismiss = { dismissed = true },
                onSendCommand = {},
                onClearLogs = { cleared = true },
                onExportLogs = { exported = true }
            )
        }

        composeTestRule.onNodeWithText("Terminal").assertExists()
        composeTestRule.onNodeWithText("Bat: 12.5V | OK").assertExists()
        composeTestRule.onNodeWithText("> Hello").assertExists()

        composeTestRule.onNodeWithContentDescription("Export Logs").performClick()
        composeTestRule.onNodeWithContentDescription("Clear Logs").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        composeTestRule.runOnIdle {
            assertTrue(exported)
            assertTrue(cleared)
            assertTrue(dismissed)
        }
    }

    @Test
    fun terminalDialog_sends_command_when_text_entered() {
        var sentCommand: String? = null

        composeTestRule.setContent {
            TerminalDialog(
                logs = emptyList(),
                telemetry = null,
                onDismiss = {},
                onSendCommand = { sentCommand = it },
                onClearLogs = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("PING")
        composeTestRule.onNodeWithContentDescription("Send").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        composeTestRule.runOnIdle {
            assertEquals("PING", sentCommand)
        }
    }
}
