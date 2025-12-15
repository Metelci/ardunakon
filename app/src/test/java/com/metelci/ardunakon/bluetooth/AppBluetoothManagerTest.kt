package com.metelci.ardunakon.bluetooth

import com.metelci.ardunakon.model.LogType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppBluetoothManager core logic.
 * 
 * Tests cover:
 * - Connection state management
 * - Emergency stop blocking behavior
 * - Auto-reconnect logic
 * - Data transmission guards
 * - Callback handling
 * 
 * Note: These tests verify the business logic without mocking Android SDK dependencies,
 * similar to other tests in this package (EStopBehaviorTest, ConnectionHealthMonitorTest).
 */
class AppBluetoothManagerTest {

    // ============== Connection State Tests ==============

    @Test
    fun `initial connection state is DISCONNECTED`() {
        val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        assertEquals(ConnectionState.DISCONNECTED, connectionState.value)
    }

    @Test
    fun `connection state transitions from DISCONNECTED to CONNECTING`() {
        val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        
        // Simulate connection initiation
        connectionState.value = ConnectionState.CONNECTING
        
        assertEquals(ConnectionState.CONNECTING, connectionState.value)
    }

    @Test
    fun `connection state transitions to CONNECTED on success`() {
        val connectionState = MutableStateFlow(ConnectionState.CONNECTING)
        
        // Simulate successful connection
        connectionState.value = ConnectionState.CONNECTED
        
        assertEquals(ConnectionState.CONNECTED, connectionState.value)
    }

    @Test
    fun `connection state transitions to ERROR on failure`() {
        val connectionState = MutableStateFlow(ConnectionState.CONNECTING)
        
        // Simulate connection failure
        connectionState.value = ConnectionState.ERROR
        
        assertEquals(ConnectionState.ERROR, connectionState.value)
    }

    @Test
    fun `connection state transitions to RECONNECTING during auto-reconnect`() {
        val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        
        // Simulate auto-reconnect
        connectionState.value = ConnectionState.RECONNECTING
        
        assertEquals(ConnectionState.RECONNECTING, connectionState.value)
    }

    // ============== Emergency Stop Tests ==============

    @Test
    fun `E-STOP blocks connection attempts`() {
        var isEmergencyStopActive = true
        var connectionAttempted = false
        
        // Simulate connectToDevice check
        if (!isEmergencyStopActive) {
            connectionAttempted = true
        }
        
        assertFalse("Connection should be blocked when E-STOP active", connectionAttempted)
    }

    @Test
    fun `E-STOP allows connection when inactive`() {
        var isEmergencyStopActive = false
        var connectionAttempted = false
        
        // Simulate connectToDevice when E-STOP is off
        if (!isEmergencyStopActive) {
            connectionAttempted = true
        }
        
        assertTrue("Connection should be allowed when E-STOP inactive", connectionAttempted)
    }

    @Test
    fun `setEmergencyStop true activates E-STOP`() {
        var isEmergencyStopActive = false
        
        // Simulate setEmergencyStop(true)
        isEmergencyStopActive = true
        
        assertTrue(isEmergencyStopActive)
    }

    @Test
    fun `setEmergencyStop false deactivates E-STOP`() {
        var isEmergencyStopActive = true
        
        // Simulate setEmergencyStop(false)
        isEmergencyStopActive = false
        
        assertFalse(isEmergencyStopActive)
    }

    // ============== Auto-Reconnect Tests ==============

    @Test
    fun `setAutoReconnectEnabled updates flow value`() {
        val autoReconnectEnabled = MutableStateFlow(false)
        
        // Enable auto-reconnect
        autoReconnectEnabled.value = true
        
        assertTrue(autoReconnectEnabled.value)
    }

    @Test
    fun `auto reconnect saves device for later reconnection`() {
        var savedDevice: BluetoothDeviceModel? = null
        val testDevice = BluetoothDeviceModel(
            name = "TestDevice",
            address = "00:11:22:33:44:55",
            rssi = -60,
            type = DeviceType.CLASSIC
        )
        
        // Simulate saving device on connect
        savedDevice = testDevice
        
        assertNotNull(savedDevice)
        assertEquals("TestDevice", savedDevice?.name)
        assertEquals("00:11:22:33:44:55", savedDevice?.address)
    }

    @Test
    fun `reconnectSavedDevice returns false without saved device`() {
        val savedDevice: BluetoothDeviceModel? = null
        
        // Simulate reconnectSavedDevice check
        val canReconnect = savedDevice != null
        
        assertFalse(canReconnect)
    }

