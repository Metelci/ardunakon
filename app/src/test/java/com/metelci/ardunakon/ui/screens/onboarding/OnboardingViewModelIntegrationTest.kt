package com.metelci.ardunakon.ui.screens.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.model.ArduinoType
import com.metelci.ardunakon.model.ConnectionTutorialStep
import com.metelci.ardunakon.model.FeatureType
import com.metelci.ardunakon.model.InterfaceElement
import com.metelci.ardunakon.model.OnboardingStep
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Extended integration tests for OnboardingViewModel.
 *
 * Focuses on step navigation, progress tracking, and configuration flows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelIntegrationTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var onboardingManager: OnboardingManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        connectionPreferences = mockk(relaxed = true)
        onboardingManager = mockk(relaxed = true)

        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            btType = null,
            wifiIp = null,
            wifiPort = 8888,
            wifiPsk = null,
            autoReconnectWifi = false,
            joystickSensitivity = 1.0f
        )
    }

    private fun createViewModel(): OnboardingViewModel {
        return OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )
    }

    // ==================== Initial State Flow ====================

    @Test
    fun `initial step is Welcome`() = runTest {
        val viewModel = createViewModel()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Welcome)
    }

    @Test
    fun `initial connection mode is BLUETOOTH`() = runTest {
        val viewModel = createViewModel()

        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
    }

    @Test
    fun `initial progress is 0`() = runTest {
        val viewModel = createViewModel()

        assertEquals(0f, viewModel.getProgressPercent(), 0.001f)
    }

    @Test
    fun `initial Arduino type is null`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.selectedArduinoType)
    }

    @Test
    fun `initial explored features is empty`() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.exploredFeatures.isEmpty())
    }

    // ==================== Navigation Forward Flow ====================

    @Test
    fun `nextStep from Welcome goes to InterfaceTour`() = runTest {
        val viewModel = createViewModel()

        viewModel.nextStep()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.InterfaceTour)
    }

    @Test
    fun `nextStep progresses through InterfaceTour elements`() = runTest {
        val viewModel = createViewModel()

        // Move past Welcome
        viewModel.nextStep()

        val firstStep = viewModel.currentStep.first()
        assertTrue(firstStep is OnboardingStep.InterfaceTour)

        val firstElement = (firstStep as OnboardingStep.InterfaceTour).element

        // Move to next interface element
        viewModel.nextStep()

        val secondStep = viewModel.currentStep.first()
        assertTrue(secondStep is OnboardingStep.InterfaceTour)

        val secondElement = (secondStep as OnboardingStep.InterfaceTour).element
        assertNotEquals(firstElement, secondElement)
    }

    @Test
    fun `nextStep eventually reaches ConnectionTutorial`() = runTest {
        val viewModel = createViewModel()

        // Progress through Welcome + all InterfaceTour elements
        val interfaceElements = InterfaceElement.tourOrder()
        repeat(interfaceElements.size + 1) {
            viewModel.nextStep()
        }

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.ConnectionTutorial)
    }

    @Test
    fun `nextStep from ConnectionTutorial last step goes to Completion`() = runTest {
        val viewModel = createViewModel()

        // Skip to last ConnectionTutorial step
        viewModel.skipToPhase(OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL))

        viewModel.nextStep()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Completion)
    }

    @Test
    fun `nextStep from Completion goes to Finished`() = runTest {
        val viewModel = createViewModel()

        viewModel.skipToPhase(OnboardingStep.Completion)

        viewModel.nextStep()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Finished)
    }

    // ==================== Navigation Backward Flow ====================

    @Test
    fun `previousStep from Welcome stays at Welcome`() = runTest {
        val viewModel = createViewModel()

        viewModel.previousStep()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Welcome)
    }

    @Test
    fun `previousStep from InterfaceTour first element goes to Welcome`() = runTest {
        val viewModel = createViewModel()

        // Move to first InterfaceTour element
        viewModel.nextStep()

        assertTrue(viewModel.currentStep.first() is OnboardingStep.InterfaceTour)

        // Go back
        viewModel.previousStep()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Welcome)
    }

    @Test
    fun `previousStep from Completion goes to ConnectionTutorial`() = runTest {
        val viewModel = createViewModel()

        viewModel.skipToPhase(OnboardingStep.Completion)

        viewModel.previousStep()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.ConnectionTutorial)
    }

    // ==================== Skip Flow ====================

    @Test
    fun `skipToPhase jumps directly to specified step`() = runTest {
        val viewModel = createViewModel()

        viewModel.skipToPhase(OnboardingStep.Completion)

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Completion)
    }

    @Test
    fun `skipOnboarding marks as finished`() = runTest {
        val viewModel = createViewModel()

        viewModel.skipOnboarding()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Finished)
        verify { onboardingManager.skipOnboarding() }
    }

    @Test
    fun `completeOnboarding marks as finished`() = runTest {
        val viewModel = createViewModel()

        viewModel.completeOnboarding()

        val step = viewModel.currentStep.first()
        assertTrue(step is OnboardingStep.Finished)
        verify { onboardingManager.completeOnboarding() }
    }

    // ==================== Progress Flow ====================

    @Test
    fun `progress increases as steps progress`() = runTest {
        val viewModel = createViewModel()

        val initialProgress = viewModel.getProgressPercent()

        viewModel.nextStep()

        val afterOneStep = viewModel.getProgressPercent()
        assertTrue(afterOneStep > initialProgress)
    }

    @Test
    fun `progress is 95 percent at Completion`() = runTest {
        val viewModel = createViewModel()

        viewModel.skipToPhase(OnboardingStep.Completion)

        assertEquals(0.95f, viewModel.getProgressPercent(), 0.001f)
    }

    @Test
    fun `progress is 100 percent at Finished`() = runTest {
        val viewModel = createViewModel()

        viewModel.skipOnboarding()

        assertEquals(1.0f, viewModel.getProgressPercent(), 0.001f)
    }

    // ==================== Arduino Type Selection Flow ====================

    @Test
    fun `selectArduinoType updates selection`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectArduinoType(ArduinoType.UNO_Q)

        assertEquals(ArduinoType.UNO_Q, viewModel.selectedArduinoType)
    }

    @Test
    fun `selectArduinoType can change selection`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectArduinoType(ArduinoType.UNO_Q)
        viewModel.selectArduinoType(ArduinoType.NANO_ESP32)

        assertEquals(ArduinoType.NANO_ESP32, viewModel.selectedArduinoType)
    }

    // ==================== Feature Exploration Flow ====================

    @Test
    fun `toggleFeatureExploration adds feature`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleFeatureExploration(FeatureType.TELEMETRY)

        assertTrue(FeatureType.TELEMETRY in viewModel.exploredFeatures)
    }

    @Test
    fun `toggleFeatureExploration removes feature on second toggle`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleFeatureExploration(FeatureType.TELEMETRY)
        viewModel.toggleFeatureExploration(FeatureType.TELEMETRY)

        assertFalse(FeatureType.TELEMETRY in viewModel.exploredFeatures)
    }

    @Test
    fun `multiple features can be explored`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleFeatureExploration(FeatureType.TELEMETRY)
        viewModel.toggleFeatureExploration(FeatureType.DEBUG_CONSOLE)

        assertTrue(FeatureType.TELEMETRY in viewModel.exploredFeatures)
        assertTrue(FeatureType.DEBUG_CONSOLE in viewModel.exploredFeatures)
    }

    // ==================== Connection Mode Flow ====================

    @Test
    fun `updateConnectionMode updates to WIFI`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateConnectionMode(ConnectionMode.WIFI)

        assertEquals(ConnectionMode.WIFI, viewModel.connectionMode)
    }

    @Test
    fun `updateConnectionMode persists preference`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateConnectionMode(ConnectionMode.WIFI)

        coVerify { connectionPreferences.saveLastConnection(type = "WIFI") }
    }

    @Test
    fun `updateConnectionMode updates to BLUETOOTH`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateConnectionMode(ConnectionMode.WIFI)
        viewModel.updateConnectionMode(ConnectionMode.BLUETOOTH)

        assertEquals(ConnectionMode.BLUETOOTH, viewModel.connectionMode)
    }

    // ==================== Onboarding Manager Integration ====================

    @Test
    fun `init calls startOnboarding`() = runTest {
        createViewModel()

        verify { onboardingManager.startOnboarding() }
    }

    @Test
    fun `navigation calls updateCurrentStep`() = runTest {
        val viewModel = createViewModel()

        viewModel.nextStep()

        verify { onboardingManager.updateCurrentStep(any()) }
    }
}
