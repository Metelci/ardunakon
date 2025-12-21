package com.metelci.ardunakon.model

import java.util.UUID

/**
 * Represents a user-defined custom command.
 *
 * Custom commands use command IDs in the range 0x20-0x3F (32-63).
 * Each command follows the standard 10-byte protocol format with a 5-byte payload.
 */
data class CustomCommand(
    /** Unique identifier for this command */
    val id: String = UUID.randomUUID().toString(),

    /** User-friendly display name */
    val name: String,

    /** Protocol command ID (must be in range 0x20-0x3F) */
    val commandId: Byte,

    /** 5-byte payload data (D1-D5 in protocol) */
    val payload: ByteArray = ByteArray(5),

    /** Button color as ARGB hex */
    val colorHex: Long = 0xFF2196F3,

    /** True for toggle button, false for momentary press */
    val isToggle: Boolean = false,

    /** Material icon name for display */
    val iconName: String = "Build"
) {
    companion object {
        /** Start of custom command ID range (inclusive) */
        const val COMMAND_ID_RANGE_START: Byte = 0x20

        /** End of custom command ID range (inclusive) */
        const val COMMAND_ID_RANGE_END: Byte = 0x3F

        /** Maximum number of custom commands */
        const val MAX_COMMANDS = 16

        /** Validate command ID is in allowed range */
        fun isValidCommandId(id: Byte): Boolean =
            id in COMMAND_ID_RANGE_START..COMMAND_ID_RANGE_END
    }

    /** Returns true if this command has a valid command ID */
    fun isValid(): Boolean = isValidCommandId(commandId) && payload.size == 5

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomCommand

        if (id != other.id) return false
        if (name != other.name) return false
        if (commandId != other.commandId) return false
        if (!payload.contentEquals(other.payload)) return false
        if (colorHex != other.colorHex) return false
        if (isToggle != other.isToggle) return false
        if (iconName != other.iconName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + commandId
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + colorHex.hashCode()
        result = 31 * result + isToggle.hashCode()
        result = 31 * result + iconName.hashCode()
        return result
    }
}

/**
 * Default/example custom commands for first-time users.
 */
val defaultCustomCommands = emptyList<CustomCommand>()