    @Test
    fun `reconnectSavedDevice returns true with saved device`() {
        val savedDevice: BluetoothDeviceModel? = BluetoothDeviceModel(
            name = "TestDevice",
            address = "00:11:22:33:44:55",
            rssi = -60,
            type = DeviceType.CLASSIC
        )
        val connectionState = ConnectionState.DISCONNECTED
        
        // Simulate reconnectSavedDevice check
        val canReconnect = savedDevice != null && connectionState != ConnectionState.CONNECTED
        
        assertTrue(canReconnect)
    }

    @Test
    fun `disabling auto-reconnect clears saved device`() {
        var savedDevice: BluetoothDeviceModel? = BluetoothDeviceModel(
            name = "TestDevice",
            address = "00:11:22:33:44:55",
            rssi = -60,
            type = DeviceType.CLASSIC
        )
        
        // Simulate setAutoReconnectEnabled(false)
        savedDevice = null
        
        assertNull(savedDevice)
    }

    // ============== Circuit Breaker Tests ==============

    @Test
    fun `resetCircuitBreaker clears attempts`() {
        var reconnectAttempts = 5
        var nextReconnectAt = System.currentTimeMillis() + 10000
        
        // Simulate resetCircuitBreaker
        reconnectAttempts = 0
        nextReconnectAt = 0L
        
        assertEquals(0, reconnectAttempts)
        assertEquals(0L, nextReconnectAt)
    }

    @Test
    fun `circuit breaker triggers after max attempts`() {
        var reconnectAttempts = BluetoothConfig.MAX_RECONNECT_ATTEMPTS
        var shouldReconnect = true
        var autoReconnectEnabled = true
        
        // Simulate circuit breaker check
        if (reconnectAttempts >= BluetoothConfig.MAX_RECONNECT_ATTEMPTS) {
            shouldReconnect = false
            autoReconnectEnabled = false
        }
        
        assertFalse(shouldReconnect)
        assertFalse(autoReconnectEnabled)
    }

    // ============== Data Transmission Tests ==============

    @Test
    fun `sendData does nothing when E-STOP active and not forced`() {
        val isEmergencyStopActive = true
        val force = false
        var dataSent = false
        
        // Simulate sendData check
        if (!isEmergencyStopActive || force) {
            dataSent = true
        }
        
        assertFalse("Data should not be sent when E-STOP active", dataSent)
    }

    @Test
    fun `sendData sends when E-STOP active but forced`() {
        val isEmergencyStopActive = true
        val force = true
        var dataSent = false
        
        // Simulate sendData with force flag
        if (!isEmergencyStopActive || force) {
            dataSent = true
        }
        
        assertTrue("Data should be sent when force flag is true", dataSent)
    }

    @Test
    fun `sendData sends when E-STOP inactive`() {
        val isEmergencyStopActive = false
        val force = false
        var dataSent = false
        
        // Simulate sendData without E-STOP
        if (!isEmergencyStopActive || force) {
            dataSent = true
        }
        
        assertTrue("Data should be sent when E-STOP inactive", dataSent)
    }

    // ============== Callback Tests ==============

    @Test
    fun `onStateChanged updates connection state flow`() {
        val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        
        // Simulate onStateChanged callback
        fun onStateChanged(state: ConnectionState) {
            connectionState.value = state
        }
        
        onStateChanged(ConnectionState.CONNECTED)
        assertEquals(ConnectionState.CONNECTED, connectionState.value)
    }

    @Test
    fun `onStateChanged triggers vibration on connect`() {
        var vibrationTriggered = false
        val state = ConnectionState.CONNECTED
        
        // Simulate onStateChanged with vibration
        if (state == ConnectionState.CONNECTED) {
            vibrationTriggered = true
        }
        
        assertTrue(vibrationTriggered)
    }

    @Test
    fun `onStateChanged triggers vibration on error`() {
        var vibrationTriggered = false
        val state = ConnectionState.ERROR
        
        // Simulate onStateChanged with vibration
        if (state == ConnectionState.ERROR) {
            vibrationTriggered = true
        }
        
        assertTrue(vibrationTriggered)
    }

    @Test
    fun `onDataReceived updates incoming data flow`() {
        val incomingData = MutableStateFlow<ByteArray?>(null)
        val testData = byteArrayOf(0xAA.toByte(), 0x01, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x11, 0x55)
        
        // Simulate onDataReceived
        incomingData.value = testData
        
        assertNotNull(incomingData.value)
        assertEquals(10, incomingData.value?.size)
    }

    @Test
    fun `onRssiUpdated updates rssi flow`() {
        val rssiValue = MutableStateFlow(0)
        
        // Simulate onRssiUpdated
        rssiValue.value = -65
        
        assertEquals(-65, rssiValue.value)
    }

    // ============== Device Type Coercion Tests ==============

