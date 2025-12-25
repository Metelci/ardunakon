package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for LineChart.
 *
 * Tests chart rendering with various data sets,
 * empty states, axis labels, and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class LineChartTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lineChart_rendersWithEmptyData() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = emptyList(),
                    label = "Empty Chart"
                )
            }
        }

        composeRule.waitForIdle()
        // Should render without crashing
    }

    @Test
    fun lineChart_rendersWithSingleDataPoint() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(50f),
                    label = "Single Point"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_rendersWithMultipleDataPoints() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(10f, 20f, 30f, 40f, 50f),
                    label = "Multi Points"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesZeroValues() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(0f, 0f, 0f),
                    label = "Zeros"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesNegativeValues() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(-10f, -20f, 5f, 15f),
                    label = "Negative Values"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesLargeValues() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(1000f, 2000f, 3000f),
                    label = "Large Values"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesMixedRange() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(0f, 100f, 50f, 200f, 25f),
                    label = "Mixed Range"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesConstantValues() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(42f, 42f, 42f, 42f),
                    label = "Constant"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesLargeDataset() {
        val largeData = (1..100).map { it.toFloat() }
        
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = largeData,
                    label = "Large Dataset"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesSpikyData() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(10f, 100f, 10f, 100f, 10f),
                    label = "Spiky"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesDecreasingTrend() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(100f, 80f, 60f, 40f, 20f),
                    label = "Decreasing"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesIncreasingTrend() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(20f, 40f, 60f, 80f, 100f),
                    label = "Increasing"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesFloatingPointPrecision() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(1.23f, 4.56f, 7.89f),
                    label = "Floating Point"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_handlesExtremeRange() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(0.01f, 10000f),
                    label = "Extreme Range"
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                LineChart(
                    data = listOf(1f, 2f, 3f, 4f, 5f),
                    label = "Standard Chart"
                )
            }
        }

        composeRule.waitForIdle()
    }
}
