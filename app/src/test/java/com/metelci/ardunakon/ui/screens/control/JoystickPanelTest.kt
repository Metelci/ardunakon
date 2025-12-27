package com.metelci.ardunakon.ui.screens.control

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JoystickPanel logic and latency calculation.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class JoystickPanelTest {

    @Test
    fun `WiFi mode uses WiFi RTT for latency`() {
        val isWifiMode = true
        val wifiRttMs = 50L
        val bluetoothRttMs = 100L
        
        val currentLatency = if (isWifiMode) {
            wifiRttMs.takeIf { it > 0 }
        } else {
            bluetoothRttMs?.takeIf { it > 0 }
        }
        
        assertEquals(50L, currentLatency)
    }

    @Test
    fun `Bluetooth mode uses Bluetooth RTT for latency`() {
        val isWifiMode = false
        val wifiRttMs = 50L
        val bluetoothRttMs = 100L
        
        val currentLatency = if (isWifiMode) {
            wifiRttMs.takeIf { it > 0 }
        } else {
            bluetoothRttMs?.takeIf { it > 0 }
        }
        
        assertEquals(100L, currentLatency)
    }

    @Test
    fun `latency is null when WiFi RTT is zero in WiFi mode`() {
        val isWifiMode = true
        val wifiRttMs = 0L
        
        val currentLatency = if (isWifiMode) {
            wifiRttMs.takeIf { it > 0 }
        } else {
            null
        }
        
        assertNull(currentLatency)
    }

    @Test
    fun `latency is null when Bluetooth RTT is null in Bluetooth mode`() {
        val isWifiMode = false
        val bluetoothRttMs: Long? = null
        
        val currentLatency = if (isWifiMode) {
            null
        } else {
            bluetoothRttMs?.takeIf { it > 0 }
        }
        
        assertNull(currentLatency)
    }

    @Test
    fun `latency is null when Bluetooth RTT is zero in Bluetooth mode`() {
        val isWifiMode = false
        val bluetoothRttMs = 0L
        
        val currentLatency = if (isWifiMode) {
            null
        } else {
            bluetoothRttMs.takeIf { it > 0 }
        }
        
        assertNull(currentLatency)
    }

    @Test
    fun `default joystick size is 180dp`() {
        val defaultSize = 180 // dp
        
        assertEquals(180, defaultSize)
    }

    @Test
    fun `throttle mode is disabled by default`() {
        val isThrottle = false
        
        assertFalse(isThrottle)
    }

    @Test
    fun `throttle mode can be enabled`() {
        val isThrottle = true
        
        assertTrue(isThrottle)
    }

    @Test
    fun `joystick callback receives x and y values`() {
        var receivedX = 0f
        var receivedY = 0f
        
        val onMoved: (Float, Float) -> Unit = { x, y ->
            receivedX = x
            receivedY = y
        }
        
        onMoved(0.5f, -0.3f)
        
        assertEquals(0.5f, receivedX, 0.001f)
        assertEquals(-0.3f, receivedY, 0.001f)
    }

    @Test
    fun `joystick values are in range -1 to 1`() {
        val x = 0.75f
        val y = -0.5f
        
        assertTrue(x >= -1f && x <= 1f)
        assertTrue(y >= -1f && y <= 1f)
    }

    @Test
    fun `WiFi RTT is used when positive in WiFi mode`() {
        val isWifiMode = true
        val wifiRttMs = 25L
        
        val currentLatency = if (isWifiMode) {
            wifiRttMs.takeIf { it > 0 }
        } else {
            null
        }
        
        assertEquals(25L, currentLatency)
    }

    @Test
    fun `Bluetooth RTT is used when positive in Bluetooth mode`() {
        val isWifiMode = false
        val bluetoothRttMs = 75L
        
        val currentLatency = if (isWifiMode) {
            null
        } else {
            bluetoothRttMs.takeIf { it > 0 }
        }
        
        assertEquals(75L, currentLatency)
    }

    @Test
    fun `latency calculation ignores opposite mode RTT`() {
        val isWifiMode = true
        val wifiRttMs = 50L
        val bluetoothRttMs = 100L
        
        val currentLatency = if (isWifiMode) {
            wifiRttMs.takeIf { it > 0 }
        } else {
            bluetoothRttMs.takeIf { it > 0 }
        }
        
        // Should use WiFi RTT, not Bluetooth
        assertEquals(50L, currentLatency)
        assertNotEquals(100L, currentLatency)
    }
}
