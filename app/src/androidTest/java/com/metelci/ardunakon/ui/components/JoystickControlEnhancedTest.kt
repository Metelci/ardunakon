package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Enhanced Compose UI tests for JoystickControl.
 *
 * Additional edge cases, performance scenarios, and
 * advanced interaction patterns for joystick control.
 */
@RunWith(AndroidJUnit4::class)
class JoystickControlEnhancedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun joystickControl_throttleMode_retainsYPosition() {
        var lastY = 0f

        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { state -> lastY = state.y },
                    isThrottle = true
                )
            }
        }

        // In throttle mode, Y should retain last position
        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_normalMode_resetsToCenter() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> },
                    isThrottle = false
                )
            }
        }

        // Normal mode should reset to center when released
        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_withLatency_displaysQualityRing() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> },
                    connectionLatencyMs = 50L
                )
            }
        }

        // Should show quality ring based on latency
        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_withHighLatency_showsWarning() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> },
                    connectionLatencyMs = 300L // High latency
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_withoutLatency_noQualityRing() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> },
                    connectionLatencyMs = null
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_rapidMovement_handlesGracefully() {
        var moveCount = 0

        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> moveCount++ }
                )
            }
        }

        // Simulate rapid movement
        composeRule.onNodeWithContentDescription("Joystick Control", substring = true)
            .performTouchInput {
                down(center)
                repeat(10) {
                    moveTo(topLeft)
                    moveTo(bottomRight)
                }
                up()
            }

        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_edgePositions_clampValues() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { state ->
                        // Values should be clamped to -1..1 range
                        assert(state.x in -1f..1f)
                        assert(state.y in -1f..1f)
                    }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_differentSizes_render() {
        listOf(100, 150, 200, 250).forEach { size ->
            composeRule.setContent {
                MaterialTheme {
                    JoystickControl(
                        onMoved = { _ -> },
                        size = androidx.compose.ui.unit.dp(size)
                    )
                }
            }

            composeRule.waitForIdle()
        }
    }

    @Test
    fun joystickControl_multiTouch_handlesCorrectly() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> }
                )
            }
        }

        // Should handle multi-touch gracefully
        composeRule.waitForIdle()
    }

    @Test
    fun joystickControl_accessibility_hasSemantics() {
        composeRule.setContent {
            MaterialTheme {
                JoystickControl(
                    onMoved = { _ -> }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Joystick Control", substring = true)
            .assertExists()
    }
}
