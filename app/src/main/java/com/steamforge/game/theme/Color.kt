package com.steamforge.game.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Steamforge gameplay palette: deep workshop charcoal, aged brass/copper and restrained patina.
// Surfaces stay deliberately dark so the board hierarchy is carried by material, light and value contrast.
val Background = Color(0xFF0B1117)
val SurfaceDark = Color(0xFF0F161C)
val Panel = Color(0xFF141D24)
val PanelRaised = Color(0xFF192229)
val Recess = Color(0xFF080D12)
val OutlineBrass = Color(0xFF5B4527)
val BrassDark = Color(0xFF493719)
val Brass = Color(0xFFA0783B)
val BrassBright = Color(0xFFD1A45A)
val Copper = Color(0xFFA65B34)
val Steel = Color(0xFF58636C)
val Patina = Color(0xFF397E80)
val TealSurface = Color(0xFF1C3F44)
val TealGlow = Color(0xFF63B7BA)
val TextWarm = Color(0xFFF0E7D9)
val TextMuted = Color(0xFF98A3AA)
val Danger = Color(0xFFB95A3B)

/**
 * Gameplay tiles are tile-first and steampunk-second. Low levels stay dark and machined instead of
 * pale/plastic; rarity is introduced gradually through warmer metal, patina and controlled teal.
 */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFF354149), Color(0xFFF0E8DC)), // 2 gunmetal
    RawTileColor(Color(0xFF44515A), Color(0xFFF0E7D8)), // 4 aged steel
    RawTileColor(Color(0xFF725A33), Color(0xFFF5E9D4)), // 8 muted brass
    RawTileColor(Color(0xFF7D5235), Color(0xFFF5E9D4)), // 16 aged copper
    RawTileColor(Color(0xFF884C2F), Color(0xFFF5E7D3)), // 32 forged copper
    RawTileColor(Color(0xFF783B2C), Color(0xFFF4E4D0)), // 64 heat-treated copper
    RawTileColor(Color(0xFF8B692F), Color(0xFFF6E9CF)), // 128 antique brass
    RawTileColor(Color(0xFF7B5D2B), Color(0xFFF6E9CF)), // 256 dark antique brass
    RawTileColor(Color(0xFF52675F), Color(0xFFF2E8D8)), // 512 patinated steel
    RawTileColor(Color(0xFF24585D), Color(0xFFF3E4C7)), // 1024 deep teal metal
    RawTileColor(Color(0xFF2C7F83), Color(0xFFFFE8B5)), // 2048 energized teal core
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 11)
}

/**
 * Restrained machined-metal bevel. The face remains broad and calm; a short highlight at the top and
 * a weighted lower edge create depth without the multi-band glossy mobile-plastic look.
 */
fun tileBevel(level: Int): Brush {
    val material = tileColors(level).background
    val highTier = level >= 9
    return Brush.verticalGradient(
        listOf(
            material.lighten(if (highTier) 1.13f else 1.10f),
            material.lighten(1.035f),
            material,
            material.darken(if (highTier) 0.82f else 0.85f),
            material.darken(if (highTier) 0.72f else 0.76f),
        ),
    )
}

private fun Color.darken(factor: Float): Color =
    Color(
        (red * factor).coerceIn(0f, 1f),
        (green * factor).coerceIn(0f, 1f),
        (blue * factor).coerceIn(0f, 1f),
        alpha,
    )

private fun Color.lighten(factor: Float): Color =
    Color(
        (red * factor).coerceIn(0f, 1f),
        (green * factor).coerceIn(0f, 1f),
        (blue * factor).coerceIn(0f, 1f),
        alpha,
    )
