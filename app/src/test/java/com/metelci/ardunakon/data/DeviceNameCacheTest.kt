package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.bluetooth.DeviceType
import com.metelci.ardunakon.security.CryptoEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DeviceNameCacheTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getName_when_file_missing_returns_null() {
        runBlocking {
            val cache = DeviceNameCache(context, PassThroughCryptoEngine)
            File(context.filesDir, "device_names.json").delete()

            assertNull(cache.getName("00:11"))
        }
    }

    @Test
    fun saveName_then_getName_returns_saved_value() {
        runBlocking {
            val cache = DeviceNameCache(context, PassThroughCryptoEngine)
            cache.saveName("00:11", "HC-05", DeviceType.CLASSIC)

            assertEquals("HC-05", cache.getName("00:11"))
        }
    }

    @Test
    fun saveName_overwrites_existing_address_entry() {
        runBlocking {
            val cache = DeviceNameCache(context, PassThroughCryptoEngine)
            cache.saveName("00:11", "Old", DeviceType.CLASSIC)
            cache.saveName("00:11", "New", DeviceType.LE)

            assertEquals("New", cache.getName("00:11"))
        }
    }

    @Test
    fun cleanOldEntries_removes_entries_older_than_cutoff() {
        runBlocking {
            val cache = DeviceNameCache(context, PassThroughCryptoEngine)
            val now = System.currentTimeMillis()
            val oldTs = now - 31L * 24 * 60 * 60 * 1000
            val newTs = now - 1_000

            // Because our crypto is pass-through, we can seed the cache file directly.
            val json = """
                [
                  {"address":"AA:AA","name":"Old","type":"CLASSIC","lastSeen":$oldTs},
                  {"address":"BB:BB","name":"New","type":"CLASSIC","lastSeen":$newTs}
                ]
            """.trimIndent()
            File(context.filesDir, "device_names.json").writeText(json)

            cache.cleanOldEntries()

            assertNull(cache.getName("AA:AA"))
            assertEquals("New", cache.getName("BB:BB"))
        }
    }

    @Test
    fun load_handles_corrupt_or_undecryptable_file_by_returning_empty_cache() {
        runBlocking {
            val cache = DeviceNameCache(context, ThrowingCryptoEngine)
            File(context.filesDir, "device_names.json").writeText("corrupt")

            assertNull(cache.getName("00:11"))
            cache.cleanOldEntries() // should not throw
        }
    }

    private data object PassThroughCryptoEngine : CryptoEngine {
        override fun encrypt(plainText: String): String = plainText
        override fun decrypt(cipherText: String): String = cipherText
    }

    private data object ThrowingCryptoEngine : CryptoEngine {
        override fun encrypt(plainText: String): String = error("encrypt should not be called")
        override fun decrypt(cipherText: String): String = throw IllegalStateException("boom")
    }
}
