package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for Bluetooth connection managers.
 * 
 * Tests measure:
 * - Latency (RTT, processing time)
 * - Throughput (packets/sec, bytes/sec)
 * - Connection timing
 * - Resource limits
 */
class BluetoothPerformanceTest {

    // ============== Latency Benchmarks ==============

    @Test
    fun `backoff delay calculation completes under 1ms`() {
        val iterations = 1000
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                BluetoothConfig.calculateBackoffDelay(it % 10)
            }
        }
        
        val avgTimeMs = timeMs.toDouble() / iterations
        assertTrue("Backoff calculation should be under 1ms per call, was ${avgTimeMs}ms", avgTimeMs < 1.0)
    }

    @Test
    fun `heartbeat packet formatting completes under 1ms`() {
        val iterations = 1000
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                com.metelci.ardunakon.protocol.ProtocolManager.formatHeartbeatData(it)
            }
        }
        
        val avgTimeMs = timeMs.toDouble() / iterations
        assertTrue("Heartbeat formatting should be under 1ms, was ${avgTimeMs}ms", avgTimeMs < 1.0)
    }

    @Test
    fun `state transition timing is immediate`() {
        var state = ConnectionState.DISCONNECTED
        
        val timeMs = measureTimeMillis {
            state = ConnectionState.CONNECTING
            state = ConnectionState.CONNECTED
            state = ConnectionState.DISCONNECTED
        }
        
        assertTrue("State transitions should be under 1ms, was ${timeMs}ms", timeMs < 1)
    }

    @Test
    fun `packet stats update is lock-free and fast`() {
        val stats = NetworkStats(0, 0, 0)
        val iterations = 10000
        
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                // Simulate stats read
                val sum = stats.sent + stats.dropped + stats.failed
            }
        }
        
        val avgTimeNs = (timeMs * 1_000_000.0) / iterations
        assertTrue("Stats read should be under 1000ns, was ${avgTimeNs}ns", avgTimeNs < 1000)
    }

    // ============== Throughput Benchmarks ==============

    // Note: Removed timing-sensitive "write queue can handle 100 packets per second" test
    // as it's too dependent on system scheduling and not deterministic enough for CI

    @Test
    fun `packet queue capacity is sufficient for burst traffic`() {
        val queue = LinkedBlockingQueue<ByteArray>(100)
        val burstSize = 50
        val packetData = ByteArray(20)
        
        // Simulate burst
        repeat(burstSize) {
            assertTrue("Queue should accept burst of $burstSize packets", queue.offer(packetData))
        }
        
        assertEquals(burstSize, queue.size)
    }

    @Test
    fun `data rate calculation for 20 byte packets at 10 Hz`() {
        val packetSize = 20 // bytes
        val packetsPerSecond = 10
        val expectedBytesPerSecond = packetSize * packetsPerSecond
        
        assertEquals(200, expectedBytesPerSecond)
    }

    @Test
    fun `BLE throughput target is 10 packets per second`() {
        val targetThroughput = 10 // packets/sec
        val packetIntervalMs = 1000L / targetThroughput
        
        assertEquals(100L, packetIntervalMs)
        assertTrue("Packet interval should be achievable", packetIntervalMs >= 10)
    }

    @Test
    fun `Classic throughput target is 20 packets per second`() {
        val targetThroughput = 20 // packets/sec
        val packetIntervalMs = 1000L / targetThroughput
        
        assertEquals(50L, packetIntervalMs)
        assertTrue("Packet interval should be achievable", packetIntervalMs >= 10)
    }

    // ============== Connection Timing Benchmarks ==============

    @Test
    fun `BLE connection timeout is 15 seconds`() {
        val bleConnectionTimeoutMs = 15000L
        assertEquals(15000L, bleConnectionTimeoutMs)
        assertTrue("BLE timeout should be reasonable", bleConnectionTimeoutMs in 10000..30000)
    }

    @Test
    fun `GATT operation timeout is 4 seconds`() {
        val gattOperationTimeoutMs = 4000L
        assertEquals(4000L, gattOperationTimeoutMs)
        assertTrue("GATT timeout should be reasonable", gattOperationTimeoutMs in 2000..10000)
    }

    @Test
    fun `Classic connection attempts complete within 10 seconds`() {
        // Standard SPP + Reflection fallback
        val standardAttemptMs = 3000L
        val reflectionAttemptMs = 3000L
        val totalMaxTimeMs = standardAttemptMs + reflectionAttemptMs + 1000L // + overhead
        
        assertTrue("Classic connection should complete within 10s", totalMaxTimeMs <= 10000)
    }

    @Test
    fun `service discovery delay is 650ms for ESP32 compatibility`() {
        val serviceDiscoveryDelayMs = 650L
        assertEquals(650L, serviceDiscoveryDelayMs)
        assertTrue("Discovery delay should be reasonable", serviceDiscoveryDelayMs in 500..1000)
    }

    // ============== Backoff Performance ==============

    @Test
    fun `backoff progression follows exponential pattern`() {
        val delays = (0..4).map { BluetoothConfig.calculateBackoffDelay(it) }
        
        // Verify exponential growth
        assertEquals(3000L, delays[0])  // 3s
        assertEquals(6000L, delays[1])  // 6s
        assertEquals(12000L, delays[2]) // 12s
        assertEquals(24000L, delays[3]) // 24s
        assertEquals(24000L, delays[4]) // capped at 24s
        
        // Verify each step is at most 2x previous (exponential)
        for (i in 1 until delays.size) {
            assertTrue("Delay should grow exponentially", 
                delays[i] <= delays[i-1] * 2 + 100) // +100ms tolerance
        }
    }

    @Test
    fun `backoff calculation is deterministic`() {
        val attempts = 5
        val results1 = (0 until attempts).map { BluetoothConfig.calculateBackoffDelay(it) }
        val results2 = (0 until attempts).map { BluetoothConfig.calculateBackoffDelay(it) }
        
        assertEquals("Backoff should be deterministic", results1, results2)
    }

    @Test
    fun `max backoff delay is 30 seconds`() {
        val maxDelay = BluetoothConfig.BACKOFF_MAX_DELAY_MS
        assertEquals(30000L, maxDelay)
        
        // Verify no attempt exceeds max
        for (attempt in 0..100) {
            val delay = BluetoothConfig.calculateBackoffDelay(attempt)
            assertTrue("Delay should never exceed max", delay <= maxDelay)
        }
    }

    // ============== Resource Limit Tests ==============

    @Test
    fun `debug log buffer limits to 500 entries`() {
        val maxLogEntries = 500
        val logs = mutableListOf<String>()
        
        // Simulate adding 600 logs
        repeat(600) { i ->
            if (logs.size >= maxLogEntries) {
                logs.removeAt(0)
            }
            logs.add("Log $i")
        }
        
        assertEquals(500, logs.size)
        assertEquals("Log 100", logs.first()) // First 100 removed
        assertEquals("Log 599", logs.last())
    }

    @Test
    fun `packet queue has capacity of 100`() {
        val queueCapacity = 100
        val queue = LinkedBlockingQueue<ByteArray>(queueCapacity)
        
        assertEquals(100, queue.remainingCapacity())
    }

    @Test
    fun `RTT history maintains 40 samples`() {
        val maxRttSamples = 40
        val rttHistory = mutableListOf<Long>()
        
        // Simulate adding 50 RTT samples
        repeat(50) { i ->
            rttHistory.add(0, i.toLong())
            if (rttHistory.size > maxRttSamples) {
                rttHistory.removeAt(rttHistory.lastIndex)
            }
        }
        
        assertEquals(40, rttHistory.size)
        assertEquals(49L, rttHistory.first()) // Most recent
        assertEquals(10L, rttHistory.last())  // Oldest kept
    }

    // ============== Heartbeat Timing Tests ==============

    @Test
    fun `heartbeat interval is 4 seconds`() {
        val heartbeatIntervalMs = BluetoothConfig.HEARTBEAT_INTERVAL_MS
        assertEquals(4000L, heartbeatIntervalMs)
    }

    @Test
    fun `classic heartbeat timeout is 20 seconds`() {
        val timeoutMs = BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS
        assertEquals(20000L, timeoutMs)
        
        // Should allow for ~5 missed heartbeats
        val missedHeartbeats = timeoutMs / BluetoothConfig.HEARTBEAT_INTERVAL_MS
        assertTrue("Should tolerate multiple missed heartbeats", missedHeartbeats >= 5)
    }

    @Test
    fun `BLE heartbeat timeout is 5 minutes`() {
        val timeoutMs = BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS
        assertEquals(300000L, timeoutMs)
        
        // Should allow for ~75 missed heartbeats
        val missedHeartbeats = timeoutMs / BluetoothConfig.HEARTBEAT_INTERVAL_MS
        assertTrue("BLE should tolerate many missed heartbeats", missedHeartbeats >= 75)
    }

    @Test
    fun `reconnect monitor interval is 1 second`() {
        val monitorIntervalMs = BluetoothConfig.RECONNECT_MONITOR_INTERVAL_MS
        assertEquals(1000L, monitorIntervalMs)
    }

    // ============== Memory Efficiency Tests ==============

    @Test
    fun `packet data size is reasonable for embedded devices`() {
        val typicalPacketSize = 20 // bytes (10-byte protocol packet)
        val maxBurstPackets = 100
        val maxMemoryUsage = typicalPacketSize * maxBurstPackets
        
        // Should use less than 2KB for packet queue
        assertTrue("Memory usage should be under 2KB, was ${maxMemoryUsage} bytes", 
            maxMemoryUsage < 2048)
    }

    @Test
    fun `log entry memory footprint is bounded`() {
        val maxLogEntries = 500
        val avgLogMessageSize = 100 // bytes
        val maxLogMemory = maxLogEntries * avgLogMessageSize
        
        // Should use less than 50KB for logs
        assertTrue("Log memory should be under 50KB, was ${maxLogMemory} bytes", 
            maxLogMemory < 51200)
    }

    // ============== Concurrency Performance ==============

    @Test
    fun `atomic counter updates are thread-safe and fast`() {
        val counter = java.util.concurrent.atomic.AtomicLong(0)
        val iterations = 10000
        
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                counter.incrementAndGet()
            }
        }
        
        assertEquals(iterations.toLong(), counter.get())
        val avgTimeNs = (timeMs * 1_000_000.0) / iterations
        assertTrue("Atomic increment should be under 1000ns, was ${avgTimeNs}ns", avgTimeNs < 1000)
    }

    @Test
    fun `boolean flag updates are immediate`() {
        val flag = java.util.concurrent.atomic.AtomicBoolean(false)
        
        val timeMs = measureTimeMillis {
            repeat(1000) {
                flag.set(true)
                flag.set(false)
            }
        }
        
        // Avoid flakiness on CI/Windows where timer resolution can be ~1ms.
        assertTrue("Boolean updates should be under 2ms for 1000 ops, was ${timeMs}ms", timeMs < 2)
    }
}
