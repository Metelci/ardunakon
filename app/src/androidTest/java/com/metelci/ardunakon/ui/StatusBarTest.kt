package com.metelci.ardunakon.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.metelci.ardunakon.ui.components.SignalStrengthIcon
import org.junit.Rule
import org.junit.Test

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

import com.metelci.ardunakon.MainActivity

@HiltAndroidTest
class StatusBarTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun signalStrengthShowsRssiText() {
        composeRule.setContent {
            SignalStrengthIcon(rssi = -60)
        }
        composeRule.onNodeWithText("-60 dBm").assertIsDisplayed()
    }
}
