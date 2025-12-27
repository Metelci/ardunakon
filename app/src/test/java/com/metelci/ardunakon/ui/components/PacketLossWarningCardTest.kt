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
class PacketLossWarningCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun packetLossWarningCard_isHidden_atOrBelowThreshold() {
        composeTestRule.setContent {
            PacketLossWarningCard(
                packetsSent = 100,
                packetsDropped = 1,
                packetsFailed = 0
            )
        }

        composeTestRule.onNodeWithText("⚠ Packet Loss Detected").assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_shows_loss_details_and_counters() {
        composeTestRule.setContent {
            PacketLossWarningCard(
                packetsSent = 100,
                packetsDropped = 6,
                packetsFailed = 2
            )
        }

        composeTestRule.onNodeWithText("⚠ Packet Loss Detected").assertExists()
        composeTestRule.onNodeWithText("8.00% loss (8/100 packets)").assertExists()
        composeTestRule.onNodeWithText("Dropped: 6").assertExists()
        composeTestRule.onNodeWithText("Failed: 2").assertExists()
    }

    @Test
    fun packetLossWarningCard_hides_dropped_label_when_zero() {
        composeTestRule.setContent {
            PacketLossWarningCard(
                packetsSent = 100,
                packetsDropped = 0,
                packetsFailed = 2
            )
        }

        composeTestRule.onNodeWithText("⚠ Packet Loss Detected").assertExists()
        composeTestRule.onNodeWithText("Dropped:", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Failed: 2").assertExists()
    }
}
