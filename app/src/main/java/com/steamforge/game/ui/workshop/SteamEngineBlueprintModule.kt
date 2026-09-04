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
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

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
    val shape = RoundedCornerShape(13.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = if (unlocked) 0.62f else 0.44f))
            .border(
                1.dp,
                (if (unlocked) TealGlow else BrassDark).copy(alpha = if (unlocked) 0.34f else 0.28f),
                shape,
            )
            .padding(horizontal = 11.dp, vertical = 9.dp)
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
                    "STEAM ENGINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (unlocked) TealGlow else BrassBright,
                )
                Text(
                    if (unlocked) "ЧЕРТЁЖ СОБРАН · МАШИНА УСТАНОВЛЕНА" else "ЧЕРТЁЖ · $owned/$total",
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

        Spacer(Modifier.height(7.dp))
        if (unlocked) {
            SteamEngineMachine(
                animationsEnabled = animationsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Recess),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(owned.toFloat() / total)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brass.copy(alpha = 0.78f)),
                )
            }
        }
    }
}

@Composable
private fun BlueprintPips(owned: Int, total: Int) {
    val shown = total.coerceAtMost(8)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(shown) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (index < owned) BrassBright else Recess)
                    .border(
                        1.dp,
                        if (index < owned) Brass.copy(alpha = 0.66f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(50),
                    ),
            )
            if (index != shown - 1) Spacer(Modifier.width(4.dp))
        }
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
