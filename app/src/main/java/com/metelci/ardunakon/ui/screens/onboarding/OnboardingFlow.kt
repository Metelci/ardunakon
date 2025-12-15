package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.metelci.ardunakon.model.OnboardingStep

/**
 * Main composable for the onboarding flow.
 * Routes to appropriate screen based on current step.
 *
 * @param onComplete Called when user completes the tutorial
 * @param onSkip Called when user skips the tutorial
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()

    // Handle finished state
    if (currentStep is OnboardingStep.Finished) {
        onComplete()
        return
    }

    AnimatedContent(
        targetState = currentStep,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                (slideOutHorizontally { width -> -width } + fadeOut())
        },
        label = "onboarding_transition"
    ) { step ->
        when (step) {
            is OnboardingStep.Welcome -> {
                WelcomeScreen(
                    onStart = { viewModel.nextStep() },
                    onSkip = {
                        viewModel.skipOnboarding()
                        onSkip()
                    }
                )
            }
            is OnboardingStep.InterfaceTour -> {
                // Show the real ControlScreen with overlay highlights
                RealScreenTour(
                    currentElement = step.element,
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() },
                    onSkip = {
                        viewModel.skipOnboarding()
                        onSkip()
                    },
                    progress = viewModel.getProgressPercent()
                )
            }
            is OnboardingStep.ConnectionTutorial -> {
                ConnectionTutorialScreen(
                    step = step.step,
                    selectedArduinoType = viewModel.selectedArduinoType,
                    onArduinoSelected = { viewModel.selectArduinoType(it) },
                    connectionMode = viewModel.connectionMode,
                    onConnectionModeChanged = { viewModel.updateConnectionMode(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() },
                    onSkip = {
                        viewModel.skipOnboarding()
                        onSkip()
                    },
                    progress = viewModel.getProgressPercent()
                )
            }
                // AdvancedFeatures step removed/merged

            is OnboardingStep.Completion -> {
                CompletionScreen(
                    onFinish = {
                        viewModel.completeOnboarding()
                        onComplete()
                    },
                    exploredFeatures = viewModel.exploredFeatures
                )
            }
            is OnboardingStep.Finished -> {
                // Already handled above
            }
        }
    }
}
