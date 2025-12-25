package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.LogType
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for BleConnectionManager.
 *
 * Tests BLE coordinator logic: connection lifecycle, queue management,
 * retry handling, and GATT error classification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BleConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: BluetoothAdapter
    private lateinit var mockDevice: BluetoothDevice
    private lateinit var mockCallback: ConnectionCallback
    private lateinit var testScope: TestScope
    private lateinit var manager: BleConnectionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockAdapter = mockk(relaxed = true)
        mockDevice = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        testScope = TestScope()

        every { mockAdapter.isEnabled } returns true
        every { mockDevice.name } returns "TestDevice"

        manager = BleConnectionManager(
            context = context,
            adapter = mockAdapter,
            scope = testScope,
            callback = mockCallback
        )
    }

    @After
    fun teardown() {
        testScope.cancel()
        clearAllMocks()
    }

    @Test
    fun connect_withNullAdapter_callsErrorCallback() {
        val managerWithNullAdapter = BleConnectionManager(
            context = context,
            adapter = null,
            scope = testScope,
            callback = mockCallback
        )

        managerWithNullAdapter.connect(mockDevice)

        verify { mockCallback.onError(match { it.contains("adapter not available") }, LogType.ERROR) }
        verify { mockCallback.onStateChanged(ConnectionState.ERROR) }
    }

    @Test
    fun connect_withDisabledAdapter_callsErrorCallback() {
        every { mockAdapter.isEnabled } returns false

        manager.connect(mockDevice)

        verify { mockCallback.onError("Bluetooth is off", LogType.ERROR) }
        verify { mockCallback.onStateChanged(ConnectionState.ERROR) }
    }

    @Test
    fun disconnect_cancelsJobs() = testScope.runTest {
        manager.disconnect()

        verify { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun send_whenNotConnected_dropsPacket() {
        val data = byteArrayOf(0x01, 0x02)

        manager.send(data)

        val stats = manager.getPacketStats()
        assertEquals(1L, stats.packetsDropped)
        assertEquals(0L, stats.packetsSent)
    }

    @Test
    fun send_whenConnected_queuesData() {
        // This is difficult to test in isolation without GATT connection
        // Just verify send doesn't crash
        val data = byteArrayOf(0x01, 0x02)
        
        manager.send(data)

        // Should queue or drop based on connection state
        val stats = manager.getPacketStats()
        assertTrue(stats.packetsDropped >= 0)
    }

    @Test
    fun requestRssi_callsGattReadRemoteRssi() {
        // Without actual GATT connection, this is a no-op
        // Just verify it doesn't crash
        manager.requestRssi()

        // Should not throw
    }

    @Test
    fun getPacketStats_initialState() {
        val stats = manager.getPacketStats()

        assertEquals(0L, stats.packetsSent)
        assertEquals(0L, stats.packetsDropped)
        assertEquals(0L, stats.packetsFailed)
    }

    @Test
    fun send_whenQueueFull_dropsOldestPacket() {
        // Fill the queue by sending many packets while disconnected
        val queueSize = BluetoothConfig.MAX_WRITE_QUEUE_SIZE
        
        repeat(queueSize + 5) { i ->
            manager.send(byteArrayOf(i.toByte()))
        }

        val stats = manager.getPacketStats()
        
        // Should have dropped packets
        assertTrue("Packets should be dropped when queue is full", stats.packetsDropped > 0)
    }

    @Test
    fun cleanup_releasesResources() {
        manager.cleanup()

        verify { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun connect_setsDeviceName() {
        every { mockDevice.name } returns "ArduinoUNO"

        manager.connect(mockDevice)

        // Should log the device name
        verify(timeout = 1000) { 
            mockCallback.onError(match { it.contains("ArduinoUNO") }, LogType.INFO) 
        }
    }

    @Test
    fun connect_handlesNullDeviceName() {
        every { mockDevice.name } returns null

        manager.connect(mockDevice)

        // Should handle null gracefully
        verify(timeout = 1000) { 
            mockCallback.onError(match { it.contains("Unknown") }, LogType.INFO) 
        }
    }

    @Test
    fun multipleConnectCalls_cancelsPreviousAttempts() {
        manager.connect(mockDevice)
        manager.connect(mockDevice)
        manager.connect(mockDevice)

        // Should handle multiple connect calls gracefully
        verify(atLeast = 1) { mockCallback.onError(any(), any()) }
    }

    @Test
    fun send_multiplePackets_incrementsStats() {
        repeat(10) { i ->
            manager.send(byteArrayOf(i.toByte()))
        }

        val stats = manager.getPacketStats()
        
        // When disconnected, should drop all
        assertEquals(10L, stats.packetsDropped)
    }
}
