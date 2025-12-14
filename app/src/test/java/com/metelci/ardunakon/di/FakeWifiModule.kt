package com.metelci.ardunakon.di

import com.metelci.ardunakon.wifi.WifiConnectionState
import com.metelci.ardunakon.wifi.WifiManager
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
    fun provideWifiManager(): WifiManager {
        val mock = mockk<WifiManager>(relaxed = true)
        every { mock.connectionState } returns MutableStateFlow(WifiConnectionState.DISCONNECTED)
        return mock
    }
}

