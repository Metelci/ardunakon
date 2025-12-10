package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import com.metelci.ardunakon.data.ProfileManager
import com.metelci.ardunakon.wifi.WifiManager

/**
 * Factory for creating ControlViewModel with required dependencies.
 */
class ControlViewModelFactory(
    private val bluetoothManager: AppBluetoothManager,
    private val wifiManager: WifiManager,
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ControlViewModel::class.java)) {
            return ControlViewModel(
                bluetoothManager = bluetoothManager,
                wifiManager = wifiManager,
                profileManager = ProfileManager(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
