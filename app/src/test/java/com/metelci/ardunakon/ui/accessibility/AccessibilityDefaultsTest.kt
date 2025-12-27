package com.metelci.ardunakon.ui.accessibility

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityDefaultsTest {

    @Test
    fun minTouchTarget_matchesAndroidRecommendation() {
        assertEquals(48.dp, AccessibilityDefaults.MIN_TOUCH_TARGET)
    }

    @Test
    fun contrastRatios_matchWcagGuidelines() {
        assertEquals(4.5f, AccessibilityDefaults.MIN_CONTRAST_RATIO, 0.0001f)
        assertEquals(3.0f, AccessibilityDefaults.MIN_CONTRAST_RATIO_LARGE, 0.0001f)
        assertEquals(7.0f, AccessibilityDefaults.ENHANCED_CONTRAST_RATIO, 0.0001f)
    }

    @Test
    fun highContrastPairs_defineBackgroundAndForegroundColors() {
        val pairs = listOf(
            AccessibilityDefaults.HighContrastPairs.PRIMARY_DARK,
            AccessibilityDefaults.HighContrastPairs.PRIMARY_LIGHT,
            AccessibilityDefaults.HighContrastPairs.SUCCESS_DARK,
            AccessibilityDefaults.HighContrastPairs.SUCCESS_LIGHT,
            AccessibilityDefaults.HighContrastPairs.ERROR_DARK,
            AccessibilityDefaults.HighContrastPairs.ERROR_LIGHT,
            AccessibilityDefaults.HighContrastPairs.WARNING_DARK,
            AccessibilityDefaults.HighContrastPairs.WARNING_LIGHT,
            AccessibilityDefaults.HighContrastPairs.NEUTRAL_DARK,
            AccessibilityDefaults.HighContrastPairs.NEUTRAL_LIGHT
        )

        pairs.forEach { pair ->
            assertNotNull(pair.first)
            assertNotNull(pair.second)
        }
    }

    @Test
    fun contentDescriptions_are_formatable() {
        val throttle = AccessibilityDefaults.ContentDescriptions.JOYSTICK_THROTTLE.format(75)
        assertTrue(throttle.contains("75"))

        val steering = AccessibilityDefaults.ContentDescriptions.JOYSTICK_STEERING.format(25, -40)
        assertTrue(steering.contains("25"))
        assertTrue(steering.contains("-40"))

        val status = AccessibilityDefaults.ContentDescriptions.CONNECTION_STATUS.format("Connected")
        assertTrue(status.contains("Connected"))

        val battery = AccessibilityDefaults.ContentDescriptions.BATTERY_LEVEL.format(12.3f)
        assertTrue(battery.contains("12.3"))
    }
}
