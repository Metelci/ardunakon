package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashLogDialogComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun crashLogDialog_empty_state_clear_and_close_invoke_callbacks() {
        composeRule.setContent {
            var cleared by remember { mutableStateOf(0) }
            var dismissed by remember { mutableStateOf(0) }

            MaterialTheme {
                Column {
                    CrashLogDialog(
                        crashLog = "",
                        view = LocalView.current,
                        onShare = {},
                        onClear = { cleared++ },
                        onDismiss = { dismissed++ }
                    )
                    Text("cleared=$cleared dismissed=$dismissed")
                }
            }
        }

        composeRule.onNodeWithText("Crash Log").assertIsDisplayed()
        composeRule.onNodeWithText("No crash logs available").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Clear").performClick()
        composeRule.onNodeWithText("cleared=1 dismissed=0").assertIsDisplayed()

        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithText("cleared=1 dismissed=1").assertIsDisplayed()
    }
}
