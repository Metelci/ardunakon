package com.metelci.ardunakon.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignalStrengthIconTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `signal strength icon renders for excellent signal`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -40)
            }
        }

        composeTestRule.onNodeWithContentDescription("Signal strength: 4 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength icon renders for good signal`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -55)
            }
        }

        composeTestRule.onNodeWithContentDescription("Signal strength: 3 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength icon renders for fair signal`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -70)
            }
        }

        composeTestRule.onNodeWithContentDescription("Signal strength: 2 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength icon renders for weak signal`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -90)
            }
        }

        composeTestRule.onNodeWithContentDescription("Signal strength: 1 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength icon renders for no signal`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -100)
            }
        }

        composeTestRule.onNodeWithContentDescription("Signal strength: 0 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength icon renders for zero rssi as no signal`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = 0)
            }
        }

        composeTestRule.onNodeWithContentDescription("Signal strength: 0 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength boundary at -50 gives 3 bars`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -50)
            }
        }

        // -50 is not > -50, so it should be 3 bars
        composeTestRule.onNodeWithContentDescription("Signal strength: 3 of 4 bars").assertExists()
    }

    @Test
    fun `signal strength boundary at -49 gives 4 bars`() {
        composeTestRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -49)
            }
        }

        // -49 > -50, so it should be 4 bars
        composeTestRule.onNodeWithContentDescription("Signal strength: 4 of 4 bars").assertExists()
    }
}
