package com.metelci.ardunakon.ota

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WifiOtaTransportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakeHttpConnection(url: URL, var code: Int = 200) : HttpURLConnection(url) {
        val sentHeaders = mutableMapOf<String, String>()
        val body = ByteArrayOutputStream()
        var disconnected = false

        override fun setRequestProperty(key: String?, value: String?) {
            if (key != null && value != null) sentHeaders[key] = value
        }

        override fun connect() {}
        override fun usingProxy(): Boolean = false
        override fun disconnect() {
            disconnected = true
        }
        override fun getOutputStream(): OutputStream = body
        override fun getInputStream() = ByteArrayInputStream(byteArrayOf())
        override fun getResponseCode(): Int = code
    }

    @Test
    fun sendChunkWritesHeadersAndBody() = runTest {
        val fake = FakeHttpConnection(URL("http://localhost/update"))
        val transport = WifiOtaTransport(context) { _, _ -> fake }

        val result = transport.sendChunk(byteArrayOf(1, 2, 3, 4), offset = 10)

        assertTrue(result)
        assertEquals("POST", fake.requestMethod)
        assertEquals("10", fake.sentHeaders["X-Offset"])
        assertEquals("4", fake.sentHeaders["X-Size"])
        assertEquals("application/octet-stream", fake.sentHeaders["Content-Type"])
        assertEquals(byteArrayOf(1, 2, 3, 4).toList(), fake.body.toByteArray().toList())
        assertTrue(fake.disconnected)
    }

    @Test
    fun sendChunkReturnsFalseOnServerError() = runTest {
        val fake = FakeHttpConnection(URL("http://localhost/update"), code = 500)
        val transport = WifiOtaTransport(context) { _, _ -> fake }

        val result = transport.sendChunk(byteArrayOf(1), offset = 0)

        assertFalse(result)
    }

    @Test
    fun completeSendsCrcPayload() = runTest {
        val fake = FakeHttpConnection(URL("http://localhost/complete"))
        val transport = WifiOtaTransport(context) { _, _ -> fake }

        val result = transport.complete(1234L)

        assertTrue(result)
        assertEquals("POST", fake.requestMethod)
        assertEquals("application/json", fake.sentHeaders["Content-Type"])
        assertEquals("""{"crc": 1234}""", String(fake.body.toByteArray()))
    }

    @Test
    fun abortSwallowsExceptions() = runTest {
        val failingTransport = WifiOtaTransport(context) { _, _ ->
            throw RuntimeException("boom")
        }

        // Should not throw
        failingTransport.abort()
    }
}
