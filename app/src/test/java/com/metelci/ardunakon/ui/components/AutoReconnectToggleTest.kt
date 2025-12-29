package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoReconnectToggleTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun autoReconnectToggle_reports_disabled_state() {
        composeTestRule.setContent {
            AutoReconnectToggle(enabled = false, onToggle = {})
        }

        composeTestRule.onNodeWithContentDescription("Auto-reconnect disabled", substring = true).assertExists()
    }

    @Test
    fun autoReconnectToggle_reports_enabled_state() {
        composeTestRule.setContent {
            AutoReconnectToggle(enabled = true, onToggle = {})
        }

        composeTestRule.onNodeWithContentDescription("Auto-reconnect enabled", substring = true).assertExists()
    }

    @Test
    fun autoReconnectToggle_invokes_callback_with_new_state() {
        var toggled: Boolean? = null

        composeTestRule.setContent {
            AutoReconnectToggle(enabled = false, onToggle = { toggled = it })
        }

        composeTestRule.onNodeWithContentDescription("Auto-reconnect disabled", substring = true).performClick()

        composeTestRule.runOnIdle {
            assertEquals(true, toggled)
        }
    }
}
