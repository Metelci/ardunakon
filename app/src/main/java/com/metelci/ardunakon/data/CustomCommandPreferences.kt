package com.metelci.ardunakon.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.metelci.ardunakon.model.CustomCommand
import com.metelci.ardunakon.security.CryptoEngine
import com.metelci.ardunakon.security.SecurityManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists custom commands using encrypted JSON storage.
 * Follows the pattern established by ConnectionPreferences.
 */
class CustomCommandPreferences(
    private val context: Context,
    private val cryptoEngine: CryptoEngine = SecurityManager()
) {
    private val fileName = "custom_commands.json"

    companion object {
        private const val TAG = "CustomCommandPrefs"
    }

    /**
     * Save a list of custom commands.
     */
    suspend fun saveCommands(commands: List<CustomCommand>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            commands.forEach { cmd ->
                jsonArray.put(commandToJson(cmd))
            }

            val root = JSONObject()
            root.put("version", 1)
            root.put("commands", jsonArray)

            val encrypted = cryptoEngine.encrypt(root.toString())
            File(context.filesDir, fileName).writeText(encrypted)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error saving commands", e)
        }
    }

    /**
     * Load all custom commands.
     */
    suspend fun loadCommands(): List<CustomCommand> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return@withContext emptyList()

            val encrypted = file.readText()
            val json = cryptoEngine.decrypt(encrypted)
            val root = JSONObject(json)

            val jsonArray = root.optJSONArray("commands") ?: return@withContext emptyList()
            val commands = mutableListOf<CustomCommand>()

            for (i in 0 until jsonArray.length()) {
                val cmdJson = jsonArray.getJSONObject(i)
                jsonToCommand(cmdJson)?.let { commands.add(it) }
            }

            commands
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error loading commands", e)
            emptyList()
        }
    }

    /**
     * Add a new command to the list.
     */
    suspend fun addCommand(command: CustomCommand) {
        val current = loadCommands().toMutableList()
        // Ensure no duplicate IDs
        current.removeAll { it.id == command.id }
        current.add(command)
        saveCommands(current)
    }

    /**
     * Update an existing command.
     */
    suspend fun updateCommand(command: CustomCommand) {
        val current = loadCommands().toMutableList()
        val index = current.indexOfFirst { it.id == command.id }
        if (index >= 0) {
            current[index] = command
            saveCommands(current)
        } else {
            addCommand(command)
        }
    }

    /**
     * Delete a command by ID.
     */
    suspend fun deleteCommand(id: String) {
        val current = loadCommands().toMutableList()
        current.removeAll { it.id == id }
        saveCommands(current)
    }

    /**
     * Get all command IDs currently in use.
     */
    suspend fun getUsedCommandIds(): Set<Byte> {
        return loadCommands().map { it.commandId }.toSet()
    }

    private fun commandToJson(cmd: CustomCommand): JSONObject {
        return JSONObject().apply {
            put("id", cmd.id)
            put("name", cmd.name)
            put("commandId", cmd.commandId.toInt())
            put("payload", Base64.encodeToString(cmd.payload, Base64.NO_WRAP))
            put("colorHex", cmd.colorHex)
            put("isToggle", cmd.isToggle)
            put("iconName", cmd.iconName)
        }
    }

    private fun jsonToCommand(json: JSONObject): CustomCommand? {
        return try {
            val payloadBase64 = json.optString("payload", "AAAAAAA=")
            val payload = Base64.decode(payloadBase64, Base64.NO_WRAP)

            CustomCommand(
                id = json.getString("id"),
                name = json.getString("name"),
                commandId = json.getInt("commandId").toByte(),
                payload = if (payload.size == 5) payload else ByteArray(5),
                colorHex = json.optLong("colorHex", 0xFF2196F3),
                isToggle = json.optBoolean("isToggle", false),
                iconName = json.optString("iconName", "Build")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing command JSON", e)
            null
        }
    }
}
