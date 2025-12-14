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
    fun provideAppBluetoothManager(@ApplicationContext context: Context): AppBluetoothManager {
        return AppBluetoothManager(context)
    }

    // TelemetryHistoryManager is exposed via AppBluetoothManager, but if needed directly:
    @Provides
    fun provideTelemetryHistoryManager(bluetoothManager: AppBluetoothManager): TelemetryHistoryManager {
        return bluetoothManager.telemetryHistoryManager
    }
}
