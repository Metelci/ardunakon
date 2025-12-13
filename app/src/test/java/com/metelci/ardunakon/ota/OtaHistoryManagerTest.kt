package com.metelci.ardunakon.ota

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OtaHistoryManagerTest {

    private lateinit var context: Context
    private lateinit var manager: OtaHistoryManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = OtaHistoryManager(context)
        manager.clearHistory()
    }

    @Test
    fun addEntryKeepsMostRecentAndTrims() {
        val files = (1..5).map { idx ->
            File(context.cacheDir, "firmware_$idx.bin").apply { writeBytes(ByteArray(idx * 10) { idx.toByte() }) }
        }

        // Add same file twice to verify de-dupe and order
        manager.addEntry(files[0], successful = true)
        manager.addEntry(files[0], successful = false)

        files.drop(1).forEach { manager.addEntry(it, successful = true) }

        val history = manager.getHistory()

        assertEquals(5, history.size)
        assertEquals(files.last().absolutePath, history.first().filePath) // most recent first
        assertEquals(1, history.count { it.filePath == files[0].absolutePath }) // duplicate collapsed

        // Add one more file to trigger trim of the oldest entry
        val overflow = File(context.cacheDir, "firmware_overflow.bin").apply { writeBytes(ByteArray(5) { 9 }) }
        manager.addEntry(overflow, successful = true)
        val trimmed = manager.getHistory()
        assertEquals(5, trimmed.size)
        assertEquals(overflow.absolutePath, trimmed.first().filePath)
        assertEquals(0, trimmed.count { it.filePath == files[0].absolutePath })
    }

    @Test
    fun validateFileChecksSizeAndCrc() {
        val file = File(context.cacheDir, "firmware_validate.bin").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        }
        manager.addEntry(file, successful = true)
        val entry = manager.getHistory().first()

        assertTrue(manager.validateFile(entry))

        // Corrupt the file to make validation fail
        file.writeBytes(byteArrayOf(9, 9, 9))
        assertFalse(manager.validateFile(entry))
    }

    @Test
    fun clearHistoryRemovesEntries() {
        val file = File(context.cacheDir, "firmware_clear.bin").apply { writeBytes(byteArrayOf(1, 2)) }
        manager.addEntry(file, successful = true)

        manager.clearHistory()

        assertTrue(manager.getHistory().isEmpty())
    }
}
