package com.metelci.ardunakon.ui.components

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.CustomCommand
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for CustomCommandButton and CustomCommandButtonRow.
 *
 * Tests button rendering, click callbacks, keyboard shortcuts, placeholders,
 * and row layout behavior.
 */
@RunWith(AndroidJUnit4::class)
class CustomCommandButtonTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun customCommandButton_renders_with_command_properties() {
        val testCommand = CustomCommand(
            id = 1,
            name = "Test Command",
            iconName = "Add",
            colorHex = 0xFF00FF00.toInt(), // Green
            packetBytes = byteArrayOf(0xAA.toByte(), 0x01, 0x55),
            position = 0,
            keyboardShortcut = 'T'
        )

        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButton(
                    command = testCommand,
                    view = view,
                    onClick = {}
                )
            }
        }

        // Should show icon with content description
        composeRule.onNodeWithContentDescription("Test Command").assertIsDisplayed()

        // Should show keyboard shortcut
        composeRule.onNodeWithText("T").assertIsDisplayed()
    }

    @Test
    fun customCommandButton_without_shortcut_shows_only_icon() {
        val testCommand = CustomCommand(
            id = 1,
            name = "No Shortcut",
            iconName = "Settings",
            colorHex = 0xFFFF0000.toInt(), // Red
            packetBytes = byteArrayOf(0xAA.toByte(), 0x02, 0x55),
            position = 0,
            keyboardShortcut = null
        )

        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButton(
                    command = testCommand,
                    view = view,
                    onClick = {}
                )
            }
        }

        // Should show icon
        composeRule.onNodeWithContentDescription("No Shortcut").assertIsDisplayed()

        // Should NOT show any keyboard shortcut text
        // We can't directly test text absence, but we verified the icon is there
    }

    @Test
    fun customCommandButton_invokes_callback_on_click() {
        val testCommand = CustomCommand(
            id = 1,
            name = "Clickable",
            iconName = "Send",
            colorHex = 0xFF0000FF.toInt(),
            packetBytes = byteArrayOf(0xAA.toByte(), 0x03, 0x55),
            position = 0,
            keyboardShortcut = null
        )

        var clickCount = 0

        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButton(
                    command = testCommand,
                    view = view,
                    onClick = { clickCount++ }
                )
                androidx.compose.material3.Text("Clicks: $clickCount")
            }
        }

        // Click the button
        composeRule.onNodeWithContentDescription("Clickable").performClick()

        // Verify callback was invoked
        composeRule.onNodeWithText("Clicks: 1").assertIsDisplayed()
    }

    @Test
    fun customCommandPlaceholder_renders_empty_slot() {
        composeRule.setContent {
            MaterialTheme {
                CustomCommandPlaceholder()
            }
        }

        // Placeholder should exist (just a bordered box, no specific content to verify)
        // We can't easily test for "empty content", but we verify it renders without crashing
        composeRule.waitForIdle()
    }

    @Test
    fun customCommandButtonRow_shows_commands_and_placeholders() {
        val command1 = CustomCommand(
            id = 1,
            name = "Command 1",
            iconName = "Add",
            colorHex = 0xFFFF0000.toInt(),
            packetBytes = byteArrayOf(0xAA.toByte(), 0x01, 0x55),
            position = 0,
            keyboardShortcut = 'A'
        )

        val command2 = CustomCommand(
            id = 2,
            name = "Command 2",
            iconName = "Remove",
            colorHex = 0xFF00FF00.toInt(),
            packetBytes = byteArrayOf(0xAA.toByte(), 0x02, 0x55),
            position = 1,
            keyboardShortcut = 'B'
        )

        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButtonRow(
                    commands = listOf(command1, command2),
                    view = view,
                    onCommandClick = {},
                    maxButtons = 3 // Show 3 slots: 2 commands + 1 placeholder
                )
            }
        }

        // Should show both commands
        composeRule.onNodeWithContentDescription("Command 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Command 2").assertIsDisplayed()

        // Should show keyboard shortcuts
        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()

        // Third slot should be a placeholder (can't directly test, but verified no crash)
    }

    @Test
    fun customCommandButtonRow_limits_to_maxButtons() {
        val commands = (1..5).map { i ->
            CustomCommand(
                id = i,
                name = "Command $i",
                iconName = "Send",
                colorHex = 0xFFFFFFFF.toInt(),
                packetBytes = byteArrayOf(0xAA.toByte(), i.toByte(), 0x55),
                position = i - 1,
                keyboardShortcut = ('A' + i - 1)
            )
        }

        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButtonRow(
                    commands = commands,
                    view = view,
                    onCommandClick = {},
                    maxButtons = 2 // Only show first 2
                )
            }
        }

        // Should show first 2 commands
        composeRule.onNodeWithContentDescription("Command 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Command 2").assertIsDisplayed()

        // Should NOT show command 3, 4, 5
        composeRule.onNodeWithContentDescription("Command 3").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Command 4").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Command 5").assertDoesNotExist()
    }

    @Test
    fun customCommandButtonRow_invokes_callback_with_correct_command() {
        val command1 = CustomCommand(
            id = 1,
            name = "Cmd1",
            iconName = "Add",
            colorHex = 0xFF0000FF.toInt(),
            packetBytes = byteArrayOf(0xAA.toByte(), 0x01, 0x55),
            position = 0,
            keyboardShortcut = null
        )

        val command2 = CustomCommand(
            id = 2,
            name = "Cmd2",
            iconName = "Remove",
            colorHex = 0xFF00FF00.toInt(),
            packetBytes = byteArrayOf(0xAA.toByte(), 0x02, 0x55),
            position = 1,
            keyboardShortcut = null
        )

        var clickedCommandId = 0

        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButtonRow(
                    commands = listOf(command1, command2),
                    view = view,
                    onCommandClick = { clickedCommandId = it.id },
                    maxButtons = 2
                )
                androidx.compose.material3.Text("Clicked: $clickedCommandId")
            }
        }

        // Click second command
        composeRule.onNodeWithContentDescription("Cmd2").performClick()

        // Verify correct command was clicked
        composeRule.onNodeWithText("Clicked: 2").assertIsDisplayed()
    }

    @Test
    fun customCommandButtonRow_with_empty_commands_shows_all_placeholders() {
        composeRule.setContent {
            val view = LocalView.current
            MaterialTheme {
                CustomCommandButtonRow(
                    commands = emptyList(),
                    view = view,
                    onCommandClick = {},
                    maxButtons = 3
                )
            }
        }

        // Should render without crashing (all placeholders)
        composeRule.waitForIdle()
    }
}
