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

/** Tile material palette derived from the visual concept: cream, copper, bronze, patina and gold. */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFFD8C6A1), Color(0xFF4A2E18)), // 2 ivory brass plate
    RawTileColor(Color(0xFFC9B184), Color(0xFF4A2E18)), // 4
    RawTileColor(Color(0xFFAA653B), Color(0xFFF3DDB7)), // 8 copper
    RawTileColor(Color(0xFFB07B31), Color(0xFFF5E4C5)), // 16 bronze
    RawTileColor(Color(0xFFC45827), Color(0xFFF6DFC0)), // 32 orange copper
    RawTileColor(Color(0xFFA83B2D), Color(0xFFF6E1C6)), // 64 red copper
    RawTileColor(Color(0xFF3F746D), Color(0xFFF2E2C2)), // 128 patina
    RawTileColor(Color(0xFF2F6662), Color(0xFFF2E2C2)), // 256 teal patina
    RawTileColor(Color(0xFFB27524), Color(0xFFF9E7C5)), // 512
    RawTileColor(Color(0xFFC08A28), Color(0xFFFFEDC8)), // 1024
    RawTileColor(Color(0xFFD3A33C), Color(0xFF231408)), // 2048 core
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 10)
}

fun tileBevel(level: Int): Brush = Brush.verticalGradient(
    listOf(
        tileColors(level).background.lighten(1.08f),
        tileColors(level).background,
        tileColors(level).background.darken(0.72f),
    ),
)

private fun Color.darken(factor: Float): Color =
    Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)

private fun Color.lighten(factor: Float): Color =
    Color((red * factor).coerceIn(0f, 1f), (green * factor).coerceIn(0f, 1f), (blue * factor).coerceIn(0f, 1f), alpha)
