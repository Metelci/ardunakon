package com.metelci.ardunakon.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metelci.ardunakon.model.ArduinoType
import com.metelci.ardunakon.model.ConnectionTutorialStep
import com.metelci.ardunakon.ui.screens.control.ConnectionMode

/**
 * Phase 3: Connection Tutorial. Guides the user through choosing device,
 * explaining modes, and finally connecting + advanced features.
 */
@Suppress("FunctionName")
@Composable
fun ConnectionTutorialScreen(
    step: ConnectionTutorialStep,
    selectedArduinoType: ArduinoType?,
    onArduinoSelected: (ArduinoType) -> Unit,
    connectionMode: com.metelci.ardunakon.ui.screens.control.ConnectionMode =
        com.metelci.ardunakon.ui.screens.control.ConnectionMode.BLUETOOTH,
    onConnectionModeChanged: (com.metelci.ardunakon.ui.screens.control.ConnectionMode) -> Unit = {},
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingProgress(progress = progress)

            Spacer(modifier = Modifier.height(24.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    ConnectionTutorialStep.CHOOSE_ARDUINO -> ChooseArduinoContent(
                        selectedType = selectedArduinoType,
                        onSelect = onArduinoSelected
                    )
                    ConnectionTutorialStep.CONNECTION_MODE -> ConnectionModeContent(
                        selectedArduinoType = selectedArduinoType,
                        connectionMode = connectionMode,
                        onModeChanged = onConnectionModeChanged
                    )
                    ConnectionTutorialStep.SETUP_FINAL -> TutorialFooter()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            TutorialIndicator(
                step = step,
                selectedArduinoType = selectedArduinoType,
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun TutorialIndicator(
    step: ConnectionTutorialStep,
    selectedArduinoType: ArduinoType?,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Add safe area padding for system nav bar
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSkip) {
            Text(
                text = "Skip Tutorial",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNext,
                enabled = step != ConnectionTutorialStep.CHOOSE_ARDUINO || selectedArduinoType != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (step == ConnectionTutorialStep.SETUP_FINAL) {
                        Color(0xFF00C853)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(if (step == ConnectionTutorialStep.SETUP_FINAL) "Finish" else "Continue")
                if (step != ConnectionTutorialStep.SETUP_FINAL) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue")
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = "Finish tutorial")
                }
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ChooseArduinoContent(selectedType: ArduinoType?, onSelect: (ArduinoType) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Select Your Device 🤖",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "What kind of Arduino are you using?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ArduinoType.entries) { type ->
                TutorialNextButton(
                    type = type,
                    isSelected = type == selectedType,
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun TutorialNextButton(type: ArduinoType, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = type.subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ConnectionModeContent(
    selectedArduinoType: ArduinoType?,
    connectionMode: ConnectionMode,
    onModeChanged: (ConnectionMode) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "🔌 Connection Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        val boardHint = selectedArduinoType?.let { "Selected device: ${it.displayName} (${it.connectionType})" }
        Text(
            text = boardHint ?: "Ardunakon supports both Bluetooth and WiFi.",
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (
                            connectionMode == com.metelci.ardunakon.ui.screens.control.ConnectionMode.BLUETOOTH
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .clickable {
                                onModeChanged(ConnectionMode.BLUETOOTH)
                            }
                    ) {
                        Text(
                            "Bluetooth",
                            color = if (
                                connectionMode == com.metelci.ardunakon.ui.screens.control.ConnectionMode.BLUETOOTH
                            ) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (
                            connectionMode == com.metelci.ardunakon.ui.screens.control.ConnectionMode.WIFI
                        ) {
                            Color(0xFF00C853)
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .clickable { onModeChanged(ConnectionMode.WIFI) }
                    ) {
                        Text(
                            "WiFi",
                            color = if (
                                connectionMode == com.metelci.ardunakon.ui.screens.control.ConnectionMode.WIFI
                            ) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val instructionText =
                    if (connectionMode == ConnectionMode.BLUETOOTH) {
                        "Bluetooth mode selected. Best for external Bluetooth modules (HC-05/HC-06) and common " +
                            "BLE serial modules (HM-10/HC-08/AT-09/MLT-BT05)."
                    } else {
                        "WiFi mode selected. Best for boards running a WiFi (UDP) sketch/firmware (for example " +
                            "Arduino UNO R4 WiFi or ESP32-based boards)."
                    }

                Text(
                    text = instructionText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You can change this later in the top-left header while using the app.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun TutorialFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🚀 Ready to Connect!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You're all set! Here's how to connect:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        StepCard(number = 1, text = "Tap the RSSI icon in the header to scan")
        Spacer(modifier = Modifier.height(8.dp))
        StepCard(number = 2, text = "Select your Arduino from the list")
        Spacer(modifier = Modifier.height(8.dp))
        StepCard(number = 3, text = "Wait for green \"Connected\" status")

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusColorItem(Color.Red, "Disconnected")
                StatusColorItem(Color.Yellow, "Connecting")
                StatusColorItem(Color(0xFF00C853), "Connected")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "💡 Tip: You can access Debug Console and Telemetry from the header icons after connecting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun StepCard(number: Int, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "$number",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun StatusColorItem(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(50),
            color = color,
            modifier = Modifier.size(12.dp)
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
