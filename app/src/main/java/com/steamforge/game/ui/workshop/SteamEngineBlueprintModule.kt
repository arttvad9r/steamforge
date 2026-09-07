package com.steamforge.game.ui.workshop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

private val BlueprintTop = Color(0xFF15303A)
private val BlueprintBottom = Color(0xFF0A171E)
private val BlueprintInk = Color(0xFF76B9BC)
private val BlueprintGrid = Color(0xFF8FC9CB)

@Composable
internal fun SteamEngineBlueprintModule(
    piecesOwned: Int,
    piecesTotal: Int,
    unlocked: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val total = piecesTotal.coerceAtLeast(1)
    val owned = piecesOwned.coerceIn(0, total)
    val fraction = owned.toFloat() / total
    val shape = RoundedCornerShape(14.dp)
    val border = if (unlocked) TealGlow else BrassDark

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        BlueprintTop.copy(alpha = if (unlocked) 0.96f else 0.90f),
                        BlueprintBottom.copy(alpha = 0.97f),
                    ),
                ),
            )
            .border(
                1.dp,
                border.copy(alpha = if (unlocked) 0.52f else 0.42f),
                shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = if (unlocked) {
                    "Steam Engine собран и установлен в мастерской"
                } else {
                    "Чертёж Steam Engine: собрано $owned из $total частей"
                }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ЧЕРТЁЖ · STEAM ENGINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (unlocked) TealGlow else BrassBright,
                )
                Text(
                    if (unlocked) "МАШИНА СОБРАНА · УСТАНОВЛЕНА В ЦЕХЕ" else "ИНЖЕНЕРНЫЙ КОМПЛЕКТ · $owned/$total",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) TextWarm else TextMuted,
                    maxLines = 2,
                )
            }
            if (!unlocked) {
                BlueprintPips(owned = owned, total = total)
            } else {
                Text("ONLINE", style = MaterialTheme.typography.labelSmall, color = TealGlow)
            }
        }

        Spacer(Modifier.height(8.dp))
        if (unlocked) {
            SteamEngineMachine(
                animationsEnabled = animationsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
            )
        } else {
            SteamEngineSchematic(
                fraction = fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
            )
            Spacer(Modifier.height(7.dp))
            BlueprintProgress(fraction = fraction, owned = owned, total = total)
        }
    }
}

@Composable
private fun BlueprintPips(owned: Int, total: Int) {
    val shown = total.coerceAtMost(8)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(shown) { index ->
            val collected = index < owned
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (collected) TealGlow.copy(alpha = 0.78f) else Recess.copy(alpha = 0.82f))
                    .border(
                        1.dp,
                        if (collected) BlueprintInk.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(2.dp),
                    ),
            )
            if (index != shown - 1) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun BlueprintProgress(fraction: Float, owned: Int, total: Int) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF061116))
                .border(1.dp, BlueprintGrid.copy(alpha = 0.16f), RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Brass.copy(alpha = 0.78f), TealGlow.copy(alpha = 0.88f)),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("СОБРАНО", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.weight(1f))
            Text("$owned / $total", style = MaterialTheme.typography.labelSmall, color = TextWarm)
        }
    }
}

