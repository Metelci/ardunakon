package com.metelci.ardunakon.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for ErrorContext.
 * Uses Robolectric to handle Android Build class dependencies.
 */
@RunWith(RobolectricTestRunner::class)
class ErrorContextTest {

    // ========== Factory Method Tests ==========

    @Test
    fun `fromThrowable creates context with exception details`() {
        val exception = RuntimeException("Test error")
        val context = ErrorContext.fromThrowable(
            throwable = exception,
            operation = "test_operation"
        )
        
        assertEquals("test_operation", context.operation)
        assertEquals("RuntimeException", context.errorType)
        assertEquals("Test error", context.message)
        assertTrue(context.stackTrace.isNotEmpty())
    }

    @Test
    fun `simple creates context with basic fields`() {
        val context = ErrorContext.simple(
            operation = "simple_op",
            errorType = "ValidationError",
            message = "Invalid input"
        )
        
        assertEquals("simple_op", context.operation)
        assertEquals("ValidationError", context.errorType)
        assertEquals("Invalid input", context.message)
        assertTrue(context.stackTrace.isEmpty())
    }

    @Test
    fun `simple handles null message`() {
        val context = ErrorContext.simple(
            operation = "null_msg_op",
            errorType = "TestError",
            message = null
        )
        
        assertNull(context.message)
    }

    // ========== Severity Tests ==========

    @Test
    fun `default severity is ERROR`() {
        val context = ErrorContext.simple(
            operation = "default_severity",
            errorType = "Test",
            message = "msg"
        )
        assertEquals(ErrorContext.Severity.ERROR, context.severity)
    }

    @Test
    fun `all severity levels exist`() {
        assertNotNull(ErrorContext.Severity.DEBUG)
        assertNotNull(ErrorContext.Severity.INFO)
        assertNotNull(ErrorContext.Severity.WARNING)
        assertNotNull(ErrorContext.Severity.ERROR)
        assertNotNull(ErrorContext.Severity.CRITICAL)
    }

    @Test
    fun `custom severity can be set`() {
        val context = ErrorContext.simple(
            operation = "warning_op",
            errorType = "Warning",
            message = "Minor issue",
            severity = ErrorContext.Severity.WARNING
        )
        assertEquals(ErrorContext.Severity.WARNING, context.severity)
    }

    // ========== Parameters Tests ==========

    @Test
    fun `parameters are stored correctly`() {
        val params = mapOf("key1" to "value1", "key2" to 42)
        val context = ErrorContext.simple(
            operation = "params_op",
            errorType = "Test",
            message = "msg",
            parameters = params
        )
        
        assertEquals("value1", context.parameters["key1"])
        assertEquals(42, context.parameters["key2"])
    }

    @Test
    fun `empty parameters allowed`() {
        val context = ErrorContext.simple(
            operation = "empty_params",
            errorType = "Test",
            message = "msg"
        )
        assertTrue(context.parameters.isEmpty())
    }

    // ========== Formatting Tests ==========

    @Test
    fun `toSummary returns compact format`() {
        val context = ErrorContext.simple(
            operation = "summary_op",
            errorType = "TestError",
            message = "Test message"
        )
        
        val summary = context.toSummary()
        assertTrue(summary.contains("summary_op"))
        assertTrue(summary.contains("TestError"))
    }

    @Test
    fun `toDebugString includes operation and type`() {
        val context = ErrorContext.simple(
            operation = "debug_op",
            errorType = "DebugError",
            message = "Debug message"
        )
        
        val debug = context.toDebugString()
        assertTrue(debug.contains("debug_op"))
        assertTrue(debug.contains("DebugError"))
        assertTrue(debug.contains("Debug message"))
    }

    // ========== Child Context Tests ==========

    @Test
    fun `child creates nested context`() {
        val parent = ErrorContext.simple(
            operation = "parent_op",
            errorType = "ParentError",
            message = "Parent message"
        )
        
        val child = parent.child(
            operation = "child_op",
            errorType = "ChildError",
            message = "Child message"
        )
        
        assertEquals("child_op", child.operation)
        assertEquals("ChildError", child.errorType)
        assertEquals("Child message", child.message)
        assertNotNull(child.parentContextId)
        assertEquals(parent.errorId, child.parentContextId)
    }

