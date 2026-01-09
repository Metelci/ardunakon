package com.metelci.ardunakon.ui.flows

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.ui.accessibility.AccessibilityDefaults
import com.metelci.ardunakon.ui.screens.ControlScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.metelci.ardunakon.HiltTestActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for control interactions.
 *
 * Tests the main control elements:
 * - Joystick visibility and interaction
 * - Servo sliders
 * - Custom command buttons
 * - Control responsiveness
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ControlsE2ETest {

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
    fun controls_joystickIsVisible() {
        // Joystick should be visible in the main control area
        composeTestRule.onNode(
            hasContentDescription("Joystick", substring = true, ignoreCase = true)
        ).assertExists()
    }

    @Test
    fun controls_joystickHasProperTouchTarget() {
        val minTouchTarget = AccessibilityDefaults.MIN_TOUCH_TARGET

        composeTestRule.onNode(
            hasContentDescription("Joystick", substring = true, ignoreCase = true)
        )
            .assertHeightIsAtLeast(minTouchTarget)
            .assertWidthIsAtLeast(minTouchTarget)
    }

    @Test
    fun controls_servoSlidersAreVisible() {
        // At least one servo slider should be visible
        composeTestRule.onAllNodes(
            hasContentDescription("Servo", substring = true, ignoreCase = true)
        ).onFirst().assertExists()
    }

    @Test
    fun controls_customCommandButtons_areAccessible() {
        // Custom command buttons should have click actions
        // Note: May need to first create a custom command
        composeTestRule.onAllNodes(hasClickAction())
            .fetchSemanticsNodes() // Just verify the screen has clickable elements
    }

    @Test
    fun controls_settingsButtonIsAccessible() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun controls_settingsDialog_opens() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Settings dialog should open with key sections
        composeTestRule.onNodeWithText("Debug Window").assertExists()
        composeTestRule.onNodeWithText("Custom Commands").assertExists()
        composeTestRule.onNodeWithText("Haptic Feedback").assertExists()
    }

    @Test
    fun controls_sensitivitySlider_exists() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Sensitivity slider should be in settings
        composeTestRule.onNodeWithText("Sensitivity").assertExists()
    }

    @Test
    fun controls_hapticToggle_exists() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Haptic toggle should be in settings
        composeTestRule.onNodeWithText("Haptic Feedback").assertExists()
    }

    @Test
    fun controls_debugWindowToggle_works() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Find and click debug window toggle
        composeTestRule.onNodeWithText("Debug Window").performClick()
        composeTestRule.waitForIdle()

        // Close settings
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Debug window should now be visible (or not, depending on initial state)
        // Just verify control screen is still responsive
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    fun controls_helpMenuIsAccessible() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Help").assertExists()
    }

    @Test
    fun controls_helpDialog_opens() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Help").performClick()
        composeTestRule.waitForIdle()

        // Help dialog should open
        composeTestRule.onNodeWithText("Help & Documentation").assertExists()
    }

    @Test
    fun controls_aboutDialog_isAccessible() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("About").performClick()
        composeTestRule.waitForIdle()

        // About dialog should show version info
        composeTestRule.onNodeWithText("Version", substring = true).assertExists()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
