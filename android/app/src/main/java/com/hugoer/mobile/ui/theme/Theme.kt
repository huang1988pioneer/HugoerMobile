package com.hugoer.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF12100E)
private val InkRaised = Color(0xFF1A1613)
private val Paper = Color(0xFFF7F3EE)
private val PaperCard = Color(0xFFFFFCF8)
private val Copper = Color(0xFFE8A15A)
private val CopperDeep = Color(0xFF8C4E1A)

private val DarkColors = darkColorScheme(
    primary = Copper,
    onPrimary = Color(0xFF3B2410),
    primaryContainer = Color(0xFF5C3A1C),
    onPrimaryContainer = Color(0xFFFFDCC2),
    inversePrimary = CopperDeep,
    secondary = Color(0xFFD4C4B4),
    onSecondary = Color(0xFF3A312A),
    secondaryContainer = Color(0xFF51463E),
    onSecondaryContainer = Color(0xFFF1E4D8),
    tertiary = Color(0xFFE7C8A4),
    onTertiary = Color(0xFF3F2E1C),
    tertiaryContainer = Color(0xFF594332),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Ink,
    onBackground = Color(0xFFF6F0E8),
    surface = InkRaised,
    onSurface = Color(0xFFF6F0E8),
    surfaceVariant = Color(0xFF2A241F),
    onSurfaceVariant = Color(0xFFC4B5A6),
    surfaceTint = Copper,
    outline = Color(0xFF5A4E44),
    outlineVariant = Color(0xFF3A312A),
    surfaceBright = Color(0xFF3A322C),
    surfaceDim = Ink,
    surfaceContainerLowest = Color(0xFF0E0C0A),
    surfaceContainerLow = InkRaised,
    surfaceContainer = Color(0xFF221C18),
    surfaceContainerHigh = Color(0xFF2A241F),
    surfaceContainerHighest = Color(0xFF352E28),
    inverseSurface = Color(0xFFF6F0E8),
    inverseOnSurface = Color(0xFF1C1612)
)

private val LightColors = lightColorScheme(
    primary = CopperDeep,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2E1500),
    inversePrimary = Copper,
    secondary = Color(0xFF6B5C50),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E4D6),
    onSecondaryContainer = Color(0xFF261910),
    tertiary = Color(0xFF6E5638),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF8E0C0),
    onTertiaryContainer = Color(0xFF271800),
    background = Paper,
    onBackground = Color(0xFF1C1612),
    surface = PaperCard,
    onSurface = Color(0xFF1C1612),
    surfaceVariant = Color(0xFFEFE6DC),
    onSurfaceVariant = Color(0xFF53453A),
    surfaceTint = CopperDeep,
    outline = Color(0xFF867466),
    outlineVariant = Color(0xFFD8CBBE),
    surfaceBright = PaperCard,
    surfaceDim = Color(0xFFE7DED4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF6F1),
    surfaceContainer = Color(0xFFF3EBE3),
    surfaceContainerHigh = Color(0xFFEDE3D9),
    surfaceContainerHighest = Color(0xFFE7DCD0),
    inverseSurface = Color(0xFF312A25),
    inverseOnSurface = Color(0xFFF7F0E8)
)

@Composable
fun HugoerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
