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
 * Gameplay tiles are tile-first and steampunk-second. Low levels stay quiet and machined; the
 * material progression becomes richer only as values grow. High tiers keep a controlled patina/
 * brass language instead of turning into a rainbow or fantasy-RPG rarity palette.
 */
data class TileColors(val background: Color, val content: Color, val glow: Boolean = false)

private data class RawTileColor(val bg: Color, val content: Color)

private val rawTileColors = listOf(
    RawTileColor(Color(0xFF303B43), Color(0xFFF0E8DC)), // 2 gunmetal
    RawTileColor(Color(0xFF3B4850), Color(0xFFF0E7D8)), // 4 aged steel
    RawTileColor(Color(0xFF5B4B31), Color(0xFFF4E8D5)), // 8 smoked brass
    RawTileColor(Color(0xFF674933), Color(0xFFF4E8D5)), // 16 dark brass / copper
    RawTileColor(Color(0xFF794A32), Color(0xFFF5E7D3)), // 32 aged copper
    RawTileColor(Color(0xFF80402E), Color(0xFFF4E4D0)), // 64 forged copper
    RawTileColor(Color(0xFF806333), Color(0xFFF6E9CF)), // 128 antique brass
    RawTileColor(Color(0xFF6D5931), Color(0xFFF6E9CF)), // 256 dark antique brass
    RawTileColor(Color(0xFF4A635D), Color(0xFFF2E8D8)), // 512 patinated steel
    RawTileColor(Color(0xFF23565A), Color(0xFFF3E4C7)), // 1024 deep teal metal
    RawTileColor(Color(0xFF2B777B), Color(0xFFFFE8B5)), // 2048 energized teal core
    RawTileColor(Color(0xFF3B6C68), Color(0xFFFFE3AE)), // 4096 tempered patina core
    RawTileColor(Color(0xFF626547), Color(0xFFFFE7B8)), // 8192 rare brass-patina alloy
)

fun tileColors(level: Int): TileColors {
    val raw = rawTileColors[(level - 1).coerceIn(0, rawTileColors.lastIndex)]
    return TileColors(raw.bg, raw.content, glow = level >= 11)
}

/**
 * Restrained machined-metal bevel. The center stays broad and calm; progressively rarer materials
 * gain slightly stronger edge separation, not extra ornament. The highlight is mixed toward white
 * rather than multiplying RGB so dark steel/copper still catches a visible, neutral workshop light.
 */
fun tileBevel(level: Int): Brush {
    val material = tileColors(level).background
    val topLift = when {
        level >= 12 -> 0.16f
        level >= 11 -> 0.15f
        level >= 9 -> 0.13f
        else -> 0.10f
    }
    val shoulderLift = when {
        level >= 11 -> 0.055f
        level >= 9 -> 0.045f
        else -> 0.035f
    }
    val lowerMid = if (level >= 9) 0.81f else 0.84f
    val lowerEdge = when {
        level >= 12 -> 0.64f
        level >= 11 -> 0.67f
        level >= 9 -> 0.70f
        else -> 0.74f
    }
    return Brush.verticalGradient(
        0.00f to material.lift(topLift),
        0.15f to material.lift(shoulderLift),
        0.48f to material,
        0.78f to material.darken(lowerMid),
        1.00f to material.darken(lowerEdge),
    )
}

private fun Color.darken(factor: Float): Color =
    Color(
        (red * factor).coerceIn(0f, 1f),
        (green * factor).coerceIn(0f, 1f),
        (blue * factor).coerceIn(0f, 1f),
        alpha,
    )

private fun Color.lift(amount: Float): Color {
    val safe = amount.coerceIn(0f, 1f)
    return Color(
        red + (1f - red) * safe,
        green + (1f - green) * safe,
        blue + (1f - blue) * safe,
        alpha,
    )
}
