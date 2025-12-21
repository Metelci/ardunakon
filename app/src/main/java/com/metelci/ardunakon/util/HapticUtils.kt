package com.metelci.ardunakon.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility object for haptic feedback that handles API deprecation gracefully.
 * 
 * KEYBOARD_TAP was deprecated in API 33 (Android 13) in favor of CONFIRM.
 * This utility provides a single place to handle the API differences.
 */
object HapticUtils {
    
    /**
     * Perform a click-style haptic feedback appropriate for button taps.
     * Uses CONFIRM on API 33+ and KEYBOARD_TAP on older versions.
     */
    fun performClick(view: View) {
        val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            HapticFeedbackConstants.CONFIRM
        } else {
            @Suppress("DEPRECATION")
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(feedbackConstant)
    }
    
    /**
     * Perform a tick-style haptic feedback for subtle interactions.
     * Uses CLOCK_TICK on all versions (still available).
     */
    fun performTick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    
    /**
     * Perform a long press haptic feedback.
     */
    fun performLongPress(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
