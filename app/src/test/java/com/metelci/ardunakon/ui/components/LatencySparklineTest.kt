package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LatencySparklineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `sparkline renders with empty data`() {
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = emptyList(),
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }

    @Test
    fun `sparkline renders with single data point`() {
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = listOf(50L),
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }

    @Test
    fun `sparkline renders with multiple data points`() {
        val data = listOf(10L, 20L, 15L, 30L, 25L, 18L)
        
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = data,
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }

    @Test
    fun `sparkline renders with high latency values`() {
        val data = listOf(100L, 500L, 1000L, 2000L, 1500L)
        
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = data,
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }

    @Test
    fun `sparkline renders with all same values`() {
        val data = listOf(50L, 50L, 50L, 50L, 50L)
        
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = data,
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }

    @Test
    fun `sparkline renders with zero values`() {
        val data = listOf(0L, 0L, 0L)
        
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = data,
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }

    @Test
    fun `sparkline renders with max history size`() {
        // Simulate 40 data points (typical max history size)
        val data = (1..40).map { it.toLong() * 10 }
        
        composeTestRule.setContent {
            MaterialTheme {
                LatencySparkline(
                    data = data,
                    modifier = Modifier.size(100.dp, 30.dp)
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Latency sparkline graph").assertExists()
    }
}
