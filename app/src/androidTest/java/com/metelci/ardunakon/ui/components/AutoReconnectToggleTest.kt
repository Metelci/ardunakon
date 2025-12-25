package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for AutoReconnectToggle.
 *
 * Tests toggle rendering, state changes, callback invocation,
 * and accessibility semantics.
 */
@RunWith(AndroidJUnit4::class)
class AutoReconnectToggleTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun toggle_renders_enabled_state() {
        composeRule.setContent {
            MaterialTheme {
                AutoReconnectToggle(
                    enabled = true,
                    onToggle = {}
                )
            }
        }

        // Should have accessibility description for enabled state
        composeRule
            .onNodeWithContentDescription("Auto-reconnect enabled", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun toggle_renders_disabled_state() {
        composeRule.setContent {
            MaterialTheme {
                AutoReconnectToggle(
                    enabled = false,
                    onToggle = {}
                )
            }
        }

        // Should have accessibility description for disabled state
        composeRule
            .onNodeWithContentDescription("Auto-reconnect disabled", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun toggle_invokes_callback_with_new_state_when_clicked() {
        var currentState = false
        var toggledToState: Boolean? = null

        composeRule.setContent {
            MaterialTheme {
                AutoReconnectToggle(
                    enabled = currentState,
                    onToggle = { newState -> toggledToState = newState }
                )
                androidx.compose.material3.Text("State: $currentState")
            }
        }

        // Click toggle
        composeRule
            .onNodeWithContentDescription("Auto-reconnect disabled", substring = true)
            .performClick()

        // Verify callback was invoked with opposite state
        assert(toggledToState == true)
    }

    @Test
    fun toggle_changes_visual_state_when_toggled() {
        var isEnabled = false

        composeRule.setContent {
            MaterialTheme {
                AutoReconnectToggle(
                    enabled = isEnabled,
                    onToggle = { isEnabled = it }
                )
            }
        }

        // Initially disabled
        composeRule
            .onNodeWithContentDescription("Auto-reconnect disabled", substring = true)
            .assertIsDisplayed()

        // Click to enable
        composeRule
            .onNodeWithContentDescription("Auto-reconnect disabled", substring = true)
            .performClick()

        // Now should show as enabled
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription("Auto-reconnect enabled", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun toggle_multiple_clicks_alternate_state() {
        var isEnabled = false
        var toggleCount = 0

        composeRule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    AutoReconnectToggle(
                        enabled = isEnabled,
                        onToggle = { 
                            isEnabled = it
                            toggleCount++
                        }
                    )
                    androidx.compose.material3.Text("Toggles: $toggleCount")
                }
            }
        }

        // Click 3 times
        repeat(3) {
            composeRule
                .onNode(hasContentDescription("Auto-reconnect", substring = true))
                .performClick()
            composeRule.waitForIdle()
        }

        // Should have toggled 3 times
        composeRule.onNodeWithText("Toggles: 3").assertIsDisplayed()

        // Final state should be enabled (started false, toggled 3 times)
        composeRule
            .onNodeWithContentDescription("Auto-reconnect enabled", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun toggle_respects_custom_size() {
        composeRule.setContent {
            MaterialTheme {
                // Custom size of 64dp (larger than default 48dp)
                AutoReconnectToggle(
                    enabled = true,
                    onToggle = {},
                    size = androidx.compose.ui.unit.dp(64)
                )
            }
        }

        // Should render without issues (size is applied)
        composeRule
            .onNodeWithContentDescription("Auto-reconnect enabled", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun toggle_accessibility_labels_are_descriptive() {
        composeRule.setContent {
            MaterialTheme {
                AutoReconnectToggle(
                    enabled = true,
                    onToggle = {}
                )
            }
        }

        // Should have full accessibility description
        composeRule
            .onNode(
                hasContentDescription("Auto-reconnect enabled. Tap to disable automatic reconnection.")
            )
            .assertIsDisplayed()
    }

    @Test
    fun toggle_disabled_accessibility_label() {
        composeRule.setContent {
            MaterialTheme {
                AutoReconnectToggle(
                    enabled = false,
                    onToggle = {}
                )
            }
        }

        // Should have full accessibility description for disabled state
        composeRule
            .onNode(
                hasContentDescription("Auto-reconnect disabled. Tap to enable automatic reconnection.")
            )
            .assertIsDisplayed()
    }
}
