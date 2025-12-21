package com.metelci.ardunakon.platform

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing platform abstraction bindings.
 * Binds Android implementations to their interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    
    @Binds
    @Singleton
    abstract fun bindPlatformInfo(impl: AndroidPlatformInfo): PlatformInfo
    
    @Binds
    @Singleton
    abstract fun bindSystemServices(impl: AndroidSystemServices): SystemServices
}
