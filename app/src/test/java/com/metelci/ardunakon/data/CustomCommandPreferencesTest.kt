package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.model.CustomCommand
import com.metelci.ardunakon.security.CryptoEngine
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomCommandPreferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun loadCommands_when_file_missing_returns_empty_list() {
        runBlocking {
            val prefs = CustomCommandPreferences(context, PassThroughCryptoEngine)
            File(context.filesDir, "custom_commands.json").delete()

            val loaded = prefs.loadCommands()
            assertTrue(loaded.isEmpty())
        }
    }

    @Test
    fun save_then_load_roundtrips_commands() {
        runBlocking {
            val prefs = CustomCommandPreferences(context, PassThroughCryptoEngine)
            File(context.filesDir, "custom_commands.json").delete()

            val cmd = CustomCommand(
                id = "cmd-1",
                name = "Test",
                commandId = 0x20,
                payload = byteArrayOf(1, 2, 3, 4, 5),
                colorHex = 0xFF00FF00,
                isToggle = true,
                iconName = "Build"
            )

            prefs.saveCommands(listOf(cmd))
            val loaded = prefs.loadCommands()
            assertEquals(listOf(cmd), loaded)
        }
    }

    @Test
    fun loadCommands_when_payload_is_wrong_size_falls_back_to_5_bytes() {
        runBlocking {
            val prefs = CustomCommandPreferences(context, PassThroughCryptoEngine)
            File(context.filesDir, "custom_commands.json").delete()

            // Save a command with a non-5 payload through the JSON layer.
            val cmd = CustomCommand(
                id = "cmd-1",
                name = "BadPayload",
                commandId = 0x20,
                payload = byteArrayOf(1, 2, 3),
                colorHex = 0xFF2196F3,
                isToggle = false,
                iconName = "Build"
            )
            prefs.saveCommands(listOf(cmd))

            val loaded = prefs.loadCommands()
            assertEquals(1, loaded.size)
            assertEquals(5, loaded.first().payload.size)
        }
    }

    @Test
    fun addCommand_replaces_existing_by_id() {
        runBlocking {
            val prefs = CustomCommandPreferences(context, PassThroughCryptoEngine)
            File(context.filesDir, "custom_commands.json").delete()

            val first = CustomCommand(
                id = "cmd-1",
                name = "First",
                commandId = 0x20,
                payload = byteArrayOf(1, 1, 1, 1, 1)
            )
            val second = first.copy(name = "Second", payload = byteArrayOf(2, 2, 2, 2, 2))

            prefs.addCommand(first)
            prefs.addCommand(second)

            val loaded = prefs.loadCommands()
            assertEquals(1, loaded.size)
            assertEquals("Second", loaded.first().name)
            assertEquals(byteArrayOf(2, 2, 2, 2, 2).toList(), loaded.first().payload.toList())
        }
    }

    @Test
    fun updateCommand_adds_when_missing_and_updates_when_present() {
        runBlocking {
            val prefs = CustomCommandPreferences(context, PassThroughCryptoEngine)
            File(context.filesDir, "custom_commands.json").delete()

            val cmd = CustomCommand(
                id = "cmd-1",
                name = "Initial",
                commandId = 0x20,
                payload = byteArrayOf(1, 1, 1, 1, 1)
            )
            prefs.updateCommand(cmd)
            assertEquals("Initial", prefs.loadCommands().single().name)

            prefs.updateCommand(cmd.copy(name = "Updated"))
            assertEquals("Updated", prefs.loadCommands().single().name)
        }
    }

    @Test
    fun deleteCommand_removes_by_id() {
        runBlocking {
            val prefs = CustomCommandPreferences(context, PassThroughCryptoEngine)
            File(context.filesDir, "custom_commands.json").delete()

            val cmd1 = CustomCommand(id = "cmd-1", name = "One", commandId = 0x20, payload = byteArrayOf(1, 1, 1, 1, 1))
            val cmd2 = CustomCommand(id = "cmd-2", name = "Two", commandId = 0x21, payload = byteArrayOf(2, 2, 2, 2, 2))
            prefs.saveCommands(listOf(cmd1, cmd2))

            prefs.deleteCommand("cmd-1")
            val loaded = prefs.loadCommands()
            assertEquals(listOf(cmd2), loaded)
        }
    }

    private data object PassThroughCryptoEngine : CryptoEngine {
        override fun encrypt(plainText: String): String = plainText
        override fun decrypt(cipherText: String): String = cipherText
    }
}
