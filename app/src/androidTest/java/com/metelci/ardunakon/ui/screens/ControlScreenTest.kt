package com.metelci.ardunakon.ui.screens

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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.ui.components.StatusCard
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ControlScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun statusCard_disconnected_renders_expected_text_and_semantics() {
        composeTestRule.setContent {
            MaterialTheme {
                StatusCard(
                    label = "Device",
                    state = ConnectionState.DISCONNECTED,
                    rssi = 0,
                    hasCrashLog = false,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Device: Disconnected").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Device status: Disconnected").assertIsDisplayed()
    }

    @Test
    fun statusCard_connected_renders_expected_text() {
        composeTestRule.setContent {
            MaterialTheme {
                StatusCard(
                    label = "Device",
                    state = ConnectionState.CONNECTED,
                    rssi = -60,
                    hasCrashLog = false,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Device: Connected").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Device status: Connected").assertIsDisplayed()
    }

    @Test
    fun statusCard_click_invokes_callback() {
        composeTestRule.setContent {
            var clicks by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    StatusCard(
                        label = "Device",
                        state = ConnectionState.DISCONNECTED,
                        rssi = 0,
                        hasCrashLog = false,
                        onClick = { clicks++ }

                    )
                    Text("clicks=$clicks")
                }
            }
        }

        composeTestRule.onNodeWithText("Device: Disconnected").performClick()
        composeTestRule.onNodeWithText("clicks=1").assertIsDisplayed()
    }
}
