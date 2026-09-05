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
                workshopParts = ui.workshopParts,
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
                coreStage = ui.coreStage,
                coreStageLabel = ui.coreStageLabel,
                pressureStage = ui.pressureStage,
                gearPressStage = ui.gearPressStage,
            )
            Spacer(Modifier.height(9.dp))

            SteamEngineBlueprintModule(
                piecesOwned = ui.steamEnginePieces,
                piecesTotal = ui.steamEnginePiecesTotal,
                unlocked = ui.steamEngineUnlocked,
                animationsEnabled = ui.animationsEnabled,
            )
            Spacer(Modifier.height(9.dp))

            MechanismUpgradeSelector(
                mechanisms = ui.mechanisms,
                onUpgrade = { mechanism ->
                    sfx.play(Sfx.COIN)
                    if (ui.hapticsEnabled) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    vm.upgradeMechanism(mechanism)
                },
            )
            Spacer(Modifier.height(9.dp))

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
                    "День ${ui.dailyRewardDay} · +${ui.dailyRewardGems} гемов · +${ui.dailyRewardWorkshopParts} детали"
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
    workshopParts: Int,
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
            CompactResource("⚙", workshopParts.toString(), BrassBright, "Детали мастерской: $workshopParts")
            Spacer(Modifier.width(8.dp))
            CompactResource("◆", gems.toString(), TealGlow, "Гемы: $gems")
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
    animationsEnabled: Boolean,
    accent: Color,
    gamesPlayed: Int,
    bestScore: Int,
    coreStage: Int,
    coreStageLabel: String,
    pressureStage: Int,
    gearPressStage: Int,
) {
    val normalizedStage = coreStage.coerceIn(0, 4)
    val normalizedPressure = pressureStage.coerceIn(0, 4)
    val normalizedPress = gearPressStage.coerceIn(0, 4)
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
                    "МЕХАНИЧЕСКОЕ ЯДРО · $coreStageLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (normalizedStage >= 3) accent else BrassBright,
                    maxLines = 1,
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
            WorkshopScene(
                animationsEnabled = animationsEnabled,
                accent = accent,
                coreStage = normalizedStage,
                pressureStage = normalizedPressure,
                gearPressStage = normalizedPress,
            )
            Box(
                Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Recess.copy(alpha = 0.90f))
                    .border(
                        1.dp,
                        (if (normalizedStage >= 3) accent else BrassDark)
                            .copy(alpha = if (normalizedStage == 0) 0.32f else 0.62f),
                        RoundedCornerShape(26.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "CORE",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (normalizedStage >= 3) accent else TextMuted,
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

@Composable
private fun MechanismUpgradeSelector(
    mechanisms: List<WorkshopMechanismUi>,
    onUpgrade: (com.steamforge.game.progression.WorkshopMechanism) -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.44f))
            .border(1.dp, BrassDark.copy(alpha = 0.34f), shape)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ВОССТАНОВЛЕНИЕ ЦЕХА", style = MaterialTheme.typography.labelMedium, color = TextWarm)
            Spacer(Modifier.weight(1f))
            Text("ВЫБЕРИТЕ УЗЕЛ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Spacer(Modifier.height(5.dp))
        mechanisms.forEachIndexed { index, mechanism ->
            MechanismUpgradeRow(mechanism, onUpgrade = { onUpgrade(mechanism.mechanism) })
            if (index != mechanisms.lastIndex) Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun MechanismUpgradeRow(
    mechanism: WorkshopMechanismUi,
    onUpgrade: () -> Unit,
) {
    val maxed = mechanism.nextCost == null
    val enabled = !maxed && mechanism.canUpgrade
    val accent = when {
        maxed -> TealGlow
        enabled -> BrassBright
        else -> TextMuted
    }
    val action = when {
        maxed -> "ГОТОВО"
        enabled -> "УЛУЧШИТЬ · ⚙ ${mechanism.nextCost}"
        else -> "НУЖНО ⚙ ${mechanism.nextCost}"
    }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Recess.copy(alpha = 0.62f))
            .border(1.dp, accent.copy(alpha = if (enabled || maxed) 0.28f else 0.12f), shape)
            .clickable(enabled = enabled, onClick = onUpgrade)
            .padding(horizontal = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${mechanism.mechanism.title}: ${mechanism.stageLabel}. $action"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                mechanism.mechanism.shortTitle,
                style = MaterialTheme.typography.labelMedium,
                color = TextWarm,
                maxLines = 1,
            )
            Text(
                mechanism.stageLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (mechanism.stage >= 3) TealGlow else TextMuted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(action, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
    }
}

@Composable
private fun InlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TextWarm,
) {
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
private fun WorkshopScene(
    animationsEnabled: Boolean,
    accent: Color,
    coreStage: Int,
    pressureStage: Int,
    gearPressStage: Int,
) {
    val machineryActive = coreStage >= 3 || pressureStage >= 3 || gearPressStage >= 3
    val angle = if (animationsEnabled && machineryActive) {
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
                contentDescription = "Цех мастерской. Ядро: стадия $coreStage. Генератор: стадия $pressureStage. Пресс: стадия $gearPressStage"
            },
    ) {
        val c = center
        val min = size.minDimension

        drawCircle(Brass.copy(alpha = 0.045f + coreStage * 0.012f), radius = min * 0.44f, center = c)

        if (coreStage == 0) {
            drawGear(c, min * 0.27f, 8f, BrassDark.copy(alpha = 0.38f))
            drawLine(
                color = Copper.copy(alpha = 0.34f),
                start = Offset(size.width * 0.24f, size.height * 0.68f),
                end = Offset(size.width * 0.43f, size.height * 0.57f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Copper.copy(alpha = 0.28f),
                start = Offset(size.width * 0.57f, size.height * 0.43f),
                end = Offset(size.width * 0.76f, size.height * 0.32f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        if (coreStage >= 1) {
            drawCircle(
                Brass.copy(alpha = 0.24f),
                radius = min * 0.38f,
                center = c,
                style = Stroke(3.dp.toPx()),
            )
            drawGear(c, min * 0.30f, angle, Brass.copy(alpha = 0.60f + coreStage * 0.05f))
        }

        if (coreStage >= 2) {
            drawGear(
                Offset(size.width * 0.69f, size.height * 0.69f),
                min * 0.105f,
                -angle * 1.4f,
                Copper.copy(alpha = 0.72f),
            )
            drawGear(
                Offset(size.width * 0.31f, size.height * 0.36f),
                min * 0.075f,
                angle * 1.9f,
                BrassDark.copy(alpha = 0.82f),
            )
        }

        if (coreStage >= 3) {
            drawCircle(accent.copy(alpha = 0.07f), radius = min * 0.49f, center = c)
            drawCircle(
                accent.copy(alpha = 0.30f),
                radius = min * 0.20f,
                center = c,
                style = Stroke(3.dp.toPx()),
            )
            drawLine(
                color = Copper.copy(alpha = 0.54f),
                start = Offset(size.width * 0.14f, c.y),
                end = Offset(size.width * 0.31f, c.y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Copper.copy(alpha = 0.54f),
                start = Offset(size.width * 0.69f, c.y),
                end = Offset(size.width * 0.86f, c.y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        if (coreStage >= 4) {
            drawGear(
                Offset(size.width * 0.76f, size.height * 0.31f),
                min * 0.065f,
                angle * 2.2f,
                BrassBright.copy(alpha = 0.86f),
            )
            drawCircle(
                accent.copy(alpha = 0.28f),
                radius = min * 0.48f,
                center = c,
                style = Stroke(2.dp.toPx()),
            )
            drawCircle(accent.copy(alpha = 0.055f), radius = min * 0.56f, center = c)
        }

        val generatorCenter = Offset(size.width * 0.14f, size.height * 0.55f)
        val generatorRadius = min * 0.085f
        val generatorMetal = if (pressureStage >= 3) BrassBright else BrassDark
        drawGear(
            generatorCenter,
            generatorRadius,
            if (pressureStage >= 3) -angle * 1.35f else 0f,
            generatorMetal.copy(alpha = 0.26f + pressureStage * 0.13f),
        )
        if (pressureStage >= 1) {
            drawLine(
                Brass.copy(alpha = 0.50f),
                Offset(generatorCenter.x - generatorRadius, generatorCenter.y + generatorRadius * 1.65f),
                Offset(generatorCenter.x + generatorRadius, generatorCenter.y + generatorRadius * 1.65f),
                3.dp.toPx(),
                StrokeCap.Round,
            )
            drawLine(
                Copper.copy(alpha = 0.46f),
                Offset(generatorCenter.x, generatorCenter.y + generatorRadius),
                Offset(generatorCenter.x, generatorCenter.y + generatorRadius * 1.65f),
                3.dp.toPx(),
                StrokeCap.Round,
            )
        }
        if (pressureStage >= 2) {
            drawCircle(
                Brass.copy(alpha = 0.54f),
                generatorRadius * 1.32f,
                generatorCenter,
                style = Stroke(2.dp.toPx()),
            )
        }
        if (pressureStage >= 3) {
            drawCircle(accent.copy(alpha = 0.24f), generatorRadius * 0.52f, generatorCenter)
            drawLine(
                Copper.copy(alpha = 0.62f),
                Offset(generatorCenter.x + generatorRadius * 1.25f, generatorCenter.y),
                Offset(size.width * 0.31f, c.y),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }
        if (pressureStage >= 4) {
            drawCircle(
                accent.copy(alpha = 0.30f),
                generatorRadius * 1.62f,
                generatorCenter,
                style = Stroke(2.dp.toPx()),
            )
        }

        val pressCenter = Offset(size.width * 0.86f, size.height * 0.55f)
        val pressRadius = min * 0.075f
        val frameAlpha = 0.24f + gearPressStage * 0.11f
        drawLine(
            BrassDark.copy(alpha = frameAlpha),
            Offset(pressCenter.x - pressRadius * 1.45f, pressCenter.y - pressRadius * 1.7f),
            Offset(pressCenter.x - pressRadius * 1.45f, pressCenter.y + pressRadius * 1.7f),
            4.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(
            BrassDark.copy(alpha = frameAlpha),
            Offset(pressCenter.x + pressRadius * 1.45f, pressCenter.y - pressRadius * 1.7f),
            Offset(pressCenter.x + pressRadius * 1.45f, pressCenter.y + pressRadius * 1.7f),
            4.dp.toPx(),
            StrokeCap.Round,
        )
        if (gearPressStage >= 1) {
            drawLine(
                Brass.copy(alpha = 0.56f),
                Offset(pressCenter.x - pressRadius * 1.45f, pressCenter.y - pressRadius * 1.7f),
                Offset(pressCenter.x + pressRadius * 1.45f, pressCenter.y - pressRadius * 1.7f),
                4.dp.toPx(),
                StrokeCap.Round,
            )
            drawLine(
                Brass.copy(alpha = 0.48f),
                Offset(pressCenter.x - pressRadius * 1.7f, pressCenter.y + pressRadius * 1.7f),
                Offset(pressCenter.x + pressRadius * 1.7f, pressCenter.y + pressRadius * 1.7f),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }
        if (gearPressStage >= 2) {
            drawGear(
                pressCenter,
                pressRadius,
                if (gearPressStage >= 3) angle * 1.55f else 0f,
                Copper.copy(alpha = 0.68f),
            )
            drawLine(
                Copper.copy(alpha = 0.56f),
                Offset(pressCenter.x, pressCenter.y - pressRadius * 1.45f),
                Offset(pressCenter.x, pressCenter.y - pressRadius * 0.82f),
                5.dp.toPx(),
                StrokeCap.Round,
            )
        }
        if (gearPressStage >= 3) {
            drawCircle(accent.copy(alpha = 0.20f), pressRadius * 0.45f, pressCenter)
            drawLine(
                Copper.copy(alpha = 0.62f),
                Offset(size.width * 0.69f, c.y),
                Offset(pressCenter.x - pressRadius * 1.35f, pressCenter.y),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }
        if (gearPressStage >= 4) {
            drawGear(
                Offset(pressCenter.x + pressRadius * 1.55f, pressCenter.y - pressRadius * 1.1f),
                pressRadius * 0.48f,
                -angle * 2f,
                BrassBright.copy(alpha = 0.82f),
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
