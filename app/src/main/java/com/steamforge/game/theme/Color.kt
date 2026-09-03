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
 * Gameplay tiles follow the approved gameplay concept: pale machined metal -> warm copper -> antique gold ->
 * oxidized green -> deep teal -> luminous teal core. Values remain the dominant visual information.
 */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFFB8AD98), Color(0xFF293036)), // 2 pale aged metal
    RawTileColor(Color(0xFF9C8F78), Color(0xFF272E33)), // 4 warm steel
    RawTileColor(Color(0xFFB07A35), Color(0xFFF4E8D2)), // 8 warm bronze
    RawTileColor(Color(0xFFB56431), Color(0xFFF5E8D3)), // 16 copper
    RawTileColor(Color(0xFFA95832), Color(0xFFF4E5D1)), // 32 forged copper
    RawTileColor(Color(0xFF983D2C), Color(0xFFF5E3CD)), // 64 red heat-treated copper
    RawTileColor(Color(0xFFAD7826), Color(0xFFF5E8CF)), // 128 antique gold
    RawTileColor(Color(0xFF9A732E), Color(0xFFF5E8CF)), // 256 dark antique gold
    RawTileColor(Color(0xFF6D7B62), Color(0xFFF3E8D2)), // 512 oxidized green metal
    RawTileColor(Color(0xFF176B73), Color(0xFFF3D99C)), // 1024 deep teal
    RawTileColor(Color(0xFF247F85), Color(0xFFF8E2A8)), // 2048 energized teal core; glow supplies the bright edge energy
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 11)
}

/**
 * A restrained machined-metal bevel. The highlight is concentrated toward the top and the lower tones carry more
 * weight, which reads as a physical plate rather than a flat mobile-game fill.
 */
fun tileBevel(level: Int): Brush {
    val material = tileColors(level).background
    val highTier = level >= 9
    return Brush.verticalGradient(
        listOf(
            material.lighten(if (highTier) 1.13f else 1.09f),
            material.lighten(if (highTier) 1.08f else 1.055f),
            material.lighten(1.018f),
            material,
            material.darken(if (highTier) 0.86f else 0.89f),
            material.darken(if (highTier) 0.74f else 0.80f),
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
