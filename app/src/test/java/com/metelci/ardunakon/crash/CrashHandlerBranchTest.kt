package com.metelci.ardunakon.crash

import android.content.Intent
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CrashHandlerBranchTest {

    private val context = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        CrashHandler.clearCrashLog(context)
    }

    @Test
    fun getShareIntent_returnsNullWhenNoLog() {
        CrashHandler.clearCrashLog(context)
        assertNull(CrashHandler.getShareIntent(context))
    }

    @Test
    fun logException_writesFallbackWhenNotInitialized() {
        CrashHandler.clearCrashLog(context)

        CrashHandler.logException(context, IllegalStateException("boom"), message = "hello")

        val file = CrashHandler.getCrashLogFile(context)
        assertTrue(file.exists())
        val text = file.readText()
        assertTrue(text.contains("=== NON-FATAL ERROR ==="))
        assertTrue(text.contains("Message: hello"))
        assertTrue(text.contains("IllegalStateException"))
    }

    @Test
    fun init_thenLogException_writesLogFile() {
        CrashHandler.init(context)
        CrashHandler.clearCrashLog(context)

        CrashHandler.logException(context, RuntimeException("oops"), message = "custom")

        val log = CrashHandler.getCrashLog(context)
        assertTrue(log.isNotEmpty())
        assertTrue(log.contains("oops") || log.contains("RuntimeException"))
    }

    @Test
    fun hasCrashLog_falseWhenEmptyFile() {
        val file: File = CrashHandler.getCrashLogFile(context)
        file.writeText("")

        assertFalse(CrashHandler.hasCrashLog(context))
    }

    @Test
    fun clearCrashLog_deletesExistingFile() {
        val file = CrashHandler.getCrashLogFile(context)
        file.writeText("Some crash data")
        assertTrue(file.exists())

        CrashHandler.clearCrashLog(context)

        assertFalse(file.exists())
    }

    @Test
    fun clearCrashLog_handlesNonExistentFile() {
        val file = CrashHandler.getCrashLogFile(context)
        file.delete()

        // Should not throw
        CrashHandler.clearCrashLog(context)

        assertFalse(file.exists())
    }

    @Test
    fun getCrashLog_returnsEmptyStringWhenFileDoesNotExist() {
        CrashHandler.clearCrashLog(context)

        val log = CrashHandler.getCrashLog(context)

        assertEquals("", log)
    }

    @Test
    fun getCrashLog_returnsFileContents() {
        val expectedContent = "=== CRASH ===\nSome error details"
        val file = CrashHandler.getCrashLogFile(context)
        file.writeText(expectedContent)

        val log = CrashHandler.getCrashLog(context)

        assertEquals(expectedContent, log)
    }
    // Tests moved to CrashHandlerInstrumentedTest.kt:
    // - logException_includesCustomMessage
    // - logException_writesMultipleExceptionsToSameFile  
    // - logException_writesToFile
    // (These require real Android file system)

    @Test
    fun getShareIntent_includesCorrectExtras() {
        val crashData = "=== CRASH ===\nTest crash data"
        CrashHandler.getCrashLogFile(context).writeText(crashData)

        val intent = CrashHandler.getShareIntent(context)

        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent!!.action)
        assertEquals("text/plain", intent.type)
        assertEquals("Ardunakon Crash Report", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(crashData, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun hasCrashLog_trueWhenFileHasContent() {
        val file = CrashHandler.getCrashLogFile(context)
        file.writeText("Some crash data")

        assertTrue(CrashHandler.hasCrashLog(context))
    }

    @Test
    fun hasCrashLog_falseWhenFileDoesNotExist() {
        CrashHandler.clearCrashLog(context)

        assertFalse(CrashHandler.hasCrashLog(context))
    }

    @Test
    fun logException_savesExceptionInfo() {
        CrashHandler.clearCrashLog(context)
        BreadcrumbManager.clear()

        BreadcrumbManager.leave("TEST", "Action 1")

        CrashHandler.logException(context, RuntimeException("test"))

        val log = CrashHandler.getCrashLog(context)
        assertTrue(log.isNotEmpty())
        assertTrue(log.contains("RuntimeException") || log.contains("test"))
    }

    @Test
    fun logException_createsLogFile() {
        CrashHandler.clearCrashLog(context)

        CrashHandler.logException(context, Exception("test"))

        val file = CrashHandler.getCrashLogFile(context)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
    // Tests moved to CrashHandlerInstrumentedTest.kt:
    // - logException_appendsMultipleLogs
    // - logException_handlesNullMessage
    // (These require real Android file system)
}
