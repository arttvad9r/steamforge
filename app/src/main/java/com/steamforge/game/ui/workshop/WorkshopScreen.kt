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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.cosmetics.CosmeticCatalog
import com.steamforge.game.sound.Sfx
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.PanelRaised
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamPanel

@Composable
fun WorkshopScreen(
    vm: WorkshopViewModel,
    sfx: SfxPlayer,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    workshopTheme: String = CosmeticCatalog.WORKSHOP_CLASSIC,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val foundryTheme = workshopTheme == CosmeticCatalog.WORKSHOP_FOUNDRY
    val accent = when {
        ui.goldGaugeCosmetic -> BrassBright
        foundryTheme -> Copper
        else -> TealGlow
    }
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
            WorkshopHeader(
                gems = ui.gems,
                streak = ui.dailyRewardStreak,
                foundryTheme = foundryTheme,
            )
            Spacer(Modifier.height(12.dp))

            WorkshopConsole(
                level = ui.level,
                levelInfo = ui.levelInfo,
                gamesPlayed = ui.gamesPlayed,
                bestScore = ui.bestScore,
                animationsEnabled = ui.animationsEnabled,
                accent = accent,
                steamEngineUnlocked = ui.steamEngineUnlocked,
                foundryTheme = foundryTheme,
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                MenuCard("▶", "ИГРАТЬ", onPlay, Modifier.weight(1f), primary = true)
                Spacer(Modifier.width(8.dp))
                MenuCard("★", "ДОСТИЖЕНИЯ", onAchievements, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                MenuCard("⚙", "НАСТРОЙКИ", onSettings, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            MetaActionPanel(
                badge = "2048",
                title = "Ежедневное испытание",
                subtitle = if (ui.dailyDone) "Сегодня выполнено" else "Новая задача на сегодня",
                accent = if (ui.dailyDone) TealGlow else BrassBright,
            ) {
                SteamButton(
                    text = if (ui.dailyDone) "ВЫПОЛНЕНО" else "ОТКРЫТЬ ИСПЫТАНИЕ",
                    onClick = onDaily,
                    enabled = !ui.dailyDone,
                    modifier = Modifier.fillMaxWidth(),
                    style = if (ui.dailyDone) SteamButtonStyle.Dark else SteamButtonStyle.Brass,
                )
            }
            Spacer(Modifier.height(10.dp))

            MetaActionPanel(
                badge = "◆",
                title = "Ежедневная награда",
                subtitle = if (ui.dailyRewardAvailable) {
                    "День ${ui.dailyRewardDay} · +${ui.dailyRewardGems} гемов"
                } else {
                    "Награда сегодня уже получена"
                },
                accent = if (ui.dailyRewardAvailable) TealGlow else TextMuted,
            ) {
                if (ui.dailyRewardAvailable) {
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
private fun WorkshopHeader(gems: Int, streak: Int, foundryTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("STEAMFORGE", style = MaterialTheme.typography.labelLarge, color = BrassBright)
            Text("Мастерская", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
            Text(
                if (foundryTheme) "FOUNDRY THEME · медь, латунь и тёмная сталь" else "Восстанавливайте ядро и открывайте новые механизмы",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            CompactResource("◆", gems.toString(), TealGlow, "Гемы: $gems")
            Spacer(Modifier.height(6.dp))
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
            .background(Panel.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
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
private fun WorkshopConsole(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    gamesPlayed: Int,
    bestScore: Int,
    animationsEnabled: Boolean,
    accent: Color,
    steamEngineUnlocked: Boolean,
    foundryTheme: Boolean,
) {
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        highlighted = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("УРОВЕНЬ МАСТЕРСКОЙ", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(level.toString(), style = MaterialTheme.typography.displaySmall, color = TextWarm)
            Spacer(Modifier.height(2.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (steamEngineUnlocked) 194.dp else 174.dp),
                contentAlignment = Alignment.Center,
            ) {
                WorkshopScene(animationsEnabled, accent, steamEngineUnlocked, foundryTheme)
                Box(
                    Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Recess.copy(alpha = 0.88f))
                        .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("CORE", style = MaterialTheme.typography.titleMedium, color = accent)
                }
                if (steamEngineUnlocked) {
                    Text(
                        "STEAM ENGINE ONLINE",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealGlow,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                InlineMetric("ПАРТИЙ", gamesPlayed.toString(), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                InlineMetric("РЕКОРД", bestScore.toString(), Modifier.weight(1f), BrassBright)
            }
        }
    }
}

@Composable
private fun InlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TextWarm,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Recess.copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
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
private fun WorkshopScene(
    animationsEnabled: Boolean,
    accent: Color,
    steamEngineUnlocked: Boolean,
    foundryTheme: Boolean,
) {
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

    Canvas(
        Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = if (steamEngineUnlocked) {
                    "Механическое ядро мастерской и установленный паровой двигатель"
                } else {
                    "Механическое ядро мастерской"
                }
            },
    ) {
        val c = center
        drawCircle((if (foundryTheme) Copper else accent).copy(alpha = if (foundryTheme) 0.13f else 0.075f), radius = size.minDimension * 0.48f, center = c)
        drawCircle((if (foundryTheme) BrassBright else Brass).copy(alpha = if (foundryTheme) 0.09f else 0.06f), radius = size.minDimension * 0.40f, center = c)
        drawGear(c, size.minDimension * 0.30f, angle, (if (foundryTheme) Copper else Brass).copy(alpha = 0.70f))
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

        if (steamEngineUnlocked) {
            val line = BrassBright.copy(alpha = 0.72f)
            val body = Copper.copy(alpha = 0.48f)
            val leftX = size.width * 0.08f
            val top = size.height * 0.34f
            val boilerW = size.width * 0.16f
            val boilerH = size.height * 0.34f
            drawRect(
                color = body,
                topLeft = Offset(leftX, top),
                size = androidx.compose.ui.geometry.Size(boilerW, boilerH),
            )
            drawRect(
                color = line,
                topLeft = Offset(leftX, top),
                size = androidx.compose.ui.geometry.Size(boilerW, boilerH),
                style = Stroke(2.dp.toPx()),
            )
            drawCircle(TealGlow.copy(alpha = 0.42f), size.minDimension * 0.035f, Offset(leftX + boilerW * 0.5f, top + boilerH * 0.24f))
            drawLine(
                line,
                Offset(leftX + boilerW, top + boilerH * 0.52f),
                Offset(c.x - size.minDimension * 0.30f, c.y),
                3.dp.toPx(),
                StrokeCap.Round,
            )

            val pistonShift = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * size.height * 0.025f
            val pistonX = size.width * 0.79f
            val pistonY = size.height * 0.43f + pistonShift
            drawRect(
                color = PanelRaised.copy(alpha = 0.92f),
                topLeft = Offset(pistonX, pistonY),
                size = androidx.compose.ui.geometry.Size(size.width * 0.10f, size.height * 0.18f),
            )
            drawRect(
                color = line,
                topLeft = Offset(pistonX, pistonY),
                size = androidx.compose.ui.geometry.Size(size.width * 0.10f, size.height * 0.18f),
                style = Stroke(2.dp.toPx()),
            )
            drawLine(
                line,
                Offset(c.x + size.minDimension * 0.28f, c.y),
                Offset(pistonX, pistonY + size.height * 0.09f),
                3.dp.toPx(),
                StrokeCap.Round,
            )
            drawLine(
                TealGlow.copy(alpha = 0.30f),
                Offset(leftX + boilerW * 0.5f, top),
                Offset(leftX + boilerW * 0.5f, top - size.height * 0.10f),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val shape = RoundedCornerShape(13.dp)
    val accent = if (primary) TealGlow else BrassBright
    val background = if (primary) {
        Brush.verticalGradient(listOf(TealSurface.copy(alpha = 0.82f), Panel, Recess))
    } else {
        Brush.verticalGradient(listOf(PanelRaised.copy(alpha = 0.88f), Panel, Recess))
    }

    Column(
        modifier = modifier
            .height(86.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, accent.copy(alpha = if (primary) 0.66f else 0.28f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .semantics { role = Role.Button; contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge, color = accent)
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

@Composable
private fun MetaActionPanel(
    badge: String,
    title: String,
    subtitle: String,
    accent: Color,
    action: @Composable () -> Unit,
) {
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(10.dp)
            Box(
                Modifier
                    .size(42.dp)
                    .clip(shape)
                    .background(PanelRaised.copy(alpha = 0.74f))
                    .border(1.dp, accent.copy(alpha = 0.35f), shape),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextWarm)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = accent)
            }
        }
        Spacer(Modifier.height(10.dp))
        action()
    }
}
