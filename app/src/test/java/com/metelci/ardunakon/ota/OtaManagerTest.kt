package com.metelci.ardunakon.ota

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.CRC32

private class FakeOtaTransport(
    private val connectResult: Boolean = true,
    private val failAtOffset: Int? = null,
    private val completeResult: Boolean = true
) : OtaTransport {
    var completeCrc: Long? = null
    val sentOffsets = mutableListOf<Int>()
    var disconnected = false

    override suspend fun connect(): Boolean = connectResult

    override suspend fun sendChunk(data: ByteArray, offset: Int): Boolean {
        sentOffsets += offset
        return failAtOffset?.let { offset == it }?.not() ?: true
    }

    override suspend fun complete(crc: Long): Boolean {
        completeCrc = crc
        return completeResult
    }

    override suspend fun abort() {
        // no-op
    }

    override fun disconnect() {
        disconnected = true
    }
}

@RunWith(RobolectricTestRunner::class)
class OtaManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun startUpdateCompletesHappyPath() = runTest {
        val file = File(context.cacheDir, "ota-success.bin")
        val content = ByteArray(OtaManager.CHUNK_SIZE * 2) { it.toByte() }
        file.writeBytes(content)
        val transport = FakeOtaTransport()
        val manager = OtaManager(context)

        val result = manager.startUpdate(file, OtaMethod.WIFI, wifiTransport = transport)

        val crc = CRC32().apply { update(content) }.value
        assertTrue(result)
        assertEquals(OtaState.COMPLETE, manager.progress.value.state)
        assertEquals(content.size.toLong(), manager.progress.value.bytesTransferred)
        assertEquals(crc, transport.completeCrc)
        assertTrue(transport.disconnected)
        assertEquals(listOf(0, OtaManager.CHUNK_SIZE), transport.sentOffsets)
    }

    @Test
    fun startUpdateStopsOnChunkFailure() = runTest {
        val file = File(context.cacheDir, "ota-fail.bin")
        val content = ByteArray(OtaManager.CHUNK_SIZE + 10) { 1 }
        file.writeBytes(content)
        val transport = FakeOtaTransport(failAtOffset = OtaManager.CHUNK_SIZE)
        val manager = OtaManager(context)

        val result = manager.startUpdate(file, OtaMethod.WIFI, wifiTransport = transport)

        assertFalse(result)
        assertEquals(OtaState.ERROR, manager.progress.value.state)
        assertEquals(OtaManager.CHUNK_SIZE.toLong(), manager.progress.value.bytesTransferred)
    }

    @Test
    fun startUpdateFailsWhenAlreadyRunning() = runTest {
        val manager = OtaManager(context)
        // Force running flag
        OtaManager::class.java.getDeclaredField("isRunning").apply {
            isAccessible = true
            setBoolean(manager, true)
        }

        val file = File(context.cacheDir, "ota-skip.bin").apply { writeBytes(ByteArray(1)) }
        val result = manager.startUpdate(file, OtaMethod.WIFI, wifiTransport = FakeOtaTransport())

        assertFalse(result)
        assertEquals(OtaState.IDLE, manager.progress.value.state)
    }

    @Test
    fun startUpdateReportsVerificationFailure() = runTest {
        val file = File(context.cacheDir, "ota-verify-fail.bin").apply { writeBytes(ByteArray(32) { 3 }) }
        val transport = FakeOtaTransport(completeResult = false)
        val manager = OtaManager(context)

        val result = manager.startUpdate(file, OtaMethod.WIFI, wifiTransport = transport)

        assertFalse(result)
        assertEquals(OtaState.ERROR, manager.progress.value.state)
        assertEquals("Verification failed", manager.progress.value.errorMessage)
    }

    @Test
    fun abortResetsProgressAndInvokesTransportAbort() = runTest {
        val manager = OtaManager(context)
        class AbortTrackingTransport : OtaTransport {
            var aborted = false
            override suspend fun connect() = true
            override suspend fun sendChunk(data: ByteArray, offset: Int) = true
            override suspend fun complete(crc: Long) = true
            override suspend fun abort() { aborted = true }
            override fun disconnect() {}
        }
        val fake = AbortTrackingTransport()
        // Inject fake transport
        OtaManager::class.java.getDeclaredField("transport").apply {
            isAccessible = true
            set(manager, fake)
        }
        manager.abort()

        assertEquals(OtaState.IDLE, manager.progress.value.state)
        assertTrue(fake.aborted)
    }

    @Test
    fun resetClearsProgress() {
        val manager = OtaManager(context)
        manager.reset()
        assertEquals(OtaState.IDLE, manager.progress.value.state)
        assertEquals(0, manager.progress.value.bytesTransferred)
    }
}
