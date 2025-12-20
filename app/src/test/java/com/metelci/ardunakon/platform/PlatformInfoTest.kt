package com.metelci.ardunakon.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformInfoTest {

    @Test
    fun version_checks_match_expected_thresholds() {
        val api20 = fakePlatform(sdkVersion = 20)
        assertFalse(api20.isAtLeastLollipop())
        assertFalse(api20.isAtLeastMarshmallow())
        assertFalse(api20.isAtLeastAndroid12())
        assertFalse(api20.isAtLeastAndroid13())

        val api21 = fakePlatform(sdkVersion = 21)
        assertTrue(api21.isAtLeastLollipop())
        assertFalse(api21.isAtLeastMarshmallow())

        val api23 = fakePlatform(sdkVersion = 23)
        assertTrue(api23.isAtLeastMarshmallow())

        val api31 = fakePlatform(sdkVersion = 31)
        assertTrue(api31.isAtLeastAndroid12())
        assertFalse(api31.isAtLeastAndroid13())

        val api33 = fakePlatform(sdkVersion = 33)
        assertTrue(api33.isAtLeastAndroid13())
    }

    @Test
    fun requiresReflectionFallback_normalizes_manufacturer() {
        assertTrue(fakePlatform(manufacturer = "Xiaomi").requiresReflectionFallback())
        assertTrue(fakePlatform(manufacturer = " redmi ").requiresReflectionFallback())
        assertTrue(fakePlatform(manufacturer = "POCO").requiresReflectionFallback())
        assertFalse(fakePlatform(manufacturer = "Samsung").requiresReflectionFallback())
    }

    @Test
    fun isXiaomiDevice_detects_known_oems_case_insensitive() {
        assertTrue(fakePlatform(manufacturer = "XIAOMI").isXiaomiDevice())
        assertTrue(fakePlatform(manufacturer = "RedMi").isXiaomiDevice())
        assertTrue(fakePlatform(manufacturer = "poco").isXiaomiDevice())
        assertFalse(fakePlatform(manufacturer = "Google").isXiaomiDevice())
    }

    private fun fakePlatform(
        sdkVersion: Int = 34,
        manufacturer: String = "unknown"
    ): PlatformInfo {
        return object : PlatformInfo {
            override val sdkVersion: Int = sdkVersion
            override val manufacturer: String = manufacturer
            override val model: String = "model"
            override val androidVersion: String = "ver"
        }
    }
}

