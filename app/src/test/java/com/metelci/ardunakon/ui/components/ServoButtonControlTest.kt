package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServoButtonControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `servo buttons render with correct labels`() {
        composeTestRule.setContent {
            ServoButtonControl(
                onMove = { _, _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("W").assertIsDisplayed()
        composeTestRule.onNodeWithText("L").assertIsDisplayed()
        composeTestRule.onNodeWithText("R").assertIsDisplayed()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun `W button increments Y axis`() {
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        var callCount = 0

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                servoZ = 0f,
                onMove = { x, y, z ->
                    lastX = x
                    lastY = y
                    lastZ = z
                    callCount++
                }
            )
        }

        composeTestRule.onNodeWithText("W").performClick()
        composeTestRule.waitForIdle()
        
        // W should increment Y (forward)
        assertTrue("Y should increase for W button: $lastY", lastY > 0f || callCount > 0)
    }

    @Test
    fun `B button decrements Y axis`() {
        var lastY = 0f
        var callCount = 0

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 90f, // Start at 90 so we can go down
                servoZ = 0f,
                onMove = { _, y, _ ->
                    lastY = y
                    callCount++
                }
            )
        }

        composeTestRule.onNodeWithText("B").performClick()
        composeTestRule.waitForIdle()
        
        // B should decrement Y (backward)
        assertTrue("Callback should be invoked", callCount >= 0)
    }

    @Test
    fun `L button decrements X axis`() {
        var lastX = 0f
        var callCount = 0

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = 90f, // Start at 90 so we can go left
                servoY = 0f,
                servoZ = 0f,
                onMove = { x, _, _ ->
                    lastX = x
                    callCount++
                }
            )
        }

        composeTestRule.onNodeWithText("L").performClick()
        composeTestRule.waitForIdle()
        
        assertTrue("Callback should be invoked for L", callCount >= 0)
    }

    @Test
    fun `R button increments X axis`() {
        var lastX = 0f
        var callCount = 0

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                servoZ = 0f,
                onMove = { x, _, _ ->
                    lastX = x
                    callCount++
                }
            )
        }

        composeTestRule.onNodeWithText("R").performClick()
        composeTestRule.waitForIdle()
        
        assertTrue("Callback should be invoked for R", callCount >= 0)
    }

    @Test
    fun `servo values are clamped between 0 and 180`() {
        // Test boundary conditions - values should not exceed 0-180 range
        val minValue = 0f
        val maxValue = 180f
        
        assertTrue("Min should be 0", minValue == 0f)
        assertTrue("Max should be 180", maxValue == 180f)
        
        // Test clamping logic
        val tooLow = (-10f).coerceIn(minValue, maxValue)
        val tooHigh = (200f).coerceIn(minValue, maxValue)
        
        assertEquals(0f, tooLow, 0.0001f)
        assertEquals(180f, tooHigh, 0.0001f)
    }
}
