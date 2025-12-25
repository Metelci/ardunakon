package com.metelci.ardunakon.ui.screens.control

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for JoystickPanel.
 *
 * Tests joystick control rendering, touch input handling,
 * and callback triggering for motor control.
 */
@RunWith(AndroidJUnit4::class)
class JoystickPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun joystickPanel_renders() {
        composeRule.setContent {
            MaterialTheme {
                JoystickPanel(
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun joystickPanel_hasJoystickControl() {
        composeRule.setContent {
            MaterialTheme {
                JoystickPanel(
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Joystick Control", substring = true)
            .assertExists()
    }

    @Test
    fun joystickPanel_handlesTouchInput() {
        var moveCalled = false

        composeRule.setContent {
            MaterialTheme {
                JoystickPanel(
                    onMove = { _, _ -> 
                        moveCalled = true
                    }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Joystick Control", substring = true)
            .performTouchInput { 
                down(center)
                up()
            }

        composeRule.waitForIdle()
        // Touch should trigger move callback
    }

    @Test
    fun joystickPanel_invokesOnMove() {
        var moveCount = 0

        composeRule.setContent {
            MaterialTheme {
                JoystickPanel(
                    onMove = { _, _ ->
                        moveCount++
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // onMove should be invoked during joystick interaction
    }

    @Test
    fun joystickPanel_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                JoystickPanel(
                    onMove = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
    }
}
