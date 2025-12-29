package com.metelci.ardunakon.di

import android.content.Context
import com.metelci.ardunakon.wifi.WifiManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WifiModule {

    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context,
        bluetoothManager: com.metelci.ardunakon.bluetooth.IBluetoothManager,
        connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
        recoveryManager: com.metelci.ardunakon.util.RecoveryManager
    ): com.metelci.ardunakon.wifi.IWifiManager {
        return WifiManager(
            context = context,
            connectionPreferences = connectionPreferences,
            recoveryManager = recoveryManager,
            onLog = { msg, type ->
                bluetoothManager.log(msg, type)
            }
        )
    }
}
