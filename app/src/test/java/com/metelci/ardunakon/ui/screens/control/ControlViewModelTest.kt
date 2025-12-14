package com.metelci.ardunakon.ui.screens.control

import android.view.View
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.data.ConnectionPreferences
import com.metelci.ardunakon.data.Profile
import com.metelci.ardunakon.data.ProfileManager
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.security.EncryptionException
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.coEvery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControlViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var bluetoothConnectionState: MutableStateFlow<ConnectionState>
    private lateinit var bluetoothEStop: MutableStateFlow<Boolean>
    private lateinit var bluetoothDebugLogs: MutableStateFlow<List<LogEntry>>
    private lateinit var bluetoothTelemetry: MutableStateFlow<com.metelci.ardunakon.bluetooth.Telemetry?>

    private lateinit var wifiConnectionState: MutableStateFlow<WifiConnectionState>
    private lateinit var wifiEncryptionError: MutableStateFlow<EncryptionException?>
    private lateinit var wifiAutoReconnect: MutableStateFlow<Boolean>

    private lateinit var bluetoothManager: AppBluetoothManager
    private lateinit var wifiManager: WifiManager
    private lateinit var profileManager: ProfileManager
    private lateinit var connectionPreferences: ConnectionPreferences

    private var viewModel: ControlViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        bluetoothConnectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
        bluetoothEStop = MutableStateFlow(false)
        bluetoothDebugLogs = MutableStateFlow(emptyList())
        bluetoothTelemetry = MutableStateFlow(null)

        wifiConnectionState = MutableStateFlow(WifiConnectionState.DISCONNECTED)
        wifiEncryptionError = MutableStateFlow(null)
        wifiAutoReconnect = MutableStateFlow(false)

        bluetoothManager = mockk(relaxed = true) {
            every { connectionState } returns bluetoothConnectionState
            every { isEmergencyStopActive } returns bluetoothEStop
            every { debugLogs } returns bluetoothDebugLogs
            every { telemetry } returns bluetoothTelemetry
        }

        wifiManager = mockk(relaxed = true) {
            every { connectionState } returns wifiConnectionState
            every { encryptionError } returns wifiEncryptionError
            every { autoReconnectEnabled } returns wifiAutoReconnect
        }

        profileManager = mockk(relaxed = true)
        connectionPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        viewModel?.setForegroundActive(false)
        Dispatchers.resetMain()
    }

    @Test
    fun init_restoresWifiConnectionModeWhenPrefsSayWifi() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "WIFI",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm

        testScheduler.advanceUntilIdle()

        assertEquals(ConnectionMode.WIFI, vm.connectionMode)
    }

    @Test
    fun observeConnectionState_resetsControlsOnBluetoothDisconnect() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns listOf(Profile(name = "p1", buttonConfigs = emptyList()))

        bluetoothConnectionState.value = ConnectionState.CONNECTED
        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        vm.leftJoystick = Pair(0.7f, -0.2f)
        vm.servoX = 1f
        vm.servoY = -1f

        bluetoothConnectionState.value = ConnectionState.DISCONNECTED
        testScheduler.advanceUntilIdle()

        assertEquals(Pair(0f, 0f), vm.leftJoystick)
        assertEquals(0f, vm.servoX)
        assertEquals(0f, vm.servoY)
    }

    @Test
    fun retryWithEncryption_clearsErrorAndRequiresEncryption() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        vm.requireEncryption = false
        vm.encryptionError = EncryptionException.NoSessionKeyException("bad")

        vm.retryWithEncryption()

        assertTrue(vm.requireEncryption)
        assertNull(vm.encryptionError)
        verify(exactly = 1) { wifiManager.clearEncryptionError() }
        // Called once in init (false), and once in retryWithEncryption() (true).
        verify(exactly = 1) { wifiManager.setRequireEncryption(false) }
        verify(exactly = 1) { wifiManager.setRequireEncryption(true) }
    }

    @Test
    fun continueWithoutEncryption_clearsErrorAndDisablesEncryptionRequirement() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "WIFI",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        vm.requireEncryption = true
        vm.encryptionError = EncryptionException.NoSessionKeyException("bad")

        vm.continueWithoutEncryption()

        assertTrue(!vm.requireEncryption)
        assertNull(vm.encryptionError)
        verify(exactly = 1) { wifiManager.clearEncryptionError() }
        // Called once in init, and once in continueWithoutEncryption().
        verify(exactly = 2) { wifiManager.setRequireEncryption(false) }
    }

    @Test
    fun dismissEncryptionError_clearsErrorAndDisconnectsWifi() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "WIFI",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        vm.encryptionError = EncryptionException.NoSessionKeyException("bad")

        vm.dismissEncryptionError()

        assertNull(vm.encryptionError)
        verify(exactly = 1) { wifiManager.clearEncryptionError() }
        verify(exactly = 1) { wifiManager.disconnect() }
    }

    @Test
    fun toggleEStop_whenInactive_enablesStopDisconnectsAndSendsPacket() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        bluetoothEStop.value = false
        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        val view = mockk<View>(relaxed = true)
        vm.toggleEStop(view)

        verify(exactly = 1) { bluetoothManager.setEmergencyStop(true) }
        verify(exactly = 1) { bluetoothManager.disconnectAllForEStop() }
        verify(exactly = 1) { bluetoothManager.sendDataToAll(any(), force = true) }
    }

    @Test
    fun toggleEStop_whenActive_disablesStopAndHoldsOfflineAfterReset() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        bluetoothEStop.value = true
        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        val view = mockk<View>(relaxed = true)
        vm.toggleEStop(view)

        verify(exactly = 1) { bluetoothManager.setEmergencyStop(false) }
        verify(exactly = 1) { bluetoothManager.holdOfflineAfterEStopReset() }
    }

    @Test
    fun handleServoCommand_CRASH_throws() = runTest {
        coEvery { connectionPreferences.loadLastConnection() } returns ConnectionPreferences.LastConnection(
            type = "BLUETOOTH",
            btAddress = null,
            wifiIp = null,
            wifiPort = 8888,
            autoReconnectWifi = false
        )
        coEvery { profileManager.loadProfiles() } returns emptyList()

        val vm = ControlViewModel(bluetoothManager, wifiManager, profileManager, connectionPreferences)
        viewModel = vm
        testScheduler.advanceUntilIdle()

        var thrown: Throwable? = null
        try {
            vm.handleServoCommand("CRASH")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is RuntimeException)
    }
}
