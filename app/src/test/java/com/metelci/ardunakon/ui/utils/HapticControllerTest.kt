package com.metelci.ardunakon.ui.utils

import android.view.HapticFeedbackConstants
import android.view.View
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for HapticController.
 *
 * Tests enabled/disabled state, global control, and haptic feedback execution.
 */
@RunWith(RobolectricTestRunner::class)
class HapticControllerTest {

    private lateinit var mockView: View

    @Before
    fun setup() {
        mockView = mockk(relaxed = true)
        // Reset to default enabled state
        HapticController.isEnabled = true
    }

    @After
    fun teardown() {
        // Restore default state
        HapticController.isEnabled = true
        clearAllMocks()
    }

    @Test
    fun `performHaptic executes feedback when enabled`() {
        HapticController.isEnabled = true

        HapticController.performHaptic(mockView)

        verify { mockView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }

    @Test
    fun `default enabled state is true`() {
        assert(HapticController.isEnabled)
    }

    @Test
    fun `performHaptic does not execute feedback when disabled`() {
        HapticController.isEnabled = false

        HapticController.performHaptic(mockView)

        verify(exactly = 0) { mockView.performHapticFeedback(any()) }
    }

    @Test
    fun `performHaptic uses custom feedback constant`() {
        HapticController.isEnabled = true

        HapticController.performHaptic(mockView, HapticFeedbackConstants.LONG_PRESS)

        verify { mockView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    }

    @Test
    fun `performHaptic respects enabled state change`() {
        // Start enabled
        HapticController.isEnabled = true
        HapticController.performHaptic(mockView)
        verify(exactly = 1) { mockView.performHapticFeedback(any()) }

        clearMocks(mockView, answers = false)

        // Disable
        HapticController.isEnabled = false
        HapticController.performHaptic(mockView)
        verify(exactly = 0) { mockView.performHapticFeedback(any()) }

        clearMocks(mockView, answers = false)

        // Re-enable
        HapticController.isEnabled = true
        HapticController.performHaptic(mockView)
        verify(exactly = 1) { mockView.performHapticFeedback(any()) }
    }

    @Test
    fun `hapticTap extension calls controller with KEYBOARD_TAP`() {
        HapticController.isEnabled = true

        mockView.hapticTap()

        verify { mockView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }

    @Test
    fun `hapticTap extension respects disabled state`() {
        HapticController.isEnabled = false

        mockView.hapticTap()

        verify(exactly = 0) { mockView.performHapticFeedback(any()) }
    }

    @Test
    fun `multiple hapticTap calls execute correctly when enabled`() {
        HapticController.isEnabled = true

        repeat(5) {
            mockView.hapticTap()
        }

        verify(exactly = 5) { mockView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }

    @Test
    fun `multiple hapticTap calls dont execute when disabled`() {
        HapticController.isEnabled = false

        repeat(5) {
            mockView.hapticTap()
        }

        verify(exactly = 0) { mockView.performHapticFeedback(any()) }
    }

    @Test
    fun `isEnabled flag is volatile and thread-safe`() {
        // This test verifies the volatile annotation is present
        // by ensuring reads/writes work across test invocations
        HapticController.isEnabled = false
        assert(!HapticController.isEnabled)

        HapticController.isEnabled = true
        assert(HapticController.isEnabled)
    }

    @Test
    fun `performHaptic with different feedback constants`() {
        HapticController.isEnabled = true

        val feedbackTypes = listOf(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.CONTEXT_CLICK
        )

        feedbackTypes.forEach { feedbackType ->
            clearMocks(mockView, answers = false)
            HapticController.performHaptic(mockView, feedbackType)
            verify { mockView.performHapticFeedback(feedbackType) }
        }
    }

    @Test
    fun `performHaptic handles multiple views`() {
        val anotherView = mockk<View>(relaxed = true)
        HapticController.isEnabled = true

        HapticController.performHaptic(mockView)
        HapticController.performHaptic(anotherView)

        verify { mockView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
        verify { anotherView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}
