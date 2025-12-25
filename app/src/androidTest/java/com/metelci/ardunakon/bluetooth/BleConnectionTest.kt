package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metelci.ardunakon.model.LogType
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Integration tests for BleConnection.
 *
 * Tests GATT connection lifecycle, MTU negotiation, service discovery,
 * error handling, and write operations using mocked Android Bluetooth APIs.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BleConnectionTest {

    private lateinit var context: Context
    private lateinit var mockDevice: BluetoothDevice
    private lateinit var mockGatt: BluetoothGatt
    private lateinit var mockService: BluetoothGattService
    private lateinit var mockTxCharacteristic: BluetoothGattCharacteristic
    private lateinit var mockRxCharacteristic: BluetoothGattCharacteristic
    private lateinit var mockDescriptor: BluetoothGattDescriptor
    
    private lateinit var callbacks: BleConnection.Callbacks
    private lateinit var connection: BleConnection
    private lateinit var testScope: TestScope
    private lateinit var connectionMutex: Mutex

    private var gattCallback: BluetoothGattCallback? = null
    private val logMessages = mutableListOf<Pair<String, LogType>>()
    private var lastState: ConnectionState? = null
    private var connectedCalled = false
    private var disconnectReason: String? = null
    private val receivedData = mutableListOf<ByteArray>()
    private var lastRssi = 0
    private var lastMtu = 0

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        connectionMutex = Mutex(locked = true) // Start locked

        // Setup mocks
        mockDevice = mockk(relaxed = true) {
            every { name } returns "TestBLE"
            every { address } returns "00:11:22:33:44:55"
            every { connectGatt(any(), any(), any()) } answers {
                gattCallback = thirdArg()
                mockGatt
            }
        }

        mockGatt = mockk(relaxed = true) {
            every { device } returns mockDevice
            every { discoverServices() } returns true
            every { requestMtu(any()) } returns true
            every { requestConnectionPriority(any()) } returns true
            every { readRemoteRssi() } returns true
            every { setCharacteristicNotification(any(), any()) } returns true
            every { getService(any()) } returns mockService
            every { services } returns listOf(mockService)
        }

        mockService = mockk(relaxed = true) {
            every { uuid } returns BleUuidRegistry.HM10_SERVICE_UUID
            every { characteristics } returns listOf(mockRxCharacteristic, mockTxCharacteristic)
            every { getCharacteristic(BleUuidRegistry.HM10_RX_CHAR_UUID) } returns mockRxCharacteristic
            every { getCharacteristic(BleUuidRegistry.HM10_TX_CHAR_UUID) } returns mockTxCharacteristic
        }

        mockRxCharacteristic = mockk(relaxed = true) {
            every { uuid } returns BleUuidRegistry.HM10_RX_CHAR_UUID
            every { properties } returns (BluetoothGattCharacteristic.PROPERTY_WRITE or 
                                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
            every { service } returns mockService
        }

        mockTxCharacteristic = mockk(relaxed = true) {
            every { uuid } returns BleUuidRegistry.HM10_TX_CHAR_UUID
            every { properties } returns BluetoothGattCharacteristic.PROPERTY_NOTIFY
            every { service } returns mockService
            every { getDescriptor(BleUuidRegistry.CCCD_UUID) } returns mockDescriptor
        }

        mockDescriptor = mockk(relaxed = true) {
            every { uuid } returns BleUuidRegistry.CCCD_UUID
        }

        // Setup callbacks
        callbacks = object : BleConnection.Callbacks {
            override fun onConnected(connection: BleConnection) {
                connectedCalled = true
            }

            override fun onDisconnected(reason: String) {
                disconnectReason = reason
            }

            override fun onDataReceived(data: ByteArray) {
                receivedData.add(data)
            }

            override fun onStateChanged(state: ConnectionState) {
                lastState = state
            }

            override fun onRssiRead(rssi: Int) {
                lastRssi = rssi
            }

            override fun onMtuChanged(mtu: Int) {
                lastMtu = mtu
            }

            override fun log(message: String, type: LogType) {
                logMessages.add(message to type)
            }

            override fun checkBluetoothPermission(): Boolean = true

            override fun onHealthUpdate(seq: Int, packetAt: Long, failures: Int) {}

            override fun getSavedDevice(): BluetoothDeviceModel? = null

            override fun onReconnect(device: BluetoothDeviceModel, isAuto: Boolean) {}
        }

        connection = BleConnection(
            device = mockDevice,
            context = context,
            callbacks = callbacks,
            config = BluetoothConfig,
            scope = testScope,
            connectionMutex = connectionMutex
        )

        logMessages.clear()
        lastState = null
        connectedCalled = false
        disconnectReason = null
        receivedData.clear()
        lastRssi = 0
        lastMtu = 0
    }

    @After
    fun teardown() {
        connection.cancel()
        testScope.cancel()
    }

    @Test
    fun `successful connection triggers correct lifecycle callbacks`() = runTest {
        connection.connect()
        advanceTimeBy(600) // Wait past initial delay

        // Simulate GATT connected
        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        // Should request MTU and discover services
        verify { mockGatt.requestMtu(512) }
        verify { mockGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) }
        verify { mockGatt.discoverServices() }

        // State should be connected
        assertEquals(ConnectionState.CONNECTED, lastState)

        // Mutex should be unlocked
        assertFalse(connectionMutex.isLocked)

        // Should have logged connection
        assertTrue(logMessages.any { it.first.contains("Connected to GATT") && it.second == LogType.SUCCESS })
    }

    @Test
    fun `MTU negotiation success updates callback`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        // Simulate successful MTU change
        gattCallback?.onMtuChanged(mockGatt, 247, BluetoothGatt.GATT_SUCCESS)

        assertEquals(247, lastMtu)
        assertTrue(logMessages.any { it.first.contains("MTU changed to 247") })
    }

    @Test
    fun `MTU negotiation failure logs warning and continues`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mock,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        // Simulate MTU failure
        gattCallback?.onMtuChanged(mockGatt, 23, BluetoothGatt.GATT_FAILURE)

        assertEquals(0, lastMtu) // Should not update
        assertTrue(logMessages.any { 
            it.first.contains("MTU negotiation failed") && it.second == LogType.WARNING 
        })
    }

    @Test
    fun `service discovery finds HM-10 characteristics`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        // Simulate service discovery
        gattCallback?.onServicesDiscovered(mockGatt, BluetoothGatt.GATT_SUCCESS)

        // Should find HM-10 module
        assertTrue(logMessages.any { it.first.contains("BLE Module detected") })

        // Should setup notifications
        verify { mockGatt.setCharacteristicNotification(any(), true) }
    }

    @Test
    fun `service discovery failure logs error and disconnects`() = runTest {
        // Mock service not found
        every { mockGatt.getService(any()) } returns null
        every { mockGatt.services } returns emptyList()

        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        gattCallback?.onServicesDiscovered(mockGatt, BluetoothGatt.GATT_SUCCESS)

        // Should error
        assertEquals(ConnectionState.ERROR, lastState)
        assertTrue(logMessages.any { 
            it.first.contains("Service/Characteristic not found") && it.second == LogType.ERROR 
        })

        // Should disconnect and close
        verify { mockGatt.disconnect() }
        verify { mockGatt.close() }
    }

    @Test
    fun `RSSI polling reads signal strength periodically`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        gattCallback?.onServicesDiscovered(mockGatt, BluetoothGatt.GATT_SUCCESS)

        // Advance past RSSI polling interval (2000ms)
        advanceTimeBy(2100)

        // Should have called readRemoteRssi at least once
        verify(atLeast = 1) { mockGatt.readRemoteRssi() }

        // Simulate RSSI response
        gattCallback?.onReadRemoteRssi(mockGatt, -65, BluetoothGatt.GATT_SUCCESS)

        assertEquals(-65, lastRssi)
    }

    @Test
    fun `RSSI failures are tracked and trigger disconnect after threshold`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        // Fail RSSI reads
        repeat(5) {
            gattCallback?.onReadRemoteRssi(mockGatt, 0, BluetoothGatt.GATT_FAILURE)
        }

        // Should trigger disconnect
        assertEquals("RSSI polling failed 5 times", disconnectReason)
    }

    @Test
    fun `transient GATT error triggers retry`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        // Simulate transient error (GATT_ERROR = 133)
        gattCallback?.onConnectionStateChange(
            mockGatt,
            133, // Transient error
            BluetoothProfile.STATE_DISCONNECTED
        )

        // Should be in ERROR state
        assertEquals(ConnectionState.ERROR, lastState)

        // Should log retry attempt
        assertTrue(logMessages.any { it.first.contains("Transient GATT error") })

        // Should close current GATT
        verify(mockGatt).close()
    }

    @Test
    fun `permanent GATT error does not retry`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        // Simulate permanent error (GATT_ILLEGAL_PARAMETER = 135)
        gattCallback?.onConnectionStateChange(
            mockGatt,
            135, // Permanent error
            BluetoothProfile.STATE_DISCONNECTED
        )

        // Should be in ERROR state
        assertEquals(ConnectionState.ERROR, lastState)

        // Should log permanent error
        assertTrue(logMessages.any { it.first.contains("Permanent GATT error") })

        // Should not schedule retry
        assertFalse(logMessages.any { it.first.contains("Retrying") })

        verify(mockGatt).close()
    }

    @Test
    fun `disconnect clears RSSI and updates state`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        // Connect first
        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        lastRssi = -60 // Set some value

        // Now disconnect
        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_DISCONNECTED
        )

        assertEquals(ConnectionState.DISCONNECTED, lastState)
        assertEquals(0, lastRssi) // Should be cleared
        assertEquals("GATT disconnected", disconnectReason)

        verify(mockGatt).close()
    }

    @Test
    fun `characteristic changed callback delivers data`() = runTest {
        val testData = byteArrayOf(0xAA.toByte(), 0x01, 0x02, 0x55)

        connection.connect()
        advanceTimeBy(600)

        every { mockTxCharacteristic.value } returns testData

        // Simulate data notification
        @Suppress("DEPRECATION")
        gattCallback?.onCharacteristicChanged(mockGatt, mockTxCharacteristic)

        assertEquals(1, receivedData.size)
        assertArrayEquals(testData, receivedData[0])
    }

    @Test
    fun `write enqueues data to write queue`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        gattCallback?.onServicesDiscovered(mockGatt, BluetoothGatt.GATT_SUCCESS)

        // Simulate descriptor write completion
        gattCallback?.onDescriptorWrite(mockDescriptor, mockDescriptor, BluetoothGatt.GATT_SUCCESS)

        // Now write should work
        val packet = byteArrayOf(0xAA.toByte(), 0x01, 0x55)
        connection.write(packet)

        // Let write queue process
        advanceTimeBy(500)

        // Should have attempted write
        val stats = connection.getPacketStats()
        assertTrue(stats.first > 0) // packetsSent
    }

    @Test
    fun `cancel stops all operations and closes GATT`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        // Cancel connection
        connection.cancel()

        // Should disconnect and close
        verify { mockGatt.disconnect() }
        verify { mockGatt.close() }
    }

    @Test
    fun `packet stats are tracked correctly`() = runTest {
        connection.connect()
        advanceTimeBy(600)

        gattCallback?.onConnectionStateChange(
            mockGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        gattCallback?.onServicesDiscovered(mockGatt, BluetoothGatt.GATT_SUCCESS)
        gattCallback?.onDescriptorWrite(mockDescriptor, mockDescriptor, BluetoothGatt.GATT_SUCCESS)

        // Write several packets
        repeat(5) {
            connection.write(byteArrayOf(it.toByte()))
        }

        advanceTimeBy(500)

        val (sent, dropped, failed) = connection.getPacketStats()
        assertTrue(sent > 0)
        // Dropped and failed depend on queue behavior
    }
}
