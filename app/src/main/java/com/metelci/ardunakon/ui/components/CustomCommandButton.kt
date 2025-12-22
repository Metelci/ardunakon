@file:Suppress("DEPRECATION")

package com.metelci.ardunakon.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.model.CustomCommand

/**
 * Compact button for displaying a custom command on the main control screen.
 * Shows icon, optional shortcut label, and uses the command's color.
 */
@Suppress("FunctionName")
@Composable
fun CustomCommandButton(
    command: CustomCommand,
    view: View,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val buttonColor = Color(command.colorHex)
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(buttonColor.copy(alpha = 0.85f))
            .border(1.dp, buttonColor, RoundedCornerShape(12.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = getIconByName(command.iconName),
                contentDescription = command.name,
                tint = Color.White,
                modifier = Modifier.size(if (command.keyboardShortcut != null) 18.dp else 24.dp)
            )
            
            // Shortcut label
            command.keyboardShortcut?.let { shortcut ->
                Text(
                    text = shortcut.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Empty placeholder for custom command slot when no command is configured.
 */
@Suppress("FunctionName")
@Composable
fun CustomCommandPlaceholder(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A3E).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFF455A64).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Empty - just a subtle placeholder
    }
}

/**
 * Row of custom command buttons with placeholders for empty slots.
 * Shows up to 2 buttons on each side.
 */
@Suppress("FunctionName")
@Composable
fun CustomCommandButtonRow(
    commands: List<CustomCommand>,
    view: View,
    onCommandClick: (CustomCommand) -> Unit,
    buttonSize: Dp = 44.dp,
    maxButtons: Int = 2,
    modifier: Modifier = Modifier
) {
    val displayCommands = commands.take(maxButtons)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show configured commands or placeholders
        for (i in 0 until maxButtons) {
            if (i < displayCommands.size) {
                CustomCommandButton(
                    command = displayCommands[i],
                    view = view,
                    onClick = { onCommandClick(displayCommands[i]) },
                    size = buttonSize
                )
            } else {
                CustomCommandPlaceholder(size = buttonSize)
            }
        }
    }
}
