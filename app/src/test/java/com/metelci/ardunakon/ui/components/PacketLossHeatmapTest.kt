package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PacketLossHeatmapTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun packetLossHeatmap_shows_legend_and_empty_state() {
        composeTestRule.setContent {
            PacketLossHeatmap(data = emptyList())
        }

        composeTestRule.onNodeWithText("Good (0-2%)").assertExists()
        composeTestRule.onNodeWithText("Moderate (2-10%)").assertExists()
        composeTestRule.onNodeWithText("High (>10%)").assertExists()
        composeTestRule.onNodeWithText("No packet loss data available").assertExists()
    }

    @Test
    fun packetLossHeatmap_hides_empty_state_when_data_present() {
        composeTestRule.setContent {
            PacketLossHeatmap(data = listOf(0.0f, 0.03f, 0.12f))
        }

        composeTestRule.onNodeWithText("No packet loss data available").assertDoesNotExist()
        composeTestRule.onNodeWithText("Good (0-2%)").assertExists()
        composeTestRule.onNodeWithText("Moderate (2-10%)").assertExists()
        composeTestRule.onNodeWithText("High (>10%)").assertExists()
    }
}
