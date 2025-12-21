package com.metelci.ardunakon.bluetooth

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    RECONNECTING
}

enum class DeviceType {
    CLASSIC,
    LE
}

data class BluetoothDeviceModel(val name: String, val address: String, val type: DeviceType, val rssi: Int = 0)

data class ConnectionHealth(
    val lastPacketAt: Long = 0L,
    val rssiFailureCount: Int = 0,
    val lastHeartbeatSeq: Int = 0,
    val lastHeartbeatAt: Long = 0L,
    val lastRttMs: Long = 0L
)
