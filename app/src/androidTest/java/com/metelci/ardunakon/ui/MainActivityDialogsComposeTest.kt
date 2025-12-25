package com.metelci.ardunakon.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.BluetoothOffDialog
import com.metelci.ardunakon.NotificationPermissionDialog
import com.metelci.ardunakon.PermissionDeniedDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityDialogsComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bluetoothOffDialog_buttons_invoke_callbacks() {
        composeRule.setContent {
            var later by remember { mutableStateOf(0) }
            var enable by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    BluetoothOffDialog(
                        onDismiss = { later++ },
                        onTurnOn = { enable++ }
                    )
                    Text("later=$later enable=$enable")
                }
            }
        }

        composeRule.onNodeWithText("Turn on Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Enable").performClick()
        composeRule.onNodeWithText("later=0 enable=1").assertIsDisplayed()
        composeRule.onNodeWithText("Later").performClick()
        composeRule.onNodeWithText("later=1 enable=1").assertIsDisplayed()
    }

    @Test
    fun notificationPermissionDialog_buttons_invoke_callbacks() {
        composeRule.setContent {
            var settings by remember { mutableStateOf(0) }
            var retry by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    NotificationPermissionDialog(
                        onDismiss = {},
                        onOpenSettings = { settings++ },
                        onRetry = { retry++ }
                    )
                    Text("settings=$settings retry=$retry")
                }
            }
        }

        composeRule.onNodeWithText("Notification Permission Needed").assertIsDisplayed()
        composeRule.onNodeWithText("Grant").performClick()
        composeRule.onNodeWithText("settings=0 retry=1").assertIsDisplayed()
        composeRule.onNodeWithText("Open Settings").performClick()
        composeRule.onNodeWithText("settings=1 retry=1").assertIsDisplayed()
    }

    @Test
    fun permissionDeniedDialog_buttons_invoke_callbacks() {
        composeRule.setContent {
            var settings by remember { mutableStateOf(0) }
            var retry by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    PermissionDeniedDialog(
                        onDismiss = {},
                        onRetry = { retry++ },
                        onOpenSettings = { settings++ }
                    )
                    Text("settings=$settings retry=$retry")
                }
            }
        }

        composeRule.onNodeWithText("Bluetooth Permissions Required").assertIsDisplayed()
        composeRule.onNodeWithText("Grant Permissions").performClick()
        composeRule.onNodeWithText("settings=0 retry=1").assertIsDisplayed()
        composeRule.onNodeWithText("Open Settings").performClick()
        composeRule.onNodeWithText("settings=1 retry=1").assertIsDisplayed()
    }
}

