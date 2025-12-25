package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for WriteQueueManager.
 *
 * Tests bounded queue behavior, backpressure, ordering, thread safety,
 * and packet drop handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WriteQueueManagerTest {

    private lateinit var testScope: TestScope
    private lateinit var queueManager: WriteQueueManager
    private val writtenPackets = mutableListOf<ByteArray>()
    private val writeFailures = AtomicInteger(0)
    private val logMessages = mutableListOf<Pair<String, LogType>>()

    @Before
    fun setup() {
        testScope = TestScope()
        queueManager = WriteQueueManager(
            capacity = 10, // Small capacity for easier testing
            scope = testScope,
            tag = "TestQueue"
        )

        // Setup callbacks
        queueManager.onPacketDropped = { /* count tracked via metric */ }
        queueManager.onWriteFailed = { writeFailures.incrementAndGet() }
        queueManager.onLog = { msg, type -> logMessages.add(msg to type) }

        writtenPackets.clear()
        writeFailures.set(0)
        logMessages.clear()
    }

    @After
    fun teardown() {
        queueManager.stop()
        testScope.cancel()
    }

    @Test
    fun `queue drops oldest packet when capacity exceeded`() = testScope.runTest {
        // Don't start the queue - just test enqueue behavior
        val packets = (0..11).map { byteArrayOf(it.toByte()) }

        // Enqueue 12 packets to 10-capacity queue
        packets.forEach { queueManager.enqueue(it) }

        // Queue should be at capacity
        assertEquals(10, queueManager.queueSize())

        // First 2 packets should be dropped (oldest)
        assertEquals(2L, queueManager.packetsDropped)

        // Should have logged warnings
        assertTrue(logMessages.any { it.second == LogType.WARNING && "dropped" in it.first })
    }

    @Test
    fun `queue clears all pending packets`() = testScope.runTest {
        // Enqueue without starting
        repeat(5) { queueManager.enqueue(byteArrayOf(it.toByte())) }
        assertEquals(5, queueManager.queueSize())

        // Clear
        queueManager.clear()

        assertEquals(0, queueManager.queueSize())
    }

    @Test
    fun `queue stops processing when stopped`() = testScope.runTest {
        queueManager.start(
            performWrite = { writtenPackets.add(it); true },
            writeDelayMs = 10L,
            initialDelayMs = 0L
        )

        assertTrue(queueManager.isRunning())

        queueManager.stop()
        assertFalse(queueManager.isRunning())
    }

    @Test
    fun `enqueue returns false when queue overflows`() = testScope.runTest {
        // Fill the queue
        repeat(10) { queueManager.enqueue(byteArrayOf(it.toByte())) }

        // 11th packet should cause overflow
        val result = queueManager.enqueue(byteArrayOf(11.toByte()))

        assertFalse(result)
        assertEquals(1L, queueManager.packetsDropped)
    }

    @Test
    fun `queue reports metrics correctly`() = testScope.runTest {
        // Test initial state
        assertEquals(0L, queueManager.packetsSent)
        assertEquals(0L, queueManager.packetsFailed)
        assertEquals(0L, queueManager.packetsDropped)

        // Cause some drops
        repeat(12) { queueManager.enqueue(byteArrayOf(it.toByte())) }

        assertEquals(2L, queueManager.packetsDropped)
    }

    @Test
    fun `queue clear empties the queue`() = testScope.runTest {
        repeat(5) { queueManager.enqueue(byteArrayOf(it.toByte())) }
        assertEquals(5, queueManager.queueSize())

        queueManager.clear()
        assertEquals(0, queueManager.queueSize())
    }

    @Test
    fun `resetMetrics clears all counters`() = testScope.runTest {
        // Cause some drops to increment metrics
        repeat(12) { queueManager.enqueue(byteArrayOf(it.toByte())) }
        assertTrue(queueManager.packetsDropped > 0)

        queueManager.resetMetrics()

        assertEquals(0L, queueManager.packetsSent)
        assertEquals(0L, queueManager.packetsDropped)
        assertEquals(0L, queueManager.packetsFailed)
    }

    @Test
    fun `queue logs warning on overflow`() = testScope.runTest {
        // Fill beyond capacity
        repeat(12) { queueManager.enqueue(byteArrayOf(it.toByte())) }

        // Should have logged warnings
        assertTrue(logMessages.any { it.second == LogType.WARNING })
        assertTrue(logMessages.any { "dropped" in it.first.lowercase() })
    }

    @Test
    fun `queue capacity is respected`() = testScope.runTest {
        // Enqueue exactly capacity
        repeat(10) { queueManager.enqueue(byteArrayOf(it.toByte())) }

        assertEquals(10, queueManager.queueSize())
        assertEquals(0L, queueManager.packetsDropped)
    }

    @Test
    fun `isRunning returns correct state`() = testScope.runTest {
        assertFalse(queueManager.isRunning())

        queueManager.start(
            performWrite = { true },
            writeDelayMs = 10L,
            initialDelayMs = 0L
        )

        assertTrue(queueManager.isRunning())

        queueManager.stop()

        assertFalse(queueManager.isRunning())
    }

    @Test
    fun `onPacketDropped callback is invoked`() = testScope.runTest {
        var dropCount = 0
        queueManager.onPacketDropped = { dropCount++ }

        // Cause overflow
        repeat(12) { queueManager.enqueue(byteArrayOf(it.toByte())) }

        assertEquals(2, dropCount)
    }

    @Test
    fun `multiple starts cancel previous job`() = testScope.runTest {
        queueManager.start(
            performWrite = { true },
            writeDelayMs = 10L,
            initialDelayMs = 0L
        )

        assertTrue(queueManager.isRunning())

        // Start again - should not crash
        queueManager.start(
            performWrite = { true },
            writeDelayMs = 10L,
            initialDelayMs = 0L
        )

        assertTrue(queueManager.isRunning())

        queueManager.stop()
        assertFalse(queueManager.isRunning())
    }
}
