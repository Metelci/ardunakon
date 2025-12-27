package com.metelci.ardunakon.ui.components

import android.view.View
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDialogKeyboardNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsDialog_close_responds_to_enter_key() {
        var dismissed = false
        val view = View(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            SettingsDialog(
                onDismiss = { dismissed = true },
                view = view,
                isDebugPanelVisible = true,
                onToggleDebugPanel = {},
                isHapticEnabled = true,
                onToggleHaptic = {},
                joystickSensitivity = 1.0f,
                onJoystickSensitivityChange = {},
                allowReflection = false,
                onToggleReflection = {},
                onResetTutorial = {}
            )
        }

        val closeButton = composeTestRule.onNodeWithContentDescription("Close")
        closeButton.performSemanticsAction(SemanticsActions.RequestFocus)
        closeButton.assertIsFocused()
        closeButton.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.runOnIdle {
            assertTrue(dismissed)
        }
    }
}
