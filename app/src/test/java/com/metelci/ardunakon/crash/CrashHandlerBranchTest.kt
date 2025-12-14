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
    fun init_thenLogException_usesHandlerFormatAndShareIntentNonNull() {
        CrashHandler.init(context)
        CrashHandler.clearCrashLog(context)

        BreadcrumbManager.clear()
        BreadcrumbManager.leave("T", "first")

        CrashHandler.logException(context, RuntimeException("oops"), message = "custom")

        val log = CrashHandler.getCrashLog(context)
        assertTrue(log.contains("=== NON-FATAL ERROR ==="))
        assertTrue(log.contains("--- Breadcrumbs (Last Actions) ---"))
        assertTrue(log.contains("T: first"))

        val share: Intent? = CrashHandler.getShareIntent(context)
        assertNotNull(share)
        assertEquals(Intent.ACTION_SEND, share!!.action)
        assertTrue(share.getStringExtra(Intent.EXTRA_TEXT)?.contains("NON-FATAL ERROR") == true)
    }

    @Test
    fun hasCrashLog_falseWhenEmptyFile() {
        val file: File = CrashHandler.getCrashLogFile(context)
        file.writeText("")

        assertFalse(CrashHandler.hasCrashLog(context))
    }
}
