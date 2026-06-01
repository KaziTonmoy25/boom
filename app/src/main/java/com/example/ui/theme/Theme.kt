package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo Light
    secondary = Color(0xFF38BDF8), // Sky Blue Light
    tertiary = Color(0xFFFBBF24), // Gold Yellow Light
    background = Color(0xFF0B111E),
    surface = Color(0x37111827),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0x22374151),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x18FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo
    secondary = Color(0xFF0284C7), // Sky Blue
    tertiary = Color(0xFFD97706), // Gold Yellow
    background = Color(0xFFF0F2F5), // Frosted light gray
    surface = Color(0xB2FFFFFF), // Frosted translucent white card background
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0x5AE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x266366F1) // Translucent border
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to gorgeous cosmic slate mode
    content: @Composable () -> Unit
) {
    // Dynamically update reactive "Frosted Glass" variables to avoid breaking existing screen imports
    if (darkTheme) {
        CosmicDarkBg = Color(0xFF0B111E) // Cool slate dark depth
        CosmicSurface = Color(0x37111827) // Beautiful translucent dark glass-like surface
        CosmicSurfaceVariant = Color(0x22374151)
        CosmicBorder = Color(0x18FFFFFF) // Thin glowing light-outline
        TextPrimary = Color(0xFFF8FAFC)
        TextSecondary = Color(0xFF94A3B8)
        TextAccent = Color(0xFF818CF8)
    } else {
        CosmicDarkBg = Color(0xFFF0F2F5) // Clean light-gray slate background
        CosmicSurface = Color(0xB2FFFFFF) // High premium frosted glass card background
        CosmicSurfaceVariant = Color(0x5AE2E8F0) // Translucent light surface variant
        CosmicBorder = Color(0x266366F1) // High-polish indigo translucent border
        TextPrimary = Color(0xFF0F172A)
        TextSecondary = Color(0xFF475569)
        TextAccent = Color(0xFF4F46E5)
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

