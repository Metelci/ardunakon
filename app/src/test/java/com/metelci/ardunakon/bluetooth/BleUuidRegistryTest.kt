package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Tests for BleUuidRegistry - validates UUID organization and lookup functions.
 */
class BleUuidRegistryTest {

    // ============== Registry Structure Tests ==============

    @Test
    fun `all variants have unique IDs`() {
        val ids = BleUuidRegistry.ALL_VARIANTS.map { it.id }
        assertEquals("All variant IDs should be unique", ids.size, ids.distinct().size)
    }

    @Test
    fun `all variants have non-empty names`() {
        BleUuidRegistry.ALL_VARIANTS.forEach { variant ->
            assertTrue("Variant ${variant.id} should have a name", variant.name.isNotEmpty())
        }
    }

    @Test
    fun `all variants have service UUIDs`() {
        BleUuidRegistry.ALL_VARIANTS.forEach { variant ->
            assertNotNull("Variant ${variant.name} should have a service UUID", variant.serviceUuid)
        }
    }

    @Test
    fun `registry contains expected number of variants`() {
        assertEquals("Registry should have 8 variants", 8, BleUuidRegistry.ALL_VARIANTS.size)
    }

    // ============== HC-08/HM-10 Variant Tests ==============

    @Test
    fun `HC08_HM10 variant has correct service UUID`() {
        val expected = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        assertEquals(expected, BleUuidRegistry.VARIANT_HC08_HM10.serviceUuid)
    }

    @Test
    fun `HC08_HM10 variant has split characteristics`() {
        val variant = BleUuidRegistry.VARIANT_HC08_HM10
        assertTrue(variant.hasSplitCharacteristics())
        assertNotNull(variant.txCharUuid)
        assertNotNull(variant.rxCharUuid)
    }

    @Test
    fun `HC08_HM10 variant has legacy characteristic`() {
        val variant = BleUuidRegistry.VARIANT_HC08_HM10
        assertTrue(variant.hasLegacyCharacteristic())
        val expected = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        assertEquals(expected, variant.legacyCharUuid)
    }

    // ============== Nordic UART Variant Tests ==============

    @Test
    fun `Nordic UART variant has correct service UUID`() {
        val expected = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        assertEquals(expected, BleUuidRegistry.VARIANT_NORDIC_UART.serviceUuid)
    }

    @Test
    fun `Nordic UART variant has split characteristics`() {
        val variant = BleUuidRegistry.VARIANT_NORDIC_UART
        assertTrue(variant.hasSplitCharacteristics())
        // Note: Nordic naming is reversed (their RX is our TX)
        val expectedTx = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val expectedRx = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        assertEquals(expectedTx, variant.txCharUuid)
        assertEquals(expectedRx, variant.rxCharUuid)
    }

    // ============== ArduinoBLE Variant Tests ==============

    @Test
    fun `ArduinoBLE variant has correct service UUID`() {
        val expected = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
        assertEquals(expected, BleUuidRegistry.VARIANT_ARDUINO_BLE.serviceUuid)
    }

    @Test
    fun `ArduinoBLE variant has both split and legacy characteristics`() {
        val variant = BleUuidRegistry.VARIANT_ARDUINO_BLE
        assertTrue(variant.hasSplitCharacteristics())
        assertTrue(variant.hasLegacyCharacteristic())
    }

    // ============== Legacy Variants (Single Characteristic) ==============

    @Test
    fun `TI HM10 variant uses legacy characteristic only`() {
        val variant = BleUuidRegistry.VARIANT_TI_HM10
        assertFalse(variant.hasSplitCharacteristics())
        assertTrue(variant.hasLegacyCharacteristic())
        val expected = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
        assertEquals(expected, variant.legacyCharUuid)
    }

    @Test
    fun `HC08 Alt variant uses legacy characteristic only`() {
        val variant = BleUuidRegistry.VARIANT_HC08_ALT
        assertFalse(variant.hasSplitCharacteristics())
        assertTrue(variant.hasLegacyCharacteristic())
    }

    @Test
    fun `AT09 variant uses legacy characteristic only`() {
        val variant = BleUuidRegistry.VARIANT_AT09
        assertFalse(variant.hasSplitCharacteristics())
        assertTrue(variant.hasLegacyCharacteristic())
    }

