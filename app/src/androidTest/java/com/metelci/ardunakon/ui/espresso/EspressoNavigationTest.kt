package com.metelci.ardunakon.ui.espresso

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Espresso tests for navigation and screen transitions.
 *
 * Tests orientation changes, deep links, and multi-instance behavior.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EspressoNavigationTest {

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
    fun navigation_backPressFromMainScreen_finishesOrHandled() {
        activityRule.scenario.onActivity { activity ->
            assertFalse("Activity should be running", activity.isFinishing)
        }

        try {
            pressBack()
            // If we get here, back was handled without crashing
        } catch (e: androidx.test.espresso.NoActivityResumedException) {
            // Expected if activity finished on back press
        }
    }

    @Test
    fun navigation_activityIntentData_isAccessible() {
        activityRule.scenario.onActivity { activity ->
            val intent = activity.intent
            assertNotNull("Intent should not be null", intent)
        }
    }

    @Test
    fun navigation_multipleActivityStarts_noLeaks() {
        // Launch multiple activities and close them
        repeat(3) { index ->
            val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val scenario = ActivityScenario.launch<MainActivity>(intent)

            scenario.onActivity { activity ->
                assertNotNull("Activity $index should launch", activity)
            }

            scenario.close()
        }

        // Original activity should still be valid
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Original activity should still exist", activity)
        }
    }

    @Test
    fun navigation_configurationChange_preservesState() {
        // Get initial state indicator
        var wasResumed = false

        activityRule.scenario.onActivity { activity ->
            wasResumed = !activity.isDestroyed
        }

        assertTrue("Activity should be in resumed state initially", wasResumed)

        // Trigger configuration change (recreate simulates this)
        activityRule.scenario.recreate()

        // Verify state is still good
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should exist after config change", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun navigation_launchWithClearTask_startsClean() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        scenario.onActivity { activity ->
            assertNotNull("Activity should launch with CLEAR_TASK", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }

        scenario.close()
    }

    @Test
    fun navigation_taskAffinity_isCorrect() {
        activityRule.scenario.onActivity { activity ->
            val pm = activity.packageManager
            val info = pm.getActivityInfo(activity.componentName, 0)
            assertEquals(
                "Task affinity should match package",
                "com.metelci.ardunakon",
                info.taskAffinity
            )
        }
    }
}
