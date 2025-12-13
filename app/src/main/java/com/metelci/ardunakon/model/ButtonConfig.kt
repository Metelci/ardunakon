package com.metelci.ardunakon.model

import androidx.compose.ui.graphics.Color

data class ButtonConfig(
    val id: Int,
    val label: String,
    val command: String, // Custom command override, or default ID
    val colorHex: Long = 0xFF2196F3, // Default Blue
    val isToggle: Boolean = false
) {
    fun getColor(): Color = Color(colorHex)
}

// Default configuration for the 4 aux buttons with Pastel Colors
val defaultButtonConfigs = listOf(
    ButtonConfig(1, "Aux 1", "1", 0xFF90CAF9), // Pastel Blue
    ButtonConfig(2, "Aux 2", "2", 0xFFA5D6A7), // Pastel Green
    ButtonConfig(3, "Aux 3", "3", 0xFFFFCC80), // Pastel Orange
    ButtonConfig(4, "Aux 4", "4", 0xFFCE93D8) // Pastel Purple
)
