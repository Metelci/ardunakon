package com.metelci.ardunakon.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for onboarding tutorial business logic.
 * Handles first-run detection, version tracking, and state management.
 */
@Singleton
class OnboardingManager @Inject constructor(
    private val preferences: OnboardingPreferences
) {
    /**
     * Returns true if onboarding should be shown to the user.
     * This is true when:
     * 1. Onboarding has never been completed, OR
     * 2. Onboarding version has been updated (re-show for major changes)
     */
    fun shouldShowOnboarding(): Boolean {
        return !preferences.isCompleted() || !preferences.isVersionCurrent()
    }

    /**
     * Marks onboarding as started/in-progress.
     */
    fun startOnboarding() {
        preferences.setInProgress(true)
        preferences.setCurrentStep(0)
    }

    /**
     * Marks onboarding as completed successfully.
     */
    fun completeOnboarding() {
        preferences.setCompleted(true)
        preferences.setSkipped(false)
        preferences.setInProgress(false)
        preferences.setVersion(OnboardingPreferences.CURRENT_VERSION)
        preferences.setCurrentStep(0)
    }

    /**
     * Marks onboarding as skipped by user.
     * Still counts as "completed" so it won't show again.
     */
    fun skipOnboarding() {
        preferences.setCompleted(true)
        preferences.setSkipped(true)
        preferences.setInProgress(false)
        preferences.setVersion(OnboardingPreferences.CURRENT_VERSION)
    }

    /**
     * Resets onboarding state so it shows again on next launch.
     * Used for "Take Tutorial" feature in Help menu.
     */
    fun resetOnboarding() {
        preferences.clear()
    }

    /**
     * Returns true if user previously skipped onboarding.
     */
    fun wasSkipped(): Boolean = preferences.isSkipped()

    /**
     * Returns true if onboarding is currently in progress.
     */
    fun isInProgress(): Boolean = preferences.isInProgress()

    /**
     * Gets the current step for resuming a paused tutorial.
     */
    fun getCurrentStep(): Int = preferences.getCurrentStep()

    /**
     * Updates the current step for resume capability.
     */
    fun updateCurrentStep(step: Int) {
        preferences.setCurrentStep(step)
    }
}
