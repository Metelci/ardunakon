package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.security.EncryptionException

/**
 * Blocking dialog shown when encryption fails during connection.
 *
 * This dialog prevents unencrypted data transmission by requiring user action.
 * The user can choose to:
 * - Retry the connection (will attempt encryption again)
 * - Continue without encryption (disables encryption requirement)
 * - Disconnect completely
 */
@Composable
fun EncryptionErrorDialog(
    error: EncryptionException,
    onRetry: () -> Unit,
    onDisableEncryption: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (error) {
        is EncryptionException.HandshakeFailedException -> "Handshake Failed"
        is EncryptionException.EncryptionFailedException -> "Encryption Failed"
        is EncryptionException.NoSessionKeyException -> "No Encryption Key"
    }

    val suggestion = when (error) {
        is EncryptionException.HandshakeFailedException ->
            "The device could not be verified. This may indicate a misconfigured device or a security issue."
        is EncryptionException.EncryptionFailedException ->
            "A cryptographic error occurred. Try reconnecting or restarting the app."
        is EncryptionException.NoSessionKeyException ->
            "No secure session was established. The device may not support encryption."
    }

    AlertDialog(
        onDismissRequest = { /* Blocking dialog - user must choose an option */ },
        title = { Text(title) },
        text = {
            Column {
                Text(error.message ?: "Unknown encryption error")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Connection stopped to prevent unencrypted data transmission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Retry Connection")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onDisableEncryption) {
                    Text("Continue Without Encryption")
                }
                TextButton(onClick = onDismiss) {
                    Text("Disconnect")
                }
            }
        }
    )
}
