package com.metelci.ardunakon.di

import android.content.Context
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.telemetry.TelemetryHistoryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(
        @ApplicationContext context: Context,
        connectionPreferences: com.metelci.ardunakon.data.ConnectionPreferences,
        cryptoEngine: com.metelci.ardunakon.security.CryptoEngine,
        recoveryManager: com.metelci.ardunakon.util.RecoveryManager
    ): com.metelci.ardunakon.bluetooth.IBluetoothManager {
        return AppBluetoothManager(
            context = context,
            connectionPreferences = connectionPreferences,
            cryptoEngine = cryptoEngine,
            recoveryManager = recoveryManager
        )
    }

    @Provides
    fun provideTelemetryHistoryManager(
        bluetoothManager: com.metelci.ardunakon.bluetooth.IBluetoothManager
    ): TelemetryHistoryManager {
        return bluetoothManager.telemetryHistoryManager
    }
}
