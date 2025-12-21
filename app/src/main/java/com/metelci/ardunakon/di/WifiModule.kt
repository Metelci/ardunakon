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
        bluetoothManager: AppBluetoothManager,
        connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences
    ): WifiManager {
        return WifiManager(
            context = context,
            connectionPreferences = connectionPreferences,
            onLog = { msg ->
                val logType = when {
                    msg.contains("Connected", ignoreCase = true) || msg.startsWith("✓") -> LogType.SUCCESS
                    msg.contains("Error", ignoreCase = true) || msg.contains("Failed", ignoreCase = true) -> LogType.ERROR
                    msg.contains("Warning", ignoreCase = true) -> LogType.WARNING
                    else -> LogType.INFO
                }
                bluetoothManager.log(msg, logType)
            }
        )
    }
}
