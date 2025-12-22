package com.metelci.ardunakon.ui.utils

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Performs haptic feedback on this View if enabled.
 * 
 * @param isEnabled Whether haptic feedback is enabled by the user
 * @param feedbackConstant The type of haptic feedback to perform
 */
fun View.performHapticFeedbackIfEnabled(
    isEnabled: Boolean,
    feedbackConstant: Int = HapticFeedbackConstants.KEYBOARD_TAP
) {
    if (isEnabled) {
        performHapticFeedback(feedbackConstant)
    }
}
