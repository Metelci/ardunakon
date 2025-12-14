package com.metelci.ardunakon.di

import android.content.Context
import com.metelci.ardunakon.data.AutoReconnectPreferences
import com.metelci.ardunakon.data.DeviceNameCache
import com.metelci.ardunakon.data.ProfileManager
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
    fun provideProfileManager(@ApplicationContext context: Context): ProfileManager {
        return ProfileManager(context)
    }

    @Provides
    @Singleton
    fun provideDeviceNameCache(@ApplicationContext context: Context): DeviceNameCache {
        return DeviceNameCache(context)
    }

    @Provides
    @Singleton
    fun provideAutoReconnectPreferences(@ApplicationContext context: Context): AutoReconnectPreferences {
        return AutoReconnectPreferences(context)
    }
}
