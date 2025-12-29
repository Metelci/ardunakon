package com.metelci.ardunakon

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityDialogsTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun bluetoothOffDialog_shows_actions_and_handles_enable() {
        var enableTapped = false
        var dismissTapped = false

        composeTestRule.setContent {
            BluetoothOffDialog(
                onDismiss = { dismissTapped = true },
                onTurnOn = { enableTapped = true }
            )
        }

        composeTestRule.onNodeWithText("Turn on Bluetooth").assertExists()
        composeTestRule.onNodeWithText("Enable").performClick()
        composeTestRule.onNodeWithText("Later").performClick()

        composeTestRule.runOnIdle {
            assertTrue(enableTapped)
            assertTrue(dismissTapped)
        }
    }

    @Test
    fun notificationPermissionDialog_shows_actions_and_handles_retry() {
        var retryTapped = false
        var settingsTapped = false

        composeTestRule.setContent {
            NotificationPermissionDialog(
                onDismiss = {},
                onOpenSettings = { settingsTapped = true },
                onRetry = { retryTapped = true }
            )
        }

        composeTestRule.onNodeWithText("Notification Permission Needed").assertExists()
        composeTestRule.onNodeWithText("Grant").performClick()
        composeTestRule.onNodeWithText("Open Settings").performClick()

        composeTestRule.runOnIdle {
            assertTrue(retryTapped)
            assertTrue(settingsTapped)
        }
    }

    @Test
    fun permissionDeniedDialog_shows_actions_and_handles_retry() {
        var retryTapped = false
        var settingsTapped = false

        composeTestRule.setContent {
            PermissionDeniedDialog(
                onDismiss = {},
                onRetry = { retryTapped = true },
                onOpenSettings = { settingsTapped = true }
            )
        }

        composeTestRule.onNodeWithText("Bluetooth Permissions Required").assertExists()
        composeTestRule.onNodeWithText("Grant Permissions").performClick()
        composeTestRule.onNodeWithText("Open Settings").performClick()

        composeTestRule.runOnIdle {
            assertTrue(retryTapped)
            assertTrue(settingsTapped)
        }
    }
}
