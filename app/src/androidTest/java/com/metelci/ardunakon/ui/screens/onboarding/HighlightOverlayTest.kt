package com.metelci.ardunakon.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for HighlightOverlay.
 *
 * Tests tutorial highlight overlays that guide
 * users through interface features.
 */
@RunWith(AndroidJUnit4::class)
class HighlightOverlayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun highlightOverlay_displaysMessage() {
        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = "This is the joystick",
                    onNext = {},
                    onSkip = {}
                )
            }
        }

        composeRule.onNodeWithText("This is the joystick").assertIsDisplayed()
    }

    @Test
    fun highlightOverlay_hasNextButton() {
        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = "Test message",
                    onNext = {},
                    onSkip = {}
                )
            }
        }

        composeRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun highlightOverlay_hasSkipButton() {
        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = "Test message",
                    onNext = {},
                    onSkip = {}
                )
            }
        }

        composeRule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun highlightOverlay_invokesOnNext() {
        var nextCalled = false

        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = "Test",
                    onNext = { nextCalled = true },
                    onSkip = {}
                )
            }
        }

        composeRule.onNodeWithText("Next").performClick()
        assert(nextCalled)
    }

    @Test
    fun highlightOverlay_invokesOnSkip() {
        var skipCalled = false

        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = "Test",
                    onNext = {},
                    onSkip = { skipCalled = true }
                )
            }
        }

        composeRule.onNodeWithText("Skip").performClick()
        assert(skipCalled)
    }

    @Test
    fun highlightOverlay_displaysLongMessage() {
        val longMessage = "This is a very long tutorial message that explains in " +
                "detail how to use this particular feature of the application interface."

        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = longMessage,
                    onNext = {},
                    onSkip = {}
                )
            }
        }

        composeRule.onNodeWithText(longMessage, substring = true).assertIsDisplayed()
    }

    @Test
    fun highlightOverlay_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                HighlightOverlay(
                    message = "Tutorial step",
                    onNext = {},
                    onSkip = {}
                )
            }
        }

        composeRule.waitForIdle()
    }
}
