@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.BuildConfig

/**
 * About dialog showing app information, version, and links.
 */
@Suppress("FunctionName")
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
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
                containerColor = Color(0xFF2D3436)
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
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFB0BEC5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                        "Arduino Controller",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Version info
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        fontSize = 12.sp,
                        color = Color(0xFFE0E0E0),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // What's New
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF243039),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF37474F),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            "What's new in ${BuildConfig.VERSION_NAME}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamically load release notes from assets
                        val context = LocalContext.current
                        val releaseNotes = remember {
                            try {
                                context.assets.open("release_notes.txt").bufferedReader().use { it.readText() }
                                    .lines()
                                    .filter { it.isNotBlank() }
                            } catch (e: Exception) {
                                listOf("Check CHANGELOG.md for details.")
                            }
                        }

                        releaseNotes.forEach { note ->
                            FeatureItem(note)
                        }
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
                            contentColor = Color(0xFF90CAF9)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF90CAF9)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open GitHub",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on GitHub")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Arduino Cloud link button
                    OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            webUrlToOpen = "https://cloud.arduino.cc"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00C853)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF00C853)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Arduino Cloud",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Arduino Cloud")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // License info
                    Text(
                        "Open source project\nBuilt with Jetpack Compose",
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E),
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
            onDismiss = { webUrlToOpen = null }
        )
    }
}

/**
 * Helper composable for feature list items.
 */
@Suppress("FunctionName")
@Composable
private fun FeatureItem(text: String) {
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
            color = Color(0xFFE0E0E0)
        )
    }
}
