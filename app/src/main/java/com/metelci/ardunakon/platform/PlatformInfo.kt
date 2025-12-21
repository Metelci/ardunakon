package com.metelci.ardunakon.platform

/**
 * Abstraction for Android platform/device information.
 * Allows business logic to be tested without Android dependencies.
 */
interface PlatformInfo {
    /** Android SDK version (e.g., 31 for Android 12) */
    val sdkVersion: Int

    /** Device manufacturer (e.g., "samsung", "xiaomi") */
    val manufacturer: String

    /** Device model name */
    val model: String

    /** Android version string (e.g., "12", "13") */
    val androidVersion: String

    /** Check if running on Android 12 (API 31) or higher */
    fun isAtLeastAndroid12(): Boolean = sdkVersion >= 31

    /** Check if running on Android 13 (API 33) or higher */
    fun isAtLeastAndroid13(): Boolean = sdkVersion >= 33

    /** Check if running on Android 6 (API 23) or higher */
    fun isAtLeastMarshmallow(): Boolean = sdkVersion >= 23

    /** Check if running on Android 5 (API 21) or higher */
    fun isAtLeastLollipop(): Boolean = sdkVersion >= 21

    /** Check if OEM requires reflection fallback for Bluetooth SPP */
    fun requiresReflectionFallback(): Boolean {
        val oem = manufacturer.lowercase().trim()
        return oem in setOf("xiaomi", "redmi", "poco")
    }

    /** Check if this is a Xiaomi/MIUI device requiring aggressive connection handling */
    fun isXiaomiDevice(): Boolean {
        val oem = manufacturer.uppercase()
        return oem.contains("XIAOMI") || oem.contains("REDMI") || oem.contains("POCO")
    }
}
