package com.metelci.ardunakon.model

enum class LogType {
    INFO,       // General info - Blue
    SUCCESS,    // Operation success - Green
    ERROR,      // Errors - Red
    WARNING,    // Warnings - Yellow
    SENT,       // Outgoing/sent data - Bright Green
    RECEIVED    // Incoming/received data - White
}

data class LogEntry(val timestamp: Long = System.currentTimeMillis(), val type: LogType, val message: String)
