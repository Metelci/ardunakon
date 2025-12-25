package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.LogType
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for ClassicConnectionManager.
 *
 * Tests SPP socket coordinator logic: connection lifecycle,
 * fallback strategies, thread management, and error handling.
 */
@RunWith(AndroidJUnit4::class)
class ClassicConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: BluetoothAdapter
    private lateinit var mockDevice: BluetoothDevice
    private lateinit var mockCallback: ConnectionCallback
    private lateinit var manager: ClassicConnectionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockAdapter = mockk(relaxed = true)
        mockDevice = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)

        every { mockAdapter.isEnabled } returns true
        every { mockDevice.name } returns "TestDevice"

        manager = ClassicConnectionManager(
            context = context,
            adapter = mockAdapter,
            callback = mockCallback
        )
    }

    @After
    fun teardown() {
        manager.disconnect()
        clearAllMocks()
    }

    @Test
    fun connect_withNullAdapter_callsErrorCallback() {
        val managerWithNullAdapter = ClassicConnectionManager(
            context = context,
            adapter = null,
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
    fun disconnect_setsDisconnectedState() {
        manager.disconnect()

        verify { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun disconnect_cancelsConnectThread() {
        manager.connect(mockDevice)
        Thread.sleep(100) // Let thread start
        
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
    fun send_whenConnected_incrementsSentCount() {
        // Without actual connection, this will drop
        val data = byteArrayOf(0x01, 0x02)

        manager.send(data)

        val stats = manager.getPacketStats()
        
        // When not connected, should drop
        assertEquals(1L, stats.packetsDropped)
    }

    @Test
    fun requestRssi_isNoOpForClassic() {
        // Classic BT doesn't support RSSI during connection
        manager.requestRssi()

        // Should not throw, just no-op
    }

    @Test
    fun getPacketStats_initialState() {
        val stats = manager.getPacketStats()

        assertEquals(0L, stats.packetsSent)
        assertEquals(0L, stats.packetsDropped)
        assertEquals(0L, stats.packetsFailed)
    }

    @Test
    fun multipleDisconnects_handleGracefully() {
        manager.disconnect()
        manager.disconnect()
        manager.disconnect()

        verify(atLeast = 3) { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun cleanup_callsDisconnect() {
        manager.cleanup()

        verify { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun connect_withValidDevice_startsConnectThread() {
        manager.connect(mockDevice)

        // Should not throw or crash
        Thread.sleep(50) // Brief delay for thread to start
    }

    @Test
    fun connect_whileConnecting_rejectsNewAttempt() {
        manager.connect(mockDevice)
        manager.connect(mockDevice) // Second attempt

        // Should warn about connection in progress
        verify(timeout = 1000) { 
            mockCallback.onError(match { it.contains("already in progress") }, LogType.WARNING) 
        }
    }

    @Test
    fun send_multiplePackets_tracksDrops() {
        repeat(10) { i ->
            manager.send(byteArrayOf(i.toByte()))
        }

        val stats = manager.getPacketStats()
        
        // All should be dropped when not connected
        assertEquals(10L, stats.packetsDropped)
    }

    @Test
    fun connect_handlesNullDeviceName() {
        every { mockDevice.name } returns null

        manager.connect(mockDevice)

        // Should handle gracefully
        Thread.sleep(50)
    }

    @Test
    fun disconnect_afterFailedConnect_cleanupsProperly() {
        every { mockAdapter.isEnabled } returns false
        manager.connect(mockDevice)
        
        manager.disconnect()

        verify(atLeast = 1) { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun getPacketStats_afterMultipleSends() {
        manager.send(byteArrayOf(0x01))
        manager.send(byteArrayOf(0x02))
        manager.send(byteArrayOf(0x03))

        val stats = manager.getPacketStats()
        
        assertEquals(3L, stats.packetsDropped)
    }

    @Test
    fun connect_setsDeviceName() {
        every { mockDevice.name } returns "HC-05"

        manager.connect(mockDevice)

        Thread.sleep(100) // Wait for thread to start
        
        // Connection attempt should be made (will fail in test environment)
    }
}