    // ========== Stack Trace Tests ==========

    @Test
    fun `fullStackTrace returns complete trace`() {
        val exception = RuntimeException("Full trace test")
        val fullTrace = ErrorContext.fullStackTrace(exception)
        
        assertTrue(fullTrace.contains("RuntimeException"))
        assertTrue(fullTrace.contains("Full trace test"))
    }

    // ========== Builder Tests ==========

    @Test
    fun `errorContext DSL creates context correctly`() {
        val context = errorContext("dsl_operation") {
            errorType("DSLError")
            message("DSL message")
            severity(ErrorContext.Severity.WARNING)
            param("key", "value")
            tag("network")
        }
        
        assertEquals("dsl_operation", context.operation)
        assertEquals("DSLError", context.errorType)
        assertEquals("DSL message", context.message)
        assertEquals(ErrorContext.Severity.WARNING, context.severity)
        assertEquals("value", context.parameters["key"])
        assertTrue(context.tags.contains("network"))
    }

    @Test
    fun `builder supports multiple params`() {
        val context = errorContext("multi_param") {
            params(mapOf("a" to 1, "b" to 2))
            param("c", 3)
        }
        
        assertEquals(1, context.parameters["a"])
        assertEquals(2, context.parameters["b"])
        assertEquals(3, context.parameters["c"])
    }

    @Test
    fun `builder supports multiple tags`() {
        val context = errorContext("multi_tag") {
            tags("tag1", "tag2", "tag3")
            tag("tag4")
        }
        
        assertTrue(context.tags.containsAll(listOf("tag1", "tag2", "tag3", "tag4")))
    }

    @Test
    fun `builder supports retry count`() {
        val context = errorContext("retry_op") {
            retryCount(3)
        }
        
        assertEquals(3, context.retryCount)
    }

    @Test
    fun `builder handles throwable`() {
        val exception = RuntimeException("Builder exception")
        val context = errorContext("throwable_op") {
            throwable(exception)
        }
        
        assertEquals("RuntimeException", context.errorType)
        assertEquals("Builder exception", context.message)
        assertTrue(context.stackTrace.isNotEmpty())
    }

    // ========== DeviceInfo Tests ==========

    @Test
    fun `DeviceInfo current captures device details`() {
        val deviceInfo = ErrorContext.DeviceInfo.current("1.0.0")
        
        assertNotNull(deviceInfo.manufacturer)
        assertNotNull(deviceInfo.model)
        assertTrue(deviceInfo.sdkVersion > 0)
        assertEquals("1.0.0", deviceInfo.appVersion)
    }

    @Test
    fun `DeviceInfo current handles null app version`() {
        val deviceInfo = ErrorContext.DeviceInfo.current(null)
        assertNull(deviceInfo.appVersion)
    }

    // ========== Extension Tests ==========

    @Test
    fun `toErrorContext extension creates context from throwable`() {
        val exception = IllegalArgumentException("Bad argument")
        val context = exception.toErrorContext(operation = "extension_test")
        
        assertEquals("extension_test", context.operation)
        assertEquals("IllegalArgumentException", context.errorType)
        assertEquals("Bad argument", context.message)
    }

    // ========== Data Class Tests ==========

    @Test
    fun `errorId is generated automatically`() {
        val context1 = ErrorContext.simple("op1", "Error", "msg1")
        val context2 = ErrorContext.simple("op2", "Error", "msg2")
        
        assertNotNull(context1.errorId)
        assertNotNull(context2.errorId)
        assertNotEquals(context1.errorId, context2.errorId)
    }

    @Test
    fun `timestamp is recorded`() {
        val before = System.currentTimeMillis()
        val context = ErrorContext.simple("op", "Error", "msg")
        val after = System.currentTimeMillis()
        
        assertTrue(context.timestamp >= before)
        assertTrue(context.timestamp <= after)
    }

    @Test
    fun `tags default to empty set`() {
        val context = ErrorContext.simple("op", "Error", "msg")
        assertTrue(context.tags.isEmpty())
    }

    @Test
    fun `retryCount defaults to zero`() {
        val context = ErrorContext.simple("op", "Error", "msg")
        assertEquals(0, context.retryCount)
    }
}
