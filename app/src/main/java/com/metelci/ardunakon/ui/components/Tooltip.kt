package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * A tooltip that looks like a speech bubble pointing UP.
 *
 * @param text The text to display.
 * @param modifier Modifier for the tooltip content.
 * @param backgroundColor Background color of the bubble.
 * @param onDismiss Callback when the tooltip is clicked or dismissed.
 */
@Composable
fun Tooltip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    arrowHeight: Dp = 8.dp,
    onDismiss: () -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = modifier
                .clickable { onDismiss() }
                .drawBehind {
                    val path = Path().apply {
                        // Draw arrow pointing up at top-left/center area
                        // Assuming anchor is top-left of this popup content
                        moveTo(24.dp.toPx(), 0f)
                        lineTo(32.dp.toPx(), arrowHeight.toPx())
                        lineTo(16.dp.toPx(), arrowHeight.toPx())
                        close()
                    }
                    drawPath(path, backgroundColor)
                }
                .padding(top = arrowHeight) // Push content down to make room for arrow
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
