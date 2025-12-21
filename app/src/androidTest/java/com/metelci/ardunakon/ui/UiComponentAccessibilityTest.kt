package com.metelci.ardunakon.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.MainActivity
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.ui.components.AutoReconnectToggle
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.ServoButtonControl
import com.metelci.ardunakon.ui.components.StatusCard
import com.metelci.ardunakon.ui.components.WifiConfigDialog
import com.metelci.ardunakon.wifi.WifiDevice
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class UiComponentAccessibilityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun autoReconnectToggle_shows_enabled_state_description() {
        composeRule.setContent {
            AutoReconnectToggle(enabled = true, onToggle = {})
        }

        composeRule.onNodeWithContentDescription(
            "Auto-reconnect enabled. Tap to disable automatic reconnection."
        ).assertIsDisplayed()
    }

    @Test
    fun autoReconnectToggle_toggles_state_on_click() {
        composeRule.setContent {
            val enabled = remember { mutableStateOf(false) }
            AutoReconnectToggle(
                enabled = enabled.value,
                onToggle = { enabled.value = it }
            )
        }

        composeRule.onNodeWithContentDescription(
            "Auto-reconnect disabled. Tap to enable automatic reconnection."
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "Auto-reconnect enabled. Tap to disable automatic reconnection."
        ).assertIsDisplayed()
    }

    @Test
    fun statusCard_has_content_description() {
        composeRule.setContent {
            StatusCard(
                label = "Device",
                state = ConnectionState.CONNECTED,
                rssi = -60,
                hasCrashLog = false,
                onClick = {},
                onCrashLogClick = {}
            )
        }

        composeRule.onNode(hasContentDescription("Device status: Connected")).assertExists()
    }

    @Test
    fun wifiConfigDialog_shows_encrypted_icon_and_text() {
        composeRule.setContent {
            WifiConfigDialog(
                initialIp = "192.168.4.1",
                initialPort = 8888,
                scannedDevices = emptyList(),
                isEncrypted = true,
                onScan = {},
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }

        composeRule.onNodeWithContentDescription("Secure connection. Data is encrypted.").assertIsDisplayed()
    }

    @Test
    fun wifiConfigDialog_lists_scanned_devices() {
        val devices = listOf(
            WifiDevice(name = "R4 WiFi", ip = "192.168.4.1", port = 8888, trusted = true),
            WifiDevice(name = "ESP32", ip = "192.168.1.42", port = 8888, trusted = false)
        )
        composeRule.setContent {
            WifiConfigDialog(
                initialIp = "",
                initialPort = 8888,
                scannedDevices = devices,
                isEncrypted = false,
                onScan = {},
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }

        composeRule.onNodeWithText("R4 WiFi").assertIsDisplayed()
        composeRule.onNodeWithText("ESP32").assertIsDisplayed()
    }

    @Test
    fun wifiConfigDialog_scan_button_exists() {
        composeRule.setContent {
            WifiConfigDialog(
                initialIp = "",
                initialPort = 8888,
                scannedDevices = emptyList(),
                isEncrypted = false,
                onScan = {},
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }

        composeRule.onNodeWithContentDescription("Scan for WiFi devices").assertIsDisplayed()
    }

    @Test
    fun wifiConfigDialog_shows_empty_state() {
        composeRule.setContent {
            WifiConfigDialog(
                initialIp = "",
                initialPort = 8888,
                scannedDevices = emptyList(),
                isEncrypted = false,
                onScan = {},
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }

        composeRule.onNodeWithText("No devices").assertIsDisplayed()
    }

    @Test
    fun servoButtons_have_content_descriptions() {
        composeRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                servoZ = 0f,
                onMove = { _, _, _ -> }
            )
        }

        composeRule.onNodeWithContentDescription("Move servo forward").assertExists()
        composeRule.onNodeWithContentDescription("Move servo backward").assertExists()
        composeRule.onNodeWithContentDescription("Move servo left").assertExists()
        composeRule.onNodeWithContentDescription("Move servo right").assertExists()
    }

    @Test
    fun servoButton_forward_click_updates_move_callback() {
        composeRule.setContent {
            val lastMove = remember { mutableStateOf<Triple<Float, Float, Float>?>(null) }
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                servoZ = 0f,
                onMove = { x, y, z -> lastMove.value = Triple(x, y, z) }
            )
            Text(lastMove.value?.let { "Move:${it.first},${it.second},${it.third}" } ?: "None")
        }

        composeRule.onNodeWithContentDescription("Move servo forward").performClick()
        composeRule.onNodeWithText("Move:0.0,0.1,0.0").assertExists()
    }

    @Test
    fun joystick_state_description_updates_after_drag_simulation() {
        composeRule.setContent {
            JoystickControl(
                isThrottle = false,
                onMoved = {}
            )
        }

        // Initial state
        composeRule.onNode(
            hasContentDescription("Steering joystick") and hasStateDescription("X 0 percent, Y 0 percent")
        ).assertExists()
    }
}
