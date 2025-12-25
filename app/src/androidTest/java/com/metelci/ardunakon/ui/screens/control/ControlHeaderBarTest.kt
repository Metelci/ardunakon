package com.metelci.ardunakon.ui.screens.control

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for ControlHeaderBar.
 *
 * Tests top bar controls including connection button,
 * menu actions, status display, and navigation.
 */
@RunWith(AndroidJUnit4::class)
class ControlHeaderBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun headerBar_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = false,
                    deviceName = null,
                    connectionType = null,
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("Ardunakon").assertIsDisplayed()
    }

    @Test
    fun headerBar_whenDisconnected_showsConnectButton() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = false,
                    deviceName = null,
                    connectionType = null,
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("Connect").assertIsDisplayed()
    }

    @Test
    fun headerBar_whenConnected_showsDeviceName() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = true,
                    deviceName = "Arduino UNO",
                    connectionType = "BLE",
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("Arduino UNO", substring = true).assertIsDisplayed()
    }

    @Test
    fun headerBar_whenConnected_showsDisconnectButton() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = true,
                    deviceName = "Test Device",
                    connectionType = "BLE",
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("Disconnect", substring = true).assertIsDisplayed()
    }

    @Test
    fun headerBar_connectButton_invokesCallback() {
        var connectCalled = false

        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = false,
                    deviceName = null,
                    connectionType = null,
                    onConnectClick = { connectCalled = true },
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("Connect").performClick()
        assert(connectCalled)
    }

    @Test
    fun headerBar_disconnectButton_invokesCallback() {
        var disconnectCalled = false

        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = true,
                    deviceName = "Device",
                    connectionType = "BLE",
                    onConnectClick = {},
                    onDisconnectClick = { disconnectCalled = true },
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Disconnect", substring = true).performClick()
        assert(disconnectCalled)
    }

    @Test
    fun headerBar_hasMenuButton() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = false,
                    deviceName = null,
                    connectionType = null,
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options", substring = true).assertExists()
    }

    @Test
    fun headerBar_displaysConnectionType() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = true,
                    deviceName = "Device",
                    connectionType = "WiFi",
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("WiFi", substring = true).assertIsDisplayed()
    }

    @Test
    fun headerBar_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                ControlHeaderBar(
                    isConnected = false,
                    deviceName = null,
                    connectionType = null,
                    onConnectClick = {},
                    onDisconnectClick = {},
                    onShowTerminal = {},
                    onShowTelemetryGraph = {},
                    onShowSettings = {},
                    onShowAbout = {},
                    onShowCrashLog = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.waitForIdle()
    }
}
