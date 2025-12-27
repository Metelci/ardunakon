package com.metelci.ardunakon.ui.screens.control.dialogs

import android.view.View
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import com.metelci.ardunakon.bluetooth.DeviceType
import com.metelci.ardunakon.security.EncryptionException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ControlDialogsAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createView(): View {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return View(context)
    }

    @Test
    fun deviceListDialog_exposes_icon_and_action_labels() {
        val device = BluetoothDeviceModel(
            name = "Demo Device",
            address = "00:11:22:33:44:55",
            type = DeviceType.LE
        )

        composeTestRule.setContent {
            DeviceListDialog(
                scannedDevices = listOf(device),
                onScan = {},
                onDeviceSelected = {},
                onDismiss = {},
                view = createView()
            )
        }

        composeTestRule.onNodeWithText("Bluetooth Devices").assertExists()
        composeTestRule.onNodeWithContentDescription("Scan").assertExists()
        composeTestRule.onNodeWithContentDescription("Bluetooth device").assertExists()
        composeTestRule.onNodeWithText("Close").assertExists()
    }

    @Test
    fun crashLogDialog_exposes_share_and_clear_actions() {
        composeTestRule.setContent {
            CrashLogDialog(
                crashLog = "Log line",
                view = createView(),
                onShare = {},
                onClear = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Crash Log").assertExists()
        composeTestRule.onNodeWithContentDescription("Share").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear").assertExists()
        composeTestRule.onNodeWithText("Close").assertExists()
    }

    @Test
    fun securityDialogs_have_action_labels() {
        composeTestRule.setContent {
            SecurityErrorDialog(
                message = "Unlock required",
                onOpenSettings = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Unlock Required").assertExists()
        composeTestRule.onNodeWithText("Open Security Settings").assertExists()
        composeTestRule.onNodeWithText("Close").assertExists()

        composeTestRule.setContent {
            EncryptionErrorDialog(
                error = EncryptionException.SecurityException("hidden"),
                onRetry = {},
                onDisableEncryption = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Security Error").assertExists()
        composeTestRule.onNodeWithText("Continue").assertExists()
        composeTestRule.onNodeWithText("Retry").assertExists()
        composeTestRule.onNodeWithText("Disconnect").assertExists()
    }
}
