package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        is EncryptionException.HandshakeFailedException -> "Security Verification Failed"
        is EncryptionException.EncryptionFailedException -> "Encryption Error"
        is EncryptionException.NoSessionKeyException -> "Security Protocol Error"
        is EncryptionException.SecurityException -> "Security Error"
    }

    val suggestion = when (error) {
        is EncryptionException.HandshakeFailedException ->
            "The device could not establish a secure connection. This may indicate an incompatible device or network security policy."
        is EncryptionException.EncryptionFailedException ->
            "A security protocol error occurred. Check your connection and try again."
        is EncryptionException.NoSessionKeyException ->
            "A secure session could not be established. The device may not support the required security protocol."
        is EncryptionException.SecurityException ->
            "A security requirement could not be satisfied. Check your device compatibility and network configuration."
    }

    AlertDialog(
        onDismissRequest = { /* Blocking dialog - user must choose an option */ },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // SECURITY FIX: Don't expose detailed error messages to prevent information leakage
                Text("Security protocol error occurred")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Connection blocked to prevent unencrypted data transmission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDisableEncryption) {
                    Text("Continue")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Disconnect")
            }
        }
    )
}

