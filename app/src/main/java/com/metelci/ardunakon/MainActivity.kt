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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.metelci.ardunakon.permissions.PermissionManager
import com.metelci.ardunakon.security.RASPManager
import com.metelci.ardunakon.service.BluetoothService
import com.metelci.ardunakon.ui.dialogs.BluetoothOffDialog
import com.metelci.ardunakon.ui.dialogs.NotificationPermissionDialog
import com.metelci.ardunakon.ui.dialogs.PermissionDeniedDialog
import com.metelci.ardunakon.ui.navigation.AppNavHost
import com.metelci.ardunakon.ui.navigation.AppRoute
import com.metelci.ardunakon.ui.screens.ControlScreen
import com.metelci.ardunakon.ui.screens.control.dialogs.SecurityCompromisedDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingManager: com.metelci.ardunakon.data.OnboardingManager

    @Inject
    lateinit var permissionManager: PermissionManager

    private var bluetoothService: BluetoothService? = null
    private var isBound by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionsDenied by mutableStateOf(false)
    private var showBluetoothOffDialog by mutableStateOf(false)
    private var showNotificationPermissionDialog by mutableStateOf(false)
    private var notificationPermissionRequested = false
    private var serviceStarted = false
    private var showOnboarding by mutableStateOf(false)
    private var deepLinkIntent by mutableStateOf<Intent?>(null)

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
        deepLinkIntent = intent

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
                val raspManager = remember { RASPManager.getInstance(this@MainActivity) }
                val isSecurityCompromised by raspManager.isSecurityCompromised.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // Critical Security Violation
                        isSecurityCompromised -> {
                            SecurityCompromisedDialog(
                                onQuitApp = {
                                    raspManager.wipeSensitiveData()
                                    quitApp()
                                }
                            )
                        }
                        else -> {
                            val navController = rememberNavController()
                            val startDestination = remember {
                                if (showOnboarding) AppRoute.Onboarding else AppRoute.Control
                            }

                            LaunchedEffect(deepLinkIntent) {
                                deepLinkIntent?.let { navController.handleDeepLink(it) }
                            }

                            AppNavHost(
                                navController = navController,
                                startDestination = startDestination,
                                controlContent = { onTakeTutorial ->
                                    if (isBound && bluetoothService != null) {
                                        ControlScreen(
                                            onTakeTutorial = {
                                                onboardingManager.resetOnboarding()
                                                showOnboarding = true
                                                onTakeTutorial()
                                            }
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                },
                                onboardingContent = { onComplete, onSkip ->
                                    com.metelci.ardunakon.ui.screens.onboarding.OnboardingFlow(
                                        onComplete = {
                                            showOnboarding = false
                                            onboardingManager.completeOnboarding()
                                            onComplete()
                                        },
                                        onSkip = {
                                            showOnboarding = false
                                            onboardingManager.skipOnboarding()
                                            onSkip()
                                        }
                                    )
                                }
                            )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkIntent = intent
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

    private fun hasBluetoothPermissions(): Boolean = permissionManager.hasBluetoothPermissions()

    private fun hasNotificationPermission(): Boolean = permissionManager.hasNotificationPermission()

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
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestPermissions()
            return
        }

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
