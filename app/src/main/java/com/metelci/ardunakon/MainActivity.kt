package com.metelci.ardunakon

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    private var showBluetoothOffDialog by mutableStateOf(false)
    private var serviceStarted = false

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
            startAndBindServiceIfPermitted()
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

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure status bar for dark theme - use light (white) icons
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false  // false = light icons for dark background
            isAppearanceLightNavigationBars = false
        }


        // Start and Bind Service
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBound && bluetoothService != null) {
                        ControlScreen(
                            bluetoothManager = bluetoothService!!.bluetoothManager,
                            isDarkTheme = true,
                            onQuitApp = {
                                quitApp()
                            }
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

                    if (showBluetoothOffDialog) {
                        BluetoothOffDialog(
                            onDismiss = { showBluetoothOffDialog = false },
                            onTurnOn = {
                                showBluetoothOffDialog = false
                                promptEnableBluetooth()
                            }
                        )
                    }
                }
            }
        }

        checkAndRequestPermissions()
        if (hasBluetoothPermissions()) {
            startAndBindServiceIfPermitted()
        }
        checkBluetoothEnabled()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        checkBluetoothEnabled()
        if (hasBluetoothPermissions()) {
            startAndBindServiceIfPermitted()
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

        if (hasBluetoothPermissions()) {
            startAndBindServiceIfPermitted()
            return
        }

        permissionLauncher.launch(permissions)
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startAndBindServiceIfPermitted() {
        if (serviceStarted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermissions()) return

        Intent(this, BluetoothService::class.java).also { intent ->
            ContextCompat.startForegroundService(this, intent) // Start foreground safely
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            serviceStarted = true
        }
    }

    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        showBluetoothOffDialog = bluetoothAdapter == null || !bluetoothAdapter.isEnabled
    }

    private fun promptEnableBluetooth() {
        try {
            val enableIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableIntent)
        } catch (e: Exception) {
            showBluetoothOffDialog = true
        }
    }

    private fun quitApp() {
        // Unbind from service
        if (isBound) {
            unbindService(connection)
            isBound = false
        }

        // Stop the foreground service
        Intent(this, BluetoothService::class.java).also { intent ->
            stopService(intent)
        }

        // Finish the activity to close the app completely
        finishAffinity()
    }
}

@Composable
fun BluetoothOffDialog(
    onDismiss: () -> Unit,
    onTurnOn: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn on Bluetooth") },
        text = {
            Text(
                "Bluetooth is off. Enable it to scan and maintain device connections.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onTurnOn) { Text("Enable") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Why we need this:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("- Bluetooth: required to scan, pair, and stream control data to your boards.", style = MaterialTheme.typography.labelSmall)
                Text("- Location (pre-Android 12): Android mandates location permission for BLE scans.", style = MaterialTheme.typography.labelSmall)
                Text("- Data stays on-device; profiles are encrypted with the system keystore.", style = MaterialTheme.typography.labelSmall)
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
