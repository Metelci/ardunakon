package com.metelci.ardunakon.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.metelci.ardunakon.MainActivity
import com.metelci.ardunakon.ui.components.JoystickControl
import com.metelci.ardunakon.ui.components.ServoButtonControl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AccessibilitySemanticsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

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
                servoZ = 0f,
                onMove = { _, _, _ -> }
            )
        }

        composeRule.onNodeWithContentDescription("Move servo forward").assertExists()
        composeRule.onNodeWithContentDescription("Move servo backward").assertExists()
        composeRule.onNodeWithContentDescription("Move servo left").assertExists()
        composeRule.onNodeWithContentDescription("Move servo right").assertExists()
    }
}
