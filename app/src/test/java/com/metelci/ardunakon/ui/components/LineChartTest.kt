package com.metelci.ardunakon.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.metelci.ardunakon.telemetry.TelemetryDataPoint
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LineChartTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun lineChart_renders_axis_labels_and_legend_for_empty_series() {
        val series = listOf(
            LineChartSeries(
                label = "Battery",
                data = emptyList(),
                color = Color.Cyan
            )
        )

        composeTestRule.setContent {
            LineChart(
                series = series,
                yAxisLabel = "Voltage",
                xAxisLabel = "Time"
            )
        }

        composeTestRule.onNodeWithText("Voltage").assertExists()
        composeTestRule.onNodeWithText("Time").assertExists()
        composeTestRule.onNodeWithText("Battery (0 pts)").assertExists()
    }

    @Test
    fun lineChart_renders_legend_with_point_count() {
        val series = listOf(
            LineChartSeries(
                label = "RTT",
                data = listOf(
                    TelemetryDataPoint(timestamp = 1000L, value = 42f),
                    TelemetryDataPoint(timestamp = 2000L, value = 65f)
                ),
                color = Color.Magenta
            )
        )

        composeTestRule.setContent {
            LineChart(
                series = series,
                yAxisLabel = "Latency (ms)",
                xAxisLabel = "Time"
            )
        }

        composeTestRule.onNodeWithText("Latency (ms)").assertExists()
        composeTestRule.onNodeWithText("RTT (2 pts)").assertExists()
    }
}
