package com.metelci.ardunakon.crash

import android.content.Context
import com.metelci.ardunakon.monitoring.PerformanceMonitor
import com.metelci.ardunakon.monitoring.SeverityLevel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashHandlerExtendedTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun resetCrashHandlerSingleton() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        val field = CrashHandler::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)
    }

    private class AppendingCrashLogWriter : CrashHandler.CrashLogWriter {
        override fun write(context: Context, content: String) {
            val file = CrashHandler.getCrashLogFile(context)
            file.parentFile?.mkdirs()
            file.appendText(content)
        }
    }

    private class FlakyCrashLogWriter(
        private val delegate: CrashHandler.CrashLogWriter
    ) : CrashHandler.CrashLogWriter {
        var calls: Int = 0
            private set

        override fun write(context: Context, content: String) {
            calls++
            if (calls == 1) return
            delegate.write(context, content)
        }
    }

    @Before
    fun setUp() {
        resetCrashHandlerSingleton()
        CrashHandler.setCrashLogWriterForTest(AppendingCrashLogWriter())
        CrashHandler.init(context)
        CrashHandler.clearCrashLog(context)
        BreadcrumbManager.clear()
        PerformanceMonitor.resetForTests()
    }

    @After
    fun tearDown() {
        CrashHandler.clearCrashLog(context)
        BreadcrumbManager.clear()
        PerformanceMonitor.resetForTests()
        CrashHandler.setCrashLogWriterForTest(AppendingCrashLogWriter())
        resetCrashHandlerSingleton()
    }

    @Test
    fun getCrashLogFile_usesExpectedFilename() {
        assertEquals("crash_log.txt", CrashHandler.getCrashLogFile(context).name)
    }

    @Test
    fun logException_includesDeviceMemoryBreadcrumbAndStackSections() {
        BreadcrumbManager.leave("TEST", "Action 1")

        CrashHandler.logException(
            context = context,
            throwable = RuntimeException("boom"),
            message = "custom",
            severity = SeverityLevel.WARNING
        )

        val log = CrashHandler.getCrashLog(context)

        assertTrue(log.contains("=== NON-FATAL ERROR ==="))
        assertTrue(log.contains("Message: custom"))
        assertTrue(log.contains("--- Device Info ---"))
        assertTrue(log.contains("App Version:"))
        assertTrue(log.contains("Android:"))
        assertTrue(log.contains("--- Memory Info ---"))
        assertTrue(log.contains("--- Breadcrumbs (Last Actions) ---"))
        assertTrue(log.contains("TEST: Action 1"))
        assertTrue(log.contains("--- Stack Trace ---"))
        assertTrue(log.contains("RuntimeException"))
    }

    @Test
    fun logException_whenWriterDoesNotWriteFirstTime_triggersFallbackWrite() {
        val flaky = FlakyCrashLogWriter(AppendingCrashLogWriter())
        CrashHandler.setCrashLogWriterForTest(flaky)

        CrashHandler.clearCrashLog(context)

        CrashHandler.logException(
            context = context,
            throwable = IllegalStateException("boom"),
            message = "hello"
        )

        val file = CrashHandler.getCrashLogFile(context)
        assertEquals(2, flaky.calls)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        val log = file.readText()
        assertTrue(log.contains("=== NON-FATAL ERROR ==="))
        assertTrue(log.contains("Message: hello"))
        assertTrue(log.contains("IllegalStateException"))
    }

    @Test
    fun logException_rotatesWhenExistingLogExceedsOneMegabyte() {
        val file: File = CrashHandler.getCrashLogFile(context)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(1024 * 1024 + 32) { 'A'.code.toByte() })
        assertTrue(file.length() > 1024 * 1024)

        CrashHandler.logException(context, RuntimeException("after-rotation"), message = "m")

        assertTrue(file.exists())
        assertTrue(file.length() < 200_000)
        assertTrue(file.readText().contains("after-rotation"))
    }

    @Test
    fun logException_doesNotFailWhenPerformanceMonitorThrows() {
        mockkObject(PerformanceMonitor.Companion)
        try {
            every { PerformanceMonitor.getInstance() } returns mockk(relaxed = true) {
                every { recordCrash(any(), any(), any()) } throws RuntimeException("monitor-fail")
            }

            CrashHandler.logException(context, RuntimeException("boom"), message = "m")

            assertTrue(CrashHandler.getCrashLog(context).isNotBlank())
        } finally {
            unmockkObject(PerformanceMonitor.Companion)
        }
    }
}
