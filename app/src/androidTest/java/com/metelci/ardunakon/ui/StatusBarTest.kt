package com.metelci.ardunakon.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import org.junit.Rule
import org.junit.Test

class StatusBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signalStrengthShowsRssiText() {
        composeRule.setContent {
            SignalStrengthIcon(rssi = -60)
        }
        composeRule.onNodeWithText("-60 dBm").assertIsDisplayed()
    }
}
