package com.metelci.ardunakon.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences for haptic feedback settings.
 * Uses SharedPreferences (no encryption needed - just boolean flag).
 */
@Singleton
class HapticPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "haptic_prefs"
        private const val KEY_ENABLED = "haptic_enabled"
    }

    /**
     * Returns whether haptic feedback is enabled.
     * Default is true (enabled).
     */
    fun isHapticEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    /**
     * Sets whether haptic feedback is enabled.
     */
    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
