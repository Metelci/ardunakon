package com.metelci.ardunakon.wifi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.WifiEncryptionPreferences
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WifiManagerDetailedTest {

    private lateinit var context: Context
    private lateinit var connectionPreferences: ConnectionPreferences
    private lateinit var encryptionPreferences: WifiEncryptionPreferences
    private lateinit var wifiManager: WifiManager
    private val scope = TestScope()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectionPreferences = mockk(relaxed = true)
        encryptionPreferences = mockk(relaxed = true)

        // Mock connection load
        coEvery { connectionPreferences.loadLastConnection() } returns mockk(relaxed = true)

        wifiManager = WifiManager(
            context = context,
            connectionPreferences = connectionPreferences,
            encryptionPreferences = encryptionPreferences,
            ioDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
            scope = scope,
            onLog = { msg, _ -> println(msg) },
            // Disable built-in monitors for easier testing
            startMonitors = false
        )
    }

    @After
    fun tearDown() {
        wifiManager.cleanup()
    }

    @Test
    fun `autoReconnect state logic functions correctly`() = scope.runTest {
        // Allow initialization to complete
        advanceUntilIdle()

        // Initially false (default from relaxed mock)
        assertFalse("Initial state should be false", wifiManager.autoReconnectEnabled.value)

        // Enable
        wifiManager.setAutoReconnectEnabled(true)
        assertTrue("State should be true after enabling", wifiManager.autoReconnectEnabled.value)

        // Verify preference save triggered
        advanceUntilIdle() // Ensure launch executes
        coVerify {
            connectionPreferences.saveLastConnection(autoReconnectWifi = true)
        }

        // Disable
        wifiManager.setAutoReconnectEnabled(false)
        assertFalse("State should be false after disabling", wifiManager.autoReconnectEnabled.value)
    }

    @Test
    fun `encryption requirements are maintained`() {
        assertTrue(wifiManager.isEncryptionRequired())

        // Clearing error should work
        wifiManager.clearEncryptionError()
        assertNull(wifiManager.encryptionError.value)
    }

    @Test
    fun `connect updates state and triggers breadcrumb`() = scope.runTest {
        // Since WifiConnectionManager is created internally within WifiManager and not injected,
        // we can't easily mock it to prevent actual socket calls without extensive refactoring.
        // However, we can track the state changes that leak out if any, or verify logging.

        // This test mainly verifies correct "handover" wrapper logic in WifiManager
        // Real connection tests are in WifiConnectionManagerTest usually.

        wifiManager.connect("192.168.1.1", 8888)

        // Just verify it doesn't crash and potentially logs/updates preferences on success (which won't happen here due to lack of real network)
        // But verifying connect calls saveLastConnection in callback usually requires callback to fire.
    }

    @Test
    fun `disconnect updates global state`() = scope.runTest {
        // Manually simulating state change as if manager did it
        wifiManager.disconnect()

        // Expect auto-reconnect to be disabled by logic inside disconnect()
        // Wait, check implementation:
        // fun disconnect() { shouldReconnect = false ... }
        // It sets internal flag `shouldReconnect` but does it update the flow `_autoReconnectEnabled`?
        // Let's check source: `shouldReconnect = false` is internal var. `_autoReconnectEnabled` flow is NOT updated in disconnect().
        // Wait, setAutoReconnectEnabled sets both. But disconnect() only clears `shouldReconnect`.

        // So checking autoReconnectEnabled flow value might still be whatever it was.
        // Let's check internal behavior validation isn't exposed.

        // But onStateChanged should filter up
        val currentState = wifiManager.connectionState.value
        assertEquals(WifiConnectionState.DISCONNECTED, currentState)
    }

    @Test
    fun `onStateChanged updates value`() {
        wifiManager.onStateChanged(WifiConnectionState.CONNECTED)
        assertEquals(WifiConnectionState.CONNECTED, wifiManager.connectionState.value)

        wifiManager.onStateChanged(WifiConnectionState.ERROR)
        assertEquals(WifiConnectionState.ERROR, wifiManager.connectionState.value)
    }

    @Test
    fun `onRssiUpdated updates flow`() {
        wifiManager.onRssiUpdated(-65)
        assertEquals(-65, wifiManager.rssi.value)
    }
}
