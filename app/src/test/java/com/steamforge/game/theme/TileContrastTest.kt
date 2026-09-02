package com.steamforge.game.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class TileContrastTest {

    @Test
    fun `gameplay tile numbers keep at least three to one contrast`() {
        for (level in 1..11) {
            val colors = tileColors(level)
            val ratio = contrastRatio(colors.content, colors.background)
            assertTrue(
                "Tile ${1 shl level} contrast was $ratio",
                ratio >= MIN_GAMEPLAY_TEXT_CONTRAST,
            )
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        const val MIN_GAMEPLAY_TEXT_CONTRAST = 3f
    }
}
