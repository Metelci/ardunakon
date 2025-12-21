package com.metelci.ardunakon.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmbeddedTerminalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `terminal renders with empty logs`() {
        composeTestRule.setContent {
            MaterialTheme {
                EmbeddedTerminal(
                    logs = emptyList(),
                    telemetry = null,
                    onSendCommand = {},
                    onClearLogs = {},
                    onMaximize = {},
                    onMinimize = {}
                )
            }
        }

        // Terminal header should be visible
        composeTestRule.onNodeWithText("Debug Terminal").assertIsDisplayed()
    }

    @Test
    fun `terminal displays log entries`() {
        val logs = listOf(
            LogEntry("Test message 1", LogType.INFO),
            LogEntry("Test message 2", LogType.WARNING),
            LogEntry("Error occurred", LogType.ERROR)
        )

        composeTestRule.setContent {
            MaterialTheme {
                EmbeddedTerminal(
                    logs = logs,
                    telemetry = null,
                    onSendCommand = {},
                    onClearLogs = {},
                    onMaximize = {},
                    onMinimize = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test message 1", substring = true).assertIsDisplayed()
    }

    @Test
    fun `terminal displays connected device info when provided`() {
        composeTestRule.setContent {
            MaterialTheme {
                EmbeddedTerminal(
                    logs = emptyList(),
                    telemetry = null,
                    connectedDeviceInfo = "HC-05 (Bluetooth)",
                    onSendCommand = {},
                    onClearLogs = {},
                    onMaximize = {},
                    onMinimize = {}
                )
            }
        }

        composeTestRule.onNodeWithText("HC-05", substring = true).assertIsDisplayed()
    }

    @Test
    fun `terminal displays telemetry info when provided`() {
        val telemetry = Telemetry(
            batteryVoltage = 8.4f,
            status = "OK",
            packetsSent = 100,
            packetsDropped = 2,
            packetsFailed = 1
        )

        composeTestRule.setContent {
            MaterialTheme {
                EmbeddedTerminal(
                    logs = emptyList(),
                    telemetry = telemetry,
                    onSendCommand = {},
                    onClearLogs = {},
                    onMaximize = {},
                    onMinimize = {}
                )
            }
        }

        // Battery voltage should be visible in status area
        composeTestRule.onNodeWithText("8.4V", substring = true).assertIsDisplayed()
    }

    @Test
    fun `terminal renders with many log entries`() {
        val logs = (1..100).map { 
            LogEntry("Log message $it", if (it % 3 == 0) LogType.WARNING else LogType.INFO)
        }

        composeTestRule.setContent {
            MaterialTheme {
                EmbeddedTerminal(
                    logs = logs,
                    telemetry = null,
                    onSendCommand = {},
                    onClearLogs = {},
                    onMaximize = {},
                    onMinimize = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Debug Terminal").assertIsDisplayed()
    }
}
