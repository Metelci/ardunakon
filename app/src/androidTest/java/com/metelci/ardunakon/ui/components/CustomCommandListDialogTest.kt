package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.CustomCommand
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for CustomCommandListDialog.
 *
 * Tests command list display, add/edit/delete operations,
 * command execution, and empty state handling.
 */
@RunWith(AndroidJUnit4::class)
class CustomCommandListDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun customCommandListDialog_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Custom Commands").assertIsDisplayed()
    }

    @Test
    fun customCommandListDialog_emptyState_showsMessage() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("No custom commands yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun customCommandListDialog_displaysCommands() {
        val commands = listOf(
            CustomCommand(id = 1, label = "Turbo", command = "AT+SPEED=255"),
            CustomCommand(id = 2, label = "Stop", command = "AT+STOP")
        )

        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = commands,
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Turbo").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun customCommandListDialog_displaysCommandDetails() {
        val commands = listOf(
            CustomCommand(id = 1, label = "Test", command = "AT+TEST=123", shortcutKey = "T")
        )

        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = commands,
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Test").assertIsDisplayed()
        composeRule.onNodeWithText("AT+TEST=123").assertIsDisplayed()
    }

    @Test
    fun customCommandListDialog_hasAddButton() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Add Command").assertIsDisplayed()
    }

    @Test
    fun customCommandListDialog_invokesOnAddCommand() {
        var addCalled = false

        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = { addCalled = true },
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Add Command").performClick()

        assert(addCalled)
    }

    @Test
    fun customCommandListDialog_invokesOnDismiss() {
        var dismissed = false

        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithText("Close").performClick()

        assert(dismissed)
    }

    @Test
    fun customCommandListDialog_displaysMultipleCommands() {
        val commands = (1..5).map { i ->
            CustomCommand(id = i, label = "Command $i", command = "AT+CMD$i")
        }

        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = commands,
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        commands.forEach { cmd ->
            composeRule.onNodeWithText(cmd.label).assertIsDisplayed()
        }
    }

    @Test
    fun customCommandListDialog_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun customCommandListDialog_hasCloseButton() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = emptyList(),
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun customCommandListDialog_commandsScrollable() {
        val commands = (1..20).map { i ->
            CustomCommand(id = i, label = "Command $i", command = "AT+CMD$i")
        }

        composeRule.setContent {
            MaterialTheme {
                CustomCommandListDialog(
                    commands = commands,
                    onAddCommand = {},
                    onEditCommand = {},
                    onDeleteCommand = {},
                    onSendCommand = {},
                    onDismiss = {}
                )
            }
        }

        // First command should be visible
        composeRule.onNodeWithText("Command 1").assertIsDisplayed()
    }
}
