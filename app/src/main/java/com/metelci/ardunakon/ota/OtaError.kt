package com.metelci.ardunakon.ota

/**
 * OTA Error Codes with detailed messages
 */
enum class OtaError(val code: String, val message: String, val suggestion: String) {
    E001("E001", "WiFi AP not found", "Make sure Arduino is running ArdunakonOTA sketch and in range"),
    E002("E002", "Connection timeout", "Move closer to Arduino or restart the device"),
    E003("E003", "Transfer interrupted", "Check Bluetooth/WiFi connection and retry"),
    E004("E004", "CRC mismatch", "File may be corrupted. Re-download and try again"),
    E005("E005", "Flash write failed", "Arduino may have insufficient storage or be damaged"),
    E006("E006", "Insufficient space", "Firmware file too large for this device"),
    E007("E007", "Authentication failed", "Check your Arduino Cloud API credentials"),
    E008("E008", "Network error", "Check your internet connection"),
    E009("E009", "Invalid firmware file", "Select a valid .bin file compiled for your board"),
    E010("E010", "Device not responding", "Arduino may need to be reset manually"),
    E011("E011", "BLE not connected", "Connect to Arduino via Bluetooth first"),
    E012("E012", "File not found", "The selected file no longer exists");
    
    fun getFullMessage(): String = "[$code] $message\n$suggestion"
    
    companion object {
        fun fromException(e: Exception): OtaError {
            return when {
                e.message?.contains("timeout", ignoreCase = true) == true -> E002
                e.message?.contains("connection", ignoreCase = true) == true -> E003
                e.message?.contains("auth", ignoreCase = true) == true -> E007
                e.message?.contains("network", ignoreCase = true) == true -> E008
                else -> E003 // Default to transfer interrupted
            }
        }
    }
}
