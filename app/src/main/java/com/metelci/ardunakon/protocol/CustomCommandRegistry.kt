package com.metelci.ardunakon.protocol

import com.metelci.ardunakon.data.CustomCommandPreferences
import com.metelci.ardunakon.model.CustomCommand
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Registry for managing custom commands at runtime.
 * Provides StateFlow for reactive UI updates and CRUD operations.
 */
@Singleton
class CustomCommandRegistry @Inject constructor(
    private val preferences: CustomCommandPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _commands = MutableStateFlow<List<CustomCommand>>(emptyList())
    val commands: StateFlow<List<CustomCommand>> = _commands.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initialize the registry by loading commands from storage.
     * Call this during app startup.
     */
    fun initialize() {
        if (_isInitialized.value) return
        scope.launch {
            _commands.value = preferences.loadCommands()
            _isInitialized.value = true
        }
    }

    /**
     * Register a new custom command.
     */
    suspend fun registerCommand(command: CustomCommand) {
        preferences.addCommand(command)
        _commands.value = preferences.loadCommands()
    }

    /**
     * Update an existing command.
     */
    suspend fun updateCommand(command: CustomCommand) {
        preferences.updateCommand(command)
        _commands.value = preferences.loadCommands()
    }

    /**
     * Remove a command by ID.
     */
    suspend fun unregisterCommand(id: String) {
        preferences.deleteCommand(id)
        _commands.value = preferences.loadCommands()
    }

    /**
     * Get a command by ID.
     */
    fun getCommand(id: String): CustomCommand? {
        return _commands.value.find { it.id == id }
    }

    /**
     * Get the next available command ID in the custom range.
     * Returns null if all 16 slots are used.
     */
    fun getNextAvailableCommandId(): Byte? {
        val usedIds = _commands.value.map { it.commandId }.toSet()
        for (id in CustomCommand.COMMAND_ID_RANGE_START..CustomCommand.COMMAND_ID_RANGE_END) {
            if (id.toByte() !in usedIds) {
                return id.toByte()
            }
        }
        return null
    }

    /**
     * Get all available command IDs (not currently in use).
     */
    fun getAvailableCommandIds(): List<Byte> {
        val usedIds = _commands.value.map { it.commandId }.toSet()
        return (CustomCommand.COMMAND_ID_RANGE_START..CustomCommand.COMMAND_ID_RANGE_END)
            .map { it.toByte() }
            .filter { it !in usedIds }
    }

    /**
     * Check if a command ID is available for use.
     */
    fun isCommandIdAvailable(commandId: Byte): Boolean {
        return _commands.value.none { it.commandId == commandId }
    }

    /**
     * Reload commands from storage (useful after external changes).
     */
    suspend fun reload() {
        _commands.value = preferences.loadCommands()
    }
}
