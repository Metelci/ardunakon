package com.metelci.ardunakon.di

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideResources(@ApplicationContext context: Context): Resources {
        return context.resources
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("ardunakon_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideCryptoEngine(): com.metelci.ardunakon.security.CryptoEngine {
        return com.metelci.ardunakon.security.SecurityManager()
    }

    @Provides
    @Singleton
    fun provideRASPManager(@ApplicationContext context: Context): com.metelci.ardunakon.security.RASPManager {
        return com.metelci.ardunakon.security.RASPManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideRecoveryManager(): com.metelci.ardunakon.util.RecoveryManager {
        return com.metelci.ardunakon.util.RecoveryManager()
    }

    @Provides
    @Singleton
    fun providePerformanceMonitor(): com.metelci.ardunakon.monitoring.IPerformanceMonitor {
        return com.metelci.ardunakon.monitoring.PerformanceMonitor.getInstance()
            ?: throw IllegalStateException("PerformanceMonitor not initialized")
    }
}
