package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.bluetooth.DeviceType
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceNameCacheTest {

    private lateinit var context: Context
    private lateinit var cache: DeviceNameCache

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "device_names.json").delete()
        cache = DeviceNameCache(context, TestCryptoEngine())
    }

    @Test
    fun saveAndLookupName() = runTest {
        cache.saveName("00:11:22:33:44:55", "Rover", DeviceType.CLASSIC)

        val name = cache.getName("00:11:22:33:44:55")

        assertEquals("Rover", name)
    }

    @Test
    fun cleanOldEntriesRemovesExpiredDevices() = runTest {
        val stale = JSONObject().apply {
            put("address", "AA:BB:CC:DD:EE:FF")
            put("name", "OldBot")
            put("type", DeviceType.CLASSIC.name)
            val fortyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(40)
            put("lastSeen", fortyDaysAgo)
        }
        val fresh = JSONObject().apply {
            put("address", "11:22:33:44:55:66")
            put("name", "FreshBot")
            put("type", DeviceType.LE.name)
            put("lastSeen", System.currentTimeMillis())
        }
        val array = JSONArray().apply {
            put(stale)
            put(fresh)
        }
        File(context.filesDir, "device_names.json").writeText(TestCryptoEngine().encrypt(array.toString()))

        cache.cleanOldEntries()

        assertNull(cache.getName("AA:BB:CC:DD:EE:FF"))
        assertEquals("FreshBot", cache.getName("11:22:33:44:55:66"))
    }
}
