package com.steamforge.game.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Approved Steamforge palette: charcoal/deep navy base, warm brass, restrained teal/patina.
val Background = Color(0xFF10161D)
val SurfaceDark = Color(0xFF16202A)
val Panel = Color(0xFF1C2731)
val PanelRaised = Color(0xFF26323D)
val Recess = Color(0xFF0C1218)
val OutlineBrass = Color(0xFF76562A)
val BrassDark = Color(0xFF64451F)
val Brass = Color(0xFFA9782E)
val BrassBright = Color(0xFFD1A45A)
val Copper = Color(0xFFC76A2A)
val Steel = Color(0xFF5B6773)
val Patina = Color(0xFF3B9A9E)
val TealSurface = Color(0xFF234F55)
val TealGlow = Color(0xFF63B7BA)
val TextWarm = Color(0xFFF0E5D0)
val TextMuted = Color(0xFFAAB2B8)
val Danger = Color(0xFFC7603A)

/**
 * Gameplay tiles stay intentionally restrained: tile-first, steampunk-second.
 * Low tiers are quiet metal plates; richer material changes arrive gradually.
 */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFFC8BEAA), Color(0xFF27313A)), // 2 aged steel/ivory
    RawTileColor(Color(0xFFBDAE93), Color(0xFF27313A)), // 4 warm steel
    RawTileColor(Color(0xFF9E7651), Color(0xFFF3E8D4)), // 8 muted copper
    RawTileColor(Color(0xFFA9782E), Color(0xFFF6EAD4)), // 16 brass
    RawTileColor(Color(0xFFAF6534), Color(0xFFF7E8D4)), // 32 forged copper
    RawTileColor(Color(0xFF96513C), Color(0xFFF7E8D4)), // 64 heat-treated copper
    RawTileColor(Color(0xFF567A78), Color(0xFFF2E9D8)), // 128 oxidized metal
    RawTileColor(Color(0xFF3F6F72), Color(0xFFF2E9D8)), // 256 deep patina
    RawTileColor(Color(0xFF8E6B34), Color(0xFFF7EAD2)), // 512 dark brass
    RawTileColor(Color(0xFFB9853B), Color(0xFF172029)), // 1024 polished brass, dark ink for readable contrast
    RawTileColor(Color(0xFFD1A45A), Color(0xFF172029)), // 2048 mechanical core
)

fun tileColors(level: Int, patinaStyle: Boolean = false): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    val material = if (patinaStyle) raw.bg.mixWith(Patina, if (level <= 2) 0.10f else 0.18f) else raw.bg
    return TileColors(material, raw.content, glow = level >= 11)
}

fun tileBevel(level: Int, patinaStyle: Boolean = false): Brush {
    val material = tileColors(level, patinaStyle).background
    val highTier = level >= 9
    return Brush.verticalGradient(
        listOf(
            material.lighten(if (highTier) 1.10f else 1.07f),
            material.lighten(1.035f),
            material,
            material.darken(if (highTier) 0.90f else 0.92f),
            material.darken(if (highTier) 0.80f else 0.84f),
        ),
    )
}

private fun Color.mixWith(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - t) + other.red * t,
        green = green * (1f - t) + other.green * t,
        blue = blue * (1f - t) + other.blue * t,
        alpha = alpha,
    )
}

private fun Color.darken(factor: Float): Color =
    Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)

private fun Color.lighten(factor: Float): Color =
    Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)
