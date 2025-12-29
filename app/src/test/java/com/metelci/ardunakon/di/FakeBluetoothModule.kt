package com.metelci.ardunakon.di

import com.metelci.ardunakon.bluetooth.IBluetoothManager
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BluetoothModule::class]
)
object FakeBluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(): IBluetoothManager {
        val mock = mockk<IBluetoothManager>(relaxed = true)
        every { mock.connectionState } returns TestBluetoothState.connectionState
        every { mock.rssiValue } returns TestBluetoothState.rssiValue
        every { mock.health } returns TestBluetoothState.health
        every { mock.telemetry } returns TestBluetoothState.telemetry
        every { mock.rttHistory } returns TestBluetoothState.rttHistory
        every { mock.telemetryHistoryManager } returns mockk<TelemetryHistoryManager>(relaxed = true)
        return mock
    }
}
