package com.metelci.ardunakon.protocol

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.data.CustomCommandPreferences
import com.metelci.ardunakon.model.CustomCommand
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomCommandRegistryTest {

    private lateinit var context: Context
    private lateinit var preferences: CustomCommandPreferences
    private lateinit var registry: CustomCommandRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "custom_commands.json").delete()
        preferences = CustomCommandPreferences(context, TestCryptoEngine())
        registry = CustomCommandRegistry(preferences)
    }

    @Test
    fun getNextAvailableCommandId_returns_first_id_when_empty() {
        assertEquals(CustomCommand.COMMAND_ID_RANGE_START, registry.getNextAvailableCommandId())
        assertTrue(registry.isCommandIdAvailable(CustomCommand.COMMAND_ID_RANGE_START))
    }

    @Test
    fun registerCommand_marks_id_used_and_advances_next_available() = runBlocking {
        val command = CustomCommand(
            id = "cmd-1",
            name = "Test",
            commandId = CustomCommand.COMMAND_ID_RANGE_START,
            payload = ByteArray(5)
        )

        registry.registerCommand(command)

        assertFalse(registry.isCommandIdAvailable(CustomCommand.COMMAND_ID_RANGE_START))
        assertEquals((CustomCommand.COMMAND_ID_RANGE_START + 1).toByte(), registry.getNextAvailableCommandId())
        assertEquals(command, registry.getCommand("cmd-1"))
    }

    @Test
    fun getNextAvailableCommandId_returns_null_when_range_is_exhausted() = runBlocking {
        val allIds = (CustomCommand.COMMAND_ID_RANGE_START..CustomCommand.COMMAND_ID_RANGE_END).toList()
        for ((index, id) in allIds.withIndex()) {
            registry.registerCommand(
                CustomCommand(
                    id = "cmd-$index",
                    name = "Cmd $index",
                    commandId = id.toByte(),
                    payload = ByteArray(5)
                )
            )
        }

        assertNull(registry.getNextAvailableCommandId())
        assertTrue(registry.getAvailableCommandIds().isEmpty())
    }
}
