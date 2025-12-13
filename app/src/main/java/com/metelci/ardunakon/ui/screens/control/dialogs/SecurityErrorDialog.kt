package com.metelci.ardunakon.ui.screens.control.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog shown when secure profile storage requires device unlock.
 */
@Composable
fun SecurityErrorDialog(
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Required") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Security Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
