package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.*
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1024dp-h2048dp")
class TelemetryGraphDialogTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun telemetryGraphDialog_shows_empty_states_and_updates_units() {
        val manager = TelemetryHistoryManager()
        var dismissed = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TelemetryGraphDialog(
                telemetryHistoryManager = manager,
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.mainClock.advanceTimeBy(2000)

        // Title
        composeTestRule.onNodeWithText("Telemetry Graphs").assertExists()

        // Default Tab content
        composeTestRule.onNodeWithText("Units: Volts", substring = true).assertExists()
        composeTestRule.onNodeWithText("No battery data", substring = true).assertExists()

        // Tab RSSI
        composeTestRule.onNodeWithText("RSSI", substring = false).performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Units: Signal strength", substring = true).assertExists()

        // Tab Latency
        // The tab text is Exactly "Latency". The unit label is "Units: Latency (ms)".
        composeTestRule.onNodeWithText("Latency", substring = false).performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Units: Latency", substring = true).assertExists()

        // Tab Quality
        // The tab text is Exactly "Quality". The unit label is "Units: Connection quality (%)".
        composeTestRule.onNodeWithText("Quality", substring = false).performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Units: Connection quality", substring = true).assertExists()

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)
        assertTrue(dismissed)
    }

    @Test
    fun telemetryGraphDialog_clears_history_and_shows_chart_label() {
        val manager = TelemetryHistoryManager()
        manager.recordBattery(12.5f)
        manager.recordRssi(-55)
        manager.recordRtt(42)
        manager.recordPacketLoss(packetsSent = 100, packetsReceived = 95, packetsDropped = 3, packetsFailed = 2)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TelemetryGraphDialog(
                telemetryHistoryManager = manager,
                onDismiss = {}
            )
        }
        composeTestRule.mainClock.advanceTimeBy(2000)

        // Title of the chart
        composeTestRule.onNodeWithText("Battery Voltage", substring = true).assertExists()

        composeTestRule.onNodeWithContentDescription("Clear History").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        assertEquals(0, manager.getHistorySize())
    }
}
