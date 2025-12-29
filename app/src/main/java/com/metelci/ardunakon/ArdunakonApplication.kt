package com.metelci.ardunakon

import android.app.Application
import com.metelci.ardunakon.crash.CrashHandler
import com.metelci.ardunakon.monitoring.PerformanceMonitor
import com.metelci.ardunakon.security.RASPManager
import com.metelci.ardunakon.security.SecurityConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArdunakonApplication : Application() {

    override fun onCreate() {
        val startTime = System.currentTimeMillis()
        super.onCreate()

        // Initialize crash handler first to catch any init errors
        CrashHandler.init(this)

        // Initialize performance monitoring
        PerformanceMonitor.init(this)

        // Initialize runtime application self-protection
        if (SecurityConfig.ENABLE_RASP) {
            RASPManager.init(this)
        }

        // Record app startup time
        val startupDuration = System.currentTimeMillis() - startTime
        PerformanceMonitor.recordStartupTime(startupDuration)
    }
}
