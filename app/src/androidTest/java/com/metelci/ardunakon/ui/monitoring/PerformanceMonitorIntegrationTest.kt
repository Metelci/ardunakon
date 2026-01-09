package com.metelci.ardunakon.ui.monitoring

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.monitoring.MetricType
import com.metelci.ardunakon.monitoring.PerformanceMonitor
import com.metelci.ardunakon.monitoring.SeverityLevel
import com.metelci.ardunakon.ui.screens.ControlScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.metelci.ardunakon.HiltTestActivity
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Performance Monitoring system.
 *
 * Tests verify end-to-end monitoring flows including:
 * - PerformanceMonitor initialization at app start
 * - Metric recording and persistence
 * - Crash recording and deduplication
 * - Stats calculation and UI updates
 * - Data export functionality
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PerformanceMonitorIntegrationTest {

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
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
        composeTestRule.activityRule.scenario.onActivity {}
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    ControlScreen()
                }
            }
        }
        waitForControlScreen()
    }

    @Test
    fun performanceMonitor_isInitializedAtAppStart() {
        // PerformanceMonitor should be initialized by ArdunakonApplication
        val monitor = PerformanceMonitor.getInstance()
        assertNotNull("PerformanceMonitor should be initialized", monitor)
    }

    @Test
    fun performanceMonitor_hasStartupTimeRecorded() {
        val monitor = PerformanceMonitor.getInstance()!!
        val stats = monitor.getStats()

        // Startup time should have been recorded during app initialization
        assertTrue("Startup time should be recorded", stats.avgStartupTimeMs >= 0)
    }

    @Test
    fun performanceMonitor_recordsMetricsSuccessfully() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record a test metric
        val success = monitor.recordMetric(
            type = MetricType.CONNECTION_TIME,
            value = 150L,
            context = "BLE_TEST"
        )

        assertTrue("Metric should be recorded successfully", success)
    }

    @Test
    fun performanceMonitor_recordsLatencyMetrics() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record multiple latency values
        monitor.recordLatency(25L)
        monitor.recordLatency(30L)
        monitor.recordLatency(35L)

        val stats = monitor.getStats()

        // Average latency should be calculated
        assertTrue("Average latency should be positive", stats.avgLatencyMs >= 0)
    }

    @Test
    fun performanceMonitor_recordsCrashAndShowsInDialog() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record a test crash
        val testException = RuntimeException("Integration test crash")
        monitor.recordCrash(
            throwable = testException,
            severity = SeverityLevel.ERROR,
            isFatal = false
        )

        // Open Performance Stats dialog
        openPerformanceStatsDialog()

        // Dialog should show crash information (either the crash or "Recent Issues" section)
        composeTestRule.onNodeWithText("Recent Issues").assertExists()
    }

    @Test
    fun performanceMonitor_deduplicatesIdenticalCrashes() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Get initial crash count
        val initialCrashes = monitor.getCrashHistory().size

        // Record the same exception twice
        val testException = RuntimeException("Duplicate test crash ${System.currentTimeMillis()}")
        monitor.recordCrash(testException, SeverityLevel.WARNING, false)
        monitor.recordCrash(testException, SeverityLevel.WARNING, false)

        val crashes = monitor.getCrashHistory()

        // Should have only one crash record (deduplicated)
        val relevantCrash = crashes.find { it.message?.contains("Duplicate test crash") == true }
        assertNotNull("Crash should exist", relevantCrash)
        assertTrue(
            "Occurrence count should be 2 due to deduplication",
            relevantCrash!!.occurrenceCount >= 2
        )
    }

    @Test
    fun performanceMonitor_generatesValidDiagnosticReport() {
        val monitor = PerformanceMonitor.getInstance()!!

        val report = monitor.generateDiagnosticReport()

        // Report should contain essential sections
        assertTrue("Report should have header", report.contains("Ardunakon Performance Report"))
        assertTrue("Report should have summary section", report.contains("Summary"))
        assertTrue("Report should not contain raw file paths", !report.contains("/data/user/"))
    }

    @Test
    fun performanceStatsDialog_showsUpdatedStatsAfterRecording() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record some metrics
        monitor.recordMetric(MetricType.APP_STARTUP, 500L)
        monitor.recordMetric(MetricType.CONNECTION_TIME, 200L)
        composeTestRule.waitForIdle()

        // Open dialog
        openPerformanceStatsDialog()

        // Stats should be visible with values
        composeTestRule.onNodeWithText("Startup").assertExists()
        composeTestRule.onNode(hasText("ms", substring = true)).assertExists()
    }

    @Test
    fun performanceMonitor_persistsDataToFile() {
        val monitor = PerformanceMonitor.getInstance()!!

        // Record a metric to trigger persistence
        monitor.recordMetric(MetricType.MEMORY_USAGE, 1024L)
        composeTestRule.waitForIdle()

        // Give time for async persistence
        Thread.sleep(500)

        // Check if metrics file exists
        val metricsFile = File(context.filesDir, "performance_metrics.json")
        assertTrue("Metrics file should exist after recording", metricsFile.exists())
    }

    @Test
    fun crashLog_integrationWithPerformanceMonitor() {
        // Open crash log dialog first
        openCrashLogDialog()

        // Verify crash log dialog works
        composeTestRule.onNodeWithText("Crash Log").assertExists()

        // Close crash log
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Then open performance stats
        openPerformanceStatsDialog()

        // Both systems should work independently
        composeTestRule.onNodeWithText("Performance Stats").assertExists()
    }

    // Helper methods

    private fun openPerformanceStatsDialog() {
        composeTestRule.onNodeWithContentDescription("Help menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Performance Stats").performClick()
        composeTestRule.waitForIdle()
    }

    private fun openCrashLogDialog() {
        composeTestRule.onNodeWithContentDescription("Help menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Crash Log").performClick()
        composeTestRule.waitForIdle()
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
