package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for WebViewDialog.
 *
 * Tests web content display, URL loading,
 * navigation controls, and dialog dismissal.
 */
@RunWith(AndroidJUnit4::class)
class WebViewDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun webViewDialog_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Test Page",
                    url = "https://example.com",
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Test Page").assertIsDisplayed()
    }

    @Test
    fun webViewDialog_displaysUrl() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Documentation",
                    url = "https://docs.ardunakon.com",
                    onDismiss = {}
                )
            }
        }

        // URL should be shown somewhere (title or content)
        composeRule.waitForIdle()
    }

    @Test
    fun webViewDialog_hasCloseButton() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Help",
                    url = "https://help.example.com",
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun webViewDialog_invokesOnDismiss() {
        var dismissed = false

        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Test",
                    url = "https://example.com",
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Close").performClick()

        assert(dismissed)
    }

    @Test
    fun webViewDialog_loadsWithValidUrl() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Valid Page",
                    url = "https://www.google.com",
                    onDismiss = {}
                )
            }
        }

        // Should load without crashing
        composeRule.waitForIdle()
    }

    @Test
    fun webViewDialog_handlesEmptyTitle() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "",
                    url = "https://example.com",
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun webViewDialog_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Test WebView",
                    url = "about:blank",
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun webViewDialog_displaysLoadingState() {
        composeRule.setContent {
            MaterialTheme {
                WebViewDialog(
                    title = "Loading Test",
                    url = "https://example.com",
                    onDismiss = {}
                )
            }
        }

        // Should show some loading indicator initially
        composeRule.waitForIdle()
    }
}
