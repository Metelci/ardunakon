package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import com.metelci.ardunakon.bluetooth.DeviceType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceListDialogComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun deviceListDialog_scan_button_invokes_callback() {
        composeRule.setContent {
            var scanCount by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    DeviceListDialog(
                        scannedDevices = emptyList(),
                        onScan = { scanCount++ },
                        onDeviceSelected = {},
                        onDismiss = {},
                        view = LocalView.current
                    )
                    Text("scanCount=$scanCount")
                }
            }
        }

        composeRule.onNodeWithText("Bluetooth Devices").assertIsDisplayed()
        composeRule.onNodeWithText("Scan").performClick()
        composeRule.onNodeWithText("scanCount=1").assertIsDisplayed()
        composeRule.onNodeWithText("Scanning...").assertIsDisplayed()
    }

    @Test
    fun deviceListDialog_clicking_device_invokes_selection_callback() {
        val device = BluetoothDeviceModel(
            name = "Ardunakon (AA:BB:CC:DD:EE:FF)",
            address = "AA:BB:CC:DD:EE:FF",
            type = DeviceType.CLASSIC,
            rssi = -55
        )

        composeRule.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            MaterialTheme {
                Column {
                    DeviceListDialog(
                        scannedDevices = listOf(device),
                        onScan = {},
                        onDeviceSelected = { selected = it.address },
                        onDismiss = {},
                        view = LocalView.current
                    )
                    Text("selected=$selected")
                }
            }
        }

        // Name is displayed without the "(MAC)" suffix.
        composeRule.onNodeWithText("Ardunakon").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("selected=AA:BB:CC:DD:EE:FF").assertIsDisplayed()
    }
}
