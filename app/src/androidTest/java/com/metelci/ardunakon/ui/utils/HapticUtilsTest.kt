package com.metelci.ardunakon.ui.utils

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for HapticUtils extension functions.
 *
 * Tests haptic feedback utilities integrated
 * with Compose UI components.
 */
@RunWith(AndroidJUnit4::class)
class HapticUtilsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hapticUtils_tapFeedback_doesNotCrash() {
        composeRule.setContent {
            MaterialTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                
                androidx.compose.material3.Button(
                    onClick = {
                        view.hapticTap()
                    }
                ) {
                    androidx.compose.material3.Text("Test Haptic")
                }
            }
        }

        composeRule.onNodeWithText("Test Haptic").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun hapticUtils_multipleTaps_handledGracefully() {
        composeRule.setContent {
            MaterialTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                
                androidx.compose.material3.Button(
                    onClick = {
                        repeat(5) {
                           view.hapticTap()
                        }
                    }
                ) {
                    androidx.compose.material3.Text("Multiple Taps")
                }
            }
        }

        composeRule.onNodeWithText("Multiple Taps").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun hapticUtils_rapidFeedback_doesNotCrash() {
        composeRule.setContent {
            MaterialTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                
                androidx.compose.material3.Button(
                    onClick = {
                        view.hapticTap()
                    }
                ) {
                    androidx.compose.material3.Text("Rapid Test")
                }
            }
        }

        // Simulate rapid clicking
        repeat(10) {
            composeRule.onNodeWithText("Rapid Test").performClick()
        }
        
        composeRule.waitForIdle()
    }
}
