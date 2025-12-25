package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetProgressAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDialogComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsDialog_customCommands_and_resetTutorial_invoke_callbacks_and_dismiss() {
        composeRule.setContent {
            var dismissed by remember { mutableStateOf(0) }
            var customCommandsOpened by remember { mutableStateOf(0) }
            var tutorialReset by remember { mutableStateOf(0) }

            MaterialTheme {
                Column {
                    SettingsDialog(
                        onDismiss = { dismissed++ },
                        view = LocalView.current,
                        isDebugPanelVisible = true,
                        onToggleDebugPanel = {},
                        isHapticEnabled = true,
                        onToggleHaptic = {},
                        joystickSensitivity = 1.0f,
                        onJoystickSensitivityChange = {},
                        allowReflection = false,
                        onToggleReflection = {},
                        customCommandCount = 3,
                        onShowCustomCommands = { customCommandsOpened++ },
                        onResetTutorial = { tutorialReset++ }
                    )
                    Text("dismissed=$dismissed custom=$customCommandsOpened reset=$tutorialReset")
                }
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Custom Commands").performClick()
        composeRule.onNodeWithText("dismissed=1 custom=1 reset=0").assertIsDisplayed()

        // Reset Tutorial also dismisses.
        composeRule.onNodeWithText("Reset Tutorial").performClick()
        composeRule.onNodeWithText("dismissed=2 custom=1 reset=1").assertIsDisplayed()
    }

    @Test
    fun settingsDialog_slider_changes_value_via_semantics() {
        composeRule.setContent {
            var sensitivity by remember { mutableStateOf(1.0f) }

            MaterialTheme {
                SettingsDialog(
                    onDismiss = {},
                    view = LocalView.current,
                    isDebugPanelVisible = true,
                    onToggleDebugPanel = {},
                    isHapticEnabled = true,
                    onToggleHaptic = {},
                    joystickSensitivity = sensitivity,
                    onJoystickSensitivityChange = { sensitivity = it },
                    allowReflection = false,
                    onToggleReflection = {},
                    customCommandCount = 0,
                    onShowCustomCommands = {},
                    onResetTutorial = {}
                )
            }
        }

        composeRule.onNodeWithText("Joystick Sensitivity").assertIsDisplayed()
        composeRule.onNodeWithText("1.0x").assertIsDisplayed()

        // Slider exposes SetProgress in the slider's valueRange; set it to the min (0.5x).
        composeRule
            .onAllNodes(hasSetProgressAction())
            .onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }

        composeRule.onNodeWithText("0.5x").assertIsDisplayed()
    }
}
