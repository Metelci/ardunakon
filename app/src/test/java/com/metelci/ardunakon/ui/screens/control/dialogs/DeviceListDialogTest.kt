package com.metelci.ardunakon.ui.screens.control.dialogs

import android.view.View
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import com.metelci.ardunakon.bluetooth.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceListDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createView(): View {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return View(context)
    }

    @Test
    fun deviceListDialog_empty_state_shows_scan_and_updates_label() {
        var scanCount = 0

        composeTestRule.setContent {
            DeviceListDialog(
                scannedDevices = emptyList(),
                onScan = { scanCount += 1 },
                onDeviceSelected = {},
                onDismiss = {},
                view = createView()
            )
        }

        composeTestRule.onNodeWithText("Bluetooth Devices").assertExists()
        composeTestRule.onNodeWithText("No devices found").assertExists()
        composeTestRule.onNodeWithText("Scan").performClick()
        composeTestRule.onNodeWithText("Scanning...").assertExists()

        composeTestRule.runOnIdle {
            assertEquals(1, scanCount)
        }
    }

    @Test
    fun deviceListDialog_selects_device_and_strips_mac_suffix() {
        var selected: BluetoothDeviceModel? = null
        val device = BluetoothDeviceModel(
            name = "Ardunakon Bot (AA:BB:CC:DD:EE:FF)",
            address = "AA:BB:CC:DD:EE:FF",
            type = DeviceType.LE,
            rssi = -55
        )

        composeTestRule.setContent {
            DeviceListDialog(
                scannedDevices = listOf(device),
                onScan = {},
                onDeviceSelected = { selected = it },
                onDismiss = {},
                view = createView()
            )
        }

        composeTestRule.onNodeWithText("Ardunakon Bot").assertExists()
        composeTestRule.onNodeWithText("Ardunakon Bot").performClick()

        composeTestRule.runOnIdle {
            assertTrue(selected == device)
        }
    }
}
