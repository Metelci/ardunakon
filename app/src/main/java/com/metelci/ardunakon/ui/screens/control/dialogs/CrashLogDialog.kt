package com.metelci.ardunakon.ui.screens.control.dialogs

import android.content.Intent
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.metelci.ardunakon.crash.CrashHandler
import com.metelci.ardunakon.ui.utils.hapticTap

/**
 * Dialog to display crash logs with share and clear options.
 */
@Suppress("FunctionName")
@Composable
fun CrashLogDialog(crashLog: String, view: View, onShare: () -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.9f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Crash Log", color = Color(0xFFFF9800))
                Row {
                    IconButton(onClick = {
                        view.hapticTap()
                        val shareIntent = CrashHandler.getShareIntent(context)
                        if (shareIntent != null) {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Crash Log"))
                        }
                        onShare()
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = Color(0xFF00E5FF))
                    }
                    IconButton(onClick = {
                        view.hapticTap()
                        CrashHandler.clearCrashLog(context)
                        onClear()
                    }) {
                        Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFFF5252))
                    }
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = crashLog.ifEmpty { "No crash logs available" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFFB0BEC5),
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF00E5FF))
            }
        }
    )
}
