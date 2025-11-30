package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.metelci.ardunakon.BuildConfig

/**
 * About dialog showing app information, version, and links.
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    var webUrlToOpen by rememberSaveable { mutableStateOf<String?>(null) }

    // Pastel gradient for buttons (matching existing design)
    val pastelBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFCE4EC),
                Color(0xFFE3F2FD),
                Color(0xFFE8F5E9)
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f),
        containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color(0xFFF5F5F5),
        title = {
            Text(
                "About Ardunakon",
                style = MaterialTheme.typography.titleLarge,
                color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                Button(
                    onClick = { webUrlToOpen = "https://github.com/metelci/ardunakon" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .background(pastelBrush, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB0BEC5), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2D3436)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "View on GitHub",
                        color = Color(0xFF2D3436)
                    )
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Close",
                    color = Color(0xFF74B9FF)
                )
            }
        }
    )

    // In-app web view for online documentation/links
    webUrlToOpen?.let { url ->
        WebViewDialog(
            url = url,
            title = "Ardunakon on GitHub",
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
