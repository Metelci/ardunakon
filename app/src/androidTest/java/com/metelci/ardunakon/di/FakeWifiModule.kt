package com.metelci.ardunakon.di

import com.metelci.ardunakon.wifi.WifiManager
import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import com.metelci.ardunakon.security.EncryptionException
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [WifiModule::class]
)
object FakeWifiModule {

    @Provides
    @Singleton
    fun provideWifiManager(): WifiManager {
        val mock = mockk<WifiManager>(relaxed = true)

        // Define stable flows
        val connectionStateFlow = MutableStateFlow(WifiConnectionState.DISCONNECTED)
        val rssiFlow = MutableStateFlow(0)
        val rttFlow = MutableStateFlow(0L)
        val rttHistoryFlow = MutableStateFlow(emptyList<Long>())
        val telemetryFlow = MutableStateFlow<Telemetry?>(null)
        val isEncryptedFlow = MutableStateFlow(false)
        val encryptionErrorFlow = MutableStateFlow<EncryptionException?>(null)

        // Mock StateFlows accessed by ControlScreen
        every { mock.connectionState } returns connectionStateFlow
        every { mock.rssi } returns rssiFlow
        every { mock.rtt } returns rttFlow
        every { mock.rttHistory } returns rttHistoryFlow
        every { mock.telemetry } returns telemetryFlow
        every { mock.isEncrypted } returns isEncryptedFlow
        every { mock.encryptionError } returns encryptionErrorFlow

        return mock
    }
}
