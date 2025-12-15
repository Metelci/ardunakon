package com.metelci.ardunakon.bluetooth

import java.util.UUID

/**
 * Represents a BLE module variant with its service and characteristic UUIDs
 */
data class BleModuleVariant(
    val id: Int,
    val name: String,
    val description: String,
    val serviceUuid: UUID,
    val txCharUuid: UUID?, // TX characteristic (notify/indicate - device to phone)
    val rxCharUuid: UUID?, // RX characteristic (write - phone to device)
    val legacyCharUuid: UUID? = null // Legacy single characteristic (bidirectional)
) {
    /**
     * Check if this variant uses split TX/RX characteristics
     */
    fun hasSplitCharacteristics(): Boolean = txCharUuid != null && rxCharUuid != null

    /**
     * Check if this variant uses legacy single characteristic
     */
    fun hasLegacyCharacteristic(): Boolean = legacyCharUuid != null
}

/**
 * Centralized registry for BLE GATT Service and Characteristic UUIDs
 *
 * Supports all major BLE module variants:
 * - HC-08, HM-10, AT-09, MLT-BT05
 * - Nordic UART Service (NUS)
 * - ArduinoBLE Library
 * - Various Chinese clones and firmware variants
 */
object BleUuidRegistry {

    /**
     * Client Characteristic Configuration Descriptor UUID
     * Used to enable notifications/indications
     */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     * Variant 1: Standard HC-08/HM-10 (Most Common)
     * Manufacturers: JNHuaMao, DSD TECH, Bolutek
     * Service: FFE0
     * Characteristics: FFE1 (legacy) or FFE1 (TX notify) + FFE2 (RX write)
     */
    val VARIANT_HC08_HM10 = BleModuleVariant(
        id = 1,
        name = "HC-08/HM-10 Standard",
        description = "Most common HC-08 and HM-10 modules (JNHuaMao, DSD TECH, Bolutek)",
        serviceUuid = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"),
        txCharUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"), // TX (notify)
        rxCharUuid = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB"), // RX (write)
        legacyCharUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB") // Legacy single char
    )

    /**
     * Variant 2: Nordic UART Service (NUS)
     * Used by: Nordic nRF51822/nRF52, some HM-10 clones, Adafruit Bluefruit
     * Service: 6E400001
     * Characteristics: 6E400002 (RX write) + 6E400003 (TX notify)
     * Note: Nordic uses reversed naming (their RX is our TX)
     */
    val VARIANT_NORDIC_UART = BleModuleVariant(
        id = 2,
        name = "Nordic UART Service",
        description = "Nordic nRF51822/nRF52 based modules, some HM-10 clones, Adafruit Bluefruit",
        serviceUuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
        txCharUuid = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"), // TX (notify) - Nordic calls this RX
        rxCharUuid = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // RX (write) - Nordic calls this TX
    )

    /**
     * Variant 3: TI CC2540/CC2541 HM-10 Firmware
     * Original JNHuaMao firmware
     * Service: FFF0
     * Characteristic: FFF1
     */
    val VARIANT_TI_HM10 = BleModuleVariant(
        id = 3,
        name = "TI CC2540/CC2541 HM-10",
        description = "TI CC2540/CC2541 HM-10 firmware (original JNHuaMao)",
        serviceUuid = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB"),
        txCharUuid = null,
        rxCharUuid = null,
        legacyCharUuid = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
    )

    /**
     * Variant 4: Alternative HC-08 Firmware
     * Some Chinese clones
     * Service: FFE5
     * Characteristic: FFE9
     */
    val VARIANT_HC08_ALT = BleModuleVariant(
        id = 4,
        name = "HC-08 Alternative",
        description = "Alternative HC-08 firmware (some Chinese clones)",
        serviceUuid = UUID.fromString("0000FFE5-0000-1000-8000-00805F9B34FB"),
        txCharUuid = null,
        rxCharUuid = null,
        legacyCharUuid = UUID.fromString("0000FFE9-0000-1000-8000-00805F9B34FB")
    )

    /**
     * Variant 5: AT-09 BLE Module
     * CC2541-based, similar to HM-10
     * Service: FFE0 (same as V1, but different characteristic)
     * Characteristic: FFE4
     */
    val VARIANT_AT09 = BleModuleVariant(
        id = 5,
        name = "AT-09",
        description = "AT-09 BLE Module (CC2541-based, similar to HM-10)",
        serviceUuid = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"),
        txCharUuid = null,
        rxCharUuid = null,
        legacyCharUuid = UUID.fromString("0000FFE4-0000-1000-8000-00805F9B34FB")
    )

