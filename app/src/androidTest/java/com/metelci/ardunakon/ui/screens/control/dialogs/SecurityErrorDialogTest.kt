package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for SecurityErrorDialog.
 *
 * Tests security unlock requirement messaging and
 * settings/dismiss actions for device authentication.
 */
@RunWith(AndroidJUnit4::class)
class SecurityErrorDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun securityErrorDialog_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Device unlock required",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Unlock Required").assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_displaysMessage() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Please unlock your device to access secure storage",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Please unlock your device to access secure storage")
            .assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_hasOpenSettingsButton() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Test message",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Open Security Settings").assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_hasCloseButton() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Test message",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_invokesOnOpenSettings() {
        var settingsCalled = false

        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Test",
                    onOpenSettings = { settingsCalled = true },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Open Security Settings").performClick()

        assert(settingsCalled)
    }

    @Test
    fun securityErrorDialog_invokesOnDismiss() {
        var dismissCalled = false

        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Test",
                    onOpenSettings = {},
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        composeRule.onNodeWithText("Close").performClick()

        assert(dismissCalled)
    }

    @Test
    fun securityErrorDialog_displaysLongMessage() {
        val longMessage = "Authentication is required to access encrypted profile data. " +
                "Please unlock your device using your PIN, pattern, password, or biometric " +
                "authentication method to continue."

        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = longMessage,
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(longMessage, substring = true).assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_handlesEmptyMessage() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        // Should still render with empty message
        composeRule.onNodeWithText("Unlock Required").assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Device authentication needed",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun securityErrorDialog_displaysShortMessage() {
        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Unlock device",
                    onOpenSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Unlock device").assertIsDisplayed()
    }

    @Test
    fun securityErrorDialog_multipleCallbacksWork() {
        var settingsCalled = false
        var dismissCalled = false

        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Test",
                    onOpenSettings = { settingsCalled = true },
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        composeRule.onNodeWithText("Open Security Settings").performClick()
        assert(settingsCalled)

        composeRule.setContent {
            MaterialTheme {
                SecurityErrorDialog(
                    message = "Test",
                    onOpenSettings = {},
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        composeRule.onNodeWithText("Close").performClick()
        assert(dismissCalled)
    }
}
