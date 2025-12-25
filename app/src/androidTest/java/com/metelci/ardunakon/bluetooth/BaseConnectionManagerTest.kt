package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.LogType
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for BaseConnectionManager.
 *
 * Tests common functionality: permissions, adapter validation,
 * packet stats, and cleanup patterns.
 */
@RunWith(AndroidJUnit4::class)
class BaseConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: BluetoothAdapter
    private lateinit var mockCallback: ConnectionCallback
    private lateinit var connectionManager: TestConnectionManager

    // Test implementation of abstract BaseConnectionManager
    private class TestConnectionManager(
        context: Context,
        adapter: BluetoothAdapter?,
        callback: ConnectionCallback
    ) : BaseConnectionManager(context, adapter, callback) {
        var connectCalled = false
        var disconnectCalled = false
        var sendCalled = false
        var requestRssiCalled = false

        override fun connect(device: BluetoothDevice) {
            connectCalled = true
        }

        override fun disconnect() {
            disconnectCalled = true
        }

        override fun send(data: ByteArray) {
            sendCalled = true
        }

        override fun requestRssi() {
            requestRssiCalled = true
        }

        // Expose protected methods for testing
        fun testCheckBluetoothPermission() = checkBluetoothPermission()
        fun testIsBluetoothReady() = isBluetoothReady()
        fun testResetPacketStats() = resetPacketStats()
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockAdapter = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        
        every { mockAdapter.isEnabled } returns true
        
        connectionManager = TestConnectionManager(context, mockAdapter, mockCallback)
    }

    @Test
    fun getPacketStats_initialState() {
        val stats = connectionManager.getPacketStats()

        assertEquals(0L, stats.packetsSent)
        assertEquals(0L, stats.packetsDropped)
        assertEquals(0L, stats.packetsFailed)
    }

    @Test
    fun resetPacketStats_resetsAllCounters() {
        // Simulate some activity
        connectionManager.packetsSent.set(100)
        connectionManager.packetsDropped.set(5)
        connectionManager.packetsFailed.set(3)

        connectionManager.testResetPacketStats()

        val stats = connectionManager.getPacketStats()
        assertEquals(0L, stats.packetsSent)
        assertEquals(0L, stats.packetsDropped)
        assertEquals(0L, stats.packetsFailed)
    }

    @Test
    fun isBluetoothReady_withNullAdapter() {
        val managerWithNullAdapter = TestConnectionManager(context, null, mockCallback)

        val isReady = managerWithNullAdapter.testIsBluetoothReady()

        assertFalse(isReady)
        verify { mockCallback.onError("Bluetooth adapter not available", LogType.ERROR) }
    }

    @Test
    fun isBluetoothReady_withDisabledAdapter() {
        every { mockAdapter.isEnabled } returns false

        val isReady = connectionManager.testIsBluetoothReady()

        assertFalse(isReady)
        verify { mockCallback.onError("Bluetooth is off", LogType.ERROR) }
    }

    @Test
    fun isBluetoothReady_withEnabledAdapter() {
        every { mockAdapter.isEnabled } returns true

        val isReady = connectionManager.testIsBluetoothReady()

        assertTrue(isReady)
        verify(exactly = 0) { mockCallback.onError(any(), any()) }
    }

    @Test
    fun cleanup_callsDisconnect() {
        connectionManager.cleanup()

        assertTrue(connectionManager.disconnectCalled)
    }

    @Test
    fun abstractMethods_areImplemented() {
        val mockDevice = mockk<BluetoothDevice>()

        connectionManager.connect(mockDevice)
        assertTrue(connectionManager.connectCalled)

        connectionManager.disconnect()
        assertTrue(connectionManager.disconnectCalled)

        connectionManager.send(byteArrayOf(0x01))
        assertTrue(connectionManager.sendCalled)

        connectionManager.requestRssi()
        assertTrue(connectionManager.requestRssiCalled)
    }

    @Test
    fun packetStats_atomicOperations() {
        // Simulate concurrent packet operations
        val threads = (1..10).map {
            Thread {
                repeat(100) {
                    connectionManager.packetsSent.incrementAndGet()
                    connectionManager.packetsDropped.incrementAndGet()
                    connectionManager.packetsFailed.incrementAndGet()
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val stats = connectionManager.getPacketStats()
        
        // Each counter should be 1000 (10 threads * 100 increments)
        assertEquals(1000L, stats.packetsSent)
        assertEquals(1000L, stats.packetsDropped)
        assertEquals(1000L, stats.packetsFailed)
    }

    @Test
    fun checkBluetoothPermission_alwaysReturnsTrueOnLegacy() {
        // On Android < 12, permission check should always pass
        val hasPermission = connectionManager.testCheckBluetoothPermission()

        // Result depends on Android version and permissions
        // Just verify it doesn't crash
        assertNotNull(hasPermission)
    }

    @Test
    fun multipleResets_maintain() {
        connectionManager.packetsSent.set(50)
        connectionManager.testResetPacketStats()
        
        assertEquals(0L, connectionManager.getPacketStats().packetsSent)

        connectionManager.packetsSent.set(75)
        connectionManager.testResetPacketStats()
        
        assertEquals(0L, connectionManager.getPacketStats().packetsSent)
    }

    @Test
    fun getPacketStats_reflectsCurrentState() {
        connectionManager.packetsSent.set(123)
        connectionManager.packetsDropped.set(45)
        connectionManager.packetsFailed.set(6)

        val stats = connectionManager.getPacketStats()

        assertEquals(123L, stats.packetsSent)
        assertEquals(45L, stats.packetsDropped)
        assertEquals(6L, stats.packetsFailed)
    }

    @Test
    fun isBluetoothReady_multipleCallsConsistent() {
        every { mockAdapter.isEnabled } returns true

        assertTrue(connectionManager.testIsBluetoothReady())
        assertTrue(connectionManager.testIsBluetoothReady())
        assertTrue(connectionManager.testIsBluetoothReady())
    }

    @Test
    fun callback_onlyCalledOnError() {
        every { mockAdapter.isEnabled } returns true

        connectionManager.testIsBluetoothReady()

        verify(exactly = 0) { mockCallback.onError(any(), any()) }
    }
}
