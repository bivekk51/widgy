package com.widgy.lyrics.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Unified 4-Tier Radii System:
 * - Small (8dp): Chips, Badges, Tags
 * - Medium (12dp): Buttons, TextFields, Action Controls
 * - Large (16dp): Cards, Content Containers, Dialogs
 * - ExtraLarge (24dp): Bottom Sheets, Docks, Top Overlays
 */
val WidgyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB454),
    onPrimary = Color(0xFF1A1000),
    primaryContainer = Color(0xFF38260D),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFF1ED760),
    onSecondary = Color(0xFF003914),
    secondaryContainer = Color(0xFF005322),
    onSecondaryContainer = Color(0xFF70F791),
    background = Color(0xFF0A0A0C),
    onBackground = Color(0xFFEDEDF2),
    surface = Color(0xFF121215),
    onSurface = Color(0xFFEDEDF2),
    surfaceVariant = Color(0xFF1C1C22),
    onSurfaceVariant = Color(0xFFA6A6B2),
    outline = Color(0xFF2E2E38),
    outlineVariant = Color(0xFF22222A)
)

@Composable
fun WidgyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = WidgyShapes,
        content = content
    )
}
