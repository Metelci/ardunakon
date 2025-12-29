package com.metelci.ardunakon

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.core.content.ContextCompat
import com.metelci.ardunakon.security.RASPManager
import com.metelci.ardunakon.service.BluetoothService
import com.metelci.ardunakon.ui.screens.ControlScreen
import com.metelci.ardunakon.ui.screens.control.dialogs.SecurityCompromisedDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var onboardingManager: com.metelci.ardunakon.data.OnboardingManager

    private var bluetoothService: BluetoothService? = null
    private var isBound by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionsDenied by mutableStateOf(false)
    private var showBluetoothOffDialog by mutableStateOf(false)
    private var showNotificationPermissionDialog by mutableStateOf(false)
    private var notificationPermissionRequested = false
    private var serviceStarted = false
    private var showOnboarding by mutableStateOf(false)

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
            @Suppress("UNUSED_VARIABLE")
            val deniedPermissions = permissions.filterValues { !it }.keys

            @Suppress("UNUSED_VARIABLE")
            val permanentlyDenied = deniedPermissions.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }

            permissionsDenied = true
            showPermissionDialog = true
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showNotificationPermissionDialog = false
            startAndBindServiceIfPermitted(forceStart = true)
        } else {
            showNotificationPermissionDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize crash handler first
        com.metelci.ardunakon.crash.CrashHandler.init(this)

        // Check if onboarding should be shown (first run or version update)
        showOnboarding = onboardingManager.shouldShowOnboarding()

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure status bar for dark theme - use light (white) icons
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // false = light icons for dark background
            isAppearanceLightNavigationBars = false
        }

        // Start and Bind Service
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // Critical Security Violation
                        RASPManager.getInstance(this).isSecurityCompromised.collectAsState().value -> {
                            SecurityCompromisedDialog(
                                onQuitApp = {
                                    RASPManager.getInstance(this).wipeSensitiveData()
                                    quitApp()
                                }
                            )
                        }
                        // Show onboarding for first-time users
                        showOnboarding -> {
                            com.metelci.ardunakon.ui.screens.onboarding.OnboardingFlow(
                                onComplete = {
                                    showOnboarding = false
                                    onboardingManager.completeOnboarding()
                                },
                                onSkip = {
                                    showOnboarding = false
                                    onboardingManager.skipOnboarding()
                                }
                            )
                        }
                        // Normal app flow
                        isBound && bluetoothService != null -> {
                            ControlScreen(
                                onQuitApp = {
                                    quitApp()
                                },
                                onTakeTutorial = {
                                    onboardingManager.resetOnboarding()
                                    showOnboarding = true
                                }
                            )
                        }
                        // Loading state
                        else -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
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

                    if (showNotificationPermissionDialog) {
                        NotificationPermissionDialog(
                            onDismiss = { showNotificationPermissionDialog = false },
                            onOpenSettings = {
                                showNotificationPermissionDialog = false
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            },
                            onRetry = {
                                showNotificationPermissionDialog = false
                                requestNotificationPermission()
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
        requestNotificationPermission()

        com.metelci.ardunakon.crash.BreadcrumbManager.leave("Lifecycle", "MainActivity Created")
    }

    override fun onDestroy() {
        com.metelci.ardunakon.crash.BreadcrumbManager.leave("Lifecycle", "MainActivity Destroyed")
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        com.metelci.ardunakon.crash.BreadcrumbManager.leave("Lifecycle", "MainActivity Resumed")
        checkBluetoothEnabled()
        if (hasBluetoothPermissions()) {
            startAndBindServiceIfPermitted()
        }
    }

    override fun onPause() {
        super.onPause()
        com.metelci.ardunakon.crash.BreadcrumbManager.leave("Lifecycle", "MainActivity Paused")
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Include NEARBY_WIFI_DEVICES for WiFi/mDNS discovery
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12: BT permissions only
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

    private fun hasBluetoothPermissions(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotificationPermission()) return
        if (notificationPermissionRequested) return
        notificationPermissionRequested = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startAndBindServiceIfPermitted(forceStart: Boolean = false) {
        if (serviceStarted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermissions()) return

        // Request notification permission but don't block app startup
        if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !forceStart) {
            requestNotificationPermission()
            // Continue anyway - notification is optional
        }

        Intent(this, BluetoothService::class.java).also { intent ->
            try {
                ContextCompat.startForegroundService(this, intent)
            } catch (e: Exception) {
                // If foreground fails, try regular start
                startService(intent)
            }
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            serviceStarted = true
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
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

@Suppress("FunctionName")
@Composable
fun BluetoothOffDialog(onDismiss: () -> Unit, onTurnOn: () -> Unit) {
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

@Suppress("FunctionName")
@Composable
fun NotificationPermissionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit, onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Permission Needed") },
        text = {
            Column {
                Text(
                    "Android 13+ requires notification permission to show the foreground service " +
                        "that keeps Bluetooth connections alive.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Granting this permission ensures the app can reconnect and stay active in the background.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { Button(onClick = onRetry) { Text("Grant") } },
        dismissButton = { TextButton(onClick = onOpenSettings) { Text("Open Settings") } }
    )
}

@Suppress("FunctionName")
@Composable
fun PermissionDeniedDialog(onDismiss: () -> Unit, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
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
                Text(
                    "- Bluetooth: required to scan, pair, and stream control data to your boards.",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "- Location (pre-Android 12): Android mandates location permission for BLE scans.",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "- Data stays on-device; settings are encrypted with the system keystore.",
                    style = MaterialTheme.typography.labelSmall
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
