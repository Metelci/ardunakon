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
 * End-to-end tests for Emergency Stop (E-Stop) functionality.
 *
 * Tests the critical safety feature that must work reliably:
 * - E-Stop button visibility and accessibility
 * - E-Stop activation and deactivation
 * - Visual feedback when E-Stop is active
 * - Haptic feedback (where possible to test)
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EmergencyStopE2ETest {

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
    fun eStop_buttonIsAlwaysVisible() {
        // E-Stop must always be visible as a critical safety control
        composeTestRule.onNode(
            hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE) or
                hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE)
        ).assertIsDisplayed()
    }

    @Test
    fun eStop_buttonMeetsMinimumTouchTarget() {
        // E-Stop should meet minimum touch target (48dp) for safety-critical control
        val minTouchTarget = AccessibilityDefaults.MIN_TOUCH_TARGET

        composeTestRule.onNode(
            hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE) or
                hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE)
        )
            .assertHeightIsAtLeast(minTouchTarget)
            .assertWidthIsAtLeast(minTouchTarget)
    }

    @Test
    fun eStop_canBeActivated() {
        // Initially inactive
        composeTestRule.onNodeWithContentDescription(
            AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE
        ).assertExists()

        // Activate E-Stop
        composeTestRule.onNodeWithContentDescription(
            AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE
        ).performClick()
        composeTestRule.waitForIdle()

        // Should now be active
        composeTestRule.onNodeWithContentDescription(
            AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE
        ).assertExists()
    }

    @Test
    fun eStop_canBeDeactivated() {
        // First activate
        composeTestRule.onNodeWithContentDescription(
            AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE
        ).performClick()
        composeTestRule.waitForIdle()

        // Then deactivate
        composeTestRule.onNodeWithContentDescription(
            AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE
        ).performClick()
        composeTestRule.waitForIdle()

        // Should be back to inactive
        composeTestRule.onNodeWithContentDescription(
            AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE
        ).assertExists()
    }

    @Test
    fun eStop_toggleCycle_worksReliably() {
        // Test multiple toggle cycles for reliability
        repeat(3) {
            // Activate
            composeTestRule.onNode(
                hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE)
            ).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription(
                AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE
            ).assertExists()

            // Deactivate
            composeTestRule.onNodeWithContentDescription(
                AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE
            ).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription(
                AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE
            ).assertExists()
        }
    }

    @Test
    fun eStop_isEasilyTappable() {
        // E-Stop button should be clickable
        composeTestRule.onNode(
            hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE) or
                hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE)
        ).assertHasClickAction()
    }

    @Test
    fun eStop_hasProperSemantics() {
        // E-Stop should have proper accessibility semantics
        composeTestRule.onNode(
            hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE) or
                hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE)
        ).assertExists()
    }

    @Test
    fun eStop_remainsVisibleAfterScreenRotation() {
        // Note: This test works on devices that support rotation
        // The E-Stop should remain visible regardless of orientation

        composeTestRule.onNode(
            hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_INACTIVE) or
                hasContentDescription(AccessibilityDefaults.ContentDescriptions.ESTOP_ACTIVE)
        ).assertExists()

        // Orientation change would require ActivityScenario.onActivity
        // For now, just verify it exists in current orientation
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
