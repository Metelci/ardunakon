package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelemetryGraphDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Telemetry Graphs").assertExists()
        composeTestRule.onNodeWithText("Units: Volts (V)").assertExists()
        composeTestRule.onNodeWithText("No battery data available", substring = true).assertExists()

        composeTestRule.onNodeWithText("RSSI").performClick()
        composeTestRule.onNodeWithText("Units: Signal strength (dBm)").assertExists()
        composeTestRule.onNodeWithText("No RSSI data available", substring = true).assertExists()

        composeTestRule.onNodeWithText("Latency").performClick()
        composeTestRule.onNodeWithText("Units: Latency (ms)").assertExists()
        composeTestRule.onNodeWithText("No RTT data available", substring = true).assertExists()

        composeTestRule.onNodeWithText("Quality").performClick()
        composeTestRule.onNodeWithText("Units: Connection quality (%)").assertExists()
        composeTestRule.onNodeWithText("No quality data available", substring = true).assertExists()

        composeTestRule.onNodeWithContentDescription("Close").performClick()
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
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNodeWithText("Battery Voltage (V)").assertExists()
        composeTestRule.onNodeWithText("No battery data available", substring = true).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Clear History").performClick()

        assertEquals(0, manager.getHistorySize())
    }
}
