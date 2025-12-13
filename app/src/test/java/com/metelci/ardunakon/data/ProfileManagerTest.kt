package com.metelci.ardunakon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.TestCryptoEngine
import com.metelci.ardunakon.model.ButtonConfig
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "profiles.json").delete()
    }

    @Test
    fun saveAndLoadProfilesRoundTrip() = runTest {
        val profile = Profile(
            name = "Custom",
            buttonConfigs = listOf(
                ButtonConfig(id = 1, label = "Test", command = "cmd", colorHex = 0xFF0000)
            ),
            sensitivity = 1.5f
        )
        val manager = ProfileManager(context, TestCryptoEngine())

        manager.saveProfiles(listOf(profile))
        val loaded = manager.loadProfiles()

        assertEquals(1, loaded.size)
        assertEquals("Custom", loaded.first().name)
        assertEquals(1.5f, loaded.first().sensitivity)
        assertEquals("cmd", loaded.first().buttonConfigs.first().command)
    }

    @Test
    fun loadProfilesFallsBackToDefaultsOnDecryptFailure() = runTest {
        File(context.filesDir, "profiles.json").writeText("invalid payload")
        val manager = ProfileManager(context, TestCryptoEngine())

        val loaded = manager.loadProfiles()

        assertTrue(loaded.isNotEmpty())
        assertEquals("Rover (Car Mode)", loaded.first().name)
    }
}
