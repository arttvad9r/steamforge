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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.sound.Sfx
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.StatPlate
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamLogoHeader
import com.steamforge.game.ui.components.SteamPanel

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
            Spacer(Modifier.height(12.dp))
            SteamLogoHeader()
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GemChip(ui.gems, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                StatPlate("СЕРИЯ ДНЕЙ", ui.dailyRewardStreak.toString(), Modifier.weight(1f), BrassBright)
            }
            Spacer(Modifier.height(12.dp))

            WorkshopConsole(
                level = ui.level,
                levelInfo = ui.levelInfo,
                gamesPlayed = ui.gamesPlayed,
                bestScore = ui.bestScore,
                animationsEnabled = ui.animationsEnabled,
                accent = accent,
            )
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                MenuCard("⚒", "ИГРАТЬ", TealSurface, onPlay, Modifier.weight(1f), accent = TealGlow)
                Spacer(Modifier.width(8.dp))
                MenuCard("★", "ДОСТИЖЕНИЯ", Color(0xFF6A3F20), onAchievements, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                MenuCard("⚙", "НАСТРОЙКИ", Color(0xFF2C3436), onSettings, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            SteamPanel(Modifier.fillMaxWidth(), highlighted = !ui.dailyDone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(BrassBright.copy(alpha = 0.45f), Recess)))
                            .border(1.dp, Brass, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("2048", style = MaterialTheme.typography.labelLarge, color = BrassBright)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ЕЖЕДНЕВНОЕ ИСПЫТАНИЕ", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                        Text(
                            if (ui.dailyDone) "Сегодня выполнено" else "Новая задача на сегодня",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ui.dailyDone) TealGlow else TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                SteamButton(
                    text = if (ui.dailyDone) "ВЫПОЛНЕНО" else "ОТКРЫТЬ ИСПЫТАНИЕ",
                    onClick = onDaily,
                    enabled = !ui.dailyDone,
                    modifier = Modifier.fillMaxWidth(),
                    style = if (ui.dailyDone) SteamButtonStyle.Dark else SteamButtonStyle.Brass,
                )
            }
            Spacer(Modifier.height(12.dp))

            SteamPanel(Modifier.fillMaxWidth(), highlighted = ui.dailyRewardAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.radialGradient(listOf(Color(0xFF9F6828), Recess)))
                            .border(1.dp, Brass, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▣", style = MaterialTheme.typography.displaySmall, color = BrassBright)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ЕЖЕДНЕВНАЯ НАГРАДА", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                        Text(
                            if (ui.dailyRewardAvailable) "День ${ui.dailyRewardDay} · +${ui.dailyRewardGems} гемов" else "Награда сегодня уже получена",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ui.dailyRewardAvailable) TealGlow else TextMuted,
                        )
                    }
                }
                if (ui.dailyRewardAvailable) {
                    Spacer(Modifier.height(10.dp))
                    SteamButton(
                        text = "ПОЛУЧИТЬ",
                        icon = "◆",
                        onClick = {
                            sfx.play(Sfx.COIN)
                            if (ui.hapticsEnabled) {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                            vm.claimDailyReward()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun GemChip(gems: Int, modifier: Modifier = Modifier) {
    SteamPanel(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
        Row(
            modifier = Modifier.semantics { contentDescription = "Гемы: $gems" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GemIcon(20)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("ГЕМЫ", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text(gems.toString(), color = TextWarm, style = MaterialTheme.typography.titleLarge)
            }
        }
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
        drawPath(path, TealGlow)
        drawPath(path, Color.White.copy(alpha = 0.55f), style = Stroke(width = s * 0.06f))
        drawLine(Color.White.copy(alpha = 0.45f), Offset(s / 2f, 0f), Offset(s / 2f, s), s * 0.035f)
    }
}

@Composable
private fun WorkshopConsole(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    gamesPlayed: Int,
    bestScore: Int,
    animationsEnabled: Boolean,
    accent: Color,
) {
    SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("УРОВЕНЬ МАСТЕРСКОЙ", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(154.dp), contentAlignment = Alignment.Center) {
                WorkshopScene(animationsEnabled, accent)
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Recess.copy(alpha = 0.88f), radius = size.minDimension * 0.32f, center = center)
                    drawCircle(Brass, radius = size.minDimension * 0.34f, center = center, style = Stroke(3.dp.toPx()))
                }
                Text(level.toString(), style = MaterialTheme.typography.displaySmall, color = TextWarm)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("XP", style = MaterialTheme.typography.titleMedium, color = TealGlow)
                Spacer(Modifier.width(8.dp))
                GaugeBar(levelInfo.fraction, accent, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${levelInfo.xpIntoLevel}/${levelInfo.xpToNext}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                StatPlate("ПАРТИЙ", gamesPlayed.toString(), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                StatPlate("РЕКОРД", bestScore.toString(), Modifier.weight(1f), BrassBright)
            }
        }
    }
}

@Composable
private fun GaugeBar(fraction: Float, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Recess)
            .border(1.dp, Brass.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .semantics { contentDescription = "Прогресс уровня: ${(fraction * 100).toInt()} процентов" },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(listOf(Copper, accent))),
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

    Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Механический манометр мастерской" }) {
        val c = center
        drawCircle(accent.copy(alpha = 0.12f), radius = size.minDimension * 0.48f, center = c)
        drawGear(c, size.minDimension * 0.42f, angle, Brass.copy(alpha = 0.72f))
        drawGear(Offset(size.width * 0.72f, size.height * 0.72f), size.minDimension * 0.14f, -angle * 1.4f, Copper.copy(alpha = 0.78f))
        drawCircle(accent.copy(alpha = 0.32f), radius = size.minDimension * 0.28f, center = c, style = Stroke(6.dp.toPx()))
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
private fun MenuCard(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = BrassBright,
) {
    val shape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(11.dp)
    val outline = if (accent == TealGlow) TealGlow.copy(alpha = 0.78f) else Brass.copy(alpha = 0.90f)

    Box(
        modifier = modifier
            .height(104.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        color.copy(alpha = 0.90f),
                        Panel.copy(alpha = 0.98f),
                        Recess,
                    ),
                ),
            )
            .border(2.dp, outline, shape)
            .clickable(onClick = onClick)
            .padding(3.dp)
            .border(1.dp, BrassBright.copy(alpha = 0.13f), innerShape)
            .semantics { role = Role.Button; contentDescription = label },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 8.dp.toPx()
            val r = 1.45.dp.toPx()
            val rivet = accent.copy(alpha = if (accent == TealGlow) 0.34f else 0.26f)
            val centers = listOf(
                Offset(inset, inset),
                Offset(size.width - inset, inset),
                Offset(inset, size.height - inset),
                Offset(size.width - inset, size.height - inset),
            )
            centers.forEach { center ->
                drawCircle(Color.Black.copy(alpha = 0.42f), r * 1.35f, center + Offset(0.6.dp.toPx(), 0.6.dp.toPx()))
                drawCircle(rivet, r, center)
                drawCircle(Color.White.copy(alpha = 0.14f), r * 0.35f, center - Offset(0.35.dp.toPx(), 0.35.dp.toPx()))
            }
            drawLine(
                Color.White.copy(alpha = 0.07f),
                Offset(inset * 1.8f, 5.dp.toPx()),
                Offset(size.width - inset * 1.8f, 5.dp.toPx()),
                1.dp.toPx(),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Recess)))
                    .border(1.dp, accent.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, style = MaterialTheme.typography.titleLarge, color = accent)
            }
            Spacer(Modifier.weight(1f))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextWarm,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