    @Test
    fun `MLT_BT05 variant has split characteristics`() {
        val variant = BleUuidRegistry.VARIANT_MLT_BT05
        assertTrue("MLT-BT05 should have split TX/RX", variant.hasSplitCharacteristics())
        assertTrue(variant.hasLegacyCharacteristic())
        // MLT-BT05 uses same FFE1 for both TX and RX (bidirectional)
        val expected = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        assertEquals(expected, variant.txCharUuid)
        assertEquals(expected, variant.rxCharUuid)
    }

    @Test
    fun `MLT_BT05_ALT variant uses legacy characteristic only`() {
        val variant = BleUuidRegistry.VARIANT_MLT_BT05_ALT
        assertFalse(variant.hasSplitCharacteristics())
        assertTrue(variant.hasLegacyCharacteristic())
        val expected = UUID.fromString("0000FFF6-0000-1000-8000-00805F9B34FB")
        assertEquals(expected, variant.legacyCharUuid)
    }

    // ============== Lookup Functions ==============

    @Test
    fun `findByServiceUuid returns correct variants`() {
        val ffE0Service = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val variants = BleUuidRegistry.findByServiceUuid(ffE0Service)
        
        assertTrue(variants.isNotEmpty())
        // FFE0 is used by HC08_HM10, MLT-BT05, and AT09
        assertTrue(variants.any { it.name == "HC-08/HM-10 Standard" })
        assertTrue(variants.any { it.name == "MLT-BT05" })
        assertTrue(variants.any { it.name == "AT-09" })
    }

    @Test
    fun `findByServiceUuid returns empty for unknown UUID`() {
        val unknownUuid = UUID.fromString("12345678-1234-1234-1234-123456789ABC")
        val variants = BleUuidRegistry.findByServiceUuid(unknownUuid)
        assertTrue(variants.isEmpty())
    }

    @Test
    fun `findById returns correct variant`() {
        val variant = BleUuidRegistry.findById(1)
        assertNotNull(variant)
        assertEquals("HC-08/HM-10 Standard", variant?.name)
    }

    @Test
    fun `findById returns null for unknown ID`() {
        val variant = BleUuidRegistry.findById(999)
        assertNull(variant)
    }

    // ============== Characteristic Candidates ==============

    @Test
    fun `getCharacteristicCandidates returns all characteristics for service`() {
        val ffE0Service = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val candidates = BleUuidRegistry.getCharacteristicCandidates(ffE0Service)
        
        assertTrue(candidates.isNotEmpty())
        // Should include FFE1 (TX), FFE2 (RX), and FFE4 (AT-09 legacy)
        val ffe1 = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        val ffe2 = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB")
        val ffe4 = UUID.fromString("0000FFE4-0000-1000-8000-00805F9B34FB")
        
        assertTrue(candidates.contains(ffe1))
        assertTrue(candidates.contains(ffe2))
        assertTrue(candidates.contains(ffe4))
    }

    @Test
    fun `getCharacteristicCandidates returns distinct UUIDs`() {
        val ffE0Service = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val candidates = BleUuidRegistry.getCharacteristicCandidates(ffE0Service)
        
        assertEquals(candidates.size, candidates.distinct().size)
    }

    @Test
    fun `getCharacteristicCandidates returns empty for unknown service`() {
        val unknownService = UUID.fromString("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")
        val candidates = BleUuidRegistry.getCharacteristicCandidates(unknownService)
        assertTrue(candidates.isEmpty())
    }

    // ============== CCCD UUID ==============

    @Test
    fun `CCCD UUID is correct`() {
        val expected = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        assertEquals(expected, BleUuidRegistry.CCCD_UUID)
    }

    // ============== Variant Discovery Order ==============

    @Test
    fun `MLT_BT05 is first in discovery order`() {
        // MLT-BT05 is prioritized (user confirmed working)
        assertEquals("MLT-BT05", BleUuidRegistry.ALL_VARIANTS.first().name)
    }

    @Test
    fun `ArduinoBLE variant is last for discovery`() {
        // ArduinoBLE should be tried after more common modules
        assertEquals("ArduinoBLE", BleUuidRegistry.ALL_VARIANTS.last().name)
    }
}