@Composable
private fun SteamEngineSchematic(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val gridColor = BlueprintGrid.copy(alpha = 0.075f)
        for (i in 1 until 8) {
            val x = size.width * i / 8f
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        }
        for (i in 1 until 4) {
            val y = size.height * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }

        val muted = BlueprintInk.copy(alpha = 0.22f)
        val active = BlueprintInk.copy(alpha = 0.78f)
        fun componentColor(threshold: Float): Color = if (fraction >= threshold) active else muted

        val y = size.height * 0.58f
        val left = size.width * 0.25f
        val right = size.width * 0.58f
        val boilerHeight = size.height * 0.28f
        val boilerColor = componentColor(0.20f)
        drawLine(
            boilerColor,
            Offset(left, y - boilerHeight / 2f),
            Offset(right, y - boilerHeight / 2f),
            2.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(
            boilerColor,
            Offset(left, y + boilerHeight / 2f),
            Offset(right, y + boilerHeight / 2f),
            2.dp.toPx(),
            StrokeCap.Round,
        )
        drawCircle(boilerColor, boilerHeight / 2f, Offset(left, y), style = Stroke(2.dp.toPx()))
        drawCircle(boilerColor, boilerHeight / 2f, Offset(right, y), style = Stroke(2.dp.toPx()))

        val chimneyColor = componentColor(0.40f)
        val chimneyX = size.width * 0.34f
        drawLine(
            chimneyColor,
            Offset(chimneyX, y - boilerHeight * 0.42f),
            Offset(chimneyX, size.height * 0.17f),
            2.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(
            chimneyColor,
            Offset(chimneyX - 8.dp.toPx(), size.height * 0.17f),
            Offset(chimneyX + 8.dp.toPx(), size.height * 0.17f),
            2.dp.toPx(),
            StrokeCap.Round,
        )

        val gaugeColor = componentColor(0.55f)
        val gauge = Offset(size.width * 0.49f, size.height * 0.28f)
        drawCircle(gaugeColor, size.height * 0.10f, gauge, style = Stroke(2.dp.toPx()))
        drawLine(
            gaugeColor,
            gauge,
            Offset(gauge.x + size.height * 0.055f, gauge.y - size.height * 0.035f),
            1.5.dp.toPx(),
            StrokeCap.Round,
        )

        val wheelColor = componentColor(0.72f)
        val wheel = Offset(size.width * 0.70f, y)
        val wheelRadius = size.height * 0.28f
        drawCircle(wheelColor, wheelRadius, wheel, style = Stroke(2.dp.toPx()))
        repeat(8) { index ->
            rotate(index * 45f, pivot = wheel) {
                drawLine(
                    wheelColor,
                    wheel,
                    Offset(wheel.x, wheel.y - wheelRadius * 0.82f),
                    1.5.dp.toPx(),
                    StrokeCap.Round,
                )
            }
        }
        drawCircle(wheelColor, wheelRadius * 0.15f, wheel, style = Stroke(1.5.dp.toPx()))

        val linkageColor = componentColor(0.90f)
        drawLine(
            linkageColor,
            Offset(right + boilerHeight / 2f, y),
            Offset(wheel.x - wheelRadius, y),
            2.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(
            muted,
            Offset(size.width * 0.20f, size.height * 0.88f),
            Offset(size.width * 0.80f, size.height * 0.88f),
            2.dp.toPx(),
            StrokeCap.Round,
        )
    }
}

@Composable
private fun SteamEngineMachine(
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val angle = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "steam-engine")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(5_500, easing = LinearEasing), RepeatMode.Restart),
            label = "steam-engine-flywheel",
        )
        animated
    } else 0f

    Canvas(modifier) {
        val y = size.height * 0.58f
        val boilerStart = Offset(size.width * 0.28f, y)
        val boilerEnd = Offset(size.width * 0.57f, y)
        val boilerRadius = size.height * 0.22f

        drawCircle(TealGlow.copy(alpha = 0.06f), size.height * 0.62f, Offset(size.width * 0.51f, y))
        drawLine(
            color = BrassDark.copy(alpha = 0.82f),
            start = boilerStart,
            end = boilerEnd,
            strokeWidth = boilerRadius * 1.55f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Copper.copy(alpha = 0.72f),
            start = boilerStart,
            end = boilerEnd,
            strokeWidth = boilerRadius * 1.02f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = BrassBright.copy(alpha = 0.42f),
            start = Offset(boilerStart.x + boilerRadius * 0.2f, y - boilerRadius * 0.22f),
            end = Offset(boilerEnd.x - boilerRadius * 0.2f, y - boilerRadius * 0.22f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val chimneyX = size.width * 0.36f
        drawLine(
            Copper.copy(alpha = 0.78f),
            Offset(chimneyX, y - boilerRadius * 0.55f),
            Offset(chimneyX, size.height * 0.18f),
            6.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(
            Brass.copy(alpha = 0.72f),
            Offset(chimneyX - 7.dp.toPx(), size.height * 0.18f),
            Offset(chimneyX + 7.dp.toPx(), size.height * 0.18f),
            3.dp.toPx(),
            StrokeCap.Round,
        )

        val gauge = Offset(size.width * 0.49f, size.height * 0.24f)
        drawCircle(Recess, size.height * 0.12f, gauge)
        drawCircle(BrassBright.copy(alpha = 0.80f), size.height * 0.12f, gauge, style = Stroke(2.dp.toPx()))
        drawLine(
            TealGlow.copy(alpha = 0.88f),
            gauge,
            Offset(gauge.x + size.height * 0.065f, gauge.y - size.height * 0.035f),
            2.dp.toPx(),
            StrokeCap.Round,
        )

        val wheel = Offset(size.width * 0.68f, y)
        val wheelRadius = size.height * 0.30f
        rotate(angle, pivot = wheel) {
            repeat(8) { index ->
                rotate(index * 45f, pivot = wheel) {
                    drawLine(
                        BrassBright.copy(alpha = 0.72f),
                        wheel,
                        Offset(wheel.x, wheel.y - wheelRadius * 0.82f),
                        2.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
            }
        }
        drawCircle(Copper.copy(alpha = 0.82f), wheelRadius, wheel, style = Stroke(4.dp.toPx()))
        drawCircle(BrassBright.copy(alpha = 0.72f), wheelRadius * 0.18f, wheel)

        drawLine(
            Copper.copy(alpha = 0.72f),
            Offset(boilerEnd.x, y),
            Offset(wheel.x - wheelRadius * 0.20f, y),
            4.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(
            BrassDark.copy(alpha = 0.70f),
            Offset(size.width * 0.22f, size.height * 0.88f),
            Offset(size.width * 0.76f, size.height * 0.88f),
            5.dp.toPx(),
            StrokeCap.Round,
        )
    }
}
