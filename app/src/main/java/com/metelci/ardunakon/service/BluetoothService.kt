package com.metelci.ardunakon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.metelci.ardunakon.MainActivity
import com.metelci.ardunakon.R
import com.metelci.ardunakon.bluetooth.AppBluetoothManager

import kotlinx.coroutines.*
import com.metelci.ardunakon.bluetooth.ConnectionState

class BluetoothService : Service() {

    private val binder = LocalBinder()
    lateinit var bluetoothManager: AppBluetoothManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = AppBluetoothManager(this)
        
        // Setup WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Ardunakon::BluetoothService")

        // Observe connection states to manage WakeLock
        scope.launch {
            bluetoothManager.connectionStates.collect { states ->
                val hasActiveConnection = states.any { 
                    it == ConnectionState.CONNECTED || it == ConnectionState.CONNECTING || it == ConnectionState.RECONNECTING 
                }
                
                if (hasActiveConnection) {
                    if (wakeLock?.isHeld == false) {
                        wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours max
                    }
                } else {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                }
            }
        }

        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "ArdunakonConnection"
        val channelName = "Bluetooth Connection"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ardunakon Active")
            .setContentText("Maintaining Bluetooth connection...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bluetoothManager.cleanup()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
