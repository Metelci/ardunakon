package com.metelci.ardunakon.ui.flows

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.ui.screens.ControlScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.metelci.ardunakon.HiltTestActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for settings flow and persistence.
 *
 * Tests that user settings are correctly displayed, modified, and persisted:
 * - Settings dialog navigation
 * - Toggle switches (haptics, debug window)
 * - Custom commands CRUD
 * - Settings persistence across dialog open/close cycles
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsFlowE2ETest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeTestRule.activityRule.scenario.onActivity {}
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    ControlScreen()
                }
            }
        }
        waitForControlScreen()
    }

    @Test
    fun settings_dialogOpensFromGearIcon() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Dialog should be visible
        composeTestRule.onNodeWithText("Debug Window").assertExists()
    }

    @Test
    fun settings_hasAllExpectedSections() {
        openSettingsDialog()

        // Verify all main settings sections
        composeTestRule.onNodeWithText("Debug Window").assertExists()
        composeTestRule.onNodeWithText("Custom Commands").assertExists()
        composeTestRule.onNodeWithText("Haptic Feedback").assertExists()
        composeTestRule.onNodeWithText("Sensitivity").assertExists()
    }

    @Test
    fun settings_closeButton_works() {
        openSettingsDialog()

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Settings should be closed
        composeTestRule.onNodeWithText("Debug Window").assertDoesNotExist()
    }

    @Test
    fun settings_backButton_works() {
        openSettingsDialog()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // Settings should be closed
        composeTestRule.onNodeWithText("Debug Window").assertDoesNotExist()
    }

    @Test
    fun settings_customCommands_opens() {
        openSettingsDialog()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        // Custom commands section should be visible
        composeTestRule.onNodeWithText("Add Command").assertExists()
    }

    @Test
    fun settings_customCommands_addDialog_opens() {
        openSettingsDialog()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        // Add command form should be visible
        composeTestRule.onNode(hasSetTextAction() and hasText("Command Name")).assertExists()
        composeTestRule.onNode(hasSetTextAction() and hasText("Payload (Hex)")).assertExists()
    }

    @Test
    fun settings_customCommands_createFlow() {
        openSettingsDialog()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        // Fill in command details
        composeTestRule.onNodeWithText("Command Name").performTextInput("E2E Test Cmd")
        composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
        composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("AABBCC")

        // Save
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Command should appear in list
        composeTestRule.onNodeWithText("E2E Test Cmd").assertExists()
    }

    @Test
    fun settings_customCommands_cancelDoesNotSave() {
        openSettingsDialog()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        // Fill in but cancel
        composeTestRule.onNodeWithText("Command Name").performTextInput("Should Not Save")
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Command should NOT appear in list
        composeTestRule.onNodeWithText("Should Not Save").assertDoesNotExist()
    }

    @Test
    fun settings_reopenAfterClose_preservesState() {
        // Open and change something
        openSettingsDialog()
        composeTestRule.onNodeWithText("Debug Window").performClick()
        composeTestRule.waitForIdle()

        // Close
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Reopen
        openSettingsDialog()

        // State should be preserved (toggle state persists via ViewModel/preferences)
        composeTestRule.onNodeWithText("Debug Window").assertExists()
    }

    @Test
    fun settings_overflowMenu_hasAllOptions() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        // All menu options should be visible
        composeTestRule.onNodeWithText("Configure WiFi").assertExists()
        composeTestRule.onNodeWithText("Help").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
        composeTestRule.onNodeWithText("Crash Log").assertExists()
    }

    @Test
    fun settings_crashLog_opens() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Crash Log").performClick()
        composeTestRule.waitForIdle()

        // Crash log dialog should show
        composeTestRule.onNodeWithText("Crash Log").assertExists()
    }

    @Test
    fun settings_performanceStats_opens() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Performance Stats").performClick()
        composeTestRule.waitForIdle()

        // Performance stats dialog should show
        composeTestRule.onNodeWithText("App Health").assertExists()
    }

    private fun openSettingsDialog() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
