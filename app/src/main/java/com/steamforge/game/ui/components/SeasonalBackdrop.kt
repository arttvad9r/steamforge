package com.steamforge.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.steamforge.game.theme.SeasonalVisualTheme

/**
 * Extra atmosphere for Home/Workshop/Event only. Gameplay keeps the neutral SteamBackdrop so seasonal
 * content never competes with the board or changes tile readability.
 */
@Composable
fun SeasonalBackdrop(
    theme: SeasonalVisualTheme,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    SteamBackdrop(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = theme.atmosphere.copy(alpha = 0.055f),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.82f, size.height * 0.14f),
            )
            drawCircle(
                color = theme.accent.copy(alpha = 0.035f),
                radius = size.minDimension * 0.48f,
                center = Offset(size.width * 0.12f, size.height * 0.78f),
            )
        }
        content()
    }
}
