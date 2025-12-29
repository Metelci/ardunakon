package com.metelci.ardunakon.bluetooth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionStateManagerTest {

    private lateinit var context: Context
    private lateinit var connectionStateManager: ConnectionStateManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectionStateManager = ConnectionStateManager(context)
    }

    @Test
    fun `initial state is DISCONNECTED`() {
        assertEquals(ConnectionState.DISCONNECTED, connectionStateManager.state.value)
        assertEquals(0, connectionStateManager.rssi.value)
        assertEquals(0L, connectionStateManager.health.value.lastPacketAt)
    }

    @Test
    fun `updateState updates state correctly`() {
        connectionStateManager.updateState(ConnectionState.CONNECTING)
        assertEquals(ConnectionState.CONNECTING, connectionStateManager.state.value)

        connectionStateManager.updateState(ConnectionState.CONNECTED)
        assertEquals(ConnectionState.CONNECTED, connectionStateManager.state.value)
    }

    @Test
    fun `updateState debounces rapid noisy changes`() {
        // Initial transition to CONNECTED
        connectionStateManager.updateState(ConnectionState.CONNECTED)

        // Immediate flip to DISCONNECTED (should be ignored due to debounce of 800ms)
        connectionStateManager.updateState(ConnectionState.DISCONNECTED)

        // State remains CONNECTED
        assertEquals(ConnectionState.CONNECTED, connectionStateManager.state.value)

        // Immediate flip to ERROR (should also be ignored)
        connectionStateManager.updateState(ConnectionState.ERROR)
        assertEquals(ConnectionState.CONNECTED, connectionStateManager.state.value)
    }

    @Test
    fun `updateRssi updates flow`() {
        connectionStateManager.updateRssi(-50)
        assertEquals(-50, connectionStateManager.rssi.value)
    }

    @Test
    fun `updateHealth updates health flow`() {
        val now = System.currentTimeMillis()
        connectionStateManager.updateHealth(seq = 10, packetAt = now, failures = 2, lastRttMs = 15)

        val health = connectionStateManager.health.value
        assertEquals(10, health.lastHeartbeatSeq)
        assertEquals(now, health.lastPacketAt)
        assertEquals(2, health.rssiFailureCount)
        assertEquals(15, health.lastRttMs)
    }

    @Test
    fun `addRttToHistory adds values and respects max size`() {
        // Add 5 values
        for (i in 1..5) {
            connectionStateManager.addRttToHistory(i.toLong())
        }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), connectionStateManager.rttHistory.value)

        // Fill up to max (BluetoothConfig.MAX_RTT_HISTORY = 40 usually)
        // Let's assume 40 from the view_file of BluetoothConfig if I checked it,
        // but typically sparklines are around 40-50.
        // Let's just add 100 values and check size.
        for (i in 6..100) {
            connectionStateManager.addRttToHistory(i.toLong())
        }

        assertTrue(connectionStateManager.rttHistory.value.size <= BluetoothConfig.MAX_RTT_HISTORY)
        // The last value should be 100
        assertEquals(100L, connectionStateManager.rttHistory.value.last())
    }

    @Test
    fun `reset clears all state`() {
        connectionStateManager.updateState(ConnectionState.CONNECTED)
        connectionStateManager.updateRssi(-70)
        connectionStateManager.updateHealth(1, 1000L, 0)

        connectionStateManager.reset()

        assertEquals(ConnectionState.DISCONNECTED, connectionStateManager.state.value)
        assertEquals(0, connectionStateManager.rssi.value)
        assertEquals(0L, connectionStateManager.health.value.lastPacketAt)
    }
}
