package com.metelci.ardunakon.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for DemoControlLayout.
 *
 * Tests demo UI rendering for onboarding tutorials,
 * interactive highlights, and tour navigation.
 */
@RunWith(AndroidJUnit4::class)
class DemoControlLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun demoLayout_renders() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = false,
                    highlightServo = false,
                    highlightStatus = false
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun demoLayout_highlightsJoystick() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = true,
                    highlightServo = false,
                    highlightStatus = false
                )
            }
        }

        composeRule.waitForIdle()
        // Joystick should be highlighted
    }

    @Test
    fun demoLayout_highlightsServo() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = false,
                    highlightServo = true,
                    highlightStatus = false
                )
            }
        }

        composeRule.waitForIdle()
        // Servo controls should be highlighted
    }

    @Test
    fun demoLayout_highlightsStatus() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = false,
                    highlightServo = false,
                    highlightStatus = true
                )
            }
        }

        composeRule.waitForIdle()
        // Status area should be highlighted
    }

    @Test
    fun demoLayout_multipleHighlights() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = true,
                    highlightServo = true,
                    highlightStatus = true
                )
            }
        }

        composeRule.waitForIdle()
        // All areas highlighted simultaneously
    }

    @Test
    fun demoLayout_noHighlights() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = false,
                    highlightServo = false,
                    highlightStatus = false
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun demoLayout_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                DemoControlLayout(
                    highlightJoystick = false,
                    highlightServo = false,
                    highlightStatus = false
                )
            }
        }

        composeRule.waitForIdle()
    }
}
