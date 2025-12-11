package com.metelci.ardunakon.bluetooth

/**
 * BLE GATT Status Codes for error classification
 */
object GattStatus {
    const val GATT_SUCCESS = 0
    const val GATT_CONNECTION_TIMEOUT = 8
    const val GATT_INSUFFICIENT_AUTHENTICATION = 5
    const val GATT_INSUFFICIENT_ENCRYPTION = 15
    const val GATT_INTERNAL_ERROR = 129
    const val GATT_DEVICE_NOT_FOUND = 133
    const val GATT_LINK_LOSS = 147  // Device-specific: BLE link layer failure

    /**
     * Classify GATT errors as transient (retry-able) or permanent
     */
    fun isTransientError(status: Int): Boolean {
        return when (status) {
            GATT_CONNECTION_TIMEOUT,      // 8 - Timeout, retry may work
            GATT_INTERNAL_ERROR,          // 129 - Android stack issue, retry may work
            GATT_LINK_LOSS,               // 147 - Link layer failure, retry may work
            62                            // 62 - Often returned as "Unknown GATT error (62)" on unstable HM-10 clones
            -> true
            else -> false
        }
    }

    fun isPermanentError(status: Int): Boolean {
        return when (status) {
            GATT_DEVICE_NOT_FOUND        // 133 - Device gone, no point retrying
            -> true
            else -> false
        }
    }

    fun getErrorDescription(status: Int): String {
        return when (status) {
            GATT_SUCCESS -> "Success"
            GATT_CONNECTION_TIMEOUT -> "Connection Timeout (8): Device didn't respond in time"
            GATT_INSUFFICIENT_AUTHENTICATION -> "Insufficient Authentication (5): Pairing required"
            GATT_INSUFFICIENT_ENCRYPTION -> "Insufficient Encryption (15): Encryption required"
            GATT_INTERNAL_ERROR -> "Internal Error (129): Android BLE stack issue"
            GATT_DEVICE_NOT_FOUND -> "Device Not Found (133): Out of range or powered off"
            GATT_LINK_LOSS -> "Link Layer Failure (147): Device reset or interference"
            62 -> "Unknown GATT error (62): Treating as transient HM-10/MLT-BT05 timeout"
            else -> "Unknown GATT error ($status)"
        }
    }
}
