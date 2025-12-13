package com.metelci.ardunakon.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.ServoButtonControl
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun joystick_exposes_content_and_state_description() {
        composeRule.setContent {
            JoystickControl(
                isThrottle = true,
                onMoved = {}
            )
        }

        composeRule.onNode(
            hasContentDescription("Throttle joystick") and
                hasStateDescription("X 0 percent, Y 0 percent")
        ).assertExists()
    }

    @Test
    fun servo_buttons_expose_clear_content_descriptions() {
        composeRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                onMove = { _, _ -> }
            )
        }

        composeRule.onNodeWithContentDescription("Move servo forward").assertExists()
        composeRule.onNodeWithContentDescription("Move servo backward").assertExists()
        composeRule.onNodeWithContentDescription("Move servo left").assertExists()
        composeRule.onNodeWithContentDescription("Move servo right").assertExists()
    }
}
