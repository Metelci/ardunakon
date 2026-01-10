package com.metelci.ardunakon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.metelci.ardunakon.MainActivity
import com.metelci.ardunakon.R
import com.metelci.ardunakon.bluetooth.ConnectionState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*

@AndroidEntryPoint
class BluetoothService : Service() {

    companion object {
        private const val TAG = "BluetoothService"
    }

    private val binder = LocalBinder()

    @Inject
    lateinit var bluetoothManager: com.metelci.ardunakon.bluetooth.IBluetoothManager

    @Inject
    lateinit var wifiManager: com.metelci.ardunakon.wifi.IWifiManager

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLockTimeoutJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate started")
        // Hilt injects managers automatically

        // Setup WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Ardunakon::BluetoothService")

        // Observe connection state to manage WakeLock
        scope.launch {
            bluetoothManager.connectionState.collect { state ->
                val hasActiveConnection = state == ConnectionState.CONNECTED ||
                    state == ConnectionState.CONNECTING ||
                    state == ConnectionState.RECONNECTING

                if (hasActiveConnection) {
                    if (wakeLock?.isHeld == false) {
                        wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max
                        wakeLockTimeoutJob?.cancel()
                        wakeLockTimeoutJob = scope.launch {
                            delay(60 * 60 * 1000L)
                            if (wakeLock?.isHeld == true) {
                                wakeLock?.release()
                                bluetoothManager.disconnect()
                                bluetoothManager.reconnectSavedDevice()
                            }
                        }
                    }
                } else {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                    wakeLockTimeoutJob?.cancel()
                }
            }
        }

        startForegroundService()
        Log.d(TAG, "Service onCreate completed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure foreground is active after system restarts the service
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "ArdunakonConnection"
        val channelName = "Bluetooth Connection"

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ardunakon Active")
            .setContentText("Maintaining Bluetooth connection...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            startForeground(1, notification)
            Log.d(TAG, "Foreground service started successfully")
        } catch (se: SecurityException) {
            // Missing POST_NOTIFICATIONS will prevent foreground start on Android 13+
            // Don't call stopSelf() - continue running without notification
            // This allows the app to function even without notification permission
            Log.w(TAG, "Foreground service failed due to missing notification permission. " +
                "Service will continue without notification, which may cause issues on Android 13+.", se)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bluetoothManager.cleanup()
        wifiManager.cleanup()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
