package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiComponentsComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun autoReconnectToggle_click_toggles_state_and_semantics() {
        composeRule.setContent {
            var enabled by remember { mutableStateOf(false) }
            MaterialTheme {
                AutoReconnectToggle(enabled = enabled, onToggle = { enabled = it })
            }
        }

        composeRule
            .onNodeWithContentDescription("Auto-reconnect disabled. Tap to enable automatic reconnection.")
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithContentDescription("Auto-reconnect enabled. Tap to disable automatic reconnection.")
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithContentDescription("Auto-reconnect disabled. Tap to enable automatic reconnection.")
            .assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_hidden_when_loss_is_low() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(packetsSent = 100, packetsDropped = 0, packetsFailed = 1)
            }
        }

        composeRule.onNodeWithText("Packet Loss Detected", substring = true).assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_shows_details_when_loss_exceeds_threshold() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(packetsSent = 100, packetsDropped = 2, packetsFailed = 0)
            }
        }

        composeRule.onNodeWithText("Packet Loss Detected", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 2").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_renders_label_for_na_and_rssi() {
        composeRule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    SignalStrengthIcon(rssi = 0, showLabels = true)
                    SignalStrengthIcon(rssi = -60, showLabels = true)
                    SignalStrengthIcon(rssi = -60, showLabels = false)
                }
            }
        }

        composeRule.onNodeWithText("N/A").assertIsDisplayed()
        composeRule.onNodeWithText("-60 dBm").assertIsDisplayed()
    }
}
