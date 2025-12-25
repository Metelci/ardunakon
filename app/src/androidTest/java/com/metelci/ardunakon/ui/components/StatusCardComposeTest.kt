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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.bluetooth.ConnectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusCardComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun statusCard_withCrashLog_shows_icon_and_click_invokes_callback() {
        composeRule.setContent {
            var clicked by remember { mutableStateOf(0) }
            var crashClicked by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    StatusCard(
                        label = "Device",
                        state = ConnectionState.CONNECTED,
                        rssi = -60,
                        hasCrashLog = true,
                        onClick = { clicked++ },
                        onCrashLogClick = { crashClicked++ }
                    )
                    Text("clicked=$clicked crashClicked=$crashClicked")
                }
            }
        }

        composeRule.onNodeWithText("Device: Connected").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("clicked=1 crashClicked=0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("View Crash Log").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("clicked=1 crashClicked=1").assertIsDisplayed()
    }
}

