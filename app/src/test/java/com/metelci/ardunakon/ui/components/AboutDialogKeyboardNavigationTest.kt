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
class AboutDialogKeyboardNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutDialog_links_open_with_enter_key() {
        composeTestRule.setContent {
            AboutDialog(onDismiss = {})
        }

        val githubButton = composeTestRule.onNodeWithText("View on GitHub")
        githubButton.performSemanticsAction(SemanticsActions.RequestFocus)
        githubButton.assertIsFocused()
        githubButton.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.onNodeWithText("About Ardunakon").assertExists()
    }
}
