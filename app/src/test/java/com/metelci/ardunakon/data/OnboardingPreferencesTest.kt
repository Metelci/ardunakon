package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: OnboardingPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = OnboardingPreferences(context)
    }

    @Test
    fun setCompleted_persistsValue() {
        assertFalse(prefs.isCompleted())
        prefs.setCompleted(true)
        assertTrue(prefs.isCompleted())
    }

    @Test
    fun setSkipped_persistsValue() {
        assertFalse(prefs.isSkipped())
        prefs.setSkipped(true)
        assertTrue(prefs.isSkipped())
    }

    @Test
    fun setInProgress_persistsValue() {
        assertFalse(prefs.isInProgress())
        prefs.setInProgress(true)
        assertTrue(prefs.isInProgress())
    }

    @Test
    fun setVersion_persistsValue() {
        assertEquals(0, prefs.getVersion())
        prefs.setVersion(5)
        assertEquals(5, prefs.getVersion())
    }

    @Test
    fun setCurrentStep_persistsValue() {
        assertEquals(0, prefs.getCurrentStep())
        prefs.setCurrentStep(3)
        assertEquals(3, prefs.getCurrentStep())
    }

    @Test
    fun clear_resetsAllValues() {
        prefs.setCompleted(true)
        prefs.setSkipped(true)
        prefs.setVersion(2)
        
        prefs.clear()
        
        assertFalse(prefs.isCompleted())
        assertFalse(prefs.isSkipped())
        assertEquals(0, prefs.getVersion())
    }
}
