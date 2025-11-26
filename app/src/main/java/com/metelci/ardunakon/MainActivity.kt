package com.metelci.ardunakon

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.metelci.ardunakon.service.BluetoothService
import com.metelci.ardunakon.ui.screens.ControlScreen

import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var bluetoothService: BluetoothService? = null
    private var isBound by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionsDenied by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted, service will handle scanning when requested
            permissionsDenied = false
        } else {
            // Check if any permission was permanently denied
            val deniedPermissions = permissions.filterValues { !it }.keys
            val permanentlyDenied = deniedPermissions.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }

            permissionsDenied = true
            showPermissionDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start and Bind Service
        Intent(this, BluetoothService::class.java).also { intent ->
            ContextCompat.startForegroundService(this, intent) // Start foreground safely
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            val colorScheme = if (isDarkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBound && bluetoothService != null) {
                        ControlScreen(
                            bluetoothManager = bluetoothService!!.bluetoothManager,
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    // Permission denial dialog
                    if (showPermissionDialog) {
                        PermissionDeniedDialog(
                            onDismiss = { showPermissionDialog = false },
                            onRetry = {
                                showPermissionDialog = false
                                checkAndRequestPermissions()
                            },
                            onOpenSettings = {
                                showPermissionDialog = false
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        checkAndRequestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        permissionLauncher.launch(permissions)
    }
}

@Composable
fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth Permissions Required") },
        text = {
            Column {
                Text(
                    "Ardunakon needs Bluetooth permissions to connect to your devices.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Without these permissions, the app cannot scan for or connect to Bluetooth modules.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    )
}
