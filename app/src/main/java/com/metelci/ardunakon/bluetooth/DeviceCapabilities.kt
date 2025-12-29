package com.metelci.ardunakon.bluetooth

/**
 * Device capabilities announced by Arduino on connect.
 *
 * @property hasServoX True when servo X is supported.
 * @property hasServoY True when servo Y is supported.
 * @property hasMotor True when motor output is supported.
 * @property hasLedMatrix True when LED matrix is supported.
 * @property hasBuzzer True when buzzer output is supported.
 * @property hasWiFi True when WiFi transport is supported.
 * @property hasBLE True when BLE transport is supported.
 * @property hasModulinoPixels True when Modulino Pixels is attached.
 * @property hasModulinoThermo True when Modulino Thermo is attached.
 * @property hasModulinoDistance True when Modulino Distance is attached.
 * @property hasModulinoBuzzer True when Modulino Buzzer is attached.
 * @property hasModulinoButtons True when Modulino Buttons is attached.
 * @property hasModulinoKnob True when Modulino Knob is attached.
 * @property hasModulinoMovement True when Modulino Movement is attached.
 * @property boardType Detected board type.
 */
data class DeviceCapabilities(
    // Core hardware
    val hasServoX: Boolean = true,
    val hasServoY: Boolean = true,
    val hasMotor: Boolean = true,
    val hasLedMatrix: Boolean = false,
    val hasBuzzer: Boolean = false,
    val hasWiFi: Boolean = false,
    val hasBLE: Boolean = false,

    // Modulino modules
    val hasModulinoPixels: Boolean = false,
    val hasModulinoThermo: Boolean = false,
    val hasModulinoDistance: Boolean = false,
    val hasModulinoBuzzer: Boolean = false,
    val hasModulinoButtons: Boolean = false,
    val hasModulinoKnob: Boolean = false,
    val hasModulinoMovement: Boolean = false,

    // Board type
    val boardType: BoardType = BoardType.UNKNOWN
) {
    companion object {
        /**
         * Default capabilities used for legacy devices without announcements.
         */
        val DEFAULT = DeviceCapabilities()

        /**
         * Parses capabilities from an announcement packet.
         *
         * Format: [CAP1][CAP2][CAP3][CAP4][CAP5].
         *
         * @param data Raw packet data.
         * @param offset Start offset of the capability bytes.
         * @return Parsed [DeviceCapabilities], or [DEFAULT] if data is too short.
         */
        fun fromPacket(data: ByteArray, offset: Int = 0): DeviceCapabilities {
            if (data.size < offset + 3) return DEFAULT

            val cap1 = data[offset].toInt() and 0xFF
            val cap2 = data[offset + 1].toInt() and 0xFF
            val boardByte = data[offset + 2].toInt() and 0xFF

            return DeviceCapabilities(
                hasServoX = (cap1 and 0x01) != 0,
                hasServoY = (cap1 and 0x02) != 0,
                hasMotor = (cap1 and 0x04) != 0,
                hasLedMatrix = (cap1 and 0x08) != 0,
                hasBuzzer = (cap1 and 0x10) != 0,
                hasWiFi = (cap1 and 0x20) != 0,
                hasBLE = (cap1 and 0x40) != 0,

                hasModulinoPixels = (cap2 and 0x01) != 0,
                hasModulinoThermo = (cap2 and 0x02) != 0,
                hasModulinoDistance = (cap2 and 0x04) != 0,
                hasModulinoBuzzer = (cap2 and 0x08) != 0,
                hasModulinoButtons = (cap2 and 0x10) != 0,
                hasModulinoKnob = (cap2 and 0x20) != 0,
                hasModulinoMovement = (cap2 and 0x40) != 0,

                boardType = BoardType.fromByte(boardByte)
            )
        }
    }

    /**
     * Formats a human-readable summary of the enabled features.
     *
     * @return Comma-separated feature list or "Basic" when none are set.
     */
    fun toDisplayString(): String {
        val features = mutableListOf<String>()
        if (hasServoX || hasServoY) features.add("Servo")
        if (hasMotor) features.add("Motor")
        if (hasLedMatrix) features.add("Matrix")
        if (hasBuzzer) features.add("Buzzer")
        if (hasWiFi) features.add("WiFi")
        if (hasBLE) features.add("BLE")
        if (hasModulinoPixels) features.add("Pixels")
        if (hasModulinoThermo) features.add("Thermo")
        if (hasModulinoDistance) features.add("Distance")
        return if (features.isEmpty()) "Basic" else features.joinToString(", ")
    }
}

/**
 * Known board types announced by the device.
 *
 * @property displayName Human-readable board name.
 */
enum class BoardType(val displayName: String) {
    UNKNOWN("Unknown"),
    UNO("Arduino UNO"),
    UNO_R4_WIFI("Arduino UNO R4 WiFi"),
    UNO_R4_MINIMA("Arduino UNO R4 Minima"),
    ESP32("ESP32");

    companion object {
        /**
         * Maps a raw byte value to the corresponding [BoardType].
         *
         * @param value Raw board type byte.
         * @return Matched [BoardType] or [UNKNOWN].
         */
        fun fromByte(value: Int): BoardType = when (value) {
            0x01 -> UNO
            0x02 -> UNO_R4_WIFI
            0x03 -> UNO_R4_MINIMA
            0x04 -> ESP32
            else -> UNKNOWN
        }
    }
}
