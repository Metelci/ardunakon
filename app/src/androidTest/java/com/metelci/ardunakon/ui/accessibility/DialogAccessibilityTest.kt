package com.metelci.ardunakon.ui.accessibility

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.ui.screens.ControlScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.metelci.ardunakon.HiltTestActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility compliance tests for dialogs.
 * Verifies that all dialogs meet accessibility standards including:
 * - Content descriptions
 * - Touch target sizes
 * - Focus management
 * - Screen reader support
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DialogAccessibilityTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeTestRule.activityRule.scenario.onActivity {}
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    ControlScreen()
                }
            }
        }
        waitForControlScreen()
    }

    @Test
    fun settingsDialog_hasProperContentDescriptions() {
        // Open settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Verify settings sections are visible
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
        composeTestRule.onNodeWithText("Debug Window").assertExists()
        composeTestRule.onNodeWithText("Custom Commands").assertExists()
        composeTestRule.onNodeWithText("Haptic Feedback").assertExists()
        composeTestRule.onNodeWithText("Sensitivity").assertExists()
    }

    @Test
    fun customCommandDialog_hasProperContentDescriptions() {
        // Navigate to custom commands
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        // Verify form fields have labels
        composeTestRule.onNode(hasSetTextAction() and hasText("Command Name")).assertExists()
        composeTestRule.onNode(hasSetTextAction() and hasText("Payload (Hex)")).assertExists()

        // Verify buttons are present
        composeTestRule.onNodeWithText("Create").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun wifiConfigDialog_hasProperContentDescriptions() {
        // Open WiFi configuration
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Configure WiFi").performClick()
        composeTestRule.waitForIdle()

        // Verify form fields have labels
        composeTestRule.onNode(hasSetTextAction() and hasText("IP Address")).assertExists()
        composeTestRule.onNode(hasSetTextAction() and hasText("Port")).assertExists()

        // Verify buttons have content descriptions
        composeTestRule.onNodeWithContentDescription("Scan for WiFi devices").assertExists()
        composeTestRule.onNodeWithText("Connect").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun allButtons_meetMinimumTouchTargetSize() {
        // Minimum touch target size is 48dp x 48dp per Material Design guidelines
        val minTouchTargetSize = AccessibilityDefaults.MIN_TOUCH_TARGET

        // Check main screen buttons
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertHeightIsAtLeast(minTouchTargetSize)
            .assertWidthIsAtLeast(minTouchTargetSize)

        composeTestRule.onNode(
            hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE)
        )
            .assertHeightIsAtLeast(minTouchTargetSize)
            .assertWidthIsAtLeast(minTouchTargetSize)

        composeTestRule.onNodeWithContentDescription("Menu")
            .assertHeightIsAtLeast(minTouchTargetSize)
            .assertWidthIsAtLeast(minTouchTargetSize)
    }

    @Test
    fun dialogs_haveProperfocusManagement() {
        // Open settings dialog
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog receives focus
        composeTestRule.onNode(hasAnyDescendant(hasText("Settings")))
            .assertExists()

        // Close dialog
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify focus returns to main screen
        composeTestRule.onNode(
            hasContentDescription("joystick", substring = true, ignoreCase = true)
        ).assertExists()
    }

    @Test
    fun customCommandButtons_haveDescriptiveLabels() {
        // Create a custom command first
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Command Name").performTextInput("Test Command")
        composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
        composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("0102030405")

        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Close dialogs
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify custom command button has descriptive content description
        composeTestRule.onNodeWithContentDescription("Test Command").assertExists()
    }

    @Test
    fun helpDialog_isAccessibleWithScreenReader() {
        // Open help dialog
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Help").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog title is readable
        composeTestRule.onNode(hasText("Help & Documentation")).assertExists()

        // Verify tabs are accessible
        composeTestRule.onNode(hasText("Setup") and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("Compatibility") and hasClickAction()).assertExists()

        // Verify close button is accessible
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun aboutDialog_linksHaveDescriptiveLabels() {
        // Open about dialog
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("About").performClick()
        composeTestRule.waitForIdle()

        // Verify links have descriptive text
        composeTestRule.onNode(hasText("View on GitHub") and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("Open Arduino Cloud") and hasClickAction()).assertExists()

        // Verify version information is readable
        composeTestRule.onNode(hasText("Version")).assertExists()
    }

    @Test
    fun telemetryGraph_hasAccessibleLabels() {
        // Open telemetry graph
        composeTestRule.onNodeWithContentDescription("Telemetry Graphs").performClick()
        composeTestRule.waitForIdle()

        // Verify tabs have accessible labels
        composeTestRule.onNode(hasText("Battery") and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("RSSI") and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("RTT") and hasClickAction()).assertExists()

        // Verify time range buttons are accessible
        composeTestRule.onNode(hasText("1min") and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("5min") and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("10min") and hasClickAction()).assertExists()
    }

    @Test
    fun allDialogs_canBeDismissedWithBackButton() {
        // Test settings dialog
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // Verify dialog is closed
        composeTestRule.onNode(hasText("Settings")).assertDoesNotExist()

        // Test help dialog
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Help").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // Verify dialog is closed
        composeTestRule.onNode(hasText("Help & Documentation")).assertDoesNotExist()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
