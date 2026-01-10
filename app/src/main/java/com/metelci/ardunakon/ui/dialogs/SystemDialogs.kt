package com.metelci.ardunakon.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog shown when Bluetooth is disabled.
 *
 * @param onDismiss Callback when user dismisses the dialog.
 * @param onTurnOn Callback when user wants to enable Bluetooth.
 */
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

/**
 * Dialog shown when notification permission is needed (Android 13+).
 *
 * @param onDismiss Callback when user dismisses the dialog.
 * @param onOpenSettings Callback to open app settings.
 * @param onRetry Callback to retry permission request.
 */
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

/**
 * Dialog shown when Bluetooth permissions are denied.
 *
 * @param onDismiss Callback when user dismisses the dialog.
 * @param onRetry Callback to retry permission request.
 * @param onOpenSettings Callback to open app settings.
 */
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

/**
 * Dialog shown when the Bluetooth service fails to start or bind.
 * This can happen due to permission issues, battery optimization, or service crashes.
 *
 * @param onRetry Callback to retry starting the service.
 * @param onOpenSettings Callback to open app settings.
 * @param onDismiss Callback when user dismisses the dialog.
 * @param reason Optional reason string describing why the service failed.
 */
@Composable
fun ServiceConnectionFailedDialog(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    reason: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection Service Issue") },
        text = {
            Column {
                Text(
                    "The Bluetooth connection service couldn't start properly. " +
                        "This may prevent device connections.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (reason != null) {
                    Text(
                        "Reason: $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    "Common causes:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "• Notification permission denied (Android 13+)",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "• Battery optimization killing the service",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "• Auto-start permission not granted (MIUI, ColorOS)",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("App Settings")
            }
        }
    )
}
