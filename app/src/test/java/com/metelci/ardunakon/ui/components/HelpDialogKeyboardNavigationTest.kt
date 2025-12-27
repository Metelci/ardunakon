package com.metelci.ardunakon.ui.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HelpDialogKeyboardNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun helpDialog_viewFullGuide_opens_with_enter_key() {
        composeTestRule.setContent {
            HelpDialog(onDismiss = {})
        }

        val viewButton = composeTestRule.onNodeWithText("View Full Guide")
        viewButton.performSemanticsAction(SemanticsActions.RequestFocus)
        viewButton.assertIsFocused()
        viewButton.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.onNodeWithText("Setup Guide").assertExists()
    }
}
