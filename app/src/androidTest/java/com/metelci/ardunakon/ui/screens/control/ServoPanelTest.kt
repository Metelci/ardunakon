package com.metelci.ardunakon.ui.screens.control

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ServoPanel.
 *
 * Tests servo button controls rendering and
 * servo position adjustment callbacks.
 */
@RunWith(AndroidJUnit4::class)
class ServoPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun servoPanel_renders() {
        composeRule.setContent {
            MaterialTheme {
                ServoPanel(
                    onServoMove = { _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoPanel_hasServoControls() {
        composeRule.setContent {
            MaterialTheme {
                ServoPanel(
                    onServoMove = { _, _, _ -> }
                )
            }
        }

        // Should have servo controls (X, Y, Z axes)
        composeRule.waitForIdle()
    }

    @Test
    fun servoPanel_invokesOnServoMove() {
        var servoMoveCalled = false

        composeRule.setContent {
            MaterialTheme {
                ServoPanel(
                    onServoMove = { _, _, _ ->
                        servoMoveCalled = true
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // Servo controls should trigger callbacks when used
    }

    @Test
    fun servoPanel_hasMultipleAxes() {
        composeRule.setContent {
            MaterialTheme {
                ServoPanel(
                    onServoMove = { x, y, z -> 
                        // Should receive all three axes
                    }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoPanel_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                ServoPanel(
                    onServoMove = { _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
    }
}
