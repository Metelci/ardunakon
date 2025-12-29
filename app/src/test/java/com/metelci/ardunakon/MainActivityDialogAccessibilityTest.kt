package com.metelci.ardunakon

import androidx.compose.ui.test.onNodeWithText
import com.metelci.ardunakon.ui.dialogs.BluetoothOffDialog
import com.metelci.ardunakon.ui.dialogs.NotificationPermissionDialog
import com.metelci.ardunakon.ui.dialogs.PermissionDeniedDialog
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityDialogAccessibilityTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun bluetoothOffDialog_has_text_actions() {
        composeTestRule.setContent {
            BluetoothOffDialog(onDismiss = {}, onTurnOn = {})
        }

        composeTestRule.onNodeWithText("Turn on Bluetooth").assertExists()
        composeTestRule.onNodeWithText("Enable").assertExists()
        composeTestRule.onNodeWithText("Later").assertExists()
    }

    @Test
    fun notificationPermissionDialog_has_text_actions() {
        composeTestRule.setContent {
            NotificationPermissionDialog(onDismiss = {}, onOpenSettings = {}, onRetry = {})
        }

        composeTestRule.onNodeWithText("Notification Permission Needed").assertExists()
        composeTestRule.onNodeWithText("Grant").assertExists()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
    }

    @Test
    fun permissionDeniedDialog_has_text_actions() {
        composeTestRule.setContent {
            PermissionDeniedDialog(onDismiss = {}, onRetry = {}, onOpenSettings = {})
        }

        composeTestRule.onNodeWithText("Bluetooth Permissions Required").assertExists()
        composeTestRule.onNodeWithText("Grant Permissions").assertExists()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
    }
}
