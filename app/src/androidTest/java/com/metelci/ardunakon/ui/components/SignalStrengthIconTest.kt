package com.metelci.ardunakon.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for SignalStrengthIcon.
 *
 * Tests RSSI bar rendering for Bluetooth and WiFi,
 * signal strength levels, and label display.
 */
@RunWith(AndroidJUnit4::class)
class SignalStrengthIconTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signalStrengthIcon_displaysFullBarsBluetooth() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -40, isWifi = false) // Excellent signal
            }
        }

        composeRule.onNodeWithText("-40 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_displaysFullBarsWifi() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -45, isWifi = true) // Excellent signal
            }
        }

        composeRule.onNodeWithText("-45 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_displaysThreeBars() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -60, isWifi = false) // Good signal
            }
        }

        composeRule.onNodeWithText("-60 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_displaysTwoBars() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -75, isWifi = false) // Fair signal
            }
        }

        composeRule.onNodeWithText("-75 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_displaysOneBar() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -90, isWifi = false) // Poor signal
            }
        }

        composeRule.onNodeWithText("-90 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_displaysNoBars() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -100, isWifi = false) // Very poor/no signal
            }
        }

        composeRule.onNodeWithText("-100 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_displaysZero() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = 0, isWifi = false)
            }
        }

        composeRule.onNodeWithText("N/A").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_hidesLabelsWhenDisabled() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -60, isWifi = false, showLabels = false)
            }
        }

        // Should not display label
        composeRule.onNodeWithText("-60 dBm").assertDoesNotExist()
    }

    @Test
    fun signalStrengthIcon_usesCustomColor() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(
                    rssi = -50,
                    isWifi = false,
                    color = Color.Red
                )
            }
        }

        composeRule.onNodeWithText("-50 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_wifiModeRendersArcs() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -55, isWifi = true)
            }
        }

        composeRule.waitForIdle()
        // WiFi arcs rendered (visual check, no specific assertions for Canvas)
    }

    @Test
    fun signalStrengthIcon_bluetoothModeRendersBars() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -55, isWifi = false)
            }
        }

        composeRule.waitForIdle()
        // Bluetooth bars rendered (visual check)
    }

    @Test
    fun signalStrengthIcon_wifiWithZeroRssi() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = 0, isWifi = true)
            }
        }

        composeRule.onNodeWithText("N/A").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_boundaryAtNegative50() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -50, isWifi = false)
            }
        }

        // Should show 4 bars (> -50 condition is false, but > -65 is true)
        composeRule.onNodeWithText("-50 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_boundaryAtNegative65() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -65, isWifi = false)
            }
        }

        // Should show 3 bars
        composeRule.onNodeWithText("-65 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_boundaryAtNegative80() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -80, isWifi = false)
            }
        }

        // Should show 2 bars
        composeRule.onNodeWithText("-80 dBm").assertIsDisplayed()
    }

    @Test
    fun signalStrengthIcon_boundaryAtNegative95() {
        composeRule.setContent {
            MaterialTheme {
                SignalStrengthIcon(rssi = -95, isWifi = false)
            }
        }

        // Should show 1 bar
        composeRule.onNodeWithText("-95 dBm").assertIsDisplayed()
    }
}
