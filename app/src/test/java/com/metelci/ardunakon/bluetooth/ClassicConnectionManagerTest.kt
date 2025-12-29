package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.model.LogType
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClassicConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: BluetoothAdapter
    private lateinit var mockCallback: ConnectionCallback
    private lateinit var manager: ClassicConnectionManager
    private lateinit var mockDevice: BluetoothDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockAdapter = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        mockDevice = mockk(relaxed = true)

        manager = ClassicConnectionManager(context, mockAdapter, mockCallback)
    }

    @Test
    fun `connect fails if Bluetooth is not ready`() {
        every { mockAdapter.isEnabled } returns false

        manager.connect(mockDevice)

        verify { mockCallback.onStateChanged(ConnectionState.ERROR) }
        verify { mockCallback.onError("Bluetooth is off", LogType.ERROR) }
    }

    @Test
    fun `disconnect cleans up threads and updates state`() {
        manager.disconnect()

        verify { mockCallback.onStateChanged(ConnectionState.DISCONNECTED) }
    }

    @Test
    fun `send increments packetsDropped when not connected`() {
        val data = "test".toByteArray()
        manager.send(data)

        val stats = manager.getPacketStats()
        assertEquals(0L, stats.sent)
        assertEquals(1L, stats.dropped)
    }

    /*
     * Note: Testing the actual socket connection logic (ConnectThread/ConnectedThread)
     * is difficult in unit tests because they use blocking IO and native Android Bluetooth APIs
     * that are final/hard to mock deeply without instrumentation.
     * We focus on the manager logic here.
     */
}
