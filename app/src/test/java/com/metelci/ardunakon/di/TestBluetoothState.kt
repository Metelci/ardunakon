package com.metelci.ardunakon.di

import com.metelci.ardunakon.bluetooth.ConnectionHealth
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.bluetooth.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow

object TestBluetoothState {
    val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val rssiValue = MutableStateFlow(0)
    val health = MutableStateFlow(ConnectionHealth())
    val telemetry = MutableStateFlow<Telemetry?>(null)
    val rttHistory = MutableStateFlow(emptyList<Long>())
}

