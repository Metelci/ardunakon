package com.metelci.ardunakon.ui.screens.control

import androidx.compose.ui.test.onNodeWithContentDescription
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JoystickPanelTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun joystickPanel_exposes_steering_description() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            JoystickPanel(onMoved = { _, _ -> })
        }

        composeTestRule.onNodeWithContentDescription("Steering joystick").assertExists()
    }

    @Test
    fun joystickPanel_exposes_throttle_description() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            JoystickPanel(onMoved = { _, _ -> }, isThrottle = true)
        }

        composeTestRule.onNodeWithContentDescription("Throttle joystick").assertExists()
    }
}
