package com.metelci.ardunakon.model

/**
 * Represents specific connection error states for Bluetooth and WiFi.
 */
sealed interface ConnectionError {
    val message: String

    data class DeviceNotFound(override val message: String = "Device not found") : ConnectionError
    data class ConnectionRejected(override val message: String = "Connection rejected") : ConnectionError
    data class Timeout(override val message: String = "Connection timed out") : ConnectionError
    data class BluetoothDisabled(override val message: String = "Bluetooth is disabled") : ConnectionError
    data class MissingPermissions(override val message: String = "Missing required permissions") : ConnectionError
    data class WifiDisabled(override val message: String = "WiFi is disabled") : ConnectionError
    data class EncryptionError(override val message: String) : ConnectionError
    data class UnknownError(override val message: String) : ConnectionError
}
