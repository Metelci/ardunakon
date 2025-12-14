package com.metelci.ardunakon.di

import android.content.Context
import com.metelci.ardunakon.wifi.WifiManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.model.LogType

@Module
@InstallIn(SingletonComponent::class)
object WifiModule {

    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context,
        bluetoothManager: AppBluetoothManager
    ): WifiManager {
        return WifiManager(
            context = context,
            onLog = { msg ->
                bluetoothManager.log(msg, LogType.INFO)
            }
        )
    }
}
