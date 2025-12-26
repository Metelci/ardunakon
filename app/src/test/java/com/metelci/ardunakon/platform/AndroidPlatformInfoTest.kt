package com.metelci.ardunakon.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidPlatformInfoTest {

    @Test
    fun verify_platform_info_values() {
        // Under Robolectric, Build fields are populated with default test values.
        val info = AndroidPlatformInfo()
        
        // We just want to ensure these properties are accessible and return non-null values
        // that match what's in the Build class (which Robolectric shadow controls).
        assertNotNull(info.sdkVersion)
        assertNotNull(info.manufacturer)
        assertNotNull(info.model)
        assertNotNull(info.androidVersion)
        
        // Ensure constants match (Robolectric's environment)
        assertEquals(android.os.Build.VERSION.SDK_INT, info.sdkVersion)
        assertEquals(android.os.Build.MANUFACTURER, info.manufacturer)
        assertEquals(android.os.Build.MODEL, info.model)
        assertEquals(android.os.Build.VERSION.RELEASE, info.androidVersion)
    }
}
