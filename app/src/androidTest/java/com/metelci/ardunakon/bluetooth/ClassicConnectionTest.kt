package com.metelci.ardunakon.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Integration tests for ClassicConnection.
 *
 * Tests Classic Bluetooth SPP connection lifecycle, socket handling,
 * multi-strategy connection attempts, read loop, and error recovery.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ClassicConnectionTest {

    private lateinit var mockDevice: BluetoothDevice
    private lateinit var mockSocket: BluetoothSocket
    private lateinit var inputStream: ByteArrayInputStream
    private lateinit var outputStream: ByteArrayOutputStream
    
    private lateinit var callbacks: ClassicConnection.Callbacks
    private lateinit var connection: ClassicConnection
    private lateinit var testScope: TestScope
    private lateinit var connectionMutex: Mutex

    private val logMessages = mutableListOf<Pair<String, LogType>>()
    private var lastState: ConnectionState? = null
    private var connectedCalled = false
    private var disconnectReason: String? = null
    private val receivedData = mutableListOf<ByteArray>()

    @Before
    fun setup() {
        testScope = TestScope()
        connectionMutex = Mutex(locked = true)

        // Prepare input/output streams
        inputStream = ByteArrayInputStream(ByteArray(0))
        outputStream = ByteArrayOutputStream()

        // Mock BluetoothSocket
        mockSocket = mockk(relaxed = true) {
            every { isConnected } returns true
            every { inputStream } returns this@ClassicConnectionTest.inputStream
            every { outputStream } returns this@ClassicConnectionTest.outputStream
        }

        // Mock BluetoothDevice
        mockDevice = mockk(relaxed = true) {
            every { name } returns "TestClassic"
            every { address } returns "00:11:22:33:44:55"
            every { bondState } returns BluetoothDevice.BOND_BONDED
            every { createRfcommSocketToServiceRecord(any()) } returns mockSocket
            every { createInsecureRfcommSocketToServiceRecord(any()) } returns mockSocket
        }

        // Setup callbacks
        callbacks = object : ClassicConnection.Callbacks {
            override fun onConnected(connection: ClassicConnection) {
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

            override fun onRssiSnapshot() {}

            override fun log(message: String, type: LogType) {
                logMessages.add(message to type)
            }

            override fun checkBluetoothPermission(): Boolean = true
        }

        connection = ClassicConnection(
            device = mockDevice,
            callbacks = callbacks,
            config = BluetoothConfig,
            scope = testScope,
            connectionMutex = connectionMutex,
            reflectionFallbackEnabled = false,
            streamInitDelayMs = 0,
            connectionId = "test_classic"
        )

        logMessages.clear()
        lastState = null
        connectedCalled = false
        disconnectReason = null
        receivedData.clear()
    }

    @After
    fun teardown() {
        connection.cancel()
        testScope.cancel()
    }

    @Test
    fun `successful SPP connection triggers lifecycle callbacks`() = runTest {
        // Start connection in thread
        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        // Wait for connection attempt
        advanceTimeBy(1000)
        yield()

        // Verify socket creation attempted
        verify(atLeast = 1) { mockDevice.createRfcommSocketToServiceRecord(any()) }

        // Since we mocked socket as connected, should succeed
        assertTrue(connectedCalled || lastState == ConnectionState.CONNECTED)

        // Mutex should be unlocked
        advanceTimeBy(500)
        yield()
        assertFalse(connectionMutex.isLocked)

        job.cancel()
    }

    @Test
    fun `connection failure tries multiple UUID strategies`() = runTest {
        // Mock socket to fail connection
        every { mockSocket.connect() } throws IOException("Connection refused")

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(5000)
        yield()

        // Should have tried multiple UUIDs (HC-05 SPP, fallback UUIDs)
        verify(atLeast = 2) { mockDevice.createRfcommSocketToServiceRecord(any()) }

        // Should have logged attempts
        assertTrue(logMessages.size > 1)

        job.cancel()
    }

    @Test
    fun `read loop delivers incoming data to callback`() = runTest {
        val testData = byteArrayOf(0xAA.toByte(), 0x01, 0x02, 0x03, 0x55)
        inputStream = ByteArrayInputStream(testData)
        every { mockSocket.inputStream } returns inputStream

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        // Wait for read loop to process
        advanceTimeBy(2000)
        yield()

        // Should have received data
        assertTrue(receivedData.isNotEmpty())
        assertTrue(receivedData.any { it.contentEquals(testData) })

        job.cancel()
    }

    @Test
    fun `write sends data to output stream`() = runTest {
        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(1000)
        yield()

        // Write data
        val testPacket = byteArrayOf(0xAA.toByte(), 0x01, 0x55)
        connection.write(testPacket)

        advanceTimeBy(500)
        yield()

        // Should have written to stream
        val written = outputStream.toByteArray()
        assertTrue(written.contains(testPacket[0]))

        job.cancel()
    }

    @Test
    fun `socket IOException during read loop triggers disconnect`() = runTest {
        // Create failing input stream
        val failingStream = mockk<InputStream>(relaxed = true) {
            every { read(any()) } throws IOException("Socket closed")
        }
        every { mockSocket.inputStream } returns failingStream

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(2000)
        yield()

        // Should have disconnected
        assertNotNull(disconnectReason)
        assertTrue(disconnectReason?.contains("Socket") ?: false)

        // Should have closed socket
        verify(atLeast = 1) { mockSocket.close() }

        job.cancel()
    }

    @Test
    fun `cancel closes socket and stops read loop`() = runTest {
        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(1000)
        yield()

        // Cancel connection
        connection.cancel()

        advanceTimeBy(500)
        yield()

        // Should have closed socket
        verify(atLeast = 1) { mockSocket.close() }

        job.cancel()
    }

    @Test
    fun `packet stats track sent packets`() = runTest {
        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(1000)
        yield()

        // Write multiple packets
        repeat(5) {
            connection.write(byteArrayOf(it.toByte()))
        }

        advanceTimeBy(500)
        yield()

        val (sent, _, _) = connection.getPacketStats()
        assertTrue(sent >= 5)

        job.cancel()
    }

    @Test
    fun `connection state transitions from disconnected to connected`() = runTest {
        val stateChanges = mutableListOf<ConnectionState>()
        
        callbacks = object : ClassicConnection.Callbacks {
            override fun onConnected(connection: ClassicConnection) {}
            override fun onDisconnected(reason: String) {}
            override fun onDataReceived(data: ByteArray) {}
            override fun onStateChanged(state: ConnectionState) {
                stateChanges.add(state)
            }
            override fun onRssiSnapshot() {}
            override fun log(message: String, type: LogType) {}
            override fun checkBluetoothPermission(): Boolean = true
        }

        connection = ClassicConnection(
            device = mockDevice,
            callbacks = callbacks,
            config = BluetoothConfig,
            scope = testScope,
            connectionMutex = connectionMutex,
            reflectionFallbackEnabled = false,
            streamInitDelayMs = 0,
            connectionId = "test_states"
        )

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(2000)
        yield()

        // Should have transitioned to CONNECTED
        assertTrue(stateChanges.contains(ConnectionState.CONNECTED))

        job.cancel()
    }

    @Test
    fun `reflection fallback not used when disabled`() = runTest {
        // Mock normal socket to fail
        every { mockSocket.connect() } throws IOException("Normal socket failed")
        every { mockDevice.createRfcommSocketToServiceRecord(any()) } returns mockSocket

        connection = ClassicConnection(
            device = mockDevice,
            callbacks = callbacks,
            config = BluetoothConfig,
            scope = testScope,
            connectionMutex = connectionMutex,
            reflectionFallbackEnabled = false,
            streamInitDelayMs = 0,
            connectionId = "test_no_reflection"
        )

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(5000)
        yield()

        // Should NOT have logs about reflection attempts
        assertFalse(logMessages.any { it.first.contains("reflection", ignoreCase = true) })

        job.cancel()
    }

    @Test
    fun `secure and insecure socket strategies are both attempted`() = runTest {
        // Fail secure socket
        val secureSocket = mockk<BluetoothSocket>(relaxed = true) {
            every { connect() } throws IOException("Secure failed")
            every { isConnected } returns false
        }

        // Succeed on insecure
        val insecureSocket = mockk<BluetoothSocket>(relaxed = true) {
            every { connect() } answers {}
            every { isConnected } returns true
            every { inputStream } returns this@ClassicConnectionTest.inputStream
            every { outputStream } returns this@ClassicConnectionTest.outputStream
        }

        every { mockDevice.createRfcommSocketToServiceRecord(any()) } returns secureSocket
        every { mockDevice.createInsecureRfcommSocketToServiceRecord(any()) } returns insecureSocket

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(5000)
        yield()

        // Should have tried both secure and insecure
        verify(atLeast = 1) { mockDevice.createRfcommSocketToServiceRecord(any()) }
        verify(atLeast = 1) { mockDevice.createInsecureRfcommSocketToServiceRecord(any()) }

        job.cancel()
    }

    @Test
    fun `empty data not delivered to callback`() = runTest {
        inputStream = ByteArrayInputStream(byteArrayOf())
        every { mockSocket.inputStream } returns inputStream

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(2000)
        yield()

        // Should not have received empty data
        assertTrue(receivedData.isEmpty() || receivedData.all { it.isNotEmpty() })

        job.cancel()
    }

    @Test
    fun `multiple rapid writes are handled sequentially`() = runTest {
        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        advanceTimeBy(1000)
        yield()

        // Rapid writes
        val packets = (0..10).map { byteArrayOf(it.toByte()) }
        packets.forEach { connection.write(it) }

        advanceTimeBy(1000)
        yield()

        // All should be written
        val (sent, _, _) = connection.getPacketStats()
        assertEquals(packets.size.toLong(), sent)

        job.cancel()
    }

    @Test
    fun `connection timeout is respected`() = runTest {
        // Mock socket that never completes
        val hangingSocket = mockk<BluetoothSocket>(relaxed = true) {
            every { connect() } answers {
                Thread.sleep(30000) // Hang forever
            }
            every { isConnected } returns false
        }

        every { mockDevice.createRfcommSocketToServiceRecord(any()) } returns hangingSocket

        val job = testScope.launch(Dispatchers.IO) {
            connection.run()
        }

        // Don't wait forever - connection should timeout and try next strategy
        advanceTimeBy(20000)
        yield()

        // Should have moved on to other strategies or failed
        assertTrue(logMessages.isNotEmpty())

        job.cancel()
    }
}
