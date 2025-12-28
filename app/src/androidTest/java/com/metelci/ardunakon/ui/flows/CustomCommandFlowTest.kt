package com.metelci.ardunakon.ui.flows

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.MainActivity
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
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun customCommandFlow_createNewCommand_success() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Open settings menu
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Navigate to custom commands
        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        // Click add command button
        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        // Fill in command details
        composeTestRule.onNodeWithText("Command Name").performTextInput("Test Command")
        composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
        composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("0102030405")

        // Save command
        composeTestRule.onNodeWithText("Save").performClick()
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
        // Create 6 commands (max limit)
        repeat(6) { index ->
            composeTestRule.onNodeWithText("Add Command").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Command Name").performTextInput("Command $index")
            composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
            composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("010203040$index")

            composeTestRule.onNodeWithText("Save").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify add button is disabled
        composeTestRule.onNodeWithText("Add Command").assertIsNotEnabled()
    }
}
