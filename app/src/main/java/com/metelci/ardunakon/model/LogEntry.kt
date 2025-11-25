package com.metelci.ardunakon.model

enum class LogType {
    INFO, SUCCESS, ERROR, WARNING
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val message: String
)
