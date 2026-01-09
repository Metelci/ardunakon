package com.metelci.ardunakon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metelci.ardunakon.R

/**
 * Futuristic app title bar with "Ardunakon" text and geometric underline accent.
 * Style 1E: Ice blue italic with sharp geometric underline design.
 */
@Suppress("FunctionName")
@Composable
fun AppTitleBar(
    modifier: Modifier = Modifier
) {
    // Define the ice blue/cyan color palette
    val primaryColor = Color(0xFF00D4FF)  // Bright cyan
    val secondaryColor = Color(0xFF00A8CC)  // Deeper teal
    val glowColor = Color(0xFF00D4FF).copy(alpha = 0.5f)

    // Use Orbitron font family for futuristic look (or fallback to default italic)
    val titleFontFamily = try {
        FontFamily(
            Font(R.font.orbitron_bold, FontWeight.Bold, FontStyle.Italic)
        )
    } catch (_: Exception) {
        FontFamily.Default
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title text with glow effect
        Text(
            text = "Ardunakon",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = titleFontFamily,
                letterSpacing = 3.sp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00A8CC),
                        Color(0xFF00D4FF),
                        Color(0xFF00FFFF),
                        Color(0xFF00D4FF),
                        Color(0xFF00A8CC)
                    )
                ),
                shadow = Shadow(
                    color = glowColor,
                    offset = Offset(0f, 0f),
                    blurRadius = 16f
                )
            )
        )

        // Geometric underline accent
        GeometricUnderline(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(8.dp)
                .padding(top = 2.dp)
        )
    }
}

/**
 * Geometric underline accent - sharp angular design with gradient
 */
@Suppress("FunctionName")
@Composable
private fun GeometricUnderline(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val strokePx = strokeWidth.toPx()

        // Create gradient brush
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0f),
                secondaryColor,
                primaryColor,
                secondaryColor,
                secondaryColor.copy(alpha = 0f)
            )
        )

        // Main horizontal line with tapered ends
        drawLine(
            brush = gradientBrush,
            start = Offset(width * 0.1f, height / 2f),
            end = Offset(width * 0.9f, height / 2f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Center diamond/chevron accent
        val diamondSize = height * 0.8f
        val chevronPath = Path().apply {
            moveTo(centerX - diamondSize * 1.5f, height / 2f)
            lineTo(centerX, height * 0.1f)
            lineTo(centerX + diamondSize * 1.5f, height / 2f)
        }

        drawPath(
            path = chevronPath,
            color = primaryColor,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round
            )
        )

        // Small accent dots at the ends
        drawCircle(
            color = primaryColor.copy(alpha = 0.8f),
            radius = strokePx * 1.2f,
            center = Offset(width * 0.15f, height / 2f)
        )

        drawCircle(
            color = primaryColor.copy(alpha = 0.8f),
            radius = strokePx * 1.2f,
            center = Offset(width * 0.85f, height / 2f)
        )
    }
}
