package com.metelci.ardunakon.di

import com.metelci.ardunakon.wifi.IWifiManager
import com.metelci.ardunakon.wifi.WifiConnectionState
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
    replaces = [WifiModule::class]
)
object FakeWifiModule {

    @Provides
    @Singleton
    fun provideWifiManager(): IWifiManager {
        val mock = mockk<IWifiManager>(relaxed = true)
        every { mock.connectionState } returns MutableStateFlow(WifiConnectionState.DISCONNECTED)
        every { mock.connectedDeviceInfo } returns MutableStateFlow(null)
        return mock
    }
}
