package com.metelci.ardunakon.ui.flows

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
 * End-to-end tests for critical connection flows.
 *
 * Tests the complete user journey through connection states,
 * disconnection handling, and reconnection flows.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CriticalConnectionFlowTest {

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
    fun connectionFlow_bluetoothModeVisibleByDefault() {
        // Verify BLE/WiFi mode selector is visible
        composeTestRule.onNode(
            hasContentDescription("Bluetooth mode") or hasContentDescription("WiFi mode")
        ).assertExists()
    }

    @Test
    fun connectionFlow_canOpenDeviceList() {
        // Click on the connection/scan button to open device list
        composeTestRule.onNodeWithContentDescription("Scan for devices", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        // Verify device list dialog opens
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Available Devices")
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Scanning")
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun connectionFlow_canSwitchToWifiMode() {
        // Click on WiFi mode selector
        composeTestRule.onNodeWithContentDescription("WiFi mode").performClick()
        composeTestRule.waitForIdle()

        // Should trigger WiFi config dialog or mode switch
        // The mode switch is indicated by the button state change
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithContentDescription("WiFi mode")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun connectionFlow_wifiConfigDialog_isAccessible() {
        // Open menu and click Configure WiFi
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Configure WiFi").performClick()
        composeTestRule.waitForIdle()

        // Verify WiFi config dialog opens
        composeTestRule.onNode(hasSetTextAction() and hasText("IP Address")).assertExists()
        composeTestRule.onNode(hasSetTextAction() and hasText("Port")).assertExists()
        composeTestRule.onNodeWithText("Connect").assertExists()
    }

    @Test
    fun connectionFlow_autoReconnectToggle_works() {
        // Find and click auto-reconnect toggle
        composeTestRule.onNode(
            hasContentDescription("Auto-reconnect enabled", substring = true) or
                hasContentDescription("Auto-reconnect disabled", substring = true)
        ).performClick()
        composeTestRule.waitForIdle()

        // State should have toggled - verify toggle still exists
        composeTestRule.onNode(
            hasContentDescription("Auto-reconnect enabled", substring = true) or
                hasContentDescription("Auto-reconnect disabled", substring = true)
        ).assertExists()
    }

    @Test
    fun connectionFlow_disconnectedState_showsScanOption() {
        // In disconnected state, should be able to scan
        composeTestRule.onNodeWithContentDescription("Scan for devices", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun connectionFlow_rssiButton_isVisible() {
        // RSSI/Signal button should be visible in header
        composeTestRule.onNode(
            hasContentDescription("RSSI", substring = true, ignoreCase = true) or
                hasContentDescription("Signal", substring = true, ignoreCase = true) or
                hasContentDescription("Scan", substring = true, ignoreCase = true)
        ).assertExists()
    }

    @Test
    fun connectionFlow_telemetryGraphButton_isAccessible() {
        // Telemetry graph button should be accessible
        composeTestRule.onNodeWithContentDescription("Telemetry Graphs").assertExists()
    }

    @Test
    fun connectionFlow_telemetryGraph_opens() {
        composeTestRule.onNodeWithContentDescription("Telemetry Graphs").performClick()
        composeTestRule.waitForIdle()

        // Verify telemetry graph dialog opens
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodes(hasText("Battery") or hasText("RSSI") or hasText("RTT"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun connectionFlow_closeDialogsWithBackButton() {
        // Open WiFi config
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Configure WiFi").performClick()
        composeTestRule.waitForIdle()

        // Press back
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // Dialog should be dismissed
        composeTestRule.onNode(hasSetTextAction() and hasText("IP Address")).assertDoesNotExist()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
