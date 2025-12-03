package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import com.metelci.ardunakon.BuildConfig

/**
 * About dialog showing app information, version, and links.
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val view = LocalView.current
    var webUrlToOpen by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title bar with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "About Ardunakon",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                    )
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

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App name and tagline
                    Text(
                        "Ardunakon",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF74B9FF),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "The Ultimate Arduino Bluetooth Controller",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Version info
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF212121),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // What's New
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDarkTheme) Color(0xFF243039) else Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDarkTheme) Color(0xFF37474F) else Color(0xFF90CAF9),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            "What's new in ${BuildConfig.VERSION_NAME}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkTheme) Color.White else Color(0xFF0D47A1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FeatureItem("UNO R4 WiFi + clones: dual BLE profile (ArduinoBLE + HM-10) for native connects", isDarkTheme)
                        FeatureItem("Docs refreshed for R4 clone setup and compatibility", isDarkTheme)
                        FeatureItem("Changelog reflects latest fixes and attribution updates", isDarkTheme)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Features
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "Key Features:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem("17 HC-05/HC-06 connection methods", isDarkTheme)
                        FeatureItem("7 HM-10 BLE UUID variants", isDarkTheme)
                        FeatureItem("Arduino UNO Q & R4 WiFi support", isDarkTheme)
                        FeatureItem("99%+ clone compatibility", isDarkTheme)
                        FeatureItem("Real-time telemetry & debugging", isDarkTheme)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GitHub link button
                    OutlinedButton(
                        onClick = { 
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            webUrlToOpen = "https://github.com/metelci/ardunakon" 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on GitHub")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // License info
                    Text(
                        "Open source project\nBuilt with Jetpack Compose",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }

    // In-app web view for online documentation/links
    webUrlToOpen?.let { url ->
        WebViewDialog(
            url = url,
            title = "About Ardunakon",
            onDismiss = { webUrlToOpen = null },
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Helper composable for feature list items.
 */
@Composable
private fun FeatureItem(text: String, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "✓",
            fontSize = 14.sp,
            color = Color(0xFF00C853),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF212121)
        )
    }
}
