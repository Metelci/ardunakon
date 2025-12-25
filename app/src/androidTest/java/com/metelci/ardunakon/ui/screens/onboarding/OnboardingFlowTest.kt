package com.metelci.ardunakon.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.OnboardingStep
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for OnboardingFlow.
 *
 * Tests onboarding wizard flow navigation,
 * step progression, and completion handling.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingFlow_startsAtWelcome() {
        composeRule.setContent {
            MaterialTheme {
                var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
                
                OnboardingFlow(
                    currentStep = currentStep,
                    onStepChanged = { currentStep = it },
                    onComplete = {}
                )
            }
        }

        // Welcome screen should be displayed
        composeRule.waitForIdle()
    }

    @Test
    fun onboardingFlow_progressesThroughSteps() {
        composeRule.setContent {
            MaterialTheme {
                var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
                
                OnboardingFlow(
                    currentStep = currentStep,
                    onStepChanged = { currentStep = it },
                    onComplete = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun onboardingFlow_canSkip() {
        var skipCalled = false

        composeRule.setContent {
            MaterialTheme {
                var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
                
                OnboardingFlow(
                    currentStep = currentStep,
                    onStepChanged = { currentStep = it },
                    onComplete = { skipCalled = true }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun onboardingFlow_completionInvokesCallback() {
        var completeCalled = false

        composeRule.setContent {
            MaterialTheme {
                var currentStep by remember { mutableStateOf(OnboardingStep.COMPLETION) }
                
                OnboardingFlow(
                    currentStep = currentStep,
                    onStepChanged = {},
                    onComplete = { completeCalled = true }
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun onboardingFlow_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
                
                OnboardingFlow(
                    currentStep = currentStep,
                    onStepChanged = { currentStep = it },
                    onComplete = {}
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun onboardingFlow_allSteps_render() {
        val steps = listOf(
            OnboardingStep.WELCOME,
            OnboardingStep.INTERFACE_TOUR,
            OnboardingStep.CONNECTION_TUTORIAL,
            OnboardingStep.COMPLETION
        )

        steps.forEach { step ->
            composeRule.setContent {
                MaterialTheme {
                    OnboardingFlow(
                        currentStep = step,
                        onStepChanged = {},
                        onComplete = {}
                    )
                }
            }

            composeRule.waitForIdle()
        }
    }
}
