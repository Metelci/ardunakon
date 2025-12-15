package com.metelci.ardunakon.data

import com.metelci.ardunakon.data.OnboardingPreferences.Companion.CURRENT_VERSION
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingManagerTest {

    private lateinit var prefs: OnboardingPreferences
    private lateinit var manager: OnboardingManager

    @Before
    fun setUp() {
        prefs = mockk(relaxed = true)
        manager = OnboardingManager(prefs)
    }

    @Test
    fun shouldShowOnboarding_firstRun_returnsTrue() {
        every { prefs.isCompleted() } returns false
        every { prefs.isVersionCurrent() } returns true // Even if version is current, if not completed -> show
        assertTrue(manager.shouldShowOnboarding())
    }

    @Test
    fun shouldShowOnboarding_completedAndCurrent_returnsFalse() {
        every { prefs.isCompleted() } returns true
        every { prefs.isVersionCurrent() } returns true
        assertFalse(manager.shouldShowOnboarding())
    }

    @Test
    fun shouldShowOnboarding_completedButOldVersion_returnsTrue() {
        every { prefs.isCompleted() } returns true
        every { prefs.isVersionCurrent() } returns false
        assertTrue(manager.shouldShowOnboarding())
    }

    @Test
    fun startOnboarding_updatesPrefs() {
        manager.startOnboarding()
        io.mockk.verify { 
            prefs.setInProgress(true)
            prefs.setCurrentStep(0)
        }
    }

    @Test
    fun completeOnboarding_updatesAllPrefs() {
        manager.completeOnboarding()
        io.mockk.verify {
            prefs.setCompleted(true)
            prefs.setSkipped(false)
            prefs.setInProgress(false)
            prefs.setVersion(CURRENT_VERSION)
            prefs.setCurrentStep(0)
        }
    }

    @Test
    fun skipOnboarding_updatesAllPrefs() {
        manager.skipOnboarding()
        io.mockk.verify {
            prefs.setCompleted(true)
            prefs.setSkipped(true)
            prefs.setInProgress(false)
            prefs.setVersion(CURRENT_VERSION)
        }
    }

    @Test
    fun resetOnboarding_clearsPrefs() {
        manager.resetOnboarding()
        io.mockk.verify { prefs.clear() }
    }
}
