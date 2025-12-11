package com.metelci.ardunakon.bluetooth

import java.util.UUID

/**
 * Centralized configuration for Bluetooth connections
 * Contains all UUID variants, timeout values, and connection parameters
 */
object BluetoothConfig {

    // ========== Classic Bluetooth UUIDs ==========

    /**
     * Standard SPP UUID (HC-06, Texas Instruments, Microchip, Telit Bluemod)
     */
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Manufacturer-specific UUIDs for HC-06 variants and clones
     * COMPREHENSIVE LIST - Covers maximum number of HC-06 clone variants
     * Note: Standard SPP (00001101) is already tried separately
     */
    val MANUFACTURER_UUIDS: List<UUID> = listOf(
        // Nordic Semiconductor nRF51822-based HC-06 clones (VERY common in Chinese clones)
        UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
        // Alternative Nordic UART Service (Nordic-based clones, nRF51/nRF52)
        UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"),
        // Object Push Profile - Many HC-06 clones (ZS-040, FC-114, linvor, JY-MCU)
        UUID.fromString("00001105-0000-1000-8000-00805F9B34FB"),
        // OBEX Object Push - Alternative HC-06 clones (some linvor firmware)
        UUID.fromString("00001106-0000-1000-8000-00805F9B34FB"),
        // Headset Profile - BT 2.0 HC-06 clones (older firmware)
        UUID.fromString("00001108-0000-1000-8000-00805F9B34FB"),
        // Hands-Free Profile - Some HC-06 modules configured as hands-free
        UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB"),
        // A/V Remote Control Profile - Rare HC-06 clones
        UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB"),
        // Advanced Audio Distribution Profile - Some multimedia HC-06 clones
        UUID.fromString("0000110D-0000-1000-8000-00805F9B34FB"),
        // Dial-up Networking Profile - Older HC-06 firmware
        UUID.fromString("00001103-0000-1000-8000-00805F9B34FB"),
        // LAN Access Profile - Some network-oriented HC-06 clones
        UUID.fromString("00001102-0000-1000-8000-00805F9B34FB"),
        // Raw RFCOMM - Bare-metal HC-06 implementations
        UUID.fromString("00000003-0000-1000-8000-00805F9B34FB"),
        // Base UUID - Last resort for completely non-standard implementations
        UUID.fromString("00000000-0000-1000-8000-00805F9B34FB")
    )

    // ========== BLE GATT UUIDs ==========

    /**
     * BLE UUID Registry is now centralized in BleUuidRegistry.kt
     * Use BleUuidRegistry.ALL_VARIANTS for structured access to all module variants
     *
     * Legacy compatibility aliases below - prefer using BleUuidRegistry directly
     */

    // CCCD (kept here as it's universally used)
    val CCCD_UUID: UUID get() = BleUuidRegistry.CCCD_UUID

    // Legacy aliases for backward compatibility (V1 - HC-08/HM-10)
    @Deprecated("Use BleUuidRegistry.VARIANT_HC08_HM10 instead", ReplaceWith("BleUuidRegistry.VARIANT_HC08_HM10.serviceUuid"))
    val BLE_SERVICE_UUID_V1: UUID get() = BleUuidRegistry.VARIANT_HC08_HM10.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_HC08_HM10 instead", ReplaceWith("BleUuidRegistry.VARIANT_HC08_HM10.legacyCharUuid!!"))
    val BLE_CHAR_UUID_V1: UUID get() = BleUuidRegistry.VARIANT_HC08_HM10.legacyCharUuid!!

    @Deprecated("Use BleUuidRegistry.VARIANT_HC08_HM10 instead", ReplaceWith("BleUuidRegistry.VARIANT_HC08_HM10.txCharUuid!!"))
    val BLE_CHAR_TX_V1: UUID get() = BleUuidRegistry.VARIANT_HC08_HM10.txCharUuid!!

    @Deprecated("Use BleUuidRegistry.VARIANT_HC08_HM10 instead", ReplaceWith("BleUuidRegistry.VARIANT_HC08_HM10.rxCharUuid!!"))
    val BLE_CHAR_RX_V1: UUID get() = BleUuidRegistry.VARIANT_HC08_HM10.rxCharUuid!!

    // Legacy aliases (V2 - Nordic UART)
    @Deprecated("Use BleUuidRegistry.VARIANT_NORDIC_UART instead", ReplaceWith("BleUuidRegistry.VARIANT_NORDIC_UART.serviceUuid"))
    val BLE_SERVICE_UUID_V2: UUID get() = BleUuidRegistry.VARIANT_NORDIC_UART.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_NORDIC_UART instead", ReplaceWith("BleUuidRegistry.VARIANT_NORDIC_UART.txCharUuid!!"))
    val BLE_CHAR_TX_V2: UUID get() = BleUuidRegistry.VARIANT_NORDIC_UART.txCharUuid!!

