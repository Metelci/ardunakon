package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.*
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1024dp-h2048dp")
class HelpDialogAccessibilityTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun helpDialog_exposes_tab_and_action_labels() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(3000)

        // Title and tabs
        composeTestRule.onNode(hasText("Help & Documentation", substring = true)).assertExists()
        composeTestRule.onNode(hasText("Setup", substring = false).and(hasClickAction())).assertExists()
        composeTestRule.onNode(hasText("Compatibility", substring = false).and(hasClickAction())).assertExists()

        // Scroll to the end to ensure everything is composed (only 2 items total: 0=text, 1=buttons)
        composeTestRule.onNodeWithTag("HelpLazyColumn").performScrollToIndex(1)

        // Items in LazyColumn
        composeTestRule.onNode(hasText("Full Guide", substring = true).and(hasClickAction())).assertExists()
        composeTestRule.onNode(hasText("Arduino Cloud", substring = true).and(hasClickAction())).assertExists()

        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun telemetryGraphDialog_exposes_icon_descriptions() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TelemetryGraphDialog(
                telemetryHistoryManager = TelemetryHistoryManager(),
                onDismiss = {}
            )
        }

        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.onNode(hasText("Telemetry Graphs", substring = true)).assertExists()
        composeTestRule.onNodeWithContentDescription("Clear History").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }
}
