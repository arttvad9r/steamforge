package com.steamforge.game.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Approved Steamforge palette: deep workshop charcoal, warm aged brass and restrained patina.
// The palette is intentionally low-saturation so material/light carry the premium feel instead of neon color.
val Background = Color(0xFF0D141B)
val SurfaceDark = Color(0xFF141D25)
val Panel = Color(0xFF1A242D)
val PanelRaised = Color(0xFF26323C)
val Recess = Color(0xFF090F14)
val OutlineBrass = Color(0xFF72552D)
val BrassDark = Color(0xFF5B421F)
val Brass = Color(0xFFA17A3E)
val BrassBright = Color(0xFFD0A45B)
val Copper = Color(0xFFB86432)
val Steel = Color(0xFF59656F)
val Patina = Color(0xFF408C8D)
val TealSurface = Color(0xFF20464C)
val TealGlow = Color(0xFF61ADB0)
val TextWarm = Color(0xFFF1E7D6)
val TextMuted = Color(0xFFA6AFB5)
val Danger = Color(0xFFB95A3B)

/**
 * Gameplay tiles stay intentionally restrained: tile-first, steampunk-second.
 * Low tiers read as quiet machined plates. Copper/brass/patina arrive gradually and the 2048 core is the only
 * deliberately luminous tier. This keeps the board readable while still giving progression a material payoff.
 */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFFAAA89F), Color(0xFF222B31)), // 2 aged nickel
    RawTileColor(Color(0xFFA89B84), Color(0xFF232B31)), // 4 warm steel
    RawTileColor(Color(0xFF927052), Color(0xFFF1E6D5)), // 8 muted bronze
    RawTileColor(Color(0xFF9E7A3F), Color(0xFFF4E9D6)), // 16 antique brass
    RawTileColor(Color(0xFFA25F38), Color(0xFFF5E7D5)), // 32 forged copper
    RawTileColor(Color(0xFF884E3E), Color(0xFFF4E6D6)), // 64 heat-treated copper
    RawTileColor(Color(0xFF55706E), Color(0xFFF0E8DB)), // 128 oxidized steel
    RawTileColor(Color(0xFF3E696B), Color(0xFFF1E9DA)), // 256 deep patina
    RawTileColor(Color(0xFF806632), Color(0xFFF3E8D3)), // 512 dark brass
    RawTileColor(Color(0xFFB48742), Color(0xFF192127)), // 1024 polished brass
    RawTileColor(Color(0xFFD0A45B), Color(0xFF172027)), // 2048 mechanical core
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 11)
}

/**
 * A restrained machined-metal bevel. The highlight is concentrated near the top edge and the lower third carries
 * more weight, which reads as a physical plate rather than a flat mobile-game gradient.
 */
fun tileBevel(level: Int): Brush {
    val material = tileColors(level).background
    val highTier = level >= 9
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to material.lighten(if (highTier) 1.13f else 1.09f),
            0.08f to material.lighten(if (highTier) 1.08f else 1.055f),
            0.34f to material.lighten(1.018f),
            0.68f to material,
            0.91f to material.darken(if (highTier) 0.86f else 0.89f),
            1.00f to material.darken(if (highTier) 0.74f else 0.80f),
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