    @Deprecated("Use BleUuidRegistry.VARIANT_NORDIC_UART instead", ReplaceWith("BleUuidRegistry.VARIANT_NORDIC_UART.rxCharUuid!!"))
    val BLE_CHAR_RX_V2: UUID get() = BleUuidRegistry.VARIANT_NORDIC_UART.rxCharUuid!!

    // Legacy aliases (V3 - TI HM-10)
    @Deprecated("Use BleUuidRegistry.VARIANT_TI_HM10 instead", ReplaceWith("BleUuidRegistry.VARIANT_TI_HM10.serviceUuid"))
    val BLE_SERVICE_UUID_V3: UUID get() = BleUuidRegistry.VARIANT_TI_HM10.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_TI_HM10 instead", ReplaceWith("BleUuidRegistry.VARIANT_TI_HM10.legacyCharUuid!!"))
    val BLE_CHAR_UUID_V3: UUID get() = BleUuidRegistry.VARIANT_TI_HM10.legacyCharUuid!!

    // Legacy aliases (V4 - HC-08 Alt)
    @Deprecated("Use BleUuidRegistry.VARIANT_HC08_ALT instead", ReplaceWith("BleUuidRegistry.VARIANT_HC08_ALT.serviceUuid"))
    val BLE_SERVICE_UUID_V4: UUID get() = BleUuidRegistry.VARIANT_HC08_ALT.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_HC08_ALT instead", ReplaceWith("BleUuidRegistry.VARIANT_HC08_ALT.legacyCharUuid!!"))
    val BLE_CHAR_UUID_V4: UUID get() = BleUuidRegistry.VARIANT_HC08_ALT.legacyCharUuid!!

    // Legacy aliases (V5 - AT-09)
    @Deprecated("Use BleUuidRegistry.VARIANT_AT09 instead", ReplaceWith("BleUuidRegistry.VARIANT_AT09.serviceUuid"))
    val BLE_SERVICE_UUID_V5: UUID get() = BleUuidRegistry.VARIANT_AT09.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_AT09 instead", ReplaceWith("BleUuidRegistry.VARIANT_AT09.legacyCharUuid!!"))
    val BLE_CHAR_UUID_V5: UUID get() = BleUuidRegistry.VARIANT_AT09.legacyCharUuid!!

    // Legacy aliases (V6 - MLT-BT05)
    @Deprecated("Use BleUuidRegistry.VARIANT_MLT_BT05 instead", ReplaceWith("BleUuidRegistry.VARIANT_MLT_BT05.serviceUuid"))
    val BLE_SERVICE_UUID_V6: UUID get() = BleUuidRegistry.VARIANT_MLT_BT05.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_MLT_BT05 instead", ReplaceWith("BleUuidRegistry.VARIANT_MLT_BT05.legacyCharUuid!!"))
    val BLE_CHAR_UUID_V6: UUID get() = BleUuidRegistry.VARIANT_MLT_BT05.legacyCharUuid!!

    // Legacy aliases (V9 - ArduinoBLE)
    @Deprecated("Use BleUuidRegistry.VARIANT_ARDUINO_BLE instead", ReplaceWith("BleUuidRegistry.VARIANT_ARDUINO_BLE.serviceUuid"))
    val BLE_SERVICE_UUID_V9: UUID get() = BleUuidRegistry.VARIANT_ARDUINO_BLE.serviceUuid

    @Deprecated("Use BleUuidRegistry.VARIANT_ARDUINO_BLE instead", ReplaceWith("BleUuidRegistry.VARIANT_ARDUINO_BLE.legacyCharUuid!!"))
    val BLE_CHAR_UUID_V9: UUID get() = BleUuidRegistry.VARIANT_ARDUINO_BLE.legacyCharUuid!!

    @Deprecated("Use BleUuidRegistry.VARIANT_ARDUINO_BLE instead", ReplaceWith("BleUuidRegistry.VARIANT_ARDUINO_BLE.txCharUuid!!"))
    val BLE_CHAR_TX_V9: UUID get() = BleUuidRegistry.VARIANT_ARDUINO_BLE.txCharUuid!!

    @Deprecated("Use BleUuidRegistry.VARIANT_ARDUINO_BLE instead", ReplaceWith("BleUuidRegistry.VARIANT_ARDUINO_BLE.rxCharUuid!!"))
    val BLE_CHAR_RX_V9: UUID get() = BleUuidRegistry.VARIANT_ARDUINO_BLE.rxCharUuid!!

    // ========== BLE Device Name Patterns ==========

    /**
     * Device names that indicate BLE-only modules
     */
    val BLE_ONLY_NAME_MARKERS: List<String> = listOf(
        "HM-10", "HM10", "AT-09", "AT09", "MLT-BT05", "BT05", "BT-05",
        "HC-08", "HC08", "CC41", "CC41-A", "BLE", "ARDUNAKON",
        "ARDUINO", "R4", "UNO R4"
    )

    /**
     * Check if device name indicates a BLE-only module
     * Used to force BLE connection path for known BLE modules
     */
    fun isBleOnlyName(name: String?): Boolean {
        val nameUpper = (name ?: "").uppercase()
        return BLE_ONLY_NAME_MARKERS.any { marker -> nameUpper.contains(marker) }
    }

