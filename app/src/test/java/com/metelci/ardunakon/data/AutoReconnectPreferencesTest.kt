package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoReconnectPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: AutoReconnectPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "auto_reconnect_prefs.json").delete()
        prefs = AutoReconnectPreferences(context, TestCryptoEngine())
    }

    @Test
    fun saveAndLoadRoundTrips() = runTest {
        prefs.saveAutoReconnectState(slot = 0, enabled = true)
        prefs.saveAutoReconnectState(slot = 1, enabled = false)

        val result = prefs.loadAutoReconnectState()

        assertArrayEquals(booleanArrayOf(true, false), result)
    }

    @Test
    fun loadReturnsDefaultsWhenCorrupted() = runTest {
        // Seed bad file contents to force decrypt failure
        File(context.filesDir, "auto_reconnect_prefs.json").writeText("bad data")

        val result = prefs.loadAutoReconnectState()

        assertArrayEquals(booleanArrayOf(false, false), result)
    }
}
