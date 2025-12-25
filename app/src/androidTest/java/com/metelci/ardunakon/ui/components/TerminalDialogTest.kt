package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for TerminalDialog.
 *
 * Tests log display, command input, callbacks,
 * and telemetry integration.
 */
@RunWith(AndroidJUnit4::class)
class TerminalDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun terminalDialog_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithText("Terminal").assertIsDisplayed()
    }

    @Test
    fun terminalDialog_displaysLogs() {
        val logs = listOf(
            LogEntry("Info message", LogType.INFO),
            LogEntry("Success message", LogType.SUCCESS),
            LogEntry("Warning message", LogType.WARNING),
            LogEntry("Error message", LogType.ERROR)
        )

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = logs,
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithText("> Info message").assertIsDisplayed()
        composeRule.onNodeWithText("> Success message").assertIsDisplayed()
        composeRule.onNodeWithText("> Warning message").assertIsDisplayed()
        composeRule.onNodeWithText("> Error message").assertIsDisplayed()
    }

    @Test
    fun terminalDialog_displaysTelemetry() {
        val telemetry = Telemetry(
            batteryVoltage = 12.5f,
            temperature = 25f,
            humidity = 50f,
            status = "Connected"
        )

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = telemetry,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithText("Bat: 12.5V | Connected").assertIsDisplayed()
    }

    @Test
    fun terminalDialog_sendButtonEnabledWhenTextInput() {
        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        // Find and enter text in input field
        composeRule.onNodeWithText("Enter command...").performTextInput("test command")

        // Send button should be enabled
        composeRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun terminalDialog_invokesOnSendCommand() {
        var sentCommand = ""

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = { sentCommand = it },
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithText("Enter command...").performTextInput("AT+VERSION")
        composeRule.onNodeWithContentDescription("Send").performClick()

        assert(sentCommand == "AT+VERSION")
    }

    @Test
    fun terminalDialog_clearsInputAfterSend() {
        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithText("Enter command...").performTextInput("test")
        composeRule.onNodeWithContentDescription("Send").performClick()

        // Input should be cleared
        composeRule.waitForIdle()
        composeRule.onNodeWithText("test").assertDoesNotExist()
    }

    @Test
    fun terminalDialog_invokesOnClearLogs() {
        var clearCalled = false

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = listOf(LogEntry("Test", LogType.INFO)),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = { clearCalled = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Clear Logs").performClick()

        assert(clearCalled)
    }

    @Test
    fun terminalDialog_invokesOnDismiss() {
        var dismissCalled = false

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = null,
                    onDismiss = { dismissCalled = true },
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Close").performClick()

        assert(dismissCalled)
    }

    @Test
    fun terminalDialog_invokesOnExportLogs() {
        var exportCalled = false

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = listOf(LogEntry("Test", LogType.INFO)),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = { exportCalled = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Export Logs").performClick()

        assert(exportCalled)
    }

    @Test
    fun terminalDialog_handlesEmptyLogs() {
        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        // Should render without crashing
        composeRule.waitForIdle()
    }

    @Test
    fun terminalDialog_handlesManyLogs() {
        val logs = (1..100).map { LogEntry("Log entry $it", LogType.INFO) }

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = logs,
                    telemetry = null,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        // Should render all logs (scroll is handled by LazyColumn)
        composeRule.waitForIdle()
    }

    @Test
    fun terminalDialog_handlesLowBatteryTelemetry() {
        val telemetry = Telemetry(
            batteryVoltage = 10.5f, // Low battery
            temperature = 25f,
            humidity = 50f,
            status = "Warning"
        )

        composeRule.setContent {
            MaterialTheme {
                TerminalDialog(
                    logs = emptyList(),
                    telemetry = telemetry,
                    onDismiss = {},
                    onSendCommand = {},
                    onClearLogs = {}
                )
            }
        }

        composeRule.onNodeWithText("Bat: 10.5V | Warning").assertIsDisplayed()
    }
}
