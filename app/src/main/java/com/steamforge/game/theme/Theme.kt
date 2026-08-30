package com.steamforge.game.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SteamforgeColors = darkColorScheme(
    primary = Brass,
    onPrimary = Color(0xFF241708),
    primaryContainer = PanelRaised,
    onPrimaryContainer = TextWarm,
    secondary = Copper,
    onSecondary = Color(0xFF241708),
    tertiary = Patina,
    onTertiary = Color(0xFF101B15),
    background = Background,
    onBackground = TextWarm,
    surface = SurfaceDark,
    onSurface = TextWarm,
    surfaceVariant = Panel,
    onSurfaceVariant = TextMuted,
    outline = OutlineBrass,
    error = Danger,
    onError = TextWarm,
)

/** Игра всегда в тёмной стимпанк-гамме: светлой темы нет сознательно. */
@Composable
fun SteamforgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SteamforgeColors,
        typography = SteamforgeTypography,
        content = content,
    )
}
