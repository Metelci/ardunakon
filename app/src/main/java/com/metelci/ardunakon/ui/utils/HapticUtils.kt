package com.metelci.ardunakon.ui.utils

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
    fun performHaptic(view: View, feedbackConstant: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        if (isEnabled) {
            view.performHapticFeedback(feedbackConstant)
        }
    }
}

/**
 * Extension function to perform haptic feedback respecting global settings.
 */
fun View.hapticTap() {
    HapticController.performHaptic(this, HapticFeedbackConstants.KEYBOARD_TAP)
}
