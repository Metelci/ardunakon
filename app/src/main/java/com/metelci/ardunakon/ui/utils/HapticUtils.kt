package com.metelci.ardunakon.ui.utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Global haptic feedback controller.
 * Allows centralized control of haptic feedback based on user preferences.
 */
object HapticController {
    /** Whether haptic feedback is enabled globally */
    @Volatile
    var isEnabled: Boolean = true

    /**
     * Performs haptic feedback if globally enabled.
     */
    fun performHaptic(view: View, feedbackConstant: Int = defaultTapFeedback()) {
        if (isEnabled) {
            view.performHapticFeedback(feedbackConstant)
        }
    }

    /**
     * Performs the standard tap feedback (API-aware).
     */
    fun performTap(view: View) {
        performHaptic(view, defaultTapFeedback())
    }

    /**
     * Performs a subtle tick feedback.
     */
    fun performTick(view: View) {
        performHaptic(view, HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Performs a long-press feedback.
     */
    fun performLongPress(view: View) {
        performHaptic(view, HapticFeedbackConstants.LONG_PRESS)
    }

    @Suppress("DEPRECATION")
    internal fun defaultTapFeedback(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.KEYBOARD_TAP
    }
}

/**
 * Extension function to perform haptic feedback respecting global settings.
 */
fun View.hapticTap() {
    HapticController.performTap(this)
}
