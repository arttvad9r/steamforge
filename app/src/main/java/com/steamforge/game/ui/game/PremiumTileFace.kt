package com.steamforge.game.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.steamforge.game.R
import com.steamforge.game.core.Tile
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TileColors

/**
 * Premium tile face: a dedicated miniature for every mechanical level plus a compact value plate.
 * The art assets stay separate from the tile material so they can be replaced one-by-one later
 * without touching game logic or animation code.
 */
@Composable
internal fun PremiumTileFace(
    tile: Tile,
    colors: TileColors,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.43f)
            drawCircle(
                color = Color.Black.copy(alpha = if (ghost) 0.10f else 0.20f),
                radius = size.minDimension * 0.31f,
                center = center + Offset(0f, 2.dp.toPx()),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (tile.level >= 8) TealGlow else BrassBright).copy(alpha = if (ghost) 0.05f else 0.13f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.39f,
                ),
                radius = size.minDimension * 0.39f,
                center = center,
            )
            if (!ghost) {
                val y = size.height * 0.69f
                drawLine(
                    Color.White.copy(alpha = 0.09f),
                    Offset(size.width * 0.20f, y),
                    Offset(size.width * 0.80f, y),
                    0.7.dp.toPx(),
                )
            }
        }

        Image(
            painter = painterResource(elementArtworkRes(tile.level)),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.70f)
                .offset(y = (-7).dp),
            contentScale = ContentScale.Fit,
            alpha = if (ghost) 0.84f else 1f,
        )

        val plateShape = RoundedCornerShape(7.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp, start = 8.dp, end = 8.dp)
                .sizeIn(minWidth = 34.dp)
                .shadow(if (ghost) 0.dp else 2.dp, plateShape)
                .clip(plateShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC2A1D13),
                            Color(0xE617100C),
                        ),
                    ),
                )
                .border(
                    1.dp,
                    (if (colors.glow) TealGlow else BrassDark).copy(alpha = if (ghost) 0.42f else 0.72f),
                    plateShape,
                )
                .padding(horizontal = 7.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tile.value.toString(),
                style = if (tile.value >= 1024) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                color = colors.content,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@DrawableRes
private fun elementArtworkRes(level: Int): Int = when (level.coerceIn(1, 11)) {
    1 -> R.drawable.element_coal
    2 -> R.drawable.element_gear
    3 -> R.drawable.element_valve
    4 -> R.drawable.element_piston
    5 -> R.drawable.element_boiler
    6 -> R.drawable.element_dynamo
    7 -> R.drawable.element_engine
    8 -> R.drawable.element_automaton
    9 -> R.drawable.element_turbine
    10 -> R.drawable.element_reactor
    else -> R.drawable.element_core
}
