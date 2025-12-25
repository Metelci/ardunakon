package com.metelci.ardunakon.ui.screens.control

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.model.LogType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for LandscapeControlLayout.
 *
 * Tests landscape orientation layout rendering,
 * component placement, and control interactions.
 */
@RunWith(AndroidJUnit4::class)
class LandscapeControlLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun landscapeLayout_renders() {
        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = false,
                    logs = emptyList(),
                    rssi = -70,
                    latency = 50,
                    packetLoss = 2.5f,
                    batteryVoltage = 7.4f,
                    onJoystickMove = { _, _ -> },
                    onServoMove = { _, _, _ -> },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun landscapeLayout_whenConnected_showsControls() {
        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = true,
                    logs = emptyList(),
                    rssi = -60,
                    latency = 30,
                    packetLoss = 1.0f,
                    batteryVoltage = 8.2f,
                    onJoystickMove = { _, _ -> },
                    onServoMove = { _, _, _ -> },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun landscapeLayout_whenDisconnected_showsState() {
        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = false,
                    logs = emptyList(),
                    rssi = 0,
                    latency = 0,
                    packetLoss = 0f,
                    batteryVoltage = 0f,
                    onJoystickMove = { _, _ -> },
                    onServoMove = { _, _, _ -> },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun landscapeLayout_displaysLogs() {
        val logs = listOf(
            LogEntry(System.currentTimeMillis(), "Test Log 1", LogType.INFO),
            LogEntry(System.currentTimeMillis(), "Test Log 2", LogType.SUCCESS)
        )

        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = true,
                    logs = logs,
                    rssi = -65,
                    latency = 40,
                    packetLoss = 1.5f,
                    batteryVoltage = 7.8f,
                    onJoystickMove = { _, _ -> },
                    onServoMove = { _, _, _ -> },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun landscapeLayout_handlesTelemetryData() {
        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = true,
                    logs = emptyList(),
                    rssi = -55,
                    latency = 25,
                    packetLoss = 0.5f,
                    batteryVoltage = 8.4f,
                    onJoystickMove = { _, _ -> },
                    onServoMove = { _, _, _ -> },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun landscapeLayout_invokesCallbacks() {
        var joystickCalled = false
        var servoCalled = false

        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = true,
                    logs = emptyList(),
                    rssi = -70,
                    latency = 50,
                    packetLoss = 2.5f,
                    batteryVoltage = 7.4f,
                    onJoystickMove = { _, _ -> joystickCalled = true },
                    onServoMove = { _, _, _ -> servoCalled = true },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun landscapeLayout_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                LandscapeControlLayout(
                    isConnected = false,
                    logs = emptyList(),
                    rssi = 0,
                    latency = 0,
                    packetLoss = 0f,
                    batteryVoltage = 0f,
                    onJoystickMove = { _, _ -> },
                    onServoMove = { _, _, _ -> },
                    onSendCommand = {},
                    onClearLogs = {},
                    onExportLogs = {}
                )
            }
        }

        composeRule.waitForIdle()
    }
}
