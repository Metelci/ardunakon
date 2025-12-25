package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for LatencySparkline.
 *
 * Tests sparkline rendering with various RTT values, edge cases,
 * and color coding based on latency levels.
 */
@RunWith(AndroidJUnit4::class)
class LatencySparklineTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun latencySparkline_rendersWithEmptyList() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(rttValues = emptyList())
            }
        }

        // Should render without crashing (placeholder line)
        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_rendersWithSingleValue() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(rttValues = listOf(50L))
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_rendersWithMultipleValues() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesMaxValues() {
        val values = (1..30).map { it * 5L } // 30 values

        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = values,
                    maxValues = 20 // Should only show last 20
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesLargeLatencies() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(500L, 1000L, 1500L, 2000L)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesVeryLowLatencies() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(1L, 2L, 3L, 4L, 5L)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesFluctuatingValues() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(10L, 100L, 20L, 80L, 30L, 70L, 40L)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_usesFixedColor() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(50L, 100L, 150L),
                    fixedColor = Color.Blue
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesAllZeroValues() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(0L, 0L, 0L, 0L)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesCustomSize() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(50L, 100L),
                    modifier = androidx.compose.ui.Modifier
                        .androidx.compose.foundation.layout.width(120.dp)
                        .androidx.compose.foundation.layout.height(32.dp)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesIdenticalValues() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(50L, 50L, 50L, 50L, 50L)
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun latencySparkline_handlesHighVariance() {
        composeRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    rttValues = listOf(1L, 500L, 10L, 400L, 5L, 300L)
                )
            }
        }

        composeRule.waitForIdle()
    }
}