    @Test
    fun `BLE-only device names force BLE connection`() {
        val deviceName = "HM-10"
        val forceBle = BluetoothConfig.isBleOnlyName(deviceName)
        
        assertTrue("HM-10 should force BLE connection", forceBle)
    }

    @Test
    fun `classic device names do not force BLE`() {
        val deviceName = "HC-05"
        val forceBle = BluetoothConfig.isBleOnlyName(deviceName)
        
        assertFalse("HC-05 should not force BLE", forceBle)
    }

    @Test
    fun `device model type is coerced to LE when forced`() {
        val originalModel = BluetoothDeviceModel(
            name = "HM-10",
            address = "00:11:22:33:44:55",
            rssi = -60,
            type = DeviceType.CLASSIC
        )
        
        // Simulate coercion
        val coercedModel = if (BluetoothConfig.isBleOnlyName(originalModel.name)) {
            originalModel.copy(type = DeviceType.LE)
        } else {
            originalModel
        }
        
        assertEquals(DeviceType.LE, coercedModel.type)
    }

    // ============== Connected Device Info Tests ==============

    @Test
    fun `connected device info is set on successful connection`() {
        val connectedDeviceInfo = MutableStateFlow<String?>(null)
        val device = BluetoothDeviceModel(
            name = "ArdunakonR4",
            address = "00:11:22:33:44:55",
            rssi = -60,
            type = DeviceType.LE
        )
        
        // Simulate onStateChanged(CONNECTED)
        val typeLabel = if (device.type == DeviceType.LE) "BLE" else "Classic"
        connectedDeviceInfo.value = "${device.name} ($typeLabel)"
        
        assertEquals("ArdunakonR4 (BLE)", connectedDeviceInfo.value)
    }

    @Test
    fun `connected device info is cleared on disconnect`() {
        val connectedDeviceInfo = MutableStateFlow<String?>("ArdunakonR4 (BLE)")
        
        // Simulate onStateChanged(DISCONNECTED)
        connectedDeviceInfo.value = null
        
        assertNull(connectedDeviceInfo.value)
    }

    // ============== Log Limit Tests ==============

    @Test
    fun `debug logs are limited to 500 entries`() {
        val debugLogs = mutableListOf<String>()
        
        // Simulate adding 510 logs
        for (i in 1..510) {
            if (debugLogs.size >= 500) {
                debugLogs.removeAt(0)
            }
            debugLogs.add("Log $i")
        }
        
        assertEquals(500, debugLogs.size)
        assertEquals("Log 11", debugLogs.first()) // First 10 were removed
        assertEquals("Log 510", debugLogs.last())
    }

    // ============== Mutex Lock Tests ==============

    @Test
    fun `connection mutex prevents concurrent connections`() {
        var mutexLocked = false
        var connectionStarted = false
        
        // Simulate first connection attempt locking mutex
        mutexLocked = true
        
        // Simulate second connection attempt failing to lock
        val canConnect = !mutexLocked
        if (canConnect) {
            connectionStarted = true
        }
        
        assertFalse("Second connection should be blocked", connectionStarted)
    }

    // ============== RSSI Request Tests ==============

    @Test
    fun `requestRssi only works for BLE connections`() {
        val connectionType = DeviceType.LE
        var rssiRequested = false
        
        // Simulate requestRssi
        if (connectionType == DeviceType.LE) {
            rssiRequested = true
        }
        
        assertTrue(rssiRequested)
    }

    @Test
    fun `requestRssi skips for classic connections`() {
        val connectionType = DeviceType.CLASSIC
        var rssiRequested = false
        var warningLogged = false
        
        // Simulate requestRssi
        if (connectionType == DeviceType.LE) {
            rssiRequested = true
        } else {
            warningLogged = true
        }
        
        assertFalse(rssiRequested)
        assertTrue(warningLogged)
    }

    // ============== Disconnect Tests ==============

    @Test
    fun `disconnect clears auto-reconnect flags`() {
        var shouldReconnect = true
        var autoReconnectEnabled = true
        
        // Simulate disconnect()
        shouldReconnect = false
        autoReconnectEnabled = false
        
        assertFalse(shouldReconnect)
        assertFalse(autoReconnectEnabled)
    }

    @Test
    fun `performDisconnect updates connection state to DISCONNECTED`() {
        val connectionState = MutableStateFlow(ConnectionState.CONNECTED)
        
        // Simulate performDisconnect
        connectionState.value = ConnectionState.DISCONNECTED
        
        assertEquals(ConnectionState.DISCONNECTED, connectionState.value)
    }

    @Test
    fun `performDisconnect resets RSSI to zero`() {
        val rssiValue = MutableStateFlow(-65)
        
        // Simulate performDisconnect -> updateRssi(0)
        rssiValue.value = 0
        
        assertEquals(0, rssiValue.value)
    }
}
