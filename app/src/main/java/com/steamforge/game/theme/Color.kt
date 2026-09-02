package com.steamforge.game.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium stylized steampunk: dark workshop, warm brass/copper, restrained teal energy accent.
val Background = Color(0xFF0D0A07)
val SurfaceDark = Color(0xFF17110D)
val Panel = Color(0xFF211813)
val PanelRaised = Color(0xFF30231A)
val Recess = Color(0xFF090705)
val OutlineBrass = Color(0xFF7A5628)
val BrassDark = Color(0xFF573817)
val Brass = Color(0xFFB77B2C)
val BrassBright = Color(0xFFE1B35C)
val Copper = Color(0xFFA6532D)
val Steel = Color(0xFF6E7474)
val Patina = Color(0xFF5B8A80)
val TealSurface = Color(0xFF174A48)
val TealGlow = Color(0xFF59D9D1)
val TextWarm = Color(0xFFF0DFC0)
val TextMuted = Color(0xFFB8A584)
val Danger = Color(0xFFC65332)

/** Tile material palette derived from the visual concept: ivory, copper, bronze, patina and core gold. */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFFD7C5A2), Color(0xFF472A15)), // 2 aged ivory brass
    RawTileColor(Color(0xFFC8AE7D), Color(0xFF432814)), // 4 warm brass
    RawTileColor(Color(0xFFA95F39), Color(0xFFF4DEBD)), // 8 copper
    RawTileColor(Color(0xFFA97734), Color(0xFFF6E3C1)), // 16 bronze
    RawTileColor(Color(0xFFBC582B), Color(0xFFF7E0C0)), // 32 hot copper
    RawTileColor(Color(0xFF99392D), Color(0xFFF8E2C8)), // 64 red copper
    RawTileColor(Color(0xFF4C7B70), Color(0xFFF2E4C7)), // 128 oxidized brass
    RawTileColor(Color(0xFF356862), Color(0xFFF2E4C7)), // 256 deep patina
    RawTileColor(Color(0xFFA96F27), Color(0xFFF9E8C7)), // 512 dark gold
    RawTileColor(Color(0xFFC08B31), Color(0xFF172029)), // 1024 bright gold, dark ink for readable contrast
    RawTileColor(Color(0xFFD4A647), Color(0xFF241508)), // 2048 mechanical core
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 10)
}

fun tileBevel(level: Int): Brush {
    val material = tileColors(level).background
    return Brush.verticalGradient(
        listOf(
            material.lighten(1.17f),
            material.lighten(1.07f),
            material,
            material.darken(0.84f),
            material.darken(0.64f),
        ),
    )
}

private fun Color.darken(factor: Float): Color =
    Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)

private fun Color.lighten(factor: Float): Color =
    Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)
