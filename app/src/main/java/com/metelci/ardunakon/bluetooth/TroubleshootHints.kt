package com.metelci.ardunakon.bluetooth

/**
 * Troubleshoot Hints - Auto-suggestions for common errors
 * Displayed inline in debug window after error logs
 */
object TroubleshootHints {

    data class Hint(val pattern: String, val explanation: String, val solution: String)

    private val hints = listOf(
        // Connection errors
        Hint(
            "socket",
            "Connection socket creation failed",
            "Check Bluetooth pairing and restart the device"
        ),
        Hint("refused", "Connection refused", "Make sure Arduino is on and paired"),
        Hint("timeout", "Connection timed out", "Move closer to Arduino or restart the device"),
        Hint("discovery", "Device not found", "Check that Arduino is on and in discoverable mode"),

        // BLE errors
        Hint("GATT", "BLE GATT error", "Turn Bluetooth off and on, then try again"),
        Hint(
            "characteristic",
            "BLE characteristic not found",
            "Verify Arduino sketch is using correct UUIDs"
        ),
        Hint("MTU", "Packet size error", "Device may not support BLE MTU size"),

        // Heartbeat errors
        Hint("heartbeat", "No heartbeat response", "Make sure Arduino is running properly"),
        Hint("missed.*ack", "ACK packets missing", "Check signal quality or move closer"),

        // Permission errors
        Hint("permission", "Permission denied", "Grant Bluetooth permission in app settings"),
        Hint(
            "BLUETOOTH",
            "Bluetooth permission missing",
            "Enable Bluetooth in Settings > Apps > Ardunakon > Permissions"
        ),

        // Hardware errors
        Hint("adapter", "No Bluetooth adapter", "Make sure your device has Bluetooth"),
        Hint("disabled", "Bluetooth is off", "Turn on Bluetooth in system settings")
    )

    /**
     * Get hint for error message (if any pattern matches)
     */
    fun getHintForError(errorMessage: String): Pair<String, String>? {
        val lowerMessage = errorMessage.lowercase()
        for (hint in hints) {
            if (lowerMessage.contains(hint.pattern.lowercase()) ||
                Regex(hint.pattern, RegexOption.IGNORE_CASE).containsMatchIn(errorMessage)
            ) {
                return Pair(hint.explanation, hint.solution)
            }
        }
        return null
    }

    /**
     * Format hint as display string
     */
    fun formatHint(explanation: String, solution: String): String = "→ $explanation. $solution"
}
