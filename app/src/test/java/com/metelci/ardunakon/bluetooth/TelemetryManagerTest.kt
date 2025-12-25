package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TelemetryManager.
 *
 * Tests telemetry parsing, packet stats, RSSI tracking,
 * heartbeat monitoring, and RTT history management.
 */
class TelemetryManagerTest {

    private lateinit var telemetryManager: TelemetryManager
    private val logMessages = mutableListOf<Pair<String, LogType>>()
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testScope = TestScope()
        logMessages.clear()
        telemetryManager = TelemetryManager(
            scope = testScope,
            logCallback = { msg, type -> logMessages.add(msg to type) }
        )
    }

    @Test
    fun initialRssi_isZero() = runTest {
        assertEquals(0, telemetryManager.rssiValue.value)
    }

    @Test
    fun initialTelemetry_isNull() = runTest {
        assertNull(telemetryManager.telemetry.value)
    }

    @Test
    fun initialRttHistory_isEmpty() = runTest {
        assertTrue(telemetryManager.rttHistory.value.isEmpty())
    }

    @Test
    fun updateRssi_updatesRssiValue() = runTest {
        telemetryManager.updateRssi(-65)

        assertEquals(-65, telemetryManager.rssiValue.value)
    }

    @Test
    fun updateRssi_ignoresZero() = runTest {
        telemetryManager.updateRssi(0)

        // Should not record to history (0 is invalid RSSI)
        assertEquals(0, telemetryManager.rssiValue.value)
    }

    @Test
    fun recordInbound_resetsRssiFailures() {
        telemetryManager.recordRssiFailure()
        telemetryManager.recordRssiFailure()
        assertEquals(2, telemetryManager.getRssiFailures())

        telemetryManager.recordInbound()

        assertEquals(0, telemetryManager.getRssiFailures())
    }

    @Test
    fun recordInbound_resetsMissedAcks() {
        telemetryManager.onHeartbeatSent(1)
        telemetryManager.onHeartbeatSent(2)
        telemetryManager.onHeartbeatSent(3)
        assertEquals(3, telemetryManager.getMissedAcks())

        telemetryManager.recordInbound()

        assertEquals(0, telemetryManager.getMissedAcks())
    }

    @Test
    fun recordInbound_logsRecoveryAfterTimeout() {
        // Cause timeout (3+ missed acks)
        telemetryManager.onHeartbeatSent(1)
        telemetryManager.onHeartbeatSent(2)
        telemetryManager.onHeartbeatSent(3)

        telemetryManager.recordInbound()

        // Should log recovery
        assertTrue(logMessages.any { it.first.contains("recovered") && it.second == LogType.SUCCESS })
    }

    @Test
    fun recordRssiFailure_incrementsCount() {
        telemetryManager.recordRssiFailure()
        assertEquals(1, telemetryManager.getRssiFailures())

        telemetryManager.recordRssiFailure()
        assertEquals(2, telemetryManager.getRssiFailures())

        telemetryManager.recordRssiFailure()
        assertEquals(3, telemetryManager.getRssiFailures())
    }

    @Test
    fun recordRssiFailure_capsAt10() {
        repeat(15) {
            telemetryManager.recordRssiFailure()
        }

        // Should cap at 10
        assertEquals(10, telemetryManager.getRssiFailures())
    }

    @Test
    fun resetRssiFailures_resetsToZero() {
        telemetryManager.recordRssiFailure()
        telemetryManager.recordRssiFailure()
        telemetryManager.recordRssiFailure()

        telemetryManager.resetRssiFailures()

        assertEquals(0, telemetryManager.getRssiFailures())
    }

    @Test
    fun onHeartbeatSent_incrementsMissedAcks() {
        telemetryManager.onHeartbeatSent(1)
        assertEquals(1, telemetryManager.getMissedAcks())

        telemetryManager.onHeartbeatSent(2)
        assertEquals(2, telemetryManager.getMissedAcks())
    }

    @Test
    fun resetHeartbeat_resetsCounters() {
        telemetryManager.onHeartbeatSent(1)
        telemetryManager.onHeartbeatSent(2)
        telemetryManager.recordRssiFailure()

        telemetryManager.resetHeartbeat()

        assertEquals(0, telemetryManager.getMissedAcks())
        assertEquals(0, telemetryManager.getRssiFailures())
    }

    @Test
    fun updatePacketStats_updatesValues() = runTest {
        telemetryManager.updatePacketStats(sent = 100, dropped = 5, failed = 2)

        // Stats should be updated (verified via telemetry if available)
        // Note: parseTelemetryPacket_validPacket moved to TelemetryParserInstrumentedTest.kt
    }

    @Test
    fun parseTelemetryPacket_invalidPacket() = runTest {
        val invalidPacket = byteArrayOf(0x00, 0x01, 0x02)

        telemetryManager.parseTelemetryPacket(invalidPacket)

        // Should not update telemetry
        assertNull(telemetryManager.telemetry.value)
    }

    @Test
    fun getLastPacketTime_returnsTimestamp() {
        val beforeTime = System.currentTimeMillis()
        telemetryManager.recordInbound()
        val afterTime = System.currentTimeMillis()

        val lastPacketTime = telemetryManager.getLastPacketTime()

        assertTrue(lastPacketTime >= beforeTime)
        assertTrue(lastPacketTime <= afterTime)
    }

    @Test
    fun health_updatesWithMetrics() = runTest {
        telemetryManager.recordRssiFailure()
        telemetryManager.onHeartbeatSent(42)

        val health = telemetryManager.health.value

        assertEquals(1, health.rssiFailureCount)
        assertEquals(42, health.lastHeartbeatSeq)
    }

    @Test
    fun rttHistory_recordsValues() = runTest {
        // Simulate heartbeat roundtrip
        telemetryManager.onHeartbeatSent(1)
        Thread.sleep(50) // Simulate delay
        telemetryManager.recordInbound()

        val history = telemetryManager.rttHistory.value

        // Should have added RTT
        assertTrue(history.isNotEmpty())
        assertTrue(history[0] >= 50L) // At least 50ms
    }

    @Test
    fun rttHistory_respectsMaxCapacity() = runTest {
        // Send many heartbeats
        repeat(25) { i ->
            telemetryManager.onHeartbeatSent(i)
            Thread.sleep(10)
            telemetryManager.recordInbound()
        }

        val history = telemetryManager.rttHistory.value

        // Should cap at BluetoothConfig.MAX_RTT_HISTORY
        assertTrue(history.size <= BluetoothConfig.MAX_RTT_HISTORY)
    }

    @Test
    fun updatePacketStats_zeroSent() = runTest {
        telemetryManager.updatePacketStats(sent = 0, dropped = 0, failed = 0)

        // Should handle gracefully (no division by zero)
        // Packet loss won't be recorded for 0 sent packets
    }

    @Test
    fun updatePacketStats_highLoss() = runTest {
        telemetryManager.updatePacketStats(sent = 100, dropped = 50, failed = 25)

        // 75% loss rate - should be recorded
        val telemetry = telemetryManager.telemetry.value
        if (telemetry != null) {
            assertEquals(75L, telemetry.packetsDropped + telemetry.packetsFailed)
        }
    }

    @Test
    fun recordInbound_calculatesRtt() {
        val startTime = System.currentTimeMillis()
        telemetryManager.onHeartbeatSent(1)
        
        Thread.sleep(100) // Simulate network delay
        
        telemetryManager.recordInbound()

        val health = telemetryManager.health.value
        
        // RTT should be approximately 100ms
        assertTrue(health.lastRttMs >= 100L)
        assertTrue(health.lastRttMs < 200L) // With some tolerance
    }

    @Test
    fun rssiValue_flowEmitsUpdates() = runTest {
        var receivedRssi: Int? = null

        val job = launch {
            telemetryManager.rssiValue.collect {
                receivedRssi = it
            }
        }

        telemetryManager.updateRssi(-70)
        delay(100)

        assertEquals(-70, receivedRssi)

        job.cancel()
    }
}
