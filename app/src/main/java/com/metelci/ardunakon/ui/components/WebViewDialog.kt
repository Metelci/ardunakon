package com.metelci.ardunakon.ui.components

import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

/**
 * WebView dialog for displaying web content inside the app.
 * Used for viewing GitHub documentation without leaving the app.
 */
@Composable
fun WebViewDialog(
    url: String = "",
    htmlContent: String? = null,
    title: String = "Documentation",
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val view = LocalView.current
    var isLoading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Title bar with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkTheme) Color(0xFF1E1E2E) else Color(0xFFE0E0E0))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                        )
                        if (isLoading) {
                            Text(
                                "Loading...",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTheme) Color(0xFF74B9FF) else Color(0xFF2D3436)
                            )
                        }
                    }
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF2D3436)
                        )
                    }
                }

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(if (isDarkTheme) Color(0xFF1E1E2E) else Color(0xFFE0E0E0))
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF74B9FF)
                        )
                    }
                }

                // WebView
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                WebView.startSafeBrowsing(context, null)
                                settings.safeBrowsingEnabled = true
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                            }
                            if (htmlContent != null) {
                                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            } else {
                                loadUrl(url)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDarkTheme) Color(0xFF1E1E2E) else Color.White)
                )
            }
        }
    }
}