    // ========== Connection Timeouts ==========

    /**
     * Heartbeat timeout for Classic Bluetooth (milliseconds)
     * Increased from 12s to 20s for better tolerance
     */
    const val HEARTBEAT_TIMEOUT_CLASSIC_MS = 20000L

    /**
     * Heartbeat timeout for BLE connections (milliseconds)
     * BLE clones like HM-10/HC-08 often stay silent; allow long idle periods
     */
    const val HEARTBEAT_TIMEOUT_BLE_MS = 300000L  // 5 minutes

    /**
     * Missed ACK threshold before reconnect for Classic BT
     */
    const val MISSED_ACK_THRESHOLD_CLASSIC = 5

    /**
     * Missed ACK threshold before reconnect for BLE
     * 60 heartbeats @4s ≈ 4 minutes before timeout
     */
    const val MISSED_ACK_THRESHOLD_BLE = 60

    /**
     * Heartbeat interval (milliseconds)
     */
    const val HEARTBEAT_INTERVAL_MS = 4000L

    /**
     * BLE connection timeout (milliseconds)
     */
    const val BLE_CONNECTION_TIMEOUT_MS = 15000L

    /**
     * GATT operation timeout (milliseconds)
     */
    const val GATT_OPERATION_TIMEOUT_MS = 4000L

    /**
     * Scan timeout duration (milliseconds)
     */
    const val SCAN_TIMEOUT_MS = 10000L

    // ========== Reconnection Parameters ==========

    /**
     * Base delay for exponential backoff (milliseconds)
     */
    const val BACKOFF_BASE_DELAY_MS = 3000L

    /**
     * Maximum backoff delay (milliseconds)
     */
    const val BACKOFF_MAX_DELAY_MS = 30000L

    /**
     * Maximum reconnection attempts before circuit breaker activates
     */
    const val MAX_RECONNECT_ATTEMPTS = 10

    /**
     * Reconnection monitor check interval (milliseconds)
     */
    const val RECONNECT_MONITOR_INTERVAL_MS = 1000L

    // ========== Classic Bluetooth Connection Parameters ==========

    /**
     * Delay after cancelling discovery (milliseconds)
     */
    const val DISCOVERY_CANCEL_DELAY_MS = 300L

    /**
     * Delay for Xiaomi/MIUI devices (milliseconds)
     */
    const val XIAOMI_STREAM_INIT_DELAY_MS = 500L

    /**
     * Socket close delay (milliseconds)
     */
    const val SOCKET_CLOSE_DELAY_MS = 100L

    /**
     * Standard connection retry delay (milliseconds)
     */
    const val CONNECTION_RETRY_DELAY_MS = 2000L

    /**
     * Aggressive retry delay for Xiaomi devices (milliseconds)
     */
    const val XIAOMI_RETRY_DELAY_MS = 600L

    /**
     * Reflection port numbers to try
     */
    val REFLECTION_PORTS = listOf(1, 2, 3)

    // ========== OEM-Specific Configuration ==========

    /**
     * OEMs that require reflection fallback
     */
    val FORCE_REFLECTION_OEMS = setOf("xiaomi", "redmi", "poco")

    // ========== Limits and Buffers ==========

    /**
     * Maximum debug log entries
     */
    const val MAX_DEBUG_LOGS = 50

    /**
     * Maximum RTT history entries
     */
    const val MAX_RTT_HISTORY = 20

    /**
     * Maximum write queue size
     */
    const val MAX_WRITE_QUEUE_SIZE = 100

    /**
     * Read buffer size (bytes)
     */
    const val READ_BUFFER_SIZE = 1024

    /**
     * Packet buffer size (bytes)
     */
    const val PACKET_BUFFER_SIZE = 20

    /**
     * Telemetry log throttle interval (milliseconds)
     */
    const val TELEMETRY_LOG_THROTTLE_MS = 20000L

    /**
     * State change debounce interval (milliseconds)
     */
    const val STATE_CHANGE_DEBOUNCE_MS = 800L

    /**
     * Minimum cleanup time between connections (milliseconds)
     */
    const val CONNECTION_CLEANUP_DELAY_MS = 200L

    // ========== Battery Validation ==========

    /**
     * Minimum valid battery voltage (volts)
     */
    const val MIN_BATTERY_VOLTAGE = 0f

    /**
     * Maximum valid battery voltage (volts)
     */
    const val MAX_BATTERY_VOLTAGE = 30f

    // ========== Vibration Durations ==========

    /**
     * Connection success vibration (milliseconds)
     */
    const val VIBRATION_CONNECTED_MS = 200L

    /**
     * Disconnection vibration (milliseconds)
     */
    const val VIBRATION_DISCONNECTED_MS = 100L

    /**
     * Error vibration (milliseconds)
     */
    const val VIBRATION_ERROR_MS = 500L
}
