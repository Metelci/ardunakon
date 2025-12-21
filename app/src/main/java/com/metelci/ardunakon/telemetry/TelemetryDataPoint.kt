package com.metelci.ardunakon.telemetry

data class TelemetryDataPoint(
    // System.currentTimeMillis()
    val timestamp: Long,
    // Metric value
    val value: Float
)

data class TelemetrySnapshot(
    val timestamp: Long,
    // 0-30V
    val batteryVoltage: Float?,
    // -100 to 0 dBm
    val rssi: Int?,
    // milliseconds
    val rtt: Long?,
    // "Safe Mode" / "Active"
    val status: String?
)

enum class MetricType {
    BATTERY_VOLTAGE,
    RSSI,
    RTT,
    STATUS
}
