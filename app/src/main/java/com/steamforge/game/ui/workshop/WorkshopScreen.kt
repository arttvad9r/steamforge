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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.sound.Sfx
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton

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
    val accent = if (ui.goldGaugeCosmetic) BrassBright else TealGlow
    val haptics = LocalHapticFeedback.current

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            WorkshopHeader(
                gems = ui.gems,
                streak = ui.dailyRewardStreak,
                onAchievements = onAchievements,
                onSettings = onSettings,
            )
            Spacer(Modifier.height(8.dp))

            WorkshopHero(
                level = ui.level,
                levelInfo = ui.levelInfo,
                animationsEnabled = ui.animationsEnabled,
                accent = accent,
                gamesPlayed = ui.gamesPlayed,
                bestScore = ui.bestScore,
            )
            Spacer(Modifier.height(10.dp))

            SteamButton(
                text = "ИГРАТЬ",
                icon = "▶",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            WorkshopMetaRow(
                badge = "2048",
                title = "Испытание дня",
                subtitle = if (ui.dailyDone) "Сегодня выполнено" else "Новая задача на сегодня",
                actionLabel = if (ui.dailyDone) "ВЫПОЛНЕНО" else "ОТКРЫТЬ",
                accent = if (ui.dailyDone) TealGlow else BrassBright,
                enabled = !ui.dailyDone,
                onClick = onDaily,
            )
            Spacer(Modifier.height(8.dp))

            WorkshopMetaRow(
                badge = "◆",
                title = "Ежедневная награда",
                subtitle = if (ui.dailyRewardAvailable) {
                    "День ${ui.dailyRewardDay} · +${ui.dailyRewardGems} гемов"
                } else {
                    "Награда сегодня уже получена"
                },
                actionLabel = if (ui.dailyRewardAvailable) "ПОЛУЧИТЬ" else "ПОЛУЧЕНО",
                accent = if (ui.dailyRewardAvailable) TealGlow else TextMuted,
                enabled = ui.dailyRewardAvailable,
                onClick = {
                    sfx.play(Sfx.COIN)
                    if (ui.hapticsEnabled) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    vm.claimDailyReward()
                },
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun WorkshopHeader(
    gems: Int,
    streak: Int,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "STEAMFORGE",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrassBright,
                )
                Text(
                    "Мастерская",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWarm,
                )
            }
            Spacer(Modifier.width(10.dp))
            BrassRoundButton("★", "Достижения", onAchievements)
            Spacer(Modifier.width(6.dp))
            BrassRoundButton("⚙", "Настройки", onSettings)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompactResource("◆", gems.toString(), TealGlow, "Гемы: $gems")
            Spacer(Modifier.width(8.dp))
            CompactResource("↟", streak.toString(), BrassBright, "Серия дней: $streak")
        }
    }
}

@Composable
private fun CompactResource(
    icon: String,
    value: String,
    accent: Color,
    description: String,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = accent, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(6.dp))
        Text(value, color = TextWarm, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WorkshopHero(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    animationsEnabled: Boolean@Composable
private fun WorkshopHero(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    animationsEnabled: Boolean,
    accent: Color,
    gamesPlayed: Int,
    bestScore: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "УРОВЕНЬ МАСТЕРСКОЙ",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
                Text(
                    "МЕХАНИЧЕСКОЕ ЯДРО",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Text(
                level.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextWarm,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(204.dp),
            contentAlignment = Alignment.Center,
        ) {
            WorkshopScene(animationsEnabled, accent)
            Box(
                Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Recess.copy(alpha = 0.90f))
                    .border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "CORE",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("XP", style = MaterialTheme.typography.labelLarge, color = accent)
            Spacer(Modifier.width(8.dp))
            GaugeBar(levelInfo.fraction, accent, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                "${levelInfo.xpIntoLevel}/${levelInfo.xpToNext}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }

        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InlineMetric("ПАРТИЙ", gamesPlayed.toString(), Modifier.weight(1f))
            Box(
                Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Color.White.copy(alpha = 0.07f)),
            )
            InlineMetric("РЕКОРД", bestScore.toString(), Modifier.weight(1f), BrassBright)
        }
    }
}

 {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
    }
}

@Composable
private fun GaugeBar(fraction: Float, accent: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(9.dp)
            .clip(shape)
            .background(Recess)
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .semantics { contentDescription = "Прогресс уровня: ${(fraction * 100).toInt()} процентов" },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(9.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(Copper.copy(alpha = 0.82f), accent))),
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
    } else 0f

    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Механическое ядро мастерской" }) {
        val c = center
        drawCircle(accent.copy(alpha = 0.075f), radius = size.minDimension * 0.48f, center = c)
        drawCircle(Brass.copy(alpha = 0.06f), radius = size.minDimension * 0.40f, center = c)
        drawGear(c, size.minDimension * 0.30f, angle, Brass.copy(alpha = 0.66f))
        drawGear(
            Offset(size.width * 0.69f, size.height * 0.69f),
            size.minDimension * 0.105f,
            -angle * 1.4f,
            Copper.copy(alpha = 0.72f),
        )
        drawGear(
            Offset(size.width * 0.31f, size.height * 0.36f),
            size.minDimension * 0.075f,
            angle * 1.9f,
            BrassDark.copy(alpha = 0.76f),
        )
        drawCircle(
            accent.copy(alpha = 0.22f),
            radius = size.minDimension * 0.20f,
            center = c,
            style = Stroke(3.dp.toPx()),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGear(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
) {
    rotate(angle, pivot = center) {
        val teeth = 12
        for (i in 0 until teeth) {
            rotate(i * (360f / teeth), pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - radius * 0.78f),
                    end = Offset(center.x, center.y - radius * 1.04f),
                    strokeWidth = radius * 0.15f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(color = color, radius = radius * 0.82f, center = center, style = Stroke(width = radius * 0.18f))
    }
}

@Composable
private fun WorkshopMetaRow(
    badge: String,
    title: String,
    subtitle: String,
    actionLabel: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    val longBadge = badge.length > 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(shape)
            .background(Panel.copy(alpha = 0.58f))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.24f else 0.12f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 11.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $subtitle. $actionLabel"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (longBadge) 42.dp else 36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Recess.copy(alpha = 0.62f))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                badge,
                style = if (longBadge) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                color = accent,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextWarm, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            actionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) accent else TextMuted,
            maxLines = 1,
        )
    }
}
