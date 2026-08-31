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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.sound.Sfx
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.Steel
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

private val OutlineDim = Color(0xFF5A4632)

@Composable
fun WorkshopScreen(
    vm: WorkshopViewModel,
    sfx: SfxPlayer,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val accent = if (ui.goldGaugeCosmetic) BrassBright else Brass
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 560.dp)
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("STEAMFORGE", style = MaterialTheme.typography.displaySmall, color = accent)
            Spacer(Modifier.weight(1f))
            GemChip(ui.gems)
        }
        Spacer(Modifier.height(14.dp))

        WorkshopPanel(
            level = ui.level,
            levelInfo = ui.levelInfo,
            gamesPlayed = ui.gamesPlayed,
            bestScore = ui.bestScore,
            animationsEnabled = ui.animationsEnabled,
            accent = accent,
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brass, contentColor = Color(0xFF241708)),
        ) {
            Text("НАЧАТЬ СМЕНУ", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))

        if (ui.dailyRewardAvailable) {
            OutlinedButton(
                onClick = {
                    sfx.play(Sfx.COIN)
                    if (ui.hapticsEnabled) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    vm.claimDailyReward()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Дневная награда · день ${ui.dailyRewardDay} · +${ui.dailyRewardGems} гемов",
                    color = BrassBright,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = onDaily,
            enabled = !ui.dailyDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.dailyDone) "Испытание дня — выполнено" else "Испытание дня")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAchievements, modifier = Modifier.fillMaxWidth()) {
            Text("Достижения (${ui.achievementsUnlocked})")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Настройки")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GemChip(gems: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = "Гемы: $gems" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GemIcon()
        Spacer(Modifier.width(6.dp))
        Text(gems.toString(), color = TextWarm, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun GemIcon(sizeDp: Int = 12) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(s / 2f, 0f)
            lineTo(s, s * 0.32f)
            lineTo(s / 2f, s)
            lineTo(0f, s * 0.32f)
            close()
        }
        drawPath(path, BrassBright)
        drawPath(path, Brass.copy(alpha = 0.7f), style = Stroke(width = s * 0.08f))
    }
}

@Composable
private fun WorkshopPanel(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    gamesPlayed: Int,
    bestScore: Int,
    animationsEnabled: Boolean,
    accent: Color,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.5.dp, OutlineDim, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Цех $level", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                Spacer(Modifier.weight(1f))
                Text(
                    "${levelInfo.xpIntoLevel}/${levelInfo.xpToNext} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(8.dp))
            GaugeBar(levelInfo.fraction, accent)
            Spacer(Modifier.height(12.dp))
            WorkshopScene(animationsEnabled, accent)
            Spacer(Modifier.height(12.dp))
            Row {
                StatCell("Партий", gamesPlayed.toString())
                Spacer(Modifier.width(12.dp))
                StatCell("Рекорд", bestScore.toString())
            }
        }
    }
}

@Composable
private fun GaugeBar(fraction: Float, accent: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Recess)
            .semantics { contentDescription = "Прогресс уровня: ${(fraction * 100).toInt()} процентов" },
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Copper, accent))),
        )
    }
}

@Composable
private fun WorkshopScene(animationsEnabled: Boolean, accent: Color) {
    val angle = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "gears")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
            label = "angle",
        )
        animated
    } else {
        0f
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .semantics { contentDescription = "Мастерская" },
    ) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = accent.copy(alpha = 0.10f),
            radius = 26.dp.toPx(),
            center = Offset(w * 0.87f, h * 0.22f),
        )
        drawCircle(color = BrassBright.copy(alpha = 0.9f), radius = 6.dp.toPx(), center = Offset(w * 0.87f, h * 0.22f))
        drawLine(
            color = Steel,
            start = Offset(w * 0.06f, h * 0.15f),
            end = Offset(w * 0.06f, h * 0.9f),
            strokeWidth = 10.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Steel,
            start = Offset(w * 0.06f, h * 0.15f),
            end = Offset(w * 0.30f, h * 0.15f),
            strokeWidth = 10.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawGear(Offset(w * 0.40f, h * 0.55f), 30.dp.toPx(), angle, accent.copy(alpha = 0.75f))
        drawGear(Offset(w * 0.62f, h * 0.62f), 20.dp.toPx(), -angle * 1.6f, Copper.copy(alpha = 0.8f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGear(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
) {
    rotate(angle, pivot = center) {
        val teeth = 9
        for (i in 0 until teeth) {
            rotate(i * (360f / teeth), pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - radius * 0.85f),
                    end = Offset(center.x, center.y - radius * 1.25f),
                    strokeWidth = radius * 0.28f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = radius * 0.34f))
        drawCircle(color = color, radius = radius * 0.22f, center = center)
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Recess)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextWarm)
    }
}
