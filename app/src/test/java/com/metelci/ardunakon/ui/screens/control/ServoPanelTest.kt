package com.metelci.ardunakon.ui.screens.control

import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.metelci.ardunakon.ui.testutils.NoOpIndication
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServoPanelTest {

    @get:Rule
    val composeTestRule = createRegisteredComposeRule()

    @Test
    fun servoPanel_moves_forward() {
        var lastMove = Triple(0f, 0f, 0f)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIndication provides NoOpIndication) {
                ServoPanel(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onServoMove = { x, y, z -> lastMove = Triple(x, y, z) }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Move servo forward").performClick()

        composeTestRule.runOnIdle {
            assertEquals(0f, lastMove.first, 0.0001f)
            assertEquals(0.1f, lastMove.second, 0.0001f)
            assertEquals(0f, lastMove.third, 0.0001f)
        }
    }

    @Test
    fun servoPanel_moves_left() {
        var lastMove = Triple(0f, 0f, 0f)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIndication provides NoOpIndication) {
                ServoPanel(
                    servoX = 0f,
                    servoY = 0f,
                    servoZ = 0f,
                    onServoMove = { x, y, z -> lastMove = Triple(x, y, z) }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Move servo left").performClick()

        composeTestRule.runOnIdle {
            assertEquals(-0.1f, lastMove.first, 0.0001f)
            assertEquals(0f, lastMove.second, 0.0001f)
            assertEquals(0f, lastMove.third, 0.0001f)
        }
    }
}
