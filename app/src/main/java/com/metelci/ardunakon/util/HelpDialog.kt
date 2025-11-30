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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.metelci.ardunakon.util.AssetReader

/**
 * Help dialog component with tabbed interface for documentation access.
 * Displays three tabs: Setup, Troubleshooting, and Compatibility.
 * Each tab has a "View Full Guide Online" button to open GitHub documentation
 * inside an in-app WebView dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var webUrlToOpen by rememberSaveable { mutableStateOf<String?>(null) }

    val tabs = listOf("Setup", "Troubleshooting", "Compatibility")
    val contentFiles = listOf(
        "docs/setup_guide.txt",
        "docs/troubleshooting.txt",
        "docs/compatibility.txt"
    )


    // Load content for selected tab
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF2D3436) else Color(0xFFF5F5F5)
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
                        color = if (isDarkTheme) Color.White else Color(0xFF2D3436)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF2D3436)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = if (isDarkTheme) Color(0xFF1E1E2E) else Color(0xFFE0E0E0),
                    contentColor = if (isDarkTheme) Color(0xFF74B9FF) else Color(0xFF2D3436),
                    modifier = Modifier.fillMaxWidth(),
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
                                        if (isDarkTheme) Color(0xFFB0BEC5) else Color(0xFF757575)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content area (scrollable)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            if (isDarkTheme) Color(0xFF1E1E2E) else Color(0xFFFFFFFF),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isDarkTheme) Color(0xFF3E3E4E) else Color(0xFFE0E0E0),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    item {
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF212121),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "View Full Guide" button
                Button(
                    onClick = {
                        // Trigger the dialog with current content
                        webUrlToOpen = "offline" 
                    },
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
                        "View Full Guide",
                        color = Color(0xFF2D3436)
                    )
                }
            }
        }
    }

    // In-app web view for offline documentation
    webUrlToOpen?.let { _ ->
        // Generate simple HTML for the text content
        val bgColor = if (isDarkTheme) "#1E1E2E" else "#FFFFFF"
        val textColor = if (isDarkTheme) "#E0E0E0" else "#212121"
        val htmlContent = """
            <html>
            <head>
            <style>
            body {
                background-color: $bgColor;
                color: $textColor;
                font-family: monospace;
                padding: 16px;
                white-space: pre-wrap;
                font-size: 14px;
                line-height: 1.5;
            }
            </style>
            </head>
            <body>
            ${content}
            </body>
            </html>
        """.trimIndent()

        WebViewDialog(
            htmlContent = htmlContent,
            title = "${tabs[selectedTab]} Guide",
            onDismiss = { webUrlToOpen = null },
            isDarkTheme = isDarkTheme
        )
    }
}
