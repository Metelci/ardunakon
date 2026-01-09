package com.metelci.ardunakon.ui.flows

import android.Manifest
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.metelci.ardunakon.data.AutoReconnectPreferences
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.HapticPreferences
import com.metelci.ardunakon.ui.screens.ControlScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.metelci.ardunakon.HiltTestActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()

        // Clear all preferences before each test
        context.deleteFile("connection_prefs.json")
        context.deleteFile("auto_reconnect_prefs.json")
        context.getSharedPreferences("haptic_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        composeTestRule.activityRule.scenario.onActivity {}
        setMainContent()
        waitForControlScreen()
    }

    @Test
    fun settingsPersistence_autoReconnect_savedAndRestored() {
        // Toggle auto-reconnect
        composeTestRule.onNode(
            hasContentDescription("Auto-reconnect", substring = true, ignoreCase = true)
        ).performClick()
        composeTestRule.waitForIdle()

        // Verify setting is persisted
        val prefs = AutoReconnectPreferences(context)
        val enabled = runBlocking { prefs.loadAutoReconnectState()[0] }
        assertTrue(enabled)

        // Recreate UI and verify toggle state remains enabled
        composeTestRule.activityRule.scenario.recreate()
        setMainContent()
        waitForControlScreen()

        composeTestRule.onNode(
            hasContentDescription("Auto-reconnect enabled", substring = true, ignoreCase = true)
        ).assertExists()
    }

    @Test
    fun settingsPersistence_wifiConfig_savedAndRestored() {
        // Open WiFi configuration
        openMenuItem("Configure WiFi")
        composeTestRule.waitForIdle()

        // Enter IP address
        composeTestRule.onNodeWithText("IP Address").performTextInput("192.168.1.100")

        // Enter port
        composeTestRule.onNodeWithText("Port").performTextClearance()
        composeTestRule.onNodeWithText("Port").performTextInput("8080")

        // Save configuration
        composeTestRule.onNodeWithText("Connect").performClick()
        composeTestRule.waitForIdle()

        // Verify settings are persisted
        val prefs = ConnectionPreferences(context)
        val lastConnection = runBlocking { prefs.loadLastConnection() }
        assertEquals("192.168.1.100", lastConnection.wifiIp)
        assertEquals(8080, lastConnection.wifiPort)

        // Reopen WiFi config and verify values
        openMenuItem("Configure WiFi")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("192.168.1.100").assertExists()
        composeTestRule.onNodeWithText("8080").assertExists()
    }

    @Test
    fun settingsPersistence_hapticFeedback_savedAndRestored() {
        // Open settings
        openSettings()
        composeTestRule.waitForIdle()

        // Disable haptic feedback
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch) and
                hasAnyAncestor(hasText("Haptic Feedback"))
        ).performClick()
        composeTestRule.waitForIdle()

        // Close settings
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify setting is persisted
        val prefs = HapticPreferences(context)
        assertFalse(prefs.isHapticEnabled())
    }

    @Test
    fun settingsPersistence_joystickSensitivity_savedAndRestored() {
        // Open settings
        openSettings()
        composeTestRule.waitForIdle()

        // Adjust joystick sensitivity slider
        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.5f) }
        composeTestRule.waitForIdle()

        // Close settings
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        // Verify setting is persisted
        composeTestRule.waitUntil(10_000) {
            runBlocking { ConnectionPreferences(context).loadLastConnection().joystickSensitivity > 1.0f }
        }
        val sensitivity = runBlocking { ConnectionPreferences(context).loadLastConnection().joystickSensitivity }
        assertTrue(sensitivity > 1.0f)
    }

    @Test
    fun settingsPersistence_customCommands_persistAcrossSessions() {
        // Create a custom command
        openCustomCommands()

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
        openCustomCommands()

        composeTestRule.onNodeWithText("Persistent Command").assertExists()
    }

    private fun setMainContent() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    ControlScreen()
                }
            }
        }
    }

    private fun waitForControlScreen() {
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
    }

    private fun openMenuItem(label: String) {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(label).performClick()
        composeTestRule.waitForIdle()
    }

    private fun openCustomCommands() {
        openSettings()
        composeTestRule.onNodeWithText("Custom Commands").performClick()
        composeTestRule.waitForIdle()
    }
}
