package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.model.ArduinoType
import com.metelci.ardunakon.model.ConnectionTutorialStep
import com.metelci.ardunakon.model.FeatureType
import com.metelci.ardunakon.model.InterfaceElement
import com.metelci.ardunakon.model.OnboardingStep
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the onboarding flow.
 * Manages navigation between tutorial phases and tracks user progress.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
    private val connectionPreferences: ConnectionPreferences
) : ViewModel() {

    private val _currentStep = MutableStateFlow<OnboardingStep>(OnboardingStep.Welcome)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    /** Selected Arduino type during connection tutorial */
    var selectedArduinoType by mutableStateOf<ArduinoType?>(null)
        private set

    /** Connection Mode (Bluetooth/WiFi) */
    var connectionMode by mutableStateOf(ConnectionMode.BLUETOOTH)
        private set

    /** Features user has chosen to explore */
    var exploredFeatures by mutableStateOf(setOf<FeatureType>())
        private set

    /** Index for interface tour element tracking */
    private var interfaceTourIndex = 0

    init {
        onboardingManager.startOnboarding()
        // Initialize connection mode from preferences
        viewModelScope.launch {
            val savedMode = connectionPreferences.loadLastConnection().type
            if (savedMode == "WIFI") {
                connectionMode = ConnectionMode.WIFI
            }
        }
    }

    fun updateConnectionMode(mode: ConnectionMode) {
        connectionMode = mode
        viewModelScope.launch {
            connectionPreferences.saveLastConnection(type = if (mode == ConnectionMode.WIFI) "WIFI" else "BLUETOOTH")
        }
    }

    // ========== Navigation ==========

    /**
     * Advances to the next step in the onboarding flow.
     */
    fun nextStep() {
        viewModelScope.launch {
            val next = calculateNextStep()
            _currentStep.value = next
            saveProgress()
        }
    }

    /**
     * Goes back to the previous step.
     */
    fun previousStep() {
        viewModelScope.launch {
            val previous = calculatePreviousStep()
            _currentStep.value = previous
            saveProgress()
        }
    }

    /**
     * Skips directly to a specific phase.
     */
    fun skipToPhase(step: OnboardingStep) {
        _currentStep.value = step
        saveProgress()
    }

    /**
     * Skips the entire onboarding flow.
     */
    fun skipOnboarding() {
        onboardingManager.skipOnboarding()
        _currentStep.value = OnboardingStep.Finished
    }

    /**
     * Completes the onboarding flow successfully.
     */
    fun completeOnboarding() {
        onboardingManager.completeOnboarding()
        _currentStep.value = OnboardingStep.Finished
    }

    // ========== Arduino Selection ==========

    fun selectArduinoType(type: ArduinoType) {
        selectedArduinoType = type
    }

    // ========== Advanced Features ==========

    fun toggleFeatureExploration(feature: FeatureType) {
        exploredFeatures = if (feature in exploredFeatures) {
            exploredFeatures - feature
        } else {
            exploredFeatures + feature
        }
    }

    // ========== Step Calculation ==========

    private fun calculateNextStep(): OnboardingStep {
        return when (val current = _currentStep.value) {
            is OnboardingStep.Welcome -> {
                // Start interface tour with first element
                interfaceTourIndex = 0
                OnboardingStep.InterfaceTour(InterfaceElement.tourOrder().first())
            }
            is OnboardingStep.InterfaceTour -> {
                val elements = InterfaceElement.tourOrder()
                interfaceTourIndex++
                if (interfaceTourIndex < elements.size) {
                    OnboardingStep.InterfaceTour(elements[interfaceTourIndex])
                } else {
                    // Move to connection tutorial
                    OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO)
                }
            }
            is OnboardingStep.ConnectionTutorial -> {
                val steps = ConnectionTutorialStep.entries
                val currentIndex = steps.indexOf(current.step)
                if (currentIndex < steps.size - 1) {
                    OnboardingStep.ConnectionTutorial(steps[currentIndex + 1])
                } else {
                    // Final step (SETUP_FINAL) completed -> finish
                    OnboardingStep.Completion
                }
            }
            is OnboardingStep.Completion -> OnboardingStep.Finished
            is OnboardingStep.Finished -> OnboardingStep.Finished
        }
    }

    private fun calculatePreviousStep(): OnboardingStep {
        return when (val current = _currentStep.value) {
            is OnboardingStep.Welcome -> OnboardingStep.Welcome
            is OnboardingStep.InterfaceTour -> {
                val elements = InterfaceElement.tourOrder()
                interfaceTourIndex--
                if (interfaceTourIndex >= 0) {
                    OnboardingStep.InterfaceTour(elements[interfaceTourIndex])
                } else {
                    interfaceTourIndex = 0
                    OnboardingStep.Welcome
                }
            }
            is OnboardingStep.ConnectionTutorial -> {
                val steps = ConnectionTutorialStep.entries
                val currentIndex = steps.indexOf(current.step)
                if (currentIndex > 0) {
                    OnboardingStep.ConnectionTutorial(steps[currentIndex - 1])
                } else {
                    // Go back to last interface tour element
                    interfaceTourIndex = InterfaceElement.tourOrder().size - 1
                    OnboardingStep.InterfaceTour(InterfaceElement.tourOrder().last())
                }
            }
            is OnboardingStep.Completion -> OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL)
            is OnboardingStep.Finished -> OnboardingStep.Completion
        }
    }

    private fun saveProgress() {
        val stepIndex = when (_currentStep.value) {
            is OnboardingStep.Welcome -> 0
            is OnboardingStep.InterfaceTour -> 1
            is OnboardingStep.ConnectionTutorial -> 2
            is OnboardingStep.Completion -> 3
            is OnboardingStep.Finished -> 4
        }
        onboardingManager.updateCurrentStep(stepIndex)
    }

    /**
     * Gets the progress percentage through the tutorial.
     */
    fun getProgressPercent(): Float {
        return when (_currentStep.value) {
            is OnboardingStep.Welcome -> 0f
            is OnboardingStep.InterfaceTour -> {
                val elementProgress = interfaceTourIndex.toFloat() / InterfaceElement.tourOrder().size
                0.1f + (0.3f * elementProgress)
            }
            is OnboardingStep.ConnectionTutorial -> {
                val step = (_currentStep.value as OnboardingStep.ConnectionTutorial).step
                val stepProgress = ConnectionTutorialStep.entries.indexOf(step).toFloat() /
                    ConnectionTutorialStep.entries.size
                0.4f + (0.5f * stepProgress)
            }
            is OnboardingStep.Completion -> 0.95f
            is OnboardingStep.Finished -> 1f
        }
    }
}
