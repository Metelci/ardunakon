package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.security.CryptoEngine
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectionPreferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun loadLastConnection_when_file_missing_returns_defaults() {
        runBlocking {
            val prefs = ConnectionPreferences(context, PassThroughCryptoEngine)

            File(context.filesDir, "connection_prefs.json").delete()

            val loaded = prefs.loadLastConnection()
            assertNull(loaded.type)
            assertEquals(8888, loaded.wifiPort)
            assertEquals(false, loaded.autoReconnectWifi)
            assertEquals(1.0f, loaded.joystickSensitivity, 0.0001f)
        }
    }

    @Test
    fun save_then_load_roundtrips_values_and_merges_partial_updates() {
        runBlocking {
            val prefs = ConnectionPreferences(context, PassThroughCryptoEngine)

            prefs.saveLastConnection(
                type = "WIFI",
                wifiIp = "192.168.4.1",
                wifiPort = 9999,
                joystickSensitivity = 1.25f
            )
            prefs.saveLastConnection(autoReconnectWifi = true)

            val loaded = prefs.loadLastConnection()
            assertEquals("WIFI", loaded.type)
            assertEquals("192.168.4.1", loaded.wifiIp)
            assertEquals(9999, loaded.wifiPort)
            assertEquals(true, loaded.autoReconnectWifi)
            assertEquals(1.25f, loaded.joystickSensitivity, 0.0001f)
        }
    }

    @Test
    fun loadLastConnection_when_decrypt_fails_returns_defaults() {
        runBlocking {
            val prefs = ConnectionPreferences(context, ThrowingCryptoEngine)

            // Write a value that would normally parse, but decryption will throw.
            File(context.filesDir, "connection_prefs.json").writeText("not-really-encrypted")

            val loaded = prefs.loadLastConnection()
            assertNull(loaded.type)
            assertEquals(8888, loaded.wifiPort)
            assertEquals(false, loaded.autoReconnectWifi)
            assertEquals(1.0f, loaded.joystickSensitivity, 0.0001f)
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
