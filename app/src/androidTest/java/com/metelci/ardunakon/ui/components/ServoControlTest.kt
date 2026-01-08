package com.metelci.ardunakon.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ServoControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testZAxisButtons() {
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

        // Test "A" button (Z min)
        composeTestRule.onNodeWithText("A").performClick()
        assertEquals("A button should set Z to -1", -1f, lastZ, 0.01f)

        // Test "Z" button (Z max)
        composeTestRule.onNodeWithText("Z").performClick()
        assertEquals("Z button should set Z to 1", 1f, lastZ, 0.01f)
    }

    @Test
    fun testYAxisButtons() {
        var lastY = 0f

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                servoZ = 0f,
                onMove = { _, y, _ ->
                    lastY = y
                }
            )
        }

        // Test "W" button (Forward/Up - increment Y)
        composeTestRule.onNodeWithText("W").performClick()
        // Default step is 0.1f. 0f + 0.1f = 0.1f
        assertEquals("W button should increment Y", 0.1f, lastY, 0.01f)

        // Reset and test "B" button (Backward/Down - decrement Y)
        // Note: content is not reset, so internal state might be at 0 if we re-set content or just check delta
        // To be safe, let's verify relative change or start from known state.
        // Actually, onMove callback receives the NEW calculated value based on PASSED servoY + step.
        // Since we pass constant 0f for servoY in setContent, every click calculates 0f + step or 0f - step.

        composeTestRule.onNodeWithText("B").performClick()
        assertEquals("B button should decrement Y", -0.1f, lastY, 0.01f)
    }

    @Test
    fun testXAxisButtons() {
        var lastX = 0f

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = 0f,
                servoY = 0f,
                servoZ = 0f,
                onMove = { x, _, _ ->
                    lastX = x
                }
            )
        }

        // Test "R" button (Right - increment X)
        composeTestRule.onNodeWithText("R").performClick()
        assertEquals("R button should increment X", 0.1f, lastX, 0.01f)

        // Test "L" button (Left - decrement X)
        composeTestRule.onNodeWithText("L").performClick()
        assertEquals("L button should decrement X", -0.1f, lastX, 0.01f)
    }

    @Test
    fun testStatePreservation() {
        // Evaluate if moving one axis preserves the other's state
        var capturedX = 0f
        var capturedY = 0f
        var capturedZ = 0f

        // Initial state: X=0.5, Z=1.0
        val initialX = 0.5f
        val initialZ = 1.0f

        composeTestRule.setContent {
            ServoButtonControl(
                servoX = initialX,
                servoY = 0f,
                servoZ = initialZ,
                onMove = { x, y, z ->
                    capturedX = x
                    capturedY = y
                    capturedZ = z
                }
            )
        }

        // Click "W" (modifies Y)
        composeTestRule.onNodeWithText("W").performClick()

        // Check if X and Z are preserved
        assertEquals("X should be preserved", initialX, capturedX, 0.01f)
        assertEquals("Z should be preserved", initialZ, capturedZ, 0.01f)
        assertEquals("Y should change", 0.1f, capturedY, 0.01f)
    }
}
