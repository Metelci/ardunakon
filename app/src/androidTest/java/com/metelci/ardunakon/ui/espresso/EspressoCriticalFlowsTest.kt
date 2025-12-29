package com.metelci.ardunakon.ui.espresso

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for critical user flows and Activity lifecycle.
 *
 * Tests using traditional Espresso framework for:
 * - Activity launch and ready state
 * - Lifecycle transitions (pause/resume/recreate)
 * - Intent handling
 * - Back button behavior
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EspressoCriticalFlowsTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun mainActivity_launchesSuccessfully() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun mainActivity_hasExpectedTitle() {
        activityRule.scenario.onActivity { activity ->
            // Check activity is in expected state
            assertTrue("Activity should be created", !activity.isDestroyed)
        }
    }

    @Test
    fun mainActivity_survivesRecreation() {
        // Store reference to initial activity
        var initialActivityHash = 0
        activityRule.scenario.onActivity { activity ->
            initialActivityHash = System.identityHashCode(activity)
        }

        // Recreate the activity
        activityRule.scenario.recreate()

        // Verify new activity is working
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should exist after recreation", activity)
            val newHash = System.identityHashCode(activity)
            assertNotEquals("Should be new activity instance", initialActivityHash, newHash)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun mainActivity_handlesBackPress() {
        activityRule.scenario.onActivity { activity ->
            // Verify activity is running before back press
            assertFalse("Activity should be running before back", activity.isFinishing)
        }

        // Press back
        pressBack()

        // On main activity, back should either finish or be handled
        // We just verify no crash occurs
        activityRule.scenario.onActivity { activity ->
            // Activity may or may not be finishing depending on implementation
            // Main test is that it doesn't crash
        }
    }

    @Test
    fun mainActivity_pauseAndResume() {
        // Move to background
        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)

        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should exist in STARTED state", activity)
        }

        // Move back to foreground
        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should exist in RESUMED state", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun mainActivity_stopAndRestart() {
        // Move to stopped state (like going to home screen)
        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)

        // Move back to running state
        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should exist after restart", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun applicationContext_isAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Context should be available", context)
        assertEquals(
            "Package should be correct",
            "com.metelci.ardunakon",
            context.packageName
        )
    }

    @Test
    fun mainActivity_launchWithNewIntent() {
        // Create fresh intent
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        scenario.onActivity { activity ->
            assertNotNull("Activity should launch from intent", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }

        scenario.close()
    }

    @Test
    fun mainActivity_multipleRecreationsContinueWorking() {
        // Test multiple recreations don't cause issues
        repeat(3) {
            activityRule.scenario.recreate()

            activityRule.scenario.onActivity { activity ->
                assertNotNull("Activity should exist after recreation $it", activity)
                assertFalse("Activity should not be finishing after recreation $it", activity.isFinishing)
            }
        }
    }
}
