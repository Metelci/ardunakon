package com.metelci.ardunakon.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

class BluetoothConfigBranchTest {

    @Test
    fun isBleOnlyName_handles_null_and_empty() {
        assertFalse(BluetoothConfig.isBleOnlyName(null))
        assertFalse(BluetoothConfig.isBleOnlyName(""))
        assertFalse(BluetoothConfig.isBleOnlyName("   "))
    }

    @Test
    fun isBleOnlyName_matches_known_markers_case_insensitive() {
        assertTrue(BluetoothConfig.isBleOnlyName("hm-10"))
        assertTrue(BluetoothConfig.isBleOnlyName("Arduino UNO R4 WiFi"))
        assertTrue(BluetoothConfig.isBleOnlyName("ArdunakonQ"))
        assertFalse(BluetoothConfig.isBleOnlyName("HC-05"))
    }

    @Test
    fun calculateBackoffDelay_increases_and_caps() {
        assertEquals(3000L, BluetoothConfig.calculateBackoffDelay(0))
        assertEquals(6000L, BluetoothConfig.calculateBackoffDelay(1))
        assertEquals(12000L, BluetoothConfig.calculateBackoffDelay(2))
        assertEquals(24000L, BluetoothConfig.calculateBackoffDelay(3))
        assertEquals(24000L, BluetoothConfig.calculateBackoffDelay(4))
        assertEquals(24000L, BluetoothConfig.calculateBackoffDelay(100))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BluetoothConfigManufacturerBranchTest {

    @Test
    fun shouldForceReflectionFallback_respects_manufacturer_allowlist() {
        val original = android.os.Build.MANUFACTURER
        try {
            ReflectionHelpers.setStaticField(android.os.Build::class.java, "MANUFACTURER", "Xiaomi")
            assertTrue(BluetoothConfig.shouldForceReflectionFallback())

            ReflectionHelpers.setStaticField(android.os.Build::class.java, "MANUFACTURER", "Samsung")
            assertFalse(BluetoothConfig.shouldForceReflectionFallback())
        } finally {
            ReflectionHelpers.setStaticField(android.os.Build::class.java, "MANUFACTURER", original)
        }
    }
}

