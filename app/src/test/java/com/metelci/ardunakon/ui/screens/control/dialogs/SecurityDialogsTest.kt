package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.security.EncryptionException
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityDialogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun securityErrorDialog_shows_message_and_actions() {
        var settingsTapped = false
        var dismissTapped = false
        val message = "Unlock your device to access secure profiles."

        composeTestRule.setContent {
            SecurityErrorDialog(
                message = message,
                onOpenSettings = { settingsTapped = true },
                onDismiss = { dismissTapped = true }
            )
        }

        composeTestRule.onNodeWithText("Unlock Required").assertExists()
        composeTestRule.onNodeWithText(message).assertExists()
        composeTestRule.onNodeWithText("Open Security Settings").performClick()
        composeTestRule.onNodeWithText("Close").performClick()

        composeTestRule.runOnIdle {
            assertTrue(settingsTapped)
            assertTrue(dismissTapped)
        }
    }

    @Test
    fun encryptionErrorDialog_uses_generic_message_and_actions() {
        var retryTapped = false
        var continueTapped = false
        var disconnectTapped = false

        composeTestRule.setContent {
            EncryptionErrorDialog(
                error = EncryptionException.HandshakeFailedException("do not leak"),
                onRetry = { retryTapped = true },
                onDisableEncryption = { continueTapped = true },
                onDismiss = { disconnectTapped = true }
            )
        }

        composeTestRule.onNodeWithText("Security Verification Failed").assertExists()
        composeTestRule.onNodeWithText("Security protocol error occurred").assertExists()
        composeTestRule.onNodeWithText("do not leak").assertDoesNotExist()

        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.onNodeWithText("Disconnect").performClick()

        composeTestRule.runOnIdle {
            assertTrue(continueTapped)
            assertTrue(retryTapped)
            assertTrue(disconnectTapped)
        }
    }
}
