package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for PacketLossHeatmap.
 *
 * Tests heatmap grid rendering, legend display,
 * and empty state handling.
 */
@RunWith(AndroidJUnit4::class)
class PacketLossHeatmapTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun packetLossHeatmap_displaysLegend() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(0.01f, 0.05f, 0.15f))
            }
        }

        composeRule.onNodeWithText("Good (0-2%)").assertIsDisplayed()
        composeRule.onNodeWithText("Moderate (2-10%)").assertIsDisplayed()
        composeRule.onNodeWithText("High (>10%)").assertIsDisplayed()
    }

    @Test
    fun packetLossHeatmap_displaysEmptyState() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = emptyList())
            }
        }

        composeRule.onNodeWithText("No packet loss data available").assertIsDisplayed()
    }

    @Test
    fun packetLossHeatmap_rendersWithGoodData() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(0.01f, 0.015f, 0.005f, 0.01f))
            }
        }

        // Should render legend
        composeRule.onNodeWithText("Good (0-2%)").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_rendersWithModerateData() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(0.03f, 0.05f, 0.07f, 0.09f))
            }
        }

        composeRule.onNodeWithText("Moderate (2-10%)").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_rendersWithHighData() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(0.15f, 0.20f, 0.25f, 0.30f))
            }
        }

        composeRule.onNodeWithText("High (>10%)").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_rendersMixedData() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(
                    data = listOf(
                        0.01f, // Good
                        0.05f, // Moderate
                        0.15f, // High
                        0.02f, // Moderate
                        0.005f // Good
                    )
                )
            }
        }

        // All legend items should be visible
        composeRule.onNodeWithText("Good (0-2%)").assertIsDisplayed()
        composeRule.onNodeWithText("Moderate (2-10%)").assertIsDisplayed()
        composeRule.onNodeWithText("High (>10%)").assertIsDisplayed()
    }

    @Test
    fun packetLossHeatmap_handlesZeroData() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(0f, 0f, 0f, 0f))
            }
        }

        composeRule.onNodeWithText("Good (0-2%)").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_handlesMaxData() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(1.0f, 1.0f, 1.0f))
            }
        }

        composeRule.onNodeWithText("High (>10%)").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_handlesLargeDataSet() {
        val data = (1..60).map { it * 0.01f }

        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = data)
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_handlesCustomTimeWindow() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(
                    data = listOf(0.05f, 0.10f, 0.15f),
                    timeWindowMs = 1800_000L // 30 minutes
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_handlesCustomThreshold() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(
                    data = listOf(0.05f, 0.10f, 0.15f),
                    packetLossThreshold = 0.75f
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun packetLossHeatmap_handlesSingleDataPoint() {
        composeRule.setContent {
            MaterialTheme {
                PacketLossHeatmap(data = listOf(0.08f))
            }
        }

        composeRule.onNodeWithText("Moderate (2-10%)").assertIsDisplayed()
    }
}
