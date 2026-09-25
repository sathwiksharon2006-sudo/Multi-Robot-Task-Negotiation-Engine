package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = AmberWarning,
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5E4100),
    onSecondaryContainer = Color(0xFFFFDEA3),
    tertiary = PurpleNegotiating,
    onTertiary = Color(0xFF381E72),
    background = IndustrialDarkBg,
    onBackground = Color(0xFFE2E8F0),
    surface = IndustrialSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = IndustrialSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = RedEmergency,
    onError = Color.White
)

@Composable
fun MultiRobotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
