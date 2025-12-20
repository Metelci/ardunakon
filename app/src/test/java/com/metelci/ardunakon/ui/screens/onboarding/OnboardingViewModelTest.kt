package com.metelci.ardunakon.ui.screens.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.OnboardingManager
import com.metelci.ardunakon.data.OnboardingPreferences
import com.metelci.ardunakon.model.ConnectionTutorialStep
import com.metelci.ardunakon.model.InterfaceElement
import com.metelci.ardunakon.model.OnboardingStep
import com.metelci.ardunakon.ui.screens.control.ConnectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // SDK 34 is compatible with Java 17 (SDK 35+ requires Java 21)
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var onboardingManager: OnboardingManager
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val timeoutMs = 1_500L

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()

        // Ensure deterministic state across tests.
        OnboardingPreferences(context).clear()
        connectionPreferences = ConnectionPreferences(context, TestCryptoEngine())
        runBlocking { connectionPreferences.saveLastConnection(type = null) }

        onboardingManager = OnboardingManager(OnboardingPreferences(context))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun waitUntil(condition: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) {
                delay(10)
            }
        }
    }

    @Test
    fun init_starts_onboarding_and_loads_saved_connection_mode() {
        runBlocking { connectionPreferences.saveLastConnection(type = "WIFI") }

        val viewModel = OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )

        runBlocking { waitUntil { viewModel.connectionMode == ConnectionMode.WIFI } }
        assertEquals(ConnectionMode.WIFI, viewModel.connectionMode)
        assertEquals(OnboardingStep.Welcome, viewModel.currentStep.value)
    }

    @Test
    fun nextStep_progresses_through_interface_tour_then_connection_tutorial_then_completion() = runBlocking {
        val viewModel = OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )

        viewModel.nextStep()
        waitUntil { viewModel.currentStep.value == OnboardingStep.InterfaceTour(InterfaceElement.ESTOP) }
        assertEquals(OnboardingStep.InterfaceTour(InterfaceElement.ESTOP), viewModel.currentStep.value)

        InterfaceElement.tourOrder().drop(1).forEach { element ->
            viewModel.nextStep()
            waitUntil { viewModel.currentStep.value == OnboardingStep.InterfaceTour(element) }
            assertEquals(OnboardingStep.InterfaceTour(element), viewModel.currentStep.value)
        }

        viewModel.nextStep()
        waitUntil {
            viewModel.currentStep.value == OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO)
        }
        assertEquals(
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO),
            viewModel.currentStep.value
        )

        viewModel.nextStep()
        waitUntil {
            viewModel.currentStep.value == OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CONNECTION_MODE)
        }
        assertEquals(
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CONNECTION_MODE),
            viewModel.currentStep.value
        )

        viewModel.nextStep()
        waitUntil {
            viewModel.currentStep.value == OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL)
        }
        assertEquals(
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL),
            viewModel.currentStep.value
        )

        viewModel.nextStep()
        waitUntil { viewModel.currentStep.value == OnboardingStep.Completion }
        assertEquals(OnboardingStep.Completion, viewModel.currentStep.value)

        viewModel.nextStep()
        waitUntil { viewModel.currentStep.value == OnboardingStep.Finished }
        assertEquals(OnboardingStep.Finished, viewModel.currentStep.value)
    }

    @Test
    fun previousStep_from_connection_tutorial_first_step_goes_back_to_last_interface_element() = runBlocking {
        val viewModel = OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )

        // Walk into connection tutorial first step.
        InterfaceElement.tourOrder().forEach { _ ->
            viewModel.nextStep()
        }
        viewModel.nextStep()
        waitUntil {
            viewModel.currentStep.value == OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO)
        }
        assertEquals(
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO),
            viewModel.currentStep.value
        )

        viewModel.previousStep()
        waitUntil { viewModel.currentStep.value == OnboardingStep.InterfaceTour(InterfaceElement.tourOrder().last()) }
        assertEquals(
            OnboardingStep.InterfaceTour(InterfaceElement.tourOrder().last()),
            viewModel.currentStep.value
        )
    }

    @Test
    fun previousStep_from_completion_returns_to_setup_final() = runBlocking {
        val viewModel = OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )

        // Go to completion.
        InterfaceElement.tourOrder().forEach { _ ->
            viewModel.nextStep()
        }
        ConnectionTutorialStep.entries.forEach { _ ->
            viewModel.nextStep()
        }
        viewModel.nextStep()
        waitUntil { viewModel.currentStep.value == OnboardingStep.Completion }
        assertEquals(OnboardingStep.Completion, viewModel.currentStep.value)

        viewModel.previousStep()
        waitUntil {
            viewModel.currentStep.value == OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL)
        }
        assertEquals(
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL),
            viewModel.currentStep.value
        )
    }

    @Test
    fun progressPercent_is_monotonic_across_happy_path() = runBlocking {
        val viewModel = OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )

        val progress = mutableListOf<Float>()
        progress += viewModel.getProgressPercent()

        // Walk through to completion.
        repeat(InterfaceElement.tourOrder().size + ConnectionTutorialStep.entries.size + 2) {
            viewModel.nextStep()
            progress += viewModel.getProgressPercent()
        }

        assertTrue(progress.zipWithNext().all { (a, b) -> b >= a })
        assertEquals(1f, progress.last(), 0.0001f)
    }

    @Test
    fun updateConnectionMode_persists_to_preferences() = runBlocking {
        val viewModel = OnboardingViewModel(
            onboardingManager = onboardingManager,
            connectionPreferences = connectionPreferences
        )

        viewModel.updateConnectionMode(ConnectionMode.WIFI)
        waitUntil { connectionPreferences.loadLastConnection().type == "WIFI" }

        val saved = connectionPreferences.loadLastConnection()
        assertEquals("WIFI", saved.type)
    }
}
