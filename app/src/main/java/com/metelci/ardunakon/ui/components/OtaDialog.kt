package com.metelci.ardunakon.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.metelci.ardunakon.ota.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced OTA Firmware Update Dialog
 * Features: Local/Cloud tabs, recent files history, file info, detailed errors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtaDialog(
    otaManager: OtaManager,
    bleTransport: BleOtaTransport?,
    wifiTransport: WifiOtaTransport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by otaManager.progress.collectAsState()
    
    // State
    var selectedTab by remember { mutableStateOf(0) } // 0=Local, 1=History
    var selectedMethod by remember { mutableStateOf(OtaMethod.WIFI) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileCrc by remember { mutableStateOf<Long?>(null) }
    
    // Managers
    val historyManager = remember { OtaHistoryManager(context) }
    val history = remember { mutableStateListOf<OtaHistoryManager.HistoryEntry>() }
    
    // Load history on first composition
    LaunchedEffect(Unit) {
        history.clear()
        history.addAll(historyManager.getHistory())
    }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val fileName = it.lastPathSegment ?: "firmware.bin"
                val cacheFile = File(context.cacheDir, "ota_firmware.bin")
                cacheFile.outputStream().use { out ->
                    inputStream?.copyTo(out)
                }
                selectedFile = cacheFile
                selectedFileName = fileName
                fileCrc = historyManager.calculateCrc32(cacheFile)
            } catch (e: Exception) {
                selectedFileName = "Error: ${e.message}"
            }
        }
    }
    
    Dialog(onDismissRequest = { 
        if (progress.state == OtaState.IDLE || progress.state == OtaState.COMPLETE || progress.state == OtaState.ERROR) {
            // Save to history on success
            selectedFile?.let { file ->
                if (progress.state == OtaState.COMPLETE) {
                    historyManager.addEntry(file, true)
                }
            }
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Firmware Update",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tabs: Local / History
                if (progress.state == OtaState.IDLE) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF2D2D3D),
                        contentColor = Color(0xFF00FF00)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) },
                            text = { Text("Local", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) },
                            text = { Text("Recent", fontSize = 11.sp) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Tab Content
                when {
                    progress.state != OtaState.IDLE -> {
                        // Show progress
                        ProgressContent(progress)
                    }
                    selectedTab == 0 -> {
                        // Local file picker
                        LocalFileContent(
                            selectedFileName = selectedFileName,
                            selectedFile = selectedFile,
                            fileCrc = fileCrc,
                            onSelectFile = { filePicker.launch("*/*") }
                        )
                    }
                    selectedTab == 1 -> {
                        // History
                        HistoryContent(
                            history = history,
                            historyManager = historyManager,
                            onEntrySelected = { entry ->
                                val file = File(entry.filePath)
                                if (file.exists()) {
                                    selectedFile = file
                                    selectedFileName = entry.fileName
                                    fileCrc = entry.crc32
                                    selectedTab = 0
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Transfer Method (only show when idle and file selected)
                if (progress.state == OtaState.IDLE && selectedFile != null) {
                    Text(
                        text = "Transfer Method:",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MethodOption(
                            label = "WiFi",
                            subtitle = "~500 KB/s",
                            selected = selectedMethod == OtaMethod.WIFI,
                            enabled = true,
                            onClick = { selectedMethod = OtaMethod.WIFI },
                            modifier = Modifier.weight(1f)
                        )
                        MethodOption(
                            label = "BLE",
                            subtitle = "~20 KB/s",
                            selected = selectedMethod == OtaMethod.BLE,
                            enabled = bleTransport != null,
                            onClick = { selectedMethod = OtaMethod.BLE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            if (progress.state == OtaState.TRANSFERRING || progress.state == OtaState.CONNECTING) {
                                scope.launch { otaManager.abort() }
                            } else {
                                otaManager.reset()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (progress.state == OtaState.TRANSFERRING) "Cancel" else "Close",
                            color = Color(0xFFFF5252)
                        )
                    }
                    
                    Button(
                        onClick = {
                            selectedFile?.let { file ->
                                scope.launch {
                                    otaManager.startUpdate(
                                        file = file,
                                        method = selectedMethod,
                                        bleTransport = if (selectedMethod == OtaMethod.BLE) bleTransport else null,
                                        wifiTransport = if (selectedMethod == OtaMethod.WIFI) wifiTransport else null
                                    )
                                }
                            }
                        },
                        enabled = selectedFile != null && progress.state == OtaState.IDLE,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("Start", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalFileContent(
    selectedFileName: String?,
    selectedFile: File?,
    fileCrc: Long?,
    onSelectFile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onSelectFile,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF00)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00))
        ) {
            Text(selectedFileName ?: "Select .bin file", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        
        // File info
        if (selectedFile != null && fileCrc != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D3D), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Size: ${selectedFile.length() / 1024} KB", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                Text("CRC: ${String.format("%08X", fileCrc)}", color = Color(0xFFB0BEC5), fontSize = 11.sp)
            }
        }
    }
}



@Composable
private fun HistoryContent(
    history: List<OtaHistoryManager.HistoryEntry>,
    historyManager: OtaHistoryManager,
    onEntrySelected: (OtaHistoryManager.HistoryEntry) -> Unit
) {
    if (history.isEmpty()) {
        Text("No recent uploads", color = Color(0xFFB0BEC5), fontSize = 12.sp)
    } else {
        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(history) { entry ->
                val isValid = historyManager.validateFile(entry)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isValid) { onEntrySelected(entry) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            entry.fileName,
                            color = if (isValid) Color.White else Color(0xFF757575),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${entry.fileSize / 1024} KB • ${if (entry.successful) "✓" else "✗"}",
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        SimpleDateFormat("MM/dd", Locale.US).format(Date(entry.uploadDate)),
                        color = Color(0xFFB0BEC5),
                        fontSize = 10.sp
                    )
                }
                Divider(color = Color(0xFF455A64))
            }
        }
    }
}

@Composable
private fun ProgressContent(progress: OtaProgress) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (progress.state) {
            OtaState.CONNECTING -> {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF00FF00))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting via ${progress.method.name}...", color = Color(0xFFB0BEC5), fontSize = 12.sp)
            }
            OtaState.TRANSFERRING, OtaState.VERIFYING -> {
                LinearProgressIndicator(
                    progress = progress.percent / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF00FF00),
                    trackColor = Color(0xFF455A64)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${progress.percent}% (${progress.bytesTransferred / 1024}/${progress.totalBytes / 1024} KB)",
                    color = Color(0xFFB0BEC5), fontSize = 12.sp
                )
                if (progress.state == OtaState.VERIFYING) {
                    Text("Verifying CRC...", color = Color(0xFFFFD54F), fontSize = 12.sp)
                }
            }
            OtaState.COMPLETE -> {
                Text("✓ Update Complete!", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                Text("Device will reboot.", color = Color(0xFFB0BEC5), fontSize = 11.sp)
            }
            OtaState.ERROR -> {
                Text("✗ Update Failed", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                val errorMsg = progress.errorMessage ?: OtaError.E003.getFullMessage()
                Text(errorMsg, color = Color(0xFFB0BEC5), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            else -> {}
        }
    }
}

@Composable
private fun MethodOption(
    label: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !enabled -> Color(0xFF2D2D3D)
        selected -> Color(0xFF1B5E20)
        else -> Color(0xFF2D2D3D)
    }
    val borderColor = when {
        !enabled -> Color(0xFF455A64)
        selected -> Color(0xFF00FF00)
        else -> Color(0xFF455A64)
    }
    
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = if (enabled) Color.White else Color(0xFF757575), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(subtitle, color = if (enabled) Color(0xFFB0BEC5) else Color(0xFF555555), fontSize = 9.sp)
        }
    }
}
