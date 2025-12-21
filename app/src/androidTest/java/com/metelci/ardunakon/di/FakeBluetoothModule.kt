package com.metelci.ardunakon.di

import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.bluetooth.BluetoothDeviceModel
import com.metelci.ardunakon.bluetooth.BluetoothScanner
import com.metelci.ardunakon.bluetooth.ConnectionHealth
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.bluetooth.TelemetryManager
import com.metelci.ardunakon.model.LogEntry
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BluetoothModule::class]
)
object FakeBluetoothModule {

    @Provides
    @Singleton
    fun provideAppBluetoothManager(): AppBluetoothManager {
        val mock = mockk<AppBluetoothManager>(relaxed = true)

        // Define stable flows
        val connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
        val rssiValueFlow = MutableStateFlow(0)
        val healthFlow = MutableStateFlow(ConnectionHealth())
        val debugLogsFlow = MutableStateFlow(emptyList<LogEntry>())
        val telemetryFlow = MutableStateFlow<Telemetry?>(null)
        val autoReconnectFlow = MutableStateFlow(true)
        val isEmergencyStopFlow = MutableStateFlow(false)
        val rttHistoryFlow = MutableStateFlow(emptyList<Long>())
        val scannedDevicesFlow = MutableStateFlow(emptyList<BluetoothDeviceModel>())
        val incomingDataFlow = MutableStateFlow<ByteArray?>(null)

        // Mock StateFlows accessed by ControlScreen
        every { mock.connectionState } returns connectionStateFlow
        every { mock.rssiValue } returns rssiValueFlow
        every { mock.health } returns healthFlow
        every { mock.debugLogs } returns debugLogsFlow
        every { mock.telemetry } returns telemetryFlow
        every { mock.autoReconnectEnabled } returns autoReconnectFlow
        every { mock.isEmergencyStopActive } returns isEmergencyStopFlow
        every { mock.rttHistory } returns rttHistoryFlow
        every { mock.scannedDevices } returns scannedDevicesFlow
        every { mock.incomingData } returns incomingDataFlow

        // Mock History Manager
        val historyMock = mockk<TelemetryHistoryManager>(relaxed = true)
        every { mock.telemetryHistoryManager } returns historyMock

        return mock
    }

    @Provides
    @Singleton
    fun provideBluetoothScanner(): BluetoothScanner {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTelemetryManager(): TelemetryManager {
        return mockk(relaxed = true)
    }
}