    /**
     * Variant 6: MLT-BT05 (Standard - FFE0/FFE1)
     * Most common MLT-BT05 modules use same UUIDs as HM-10
     * This is the DEFAULT firmware configuration
     * Service: FFE0
     * Characteristics: FFE1 (TX/RX bidirectional)
     */
    val VARIANT_MLT_BT05 = BleModuleVariant(
        id = 6,
        name = "MLT-BT05",
        description = "MLT-BT05 standard firmware (same as HM-10)",
        serviceUuid = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"),
        txCharUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"), // TX (notify)
        rxCharUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"), // RX (write) - same char, bidirectional
        legacyCharUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
    )

    /**
     * Variant 7: MLT-BT05 Alternate Firmware (FFF0/FFF6)
     * Some MLT-BT05 modules with custom/alternate firmware
     * Service: FFF0
     * Characteristic: FFF6
     */
    val VARIANT_MLT_BT05_ALT = BleModuleVariant(
        id = 7,
        name = "MLT-BT05 (Alt)",
        description = "MLT-BT05 with alternate firmware (FFF0/FFF6)",
        serviceUuid = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB"),
        txCharUuid = null,
        rxCharUuid = null,
        legacyCharUuid = UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB")
    )

    /**
     * Variant 9: ArduinoBLE Library
     * Standard example from Arduino (Uno R4 WiFi, Nano 33 IoT)
     * Service: 19B10000
     * Characteristics: 19B10001 (legacy) or 19B10001 (TX notify) + 19B10002 (RX write)
     */
    val VARIANT_ARDUINO_BLE = BleModuleVariant(
        id = 9,
        name = "ArduinoBLE",
        description = "ArduinoBLE Library Standard Example (Uno R4 WiFi / Nano 33 IoT)",
        serviceUuid = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"),
        txCharUuid = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"), // TX (notify)
        rxCharUuid = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214"), // RX (write)
        legacyCharUuid = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214") // Legacy single char
    )

    /**
     * Variant 10: ArduinoBLE custom UUIDs (e.g., "GameController" sketch)
     * Service: 12345678-1234-1234-1234-123456789abc
     * Characteristic: 87654321-4321-4321-4321-cba987654321 (read/write, often no notify)
     */
    val VARIANT_ARDUINO_CUSTOM_GAME_CONTROLLER = BleModuleVariant(
        id = 10,
        name = "ArduinoBLE (Custom UUIDs)",
        description = "ArduinoBLE custom service/characteristic (e.g., GameController example sketch)",
        serviceUuid = UUID.fromString("12345678-1234-1234-1234-123456789abc"),
        txCharUuid = null,
        rxCharUuid = UUID.fromString("87654321-4321-4321-4321-cba987654321"),
        legacyCharUuid = UUID.fromString("87654321-4321-4321-4321-cba987654321")
    )

    /**
     * All supported variants in discovery order (most common first)
     * Note: MLT-BT05 is prioritized as it's a known working module
     */
    val ALL_VARIANTS = listOf(
        VARIANT_MLT_BT05, // MLT-BT05 standard (FFE0/FFE1) - prioritized (user confirmed)
        VARIANT_HC08_HM10, // HC-08/HM-10 (FFE0/FFE1)
        VARIANT_MLT_BT05_ALT, // MLT-BT05 alternate firmware (FFF0/FFF6)
        VARIANT_NORDIC_UART, // Nordic-based modules
        VARIANT_TI_HM10, // Original TI firmware (FFF0/FFF1)
        VARIANT_HC08_ALT, // Alternative HC-08
        VARIANT_AT09, // AT-09 variant
        VARIANT_ARDUINO_CUSTOM_GAME_CONTROLLER, // ArduinoBLE custom UUID sketch
        VARIANT_ARDUINO_BLE // Arduino library standard example
    )

    /**
     * Find variant by service UUID
     */
    fun findByServiceUuid(serviceUuid: UUID): List<BleModuleVariant> =
        ALL_VARIANTS.filter { it.serviceUuid == serviceUuid }

    /**
     * Find variant by ID
     */
    fun findById(id: Int): BleModuleVariant? = ALL_VARIANTS.find { it.id == id }

    /**
     * Get characteristic candidates for a service
     * Returns list of possible characteristics to try
     */
    fun getCharacteristicCandidates(serviceUuid: UUID): List<UUID> {
        val variants = findByServiceUuid(serviceUuid)
        val candidates = mutableListOf<UUID>()

        variants.forEach { variant ->
            // Add split characteristics first (preferred)
            variant.txCharUuid?.let { candidates.add(it) }
            variant.rxCharUuid?.let { candidates.add(it) }
            // Then legacy
            variant.legacyCharUuid?.let { candidates.add(it) }
        }

        return candidates.distinct()
    }
}
