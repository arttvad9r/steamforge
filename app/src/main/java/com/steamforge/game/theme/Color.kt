package com.steamforge.game.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Палитра: тёмная сталь, латунь, медь, тёплый свет
val Background = Color(0xFF16100B)
val SurfaceDark = Color(0xFF221812)
val Panel = Color(0xFF2C2018)
val PanelRaised = Color(0xFF352820)
val Recess = Color(0xFF100B07)
val OutlineBrass = Color(0xFF8A6A35)
val Brass = Color(0xFFD9A441)
val BrassBright = Color(0xFFF2C14E)
val Copper = Color(0xFFB4713D)
val Steel = Color(0xFF7C8288)
val Patina = Color(0xFF7FA08C)
val TextWarm = Color(0xFFF2E4C9)
val TextMuted = Color(0xFFB3A184)
val Danger = Color(0xFFC45A3B)

/** Цвет плитки по уровню: фон и цвет текста. Контраст проверен. */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFF3D3227), TextWarm), // 2 Уголь
    RawTileColor(Color(0xFF4A3B29), TextWarm), // 4
    RawTileColor(Color(0xFF5E4729), TextWarm), // 8
    RawTileColor(Color(0xFF72532C), TextWarm), // 16
    RawTileColor(Color(0xFF886330), TextWarm), // 32
    RawTileColor(Color(0xFF9E7532), TextWarm), // 64
    RawTileColor(Color(0xFFB48734), Color(0xFF241708)), // 128
    RawTileColor(Color(0xFFC89A38), Color(0xFF241708)), // 256
    RawTileColor(Color(0xFFDCA93F), Color(0xFF241708)), // 512
    RawTileColor(Color(0xFFECB948), Color(0xFF241708)), // 1024
    RawTileColor(Color(0xFFF5C84F), Color(0xFF241708)), // 2048 Ядро
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 10)
}

fun tileBevel(level: Int): Brush = Brush.verticalGradient(
    listOf(
        tileColors(level).background.copy(alpha = 1f),
        tileColors(level).background.darken(0.82f),
    ),
)

private fun Color.darken(factor: Float): Color =
    Color((red * factor), (green * factor), (blue * factor), alpha)
