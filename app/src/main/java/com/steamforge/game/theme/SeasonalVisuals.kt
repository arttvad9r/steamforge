package com.steamforge.game.theme

import androidx.compose.ui.graphics.Color
import com.steamforge.game.progression.EventTheme

/**
 * Seasonal visuals are deliberately token-based. Remote/event data selects one of a small approved
 * industrial palettes; it can never inject arbitrary colors into gameplay-critical UI.
 */
data class SeasonalVisualTheme(
    val id: String,
    val accent: Color,
    val atmosphere: Color,
    val secondary: Color,
    val badge: String,
)

object SeasonalVisuals {
    val Default = SeasonalVisualTheme(
        id = "default",
        accent = BrassBright,
        atmosphere = TealGlow,
        secondary = Brass,
        badge = "◆",
    )

    private val Forge = SeasonalVisualTheme(
        id = "forge",
        accent = Color(0xFFE08A3A),
        atmosphere = Color(0xFFC76A2A),
        secondary = BrassBright,
        badge = "F",
    )

    private val PatinaCycle = SeasonalVisualTheme(
        id = "patina",
        accent = TealGlow,
        atmosphere = Patina,
        secondary = Steel,
        badge = "P",
    )

    private val BrassWorks = SeasonalVisualTheme(
        id = "brass",
        accent = BrassBright,
        atmosphere = Brass,
        secondary = Copper,
        badge = "B",
    )

    private val SteelShift = SeasonalVisualTheme(
        id = "steel",
        accent = Color(0xFF8FA3B4),
        atmosphere = Steel,
        secondary = TealGlow,
        badge = "S",
    )

    fun resolve(theme: EventTheme?): SeasonalVisualTheme {
        if (theme == null) return Default
        return when (theme.accent.trim().lowercase()) {
            "forge-orange", "forge", "copper" -> Forge
            "patina-teal", "patina", "teal" -> PatinaCycle
            "brass", "warm-brass" -> BrassWorks
            "steel", "steel-blue" -> SteelShift
            else -> when (theme.id.trim().lowercase()) {
                "foundry", "forge" -> Forge
                "patina" -> PatinaCycle
                "brass" -> BrassWorks
                "steel" -> SteelShift
                else -> Default
            }
        }
    }
}
