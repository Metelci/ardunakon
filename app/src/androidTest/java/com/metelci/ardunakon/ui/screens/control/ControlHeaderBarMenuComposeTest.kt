package com.metelci.ardunakon.ui.screens.control

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.wifi.WifiConnectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlHeaderBarMenuComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun headerMenu_items_invoke_callbacks() {
        composeRule.setContent {
            val context = LocalContext.current
            val view = LocalView.current
            var help by remember { mutableStateOf(0) }
            var about by remember { mutableStateOf(0) }
            var crash by remember { mutableStateOf(0) }
            var cloud by remember { mutableStateOf(0) }
            var quit by remember { mutableStateOf(0) }

            MaterialTheme {
                Column {
                    ControlHeaderBar(
                        connectionMode = ConnectionMode.BLUETOOTH,
                        bluetoothConnectionState = ConnectionState.DISCONNECTED,
                        wifiConnectionState = WifiConnectionState.DISCONNECTED,
                        rssiValue = -60,
                        wifiRssi = -45,
                        rttHistory = listOf(10L),
                        wifiRttHistory = listOf(10L),
                        isEStopActive = false,
                        autoReconnectEnabled = true,
                        onToggleAutoReconnect = {},
                        isWifiEncrypted = false,
                        onScanDevices = {},
                        onReconnectDevice = {},
                        onSwitchToWifi = {},
                        onSwitchToBluetooth = {},
                        onConfigureWifi = {},
                        onTelemetryGraph = {},
                        onToggleEStop = {},
                        onShowSettings = {},
                        onShowHelp = { help++ },
                        onShowAbout = { about++ },
                        onShowCrashLog = { crash++ },
                        onOpenArduinoCloud = { cloud++ },
                        onQuitApp = { quit++ },
                        context = context,
                        view = view
                    )
                    Text("help=$help about=$about crash=$crash cloud=$cloud quit=$quit")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.onNodeWithText("Help").performClick()
        composeRule.onNodeWithText("help=1 about=0 crash=0 cloud=0 quit=0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.onNodeWithText("About").performClick()
        composeRule.onNodeWithText("help=1 about=1 crash=0 cloud=0 quit=0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.onNodeWithText("View Crash Log").performClick()
        composeRule.onNodeWithText("help=1 about=1 crash=1 cloud=0 quit=0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.onNodeWithText("Open Arduino Cloud").performClick()
        composeRule.onNodeWithText("help=1 about=1 crash=1 cloud=1 quit=0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Menu").performClick()
        composeRule.onNodeWithText("Quit App").performClick()
        composeRule.onNodeWithText("help=1 about=1 crash=1 cloud=1 quit=1").assertIsDisplayed()
    }
}

