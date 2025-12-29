@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.metelci.ardunakon.util.AssetReader

/**
 * Help dialog component with tabbed interface for documentation access.
 * Displays tabs for Setup and Compatibility.
 * Each tab has a "View Full Guide" button to open the content
 * inside an in-app WebView dialog with enhanced readability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun HelpDialog(onDismiss: () -> Unit, onTakeTutorial: (() -> Unit)? = null) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var webUrlToOpen by rememberSaveable { mutableStateOf<String?>(null) }
    val isPortrait = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val dialogHeight = if (isPortrait) 0.9f else 0.95f

    val tabs = listOf("Setup", "Compatibility")
    val contentFiles = listOf(
        "docs/setup_guide.html",
        "docs/compatibility.html"
    )

    // Load HTML content for selected tab
    val content = remember(selectedTab) {
        AssetReader.readAssetFile(context, contentFiles[selectedTab])
    }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(dialogHeight),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D3436)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title bar with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Help & Documentation",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFB0BEC5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E1E2E),
                    contentColor = Color(0xFF74B9FF),
                    modifier = Modifier.fillMaxWidth().testTag("HelpTabs"),
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) {
                                        Color(0xFF74B9FF)
                                    } else {
                                        Color(0xFFB0BEC5)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content area (scrollable) containing text AND buttons
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            Color(0xFF1E1E2E),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            Color(0xFF3E3E4E),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                        .testTag("HelpLazyColumn")
                ) {
                    item {
                        Text(
                            text = when (selectedTab) {
                                0 ->
                                    "Step-by-step instructions for wiring your Arduino, " +
                                        "configuring Bluetooth modules, and troubleshooting connections."
                                1 ->
                                    "Detailed report on supported Bluetooth protocols, UUIDs, " +
                                        "and hardware version compatibility for various modules."
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0BEC5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        // "View Full Guide" button
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    // Trigger the dialog with current content
                                    webUrlToOpen = "offline"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier
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
                                    "View Full Guide",
                                    color = Color(0xFF2D3436)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Take Tutorial button (if callback provided)
                        if (onTakeTutorial != null) {
                            Button(
                                onClick = {
                                    onTakeTutorial()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00C853)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "🎓 Take Tutorial",
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Arduino Cloud Link
                        OutlinedButton(
                            onClick = {
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
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Arduino Cloud")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // In-app web view handling
    webUrlToOpen?.let { urlString ->
        if (urlString.startsWith("http")) {
            WebViewDialog(
                url = urlString,
                title = "Arduino Cloud",
                onDismiss = { webUrlToOpen = null }
            )
        } else {
            // Load HTML directly from asset file
            WebViewDialog(
                htmlContent = content,
                title = "${tabs[selectedTab]} Guide",
                onDismiss = { webUrlToOpen = null }
            )
        }
    }
}
