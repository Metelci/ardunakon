package com.metelci.ardunakon.di

import android.content.Context
import com.metelci.ardunakon.data.AutoReconnectPreferences
import com.metelci.ardunakon.data.DeviceNameCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDeviceNameCache(
        @ApplicationContext context: Context,
        cryptoEngine: com.metelci.ardunakon.security.CryptoEngine
    ): DeviceNameCache {
        return DeviceNameCache(context, cryptoEngine)
    }

    @Provides
    @Singleton
    fun provideAutoReconnectPreferences(
        @ApplicationContext context: Context,
        cryptoEngine: com.metelci.ardunakon.security.CryptoEngine
    ): AutoReconnectPreferences {
        return AutoReconnectPreferences(context, cryptoEngine)
    }

    @Provides
    @Singleton
    fun provideConnectionPreferences(
        @ApplicationContext context: Context,
        cryptoEngine: com.metelci.ardunakon.security.CryptoEngine
    ): com.metelci.ardunakon.data.ConnectionPreferences {
        return com.metelci.ardunakon.data.ConnectionPreferences(context, cryptoEngine)
    }
}
