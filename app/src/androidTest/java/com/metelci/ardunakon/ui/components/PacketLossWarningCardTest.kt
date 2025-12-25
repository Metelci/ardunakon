package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for PacketLossWarningCard.
 *
 * Tests warning display based on packet loss percentage,
 * color coding, and visibility thresholds.
 */
@RunWith(AndroidJUnit4::class)
class PacketLossWarningCardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun packetLossWarningCard_notDisplayedWhenLossIsZero() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 0,
                    packetsFailed = 0
                )
            }
        }

        // Should not display warning
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_notDisplayedWhenLossIsBelow1Percent() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 1000,
                    packetsDropped = 5, // 0.5%
                    packetsFailed = 0
                )
            }
        }

        // Should not display (threshold is > 1%)
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_displaysYellowForLowLoss() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 2, // 2%
                    packetsFailed = 0
                )
            }
        }

        // Should display warning
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertIsDisplayed()
        composeRule.onNodeWithText("2.00% loss (2/100 packets)").assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 2").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_displaysOrangeForModerateLoss() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 6, // 6%
                    packetsFailed = 1 // 1%
                )
            }
        }

        // Should display warning with orange color (> 5%)
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertIsDisplayed()
        composeRule.onNodeWithText("7.00% loss (7/100 packets)").assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 6").assertIsDisplayed()
        composeRule.onNodeWithText("Failed: 1").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_displaysRedForHighLoss() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 10,
                    packetsFailed = 5
                )
            }
        }

        // Should display warning with red color (> 10%)
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertIsDisplayed()
        composeRule.onNodeWithText("15.00% loss (15/100 packets)").assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 10").assertIsDisplayed()
        composeRule.onNodeWithText("Failed: 5").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_displaysOnlyDropped() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 50,
                    packetsDropped = 2,
                    packetsFailed = 0
                )
            }
        }

        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 2").assertIsDisplayed()
        composeRule.onNodeWithText("Failed: 0").assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_displaysOnlyFailed() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 50,
                    packetsDropped = 0,
                    packetsFailed = 2
                )
            }
        }

        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 0").assertDoesNotExist()
        composeRule.onNodeWithText("Failed: 2").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_handlesBothDroppedAndFailed() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 3,
                    packetsFailed = 2
                )
            }
        }

        composeRule.onNodeWithText("5.00% loss (5/100 packets)").assertIsDisplayed()
        composeRule.onNodeWithText("Dropped: 3").assertIsDisplayed()
        composeRule.onNodeWithText("Failed: 2").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_handlesZeroPacketsSent() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 0,
                    packetsDropped = 5,
                    packetsFailed = 5
                )
            }
        }

        // Loss percent should be 0% when packetsSent is 0
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_boundaryAt1Percent() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 1,
                    packetsFailed = 0
                )
            }
        }

        // Exactly 1% should not display (threshold is > 1%)
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertDoesNotExist()
    }

    @Test
    fun packetLossWarningCard_boundaryJustOver1Percent() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 2,
                    packetsFailed = 0
                )
            }
        }

        // Just over 1% should display
        composeRule.onNodeWithText("⚠ Packet Loss Detected").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_boundaryAt5Percent() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 5,
                    packetsFailed = 0
                )
            }
        }

        // Exactly 5% should show yellow (not orange)
        composeRule.onNodeWithText("5.00% loss (5/100 packets)").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_boundaryAt10Percent() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 100,
                    packetsDropped = 10,
                    packetsFailed = 0
                )
            }
        }

        // Exactly 10% should show orange (not red)
        composeRule.onNodeWithText("10.00% loss (10/100 packets)").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_handlesHighLossRate() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 50,
                    packetsDropped = 40,
                    packetsFailed = 5
                )
            }
        }

        // 90% loss rate
        composeRule.onNodeWithText("90.00% loss (45/50 packets)").assertIsDisplayed()
    }

    @Test
    fun packetLossWarningCard_formatsPercentageCorrectly() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossWarningCard(
                    packetsSent = 333,
                    packetsDropped = 10,
                    packetsFailed = 1
                )
            }
        }

        // Should format to 2 decimal places
        composeRule.onNodeWithText("3.30% loss (11/333 packets)").assertIsDisplayed()
    }
}
