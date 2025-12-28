package com.metelci.ardunakon.ui.flows

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.MainActivity
import com.metelci.ardunakon.ui.screens.ControlScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end instrumented tests for custom command flows.
 * Tests the complete user journey for creating, editing, and using custom commands.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CustomCommandFlowTest {

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
    val composeTestRule = createAndroidComposeRule<MainActivity>()

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
    fun customCommandFlow_createNewCommand_success() {
        openCustomCommands()

        // Click add command button
        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        // Fill in command details
        composeTestRule.onNodeWithText("Command Name").performTextInput("Test Command")
        composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
        composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("0102030405")

        // Save command
        composeTestRule.onNodeWithText("Create").performClick()
        composeTestRule.waitForIdle()

        // Verify command appears in list
        composeTestRule.onNodeWithText("Test Command").assertExists()
    }

    @Test
    fun customCommandFlow_editExistingCommand_success() {
        // Create a command first
        customCommandFlow_createNewCommand_success()

        // Find and edit the command
        composeTestRule.onNodeWithContentDescription("Edit Test Command").performClick()
        composeTestRule.waitForIdle()

        // Change the name
        composeTestRule.onNodeWithText("Command Name").performTextClearance()
        composeTestRule.onNodeWithText("Command Name").performTextInput("Updated Command")

        // Save changes
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify updated name
        composeTestRule.onNodeWithText("Updated Command").assertExists()
    }

    @Test
    fun customCommandFlow_deleteCommand_success() {
        // Create a command first
        customCommandFlow_createNewCommand_success()

        // Delete the command
        composeTestRule.onNodeWithContentDescription("Delete Test Command").performClick()
        composeTestRule.waitForIdle()

        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()

        // Verify command is removed
        composeTestRule.onNodeWithText("Test Command").assertDoesNotExist()
    }

    @Test
    fun customCommandFlow_sendCommand_triggersAction() {
        // Create a command first
        customCommandFlow_createNewCommand_success()

        // Close the custom commands dialog
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Find and click the custom command button on main screen
        composeTestRule.onNodeWithContentDescription("Test Command").performClick()
        composeTestRule.waitForIdle()

        // Verify command was sent (check debug log or connection state)
        // This would require checking the actual Bluetooth/WiFi state
    }

    @Test
    fun customCommandFlow_maxCommandsLimit_disablesAddButton() {
        openCustomCommands()

        // Create 6 commands (max limit)
        repeat(6) { index ->
            composeTestRule.onNodeWithText("Add Command").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Command Name").performTextInput("Command $index")
            composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
            composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("010203040$index")

            composeTestRule.onNodeWithText("Create").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify add button is disabled
        composeTestRule.onNodeWithText("Add Command").assertIsNotEnabled()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openCustomCommands() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()
    }
}
