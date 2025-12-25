package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.wifi.WifiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WifiConfigDialogComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wifiConfigDialog_scan_invokes_callback_and_shows_scanning_state() {
        composeRule.setContent {
            var scanCount by remember { mutableStateOf(0) }
            MaterialTheme {
                Column {
                    WifiConfigDialog(
                        initialIp = "",
                        initialPort = 8888,
                        scannedDevices = emptyList(),
                        isEncrypted = false,
                        onScan = { scanCount++ },
                        onDismiss = {},
                        onSave = { _, _ -> }
                    )
                    Text("scanCount=$scanCount")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Scan for WiFi devices").performClick()
        composeRule.onNodeWithText("scanCount=1").assertIsDisplayed()
        composeRule.onNodeWithText("Scanning...").assertIsDisplayed()
    }

    @Test
    fun wifiConfigDialog_select_device_updates_fields_and_connect_invokes_onSave() {
        val devices = listOf(
            WifiDevice(name = "ESP32", ip = "192.168.1.42", port = 7777, trusted = false)
        )

        composeRule.setContent {
            var lastSave by remember { mutableStateOf("none") }
            MaterialTheme {
                Column {
                    WifiConfigDialog(
                        initialIp = "0.0.0.0",
                        initialPort = 8888,
                        scannedDevices = devices,
                        isEncrypted = false,
                        onScan = {},
                        onDismiss = {},
                        onSave = { ip, port -> lastSave = "$ip:$port" }
                    )
                    Text("saved=$lastSave")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Select ESP32 at 192.168.1.42").performClick()

        composeRule
            .onAllNodes(hasSetTextAction() and hasText("192.168.1.42"), useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
        composeRule
            .onAllNodes(hasSetTextAction() and hasText("7777"), useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Connect to WiFi device").performClick()
        composeRule.onNodeWithText("saved=192.168.1.42:7777").assertIsDisplayed()
    }

    @Test
    fun wifiConfigDialog_invalid_port_falls_back_to_default_8888() {
        composeRule.setContent {
            var lastSave by remember { mutableStateOf("none") }
            MaterialTheme {
                Column {
                    WifiConfigDialog(
                        initialIp = "192.168.4.1",
                        initialPort = 8888,
                        scannedDevices = emptyList(),
                        isEncrypted = false,
                        onScan = {},
                        onDismiss = {},
                        onSave = { ip, port -> lastSave = "$ip:$port" }
                    )
                    Text("saved=$lastSave")
                }
            }
        }

        // Replace the "Port" field content with a non-integer.
        val portField = composeRule
            .onAllNodes(hasSetTextAction() and hasText("8888"), useUnmergedTree = true)
            .onFirst()
        portField.performTextClearance()
        portField.performTextInput("abc")

        composeRule.onNodeWithContentDescription("Connect to WiFi device").performClick()
        composeRule.onNodeWithText("saved=192.168.4.1:8888").assertIsDisplayed()
    }
}
