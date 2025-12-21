package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.security.EncryptionException
import org.junit.Rule
import org.junit.Test

class EncryptionErrorDialogComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun encryptionErrorDialog_handshakeFailure_does_not_leak_error_details_and_buttons_invoke_callbacks() {
        composeRule.setContent {
            var retry by remember { mutableStateOf(0) }
            var continueWithoutEncryption by remember { mutableStateOf(0) }
            var disconnect by remember { mutableStateOf(0) }

            MaterialTheme {
                Column {
                    EncryptionErrorDialog(
                        error = EncryptionException.HandshakeFailedException("Wrong PSK: super-secret"),
                        onRetry = { retry++ },
                        onDisableEncryption = { continueWithoutEncryption++ },
                        onDismiss = { disconnect++ }
                    )
                    Text("retry=$retry continue=$continueWithoutEncryption disconnect=$disconnect")
                }
            }
        }

        composeRule.onNodeWithText("Security Verification Failed").assertIsDisplayed()
        composeRule.onNodeWithText("Security protocol error occurred").assertIsDisplayed()
        composeRule.onNodeWithText("Wrong PSK", substring = true).assertDoesNotExist()

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("retry=0 continue=1 disconnect=0").assertIsDisplayed()

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("retry=1 continue=1 disconnect=0").assertIsDisplayed()

        composeRule.onNodeWithText("Disconnect").performClick()
        composeRule.onNodeWithText("retry=1 continue=1 disconnect=1").assertIsDisplayed()
    }

    @Test
    fun encryptionErrorDialog_noSessionKey_shows_protocol_error_title() {
        composeRule.setContent {
            MaterialTheme {
                EncryptionErrorDialog(
                    error = EncryptionException.NoSessionKeyException("missing"),
                    onRetry = {},
                    onDisableEncryption = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Security Protocol Error").assertIsDisplayed()
        composeRule.onNodeWithText("Connection blocked to prevent unencrypted data transmission.").assertIsDisplayed()
    }
}

