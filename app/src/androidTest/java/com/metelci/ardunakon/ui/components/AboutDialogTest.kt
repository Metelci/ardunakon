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
 * Compose UI tests for AboutDialog.
 *
 * Tests app information display, version, links,
 * and dialog dismissal.
 */
@RunWith(AndroidJUnit4::class)
class AboutDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aboutDialog_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithText("About Ardunakon").assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysAppDescription() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Control your Arduino RC car via Bluetooth or WiFi", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysVersion() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        // Version should be displayed (BuildConfig.VERSION_NAME)
        composeRule.onNodeWithText("Version", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysFeatures() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        // Should display feature list
        composeRule.onNodeWithText("Features", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysGitHubLink() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithText("View on GitHub", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysLicense() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithText("License", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutDialog_invokesOnDismiss() {
        var dismissCalled = false

        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = { dismissCalled = true })
            }
        }

        composeRule.onNodeWithText("Close").performClick()

        assert(dismissCalled)
    }

    @Test
    fun aboutDialog_hasCloseButton() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun aboutDialog_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(onDismiss = {})
            }
        }

        // Should render successfully
        composeRule.waitForIdle()
    }
}
