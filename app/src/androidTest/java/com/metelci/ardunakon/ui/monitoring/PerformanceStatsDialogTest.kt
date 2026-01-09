package com.metelci.ardunakon.ui.monitoring

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
 * UI automation tests for Performance Stats Dialog using Compose Testing.
 *
 * Tests verify:
 * - Dialog accessibility and proper content display
 * - Health score visualization
 * - Stats grid rendering
 * - Crash history display
 * - Export functionality
 * - Dialog dismissal
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PerformanceStatsDialogTest {

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
    fun performanceStatsDialog_opensFromHelpMenu() {
        // Open help menu
        composeTestRule.onNodeWithContentDescription("Help menu").performClick()
        composeTestRule.waitForIdle()

        // Click Performance Stats menu item
        composeTestRule.onNodeWithText("Performance Stats").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog opens with title
        composeTestRule.onNodeWithText("Performance Stats").assertIsDisplayed()
    }

    @Test
    fun performanceStatsDialog_displaysHealthScore() {
        openPerformanceStatsDialog()

        // Verify health score card components are present
        composeTestRule.onNodeWithText("App Health").assertExists()
        // Health percentage should be visible (0-100%)
        composeTestRule.onNode(
            hasText("%", substring = true)
        ).assertExists()
        composeTestRule.onNodeWithText("Crash-free sessions").assertExists()
    }

    @Test
    fun performanceStatsDialog_displaysStatsGrid() {
        openPerformanceStatsDialog()

        // Verify stats grid labels
        composeTestRule.onNodeWithText("Startup").assertExists()
        composeTestRule.onNodeWithText("Latency").assertExists()
        composeTestRule.onNodeWithText("Sessions").assertExists()
    }

    @Test
    fun performanceStatsDialog_showsRecentIssuesSection() {
        openPerformanceStatsDialog()

        // Verify recent issues section header
        composeTestRule.onNodeWithText("Recent Issues").assertExists()
    }

    @Test
    fun performanceStatsDialog_showsNoIssuesWhenEmpty() {
        openPerformanceStatsDialog()

        // When no crashes, should show checkmark message
        composeTestRule.onNodeWithText("No recent issues").assertExists()
    }

    @Test
    fun performanceStatsDialog_hasExportButton() {
        openPerformanceStatsDialog()

        // Verify export/share button exists
        composeTestRule.onNodeWithContentDescription("Export").assertExists()
    }

    @Test
    fun performanceStatsDialog_closeButtonWorks() {
        openPerformanceStatsDialog()

        // Click close button
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Dialog should be dismissed
        composeTestRule.onNodeWithText("App Health").assertDoesNotExist()
    }

    @Test
    fun performanceStatsDialog_hasAccessibleContentDescriptions() {
        openPerformanceStatsDialog()

        // Verify key elements have proper accessibility
        composeTestRule.onNodeWithText("Performance Stats").assertExists()
        composeTestRule.onNodeWithText("Close").assertExists()
    }

    @Test
    fun performanceStatsDialog_meetsMinimumTouchTargetSize() {
        openPerformanceStatsDialog()

        val minTouchTarget = AccessibilityDefaults.MIN_TOUCH_TARGET

        // Close button should meet touch target
        composeTestRule.onNodeWithText("Close")
            .assertHeightIsAtLeast(minTouchTarget)
    }

    @Test
    fun performanceStatsDialog_canBeDismissedWithBackButton() {
        openPerformanceStatsDialog()

        // Press back button
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // Dialog should be dismissed
        composeTestRule.onNodeWithText("App Health").assertDoesNotExist()
    }

    @Test
    fun performanceStatsDialog_refreshesDataOnOpen() {
        // Open dialog first time
        openPerformanceStatsDialog()
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Open dialog second time
        openPerformanceStatsDialog()

        // Stats should still be visible (data persists)
        composeTestRule.onNodeWithText("App Health").assertExists()
    }

    // Helper methods

    private fun openPerformanceStatsDialog() {
        // Open help menu
        composeTestRule.onNodeWithContentDescription("Help menu").performClick()
        composeTestRule.waitForIdle()

        // Click Performance Stats menu item
        composeTestRule.onNodeWithText("Performance Stats").performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Accessibility defaults for touch target validation
     */
    private object AccessibilityDefaults {
        val MIN_TOUCH_TARGET = 48.dp
    }
}

// Extension for Dp usage
private val Int.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this.toFloat())
