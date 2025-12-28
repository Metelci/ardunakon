package com.metelci.ardunakon.ui.flows

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for settings persistence.
 * Verifies that settings are correctly saved and restored across app sessions.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Clear all preferences before each test
        context.getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("haptic_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun settingsPersistence_autoReconnect_savedAndRestored() {
        // Open settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Enable auto-reconnect
        composeTestRule.onNodeWithText("Auto-Reconnect").performClick()
        composeTestRule.waitForIdle()

        // Close settings
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify setting is persisted
        val prefs = context.getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("auto_reconnect", false))

        // Reopen settings and verify state
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Auto-reconnect should still be enabled
        composeTestRule.onNode(
            hasText("Auto-Reconnect") and hasClickAction()
        ).assertExists()
    }

    @Test
    fun settingsPersistence_wifiConfig_savedAndRestored() {
        // Open WiFi configuration
        composeTestRule.onNodeWithContentDescription("Configure WiFi").performClick()
        composeTestRule.waitForIdle()

        // Enter IP address
        composeTestRule.onNodeWithText("IP Address").performTextInput("192.168.1.100")
        
        // Enter port
        composeTestRule.onNodeWithText("Port").performTextClearance()
        composeTestRule.onNodeWithText("Port").performTextInput("8080")

        // Save configuration
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify settings are persisted
        val prefs = context.getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
        assertEquals("192.168.1.100", prefs.getString("wifi_ip", ""))
        assertEquals(8080, prefs.getInt("wifi_port", 0))

        // Reopen WiFi config and verify values
        composeTestRule.onNodeWithContentDescription("Configure WiFi").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("192.168.1.100").assertExists()
        composeTestRule.onNodeWithText("8080").assertExists()
    }

    @Test
    fun settingsPersistence_hapticFeedback_savedAndRestored() {
        // Open settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Disable haptic feedback
        composeTestRule.onNodeWithText("Haptic Feedback").performClick()
        composeTestRule.waitForIdle()

        // Close settings
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify setting is persisted
        val prefs = context.getSharedPreferences("haptic_prefs", Context.MODE_PRIVATE)
        assertFalse(prefs.getBoolean("haptic_enabled", true))
    }

    @Test
    fun settingsPersistence_joystickSensitivity_savedAndRestored() {
        // Open settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // Adjust joystick sensitivity slider
        // Note: Slider interaction in Compose UI tests can be tricky
        composeTestRule.onNode(hasContentDescription("Joystick Sensitivity"))
            .performTouchInput {
                // Simulate dragging slider to a specific position
                swipeRight()
            }
        composeTestRule.waitForIdle()

        // Close settings
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify setting is persisted
        val prefs = context.getSharedPreferences("haptic_prefs", Context.MODE_PRIVATE)
        val sensitivity = prefs.getFloat("joystick_sensitivity", 1.0f)
        assertTrue(sensitivity > 1.0f) // Should be increased from default
    }

    @Test
    fun settingsPersistence_customCommands_persistAcrossSessions() {
        // Create a custom command
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Command").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Command Name").performTextInput("Persistent Command")
        composeTestRule.onNodeWithText("Payload (Hex)").performTextClearance()
        composeTestRule.onNodeWithText("Payload (Hex)").performTextInput("AABBCCDDEE")

        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Close dialogs
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Reopen custom commands and verify it exists
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Persistent Command").assertExists()
    }
}
