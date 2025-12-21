package com.metelci.ardunakon.platform

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of PlatformInfo.
 * Provides access to device/platform information via Build class.
 */
@Singleton
class AndroidPlatformInfo @Inject constructor() : PlatformInfo {

    override val sdkVersion: Int = Build.VERSION.SDK_INT

    override val manufacturer: String = Build.MANUFACTURER ?: "unknown"

    override val model: String = Build.MODEL ?: "unknown"

    override val androidVersion: String = Build.VERSION.RELEASE ?: "unknown"
}
