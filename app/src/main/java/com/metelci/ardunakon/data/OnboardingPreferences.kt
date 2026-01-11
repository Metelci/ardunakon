package com.metelci.ardunakon.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences for onboarding tutorial state.
 * Uses SharedPreferences (no encryption needed - just boolean flags and version).
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_VERSION = "version"
        private const val KEY_SKIPPED = "skipped"
        private const val KEY_IN_PROGRESS = "in_progress"
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_BLUETOOTH_TOOLTIP_SHOWN = "bt_tooltip_shown"
        private const val KEY_PERMISSION_WARNING_DISMISSED = "permission_warning_dismissed"

        /** Current onboarding version - increment when tutorial content changes significantly */
        const val CURRENT_VERSION = 1
    }

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun setCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    }

    fun isSkipped(): Boolean = prefs.getBoolean(KEY_SKIPPED, false)

    fun setSkipped(skipped: Boolean) {
        prefs.edit().putBoolean(KEY_SKIPPED, skipped).apply()
    }

    fun isInProgress(): Boolean = prefs.getBoolean(KEY_IN_PROGRESS, false)

    fun setInProgress(inProgress: Boolean) {
        prefs.edit().putBoolean(KEY_IN_PROGRESS, inProgress).apply()
    }

    fun getVersion(): Int = prefs.getInt(KEY_VERSION, 0)

    fun setVersion(version: Int) {
        prefs.edit().putInt(KEY_VERSION, version).apply()
    }

    fun isVersionCurrent(): Boolean = getVersion() == CURRENT_VERSION

    fun getCurrentStep(): Int = prefs.getInt(KEY_CURRENT_STEP, 0)

    fun setCurrentStep(step: Int) {
        prefs.edit().putInt(KEY_CURRENT_STEP, step).apply()
    }

    fun isBluetoothTooltipShown(): Boolean = prefs.getBoolean(KEY_BLUETOOTH_TOOLTIP_SHOWN, false)

    fun setBluetoothTooltipShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_BLUETOOTH_TOOLTIP_SHOWN, shown).apply()
    }

    fun isPermissionWarningDismissed(): Boolean = prefs.getBoolean(KEY_PERMISSION_WARNING_DISMISSED, false)

    fun setPermissionWarningDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSION_WARNING_DISMISSED, dismissed).apply()
    }

    /**
     * Clears all onboarding preferences (for reset functionality).
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
