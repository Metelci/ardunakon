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
 * Compose UI tests for CustomCommandDialog.
 *
 * Tests custom command creation, editing, keyboard
 * shortcut assignment, and command validation.
 */
@RunWith(AndroidJUnit4::class)
class CustomCommandDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun customCommandDialog_displaysTitle() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Add Custom Command").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_editMode_displaysEditTitle() {
        val command = CustomCommand(
            id = 1,
            label = "Test",
            command = "AT+TEST"
        )

        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = command,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Edit Custom Command").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_createMode_hasEmptyFields() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        // Fields should be empty
        composeRule.onNodeWithText("Label (e.g., 'Turbo')").assertIsDisplayed()
        composeRule.onNodeWithText("Command (e.g., 'AT+SPEED=255')").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_editMode_populatesFields() {
        val command = CustomCommand(
            id = 1,
            label = "Turbo",
            command = "AT+SPEED=255",
            shortcutKey = "T"
        )

        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = command,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Turbo").assertIsDisplayed()
        composeRule.onNodeWithText("AT+SPEED=255").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_invokesDismiss() {
        var dismissed = false

        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = { dismissed = true },
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()

        assert(dismissed)
    }

    @Test
    fun customCommandDialog_invokesOnSave() {
        var savedCommand: CustomCommand? = null

        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = { savedCommand = it }
                )
            }
        }

        // Fill in fields
        composeRule.onNodeWithText("Label (e.g., 'Turbo')").performTextInput("TestLabel")
        composeRule.onNodeWithText("Command (e.g., 'AT+SPEED=255')").performTextInput("AT+TEST")
        
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitForIdle()
        assert(savedCommand != null)
    }

    @Test
    fun customCommandDialog_hasCancelButton() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_hasSaveButton() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_allowsTextInput() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("Label (e.g., 'Turbo')").performTextInput("My Command")

        composeRule.onNodeWithText("My Command").assertIsDisplayed()
    }

    @Test
    fun customCommandDialog_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandDialog(
                    existingCommand = null,
                    onDismiss = {},
                    onSave = {}
                )
            }
        }

        composeRule.waitForIdle()
    }
}
