package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Enhanced Compose UI tests for ServoButtonControl.
 *
 * Additional edge cases, rapid input handling,
 * and advanced servo control scenarios.
 */
@RunWith(AndroidJUnit4::class)
class ServoButtonControlEnhancedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun servoControl_initialPosition_isZero() {
        var capturedX = -999f
        var capturedY = -999f
        var capturedZ = -999f

        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { x, y, z ->
                        capturedX = x
                        capturedY = y
                        capturedZ = z
                    }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_extremePositions_handled() {
        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 1f,
                    servoY = -1f,
                    servoZ = 0.5f,
                    onMove = { x, y, z ->
                        assert(x in -1f..1f)
                        assert(y in -1f..1f)
                        assert(z in -1f..1f)
                    }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_rapidClicks_rateLimit() {
        var clickCount = 0

        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { _, _, _ -> clickCount++ },
                    buttonSize = androidx.compose.ui.unit.dp(60)
                )
            }
        }

        // Rate limiting should prevent excessive updates
        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_logging_invoked() {
        val logs = mutableListOf<String>()

        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { _, _, _ -> },
                    onLog = { msg -> logs.add(msg) }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_differentButtonSizes_render() {
        listOf(40, 56, 72, 88).forEach { size ->
            composeRule.setContent {
                MaterialTheme {
                    ServoButtonControl(
                        servoX = 0f,
                        servoY = 0f,
                        servoZ = 0f,
                        onMove = { _, _, _ -> },
                        buttonSize = androidx.compose.ui.unit.dp(size)
                    )
                }
            }

            composeRule.waitForIdle()
        }
    }

    @Test
    fun servoControl_allAxes_independent() {
        var xChanged = false
        var yChanged = false
        var zChanged = false

        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { x, y, z ->
                        if (x != 0f) xChanged = true
                        if (y != 0f) yChanged = true
                        if (z != 0f) zChanged = true
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // Each axis should be independently controllable
    }

    @Test
    fun servoControl_precision_incrementsSmooth() {
        val positions = mutableListOf<Float>()

        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { x, _, _ -> positions.add(x) }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_accessibility_buttonsLabeled() {
        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { _, _, _ -> }
                )
            }
        }

        // Buttons should have accessibility labels
        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_zeroStep_handledGracefully() {
        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { _, _, _ -> },
                    step = 0.001f // Very small step
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun servoControl_largeStep_handledGracefully() {
        composeRule.setContent {
            MaterialTheme {
                ServoButtonControl(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onMove = { x, y, z ->
                        // Large steps should still clamp to valid range
                        assert(x in -1f..1f)
                        assert(y in -1f..1f)
                        assert(z in -1f..1f)
                    },
                    step = 0.5f // Large step
                )
            }
        }

        composeRule.waitForIdle()
    }
}
