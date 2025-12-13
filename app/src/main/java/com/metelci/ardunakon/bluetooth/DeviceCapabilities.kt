package com.metelci.ardunakon.bluetooth

/**
 * Device Capabilities - Announced by Arduino on connect
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
        // Default - assume all capabilities (legacy devices)
        val DEFAULT = DeviceCapabilities()

        /**
         * Parse capabilities from announcement packet
         * Format: [CAP1][CAP2][CAP3][CAP4][CAP5]
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

enum class BoardType(val displayName: String) {
    UNKNOWN("Unknown"),
    UNO("Arduino UNO"),
    UNO_R4_WIFI("Arduino UNO R4 WiFi"),
    UNO_R4_MINIMA("Arduino UNO R4 Minima"),
    ESP32("ESP32");

    companion object {
        fun fromByte(value: Int): BoardType = when (value) {
            0x01 -> UNO
            0x02 -> UNO_R4_WIFI
            0x03 -> UNO_R4_MINIMA
            0x04 -> ESP32
            else -> UNKNOWN
        }
    }
}
