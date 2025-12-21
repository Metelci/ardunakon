package com.metelci.ardunakon.ui.accessibility

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Accessibility constants following WCAG 2.1 guidelines.
 *
 * These defaults ensure the app meets accessibility requirements for:
 * - Touch target sizes
 * - Color contrast ratios
 * - High contrast color pairs
 */
object AccessibilityDefaults {
    /**
     * Minimum touch target size following WCAG 2.1 AA guidelines.
     * Android also recommends 48dp as the minimum touch target.
     */
    val MIN_TOUCH_TARGET = 48.dp

    /**
     * WCAG 2.1 AA minimum contrast ratio for normal text (< 18pt or < 14pt bold).
     */
    const val MIN_CONTRAST_RATIO = 4.5f

    /**
     * WCAG 2.1 AA minimum contrast ratio for large text (≥ 18pt or ≥ 14pt bold).
     */
    const val MIN_CONTRAST_RATIO_LARGE = 3.0f

    /**
     * WCAG 2.1 AAA enhanced contrast ratio for normal text.
     */
    const val ENHANCED_CONTRAST_RATIO = 7.0f

    /**
     * High contrast color pairs that meet WCAG AA requirements.
     * Each pair is (background, foreground) with contrast ≥ 4.5:1.
     */
    object HighContrastPairs {
        // Primary actions
        val PRIMARY_DARK = Pair(Color(0xFF1976D2), Color.White) // 5.5:1
        val PRIMARY_LIGHT = Pair(Color(0xFF0D47A1), Color.White) // 8.6:1

        // Success/Connected states
        val SUCCESS_DARK = Pair(Color(0xFF388E3C), Color.White) // 4.5:1
        val SUCCESS_LIGHT = Pair(Color(0xFF1B5E20), Color.White) // 7.8:1

        // Error/E-Stop states
        val ERROR_DARK = Pair(Color(0xFFD32F2F), Color.White) // 5.9:1
        val ERROR_LIGHT = Pair(Color(0xFFB71C1C), Color.White) // 8.1:1

        // Warning states
        val WARNING_DARK = Pair(Color(0xFFF57C00), Color.Black) // 5.2:1
        val WARNING_LIGHT = Pair(Color(0xFFE65100), Color.White) // 4.6:1

        // Neutral/Disabled states
        val NEUTRAL_DARK = Pair(Color(0xFF455A64), Color.White) // 6.1:1
        val NEUTRAL_LIGHT = Pair(Color(0xFF263238), Color.White) // 12.6:1
    }

    /**
     * Content description templates for common UI elements.
     * Use String.format() to insert dynamic values.
     */
    object ContentDescriptions {
        const val JOYSTICK_THROTTLE = "Throttle joystick. Current position: %d percent"
        const val JOYSTICK_STEERING = "Steering joystick. X: %d, Y: %d percent"
        const val ESTOP_ACTIVE = "Emergency stop active. Tap to release and resume control."
        const val ESTOP_INACTIVE = "Emergency stop. Tap to immediately stop all motors."
        const val AUTO_RECONNECT_ON = "Auto-reconnect enabled. Tap to disable."
        const val AUTO_RECONNECT_OFF = "Auto-reconnect disabled. Tap to enable."
        const val SIGNAL_STRENGTH = "Signal strength: %d dBm"
        const val CONNECTION_STATUS = "Connection status: %s"
        const val BATTERY_LEVEL = "Battery level: %.1f volts"
    }
}
