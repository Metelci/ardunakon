package com.metelci.ardunakon.crash

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for CrashHandler file operations.
 *
 * These tests require real Android file system access and
 * don't work reliably in Robolectric environment.
 */
@RunWith(AndroidJUnit4::class)
class CrashHandlerInstrumentedTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Before
    fun setup() {
        CrashHandler.clearCrashLog(context)
    }

    @After
    fun tearDown() {
        CrashHandler.clearCrashLog(context)
    }

    @Test
    fun logException_includesCustomMessage() {
        CrashHandler.logException(context, RuntimeException("test error"), "Custom error message")

        val file = CrashHandler.getCrashLogFile(context)
        assertTrue("Log file should exist", file.exists())
        assertTrue("File should have content", file.length() > 0)

        val log = CrashHandler.getCrashLog(context)
        assertTrue("Log should contain custom message", log.contains("Custom error message"))
    }

    @Test
    fun logException_writesMultipleExceptionsToSameFile() {
        CrashHandler.logException(context, IllegalStateException("first"), "Error 1")
        CrashHandler.logException(context, NullPointerException("second"), "Error 2")

        val file = CrashHandler.getCrashLogFile(context)
        assertTrue("Log file should exist", file.exists())

        val log = CrashHandler.getCrashLog(context)
        assertTrue("Log should not be empty", log.isNotEmpty())
        assertTrue("Log should contain first error", log.contains("Error 1"))
        assertTrue("Log should contain second error", log.contains("Error 2"))
    }

    @Test
    fun logException_writesToFile() {
        CrashHandler.logException(context, Exception("test"))

        val file = CrashHandler.getCrashLogFile(context)
        assertTrue("Log file should exist", file.exists())

        val log = CrashHandler.getCrashLog(context)
        assertTrue("Log should not be empty", log.isNotEmpty())
    }

    @Test
    fun logException_appendsMultipleLogs() {
        CrashHandler.logException(context, Exception("first"))
        val firstLen = CrashHandler.getCrashLogFile(context).length()

        CrashHandler.logException(context, Exception("second"))
        val secondLen = CrashHandler.getCrashLogFile(context).length()

        assertTrue("Second log should make file larger", secondLen > firstLen)
    }

    @Test
    fun logException_handlesNullMessage() {
        // Should not throw with null message
        CrashHandler.logException(context, Exception("test"), message = null)

        val file = CrashHandler.getCrashLogFile(context)
        assertTrue("Log file should exist", file.exists())

        val log = CrashHandler.getCrashLog(context)
        assertTrue("Log should not be empty", log.isNotEmpty())
    }
}
