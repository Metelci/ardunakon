package com.metelci.ardunakon.ui.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WifiConfigDialogKeyboardNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun wifiConfigDialog_connect_responds_to_enter_key() {
        var savedIp: String? = null
        var savedPort: Int? = null

        composeTestRule.setContent {
            WifiConfigDialog(
                initialIp = "192.168.4.1",
                initialPort = 8888,
                scannedDevices = emptyList(),
                onScan = {},
                onDismiss = {},
                onSave = { ip, port ->
                    savedIp = ip
                    savedPort = port
                }
            )
        }

        val connectButton = composeTestRule.onNodeWithContentDescription("Connect to WiFi device")
        connectButton.performSemanticsAction(SemanticsActions.RequestFocus)
        connectButton.assertIsFocused()
        connectButton.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.runOnIdle {
            assertEquals("192.168.4.1", savedIp)
            assertEquals(8888, savedPort)
        }
    }
}
