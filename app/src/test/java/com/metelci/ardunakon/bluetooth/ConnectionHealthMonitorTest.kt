package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Tests for ConnectionHealthMonitor - validates backoff, timeout detection, and circuit breaker logic.
 */
class ConnectionHealthMonitorTest {

    private lateinit var monitor: ConnectionHealthMonitor
    private lateinit var callbacks: TestHealthCallbacks

    class TestHealthCallbacks : ConnectionHealthMonitor.HealthCallbacks {
        private var _connectionState = ConnectionState.DISCONNECTED
        private var _connectionType: DeviceType? = DeviceType.CLASSIC
        private var _autoReconnectEnabled = true
        private var _hasSavedDevice = true
        
        var heartbeatTimeoutCalled = false
        var missedAckThresholdCalled = false
        var reconnectAttemptCalled = false
        var circuitBreakerCalled = false
        var lastReconnectAttempt = 0
        var lastReconnectDelay = 0L

        override fun onHeartbeatTimeout(reason: String) {
            heartbeatTimeoutCalled = true
        }

        override fun onMissedAckThresholdReached() {
            missedAckThresholdCalled = true
        }

        override fun onReconnectAttempt(attempt: Int, delayMs: Long) {
            reconnectAttemptCalled = true
            lastReconnectAttempt = attempt
            lastReconnectDelay = delayMs
        }

        override fun onCircuitBreakerTripped() {
            circuitBreakerCalled = true
        }

        override fun getConnectionState() = _connectionState
        override fun getConnectionType() = _connectionType
        override fun isAutoReconnectEnabled() = _autoReconnectEnabled
        override fun hasSavedDevice() = _hasSavedDevice
    }

    @Before
    fun setup() {
        callbacks = TestHealthCallbacks()
        monitor = ConnectionHealthMonitor(
            scope = CoroutineScope(Dispatchers.Unconfined),
            callbacks = callbacks
        )
    }

    // ============== Backoff Calculation Tests ==============

    @Test
    fun `backoff delay starts at base delay`() {
        val config = ConnectionHealthMonitor.HealthConfig()
        val delay = monitor.calculateBackoffDelay(0)
        assertEquals(config.backoffBaseDelayMs, delay)
    }

    @Test
    fun `backoff delay doubles each attempt`() {
        val base = BluetoothConfig.BACKOFF_BASE_DELAY_MS
        assertEquals(base * 1, monitor.calculateBackoffDelay(0))  // 3000
        assertEquals(base * 2, monitor.calculateBackoffDelay(1))  // 6000
        assertEquals(base * 4, monitor.calculateBackoffDelay(2))  // 12000
        assertEquals(base * 8, monitor.calculateBackoffDelay(3))  // 24000
    }

    @Test
    fun `backoff delay caps at max after 4 attempts`() {
        val delay4 = monitor.calculateBackoffDelay(4)
        val delay10 = monitor.calculateBackoffDelay(10)
        val delay100 = monitor.calculateBackoffDelay(100)
        
        // All should be capped at 24000 (3000 * 8)
        assertEquals(delay4, delay10)
        assertEquals(delay4, delay100)
    }

    // ============== Reconnect State Tests ==============

    @Test
    fun `initial reconnect attempts is zero`() {
        assertEquals(0, monitor.getReconnectAttempts())
    }

    @Test
    fun `scheduleNextReconnect increments attempts`() {
        monitor.scheduleNextReconnect()
        assertEquals(1, monitor.getReconnectAttempts())
        
        monitor.scheduleNextReconnect()
        assertEquals(2, monitor.getReconnectAttempts())
    }

    @Test
    fun `resetReconnectBackoff clears attempts`() {
        monitor.scheduleNextReconnect()
        monitor.scheduleNextReconnect()
        monitor.scheduleNextReconnect()
        assertEquals(3, monitor.getReconnectAttempts())
        
        monitor.resetReconnectBackoff()
        assertEquals(0, monitor.getReconnectAttempts())
    }

    @Test
    fun `canReconnectNow returns true initially`() {
        assertTrue(monitor.canReconnectNow())
    }

    @Test
    fun `canReconnectNow returns false immediately after scheduling`() {
        monitor.scheduleNextReconnect()
        assertFalse(monitor.canReconnectNow())
    }

    // ============== Circuit Breaker Tests ==============

    @Test
    fun `circuit breaker not tripped initially`() {
        assertFalse(monitor.isCircuitBreakerTripped())
    }

    @Test
    fun `circuit breaker trips after max attempts`() {
        val config = ConnectionHealthMonitor.HealthConfig()
        repeat(config.maxReconnectAttempts) {
            monitor.scheduleNextReconnect()
        }
        assertTrue(monitor.isCircuitBreakerTripped())
    }

    @Test
    fun `circuit breaker resets after resetReconnectBackoff`() {
        val config = ConnectionHealthMonitor.HealthConfig()
        repeat(config.maxReconnectAttempts) {
            monitor.scheduleNextReconnect()
        }
        assertTrue(monitor.isCircuitBreakerTripped())
        
        monitor.resetReconnectBackoff()
        assertFalse(monitor.isCircuitBreakerTripped())
    }

    // ============== Packet Recording Tests ==============

    @Test
    fun `recordInboundPacket resets reconnect backoff`() {
        monitor.scheduleNextReconnect()
        monitor.scheduleNextReconnect()
        assertEquals(2, monitor.getReconnectAttempts())
        
        monitor.recordInboundPacket()
        assertEquals(0, monitor.getReconnectAttempts())
    }

    // ============== Config Tests ==============

    @Test
    fun `default config uses BluetoothConfig values`() {
        val config = ConnectionHealthMonitor.HealthConfig()
        
        assertEquals(BluetoothConfig.HEARTBEAT_TIMEOUT_CLASSIC_MS, config.heartbeatTimeoutClassicMs)
        assertEquals(BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS, config.heartbeatTimeoutBleMs)
        assertEquals(BluetoothConfig.MISSED_ACK_THRESHOLD_CLASSIC, config.missedAckThresholdClassic)
        assertEquals(BluetoothConfig.MISSED_ACK_THRESHOLD_BLE, config.missedAckThresholdBle)
        assertEquals(BluetoothConfig.MAX_RECONNECT_ATTEMPTS, config.maxReconnectAttempts)
    }

    @Test
    fun `custom config overrides defaults`() {
        val customConfig = ConnectionHealthMonitor.HealthConfig(
            heartbeatTimeoutClassicMs = 5000L,
            maxReconnectAttempts = 5
        )
        
        assertEquals(5000L, customConfig.heartbeatTimeoutClassicMs)
        assertEquals(5, customConfig.maxReconnectAttempts)
        // Other values should still be defaults
        assertEquals(BluetoothConfig.HEARTBEAT_TIMEOUT_BLE_MS, customConfig.heartbeatTimeoutBleMs)
    }
}
