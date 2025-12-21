package com.metelci.ardunakon.platform

/**
 * Fake implementation of PlatformInfo for unit testing.
 * Allows tests to control platform behavior without Android dependencies.
 */
class FakePlatformInfo(
    override val sdkVersion: Int = 33,
    override val manufacturer: String = "Test",
    override val model: String = "TestDevice",
    override val androidVersion: String = "13"
) : PlatformInfo {
    
    companion object {
        /** Pre-configured fake for Android 12 (API 31) */
        fun android12() = FakePlatformInfo(sdkVersion = 31, androidVersion = "12")
        
        /** Pre-configured fake for Android 13 (API 33) */
        fun android13() = FakePlatformInfo(sdkVersion = 33, androidVersion = "13")
        
        /** Pre-configured fake for Xiaomi device */
        fun xiaomi() = FakePlatformInfo(manufacturer = "Xiaomi", model = "Redmi Note 10")
        
        /** Pre-configured fake for Samsung device */
        fun samsung() = FakePlatformInfo(manufacturer = "Samsung", model = "Galaxy S21")
    }
}
