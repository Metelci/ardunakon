package com.metelci.ardunakon.telemetry

data class TelemetryDataPoint(
    val timestamp: Long,  // System.currentTimeMillis()
    val value: Float      // Metric value
)

data class TelemetrySnapshot(
    val timestamp: Long,
    val batteryVoltage: Float?,   // 0-30V
    val rssi: Int?,               // -100 to 0 dBm
    val rtt: Long?,               // milliseconds
    val status: String?           // "Safe Mode" / "Active"
)

enum class MetricType {
    BATTERY_VOLTAGE,
    RSSI,
    RTT,
    STATUS
}
