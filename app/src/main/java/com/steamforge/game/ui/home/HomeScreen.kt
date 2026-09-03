package com.steamforge.game.ui.home

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onPlay: () -> Unit,
    onWorkshop: () -> Unit,
    onContracts: () -> Unit,
    onDaily: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "STEAMFORGE",
                        style = MaterialTheme.typography.displaySmall,
                        color = BrassBright,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "MECHANICAL 2048",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                BrassRoundButton(
                    symbol = "⚙",
                    description = "Настройки",
                    onClick = onSettings,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(Modifier.height(6.dp))

            HomeCoreScene()
            Text(
                "СОБЕРИТЕ МЕХАНИЧЕСКОЕ ЯДРО",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = TextWarm,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Объединяйте детали, развивайте мастерскую и доберитесь до 2048.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))

            SteamButton(
                text = if (ui.hasSavedRun) "ПРОДОЛЖИТЬ" else "ИГРАТЬ",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
                icon = "▶",
            )
            Spacer(Modifier.height(10.dp))

            HomeStatusRail(
                bestScore = ui.bestScore,
                workshopLevel = ui.workshopLevel,
                gems = ui.gems,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                HomeEntryCard(
                    icon = "⚒",
                    title = "Мастерская",
                    subtitle = "LV ${ui.workshopLevel} · серия ${ui.dailyRewardStreak}",
                    onClick = onWorkshop,
                    modifier = Modifier.weight(1f),
                    accent = TealGlow,
                )
                Spacer(Modifier.width(8.dp))
                HomeEntryCard(
                    icon = "≡",
                    title = "Контракты",
                    subtitle = "3 задания сегодня",
                    onClick = onContracts,
                    modifier = Modifier.weight(1f),
                    accent = TextWarm,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                HomeEntryCard(
                    icon = if (ui.dailyDone) "✓" else "2048",
                    title = "Испытание",
                    subtitle = if (ui.dailyDone) "Выполнено" else "Задача дня",
                    onClick = onDaily,
                    modifier = Modifier.weight(1f),
                    accent = if (ui.dailyDone) TealGlow else BrassBright,
                )
                Spacer(Modifier.width(8.dp))
                HomeEntryCard(
                    icon = "▣",
                    title = "Коллекция",
                    subtitle = "${ui.achievementsUnlocked} открыто",
                    onClick = onAchievements,
                    modifier = Modifier.weight(1f),
                    accent = BrassBright,
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun HomeCoreScene() {
    val transition = rememberInfiniteTransition(label = "home-core")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "home-core-angle",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val unit = size.minDimension
            drawCircle(TealGlow.copy(alpha = 0.055f), unit * 0.48f, c)
            drawCircle(Brass.copy(alpha = 0.045f), unit * 0.37f, c)
            drawHomeGear(c, unit * 0.27f, angle, Brass.copy(alpha = 0.62f))
            drawHomeGear(
                Offset(size.width * 0.68f, size.height * 0.66f),
                unit * 0.105f,
                -angle * 1.4f,
                Copper.copy(alpha = 0.66f),
            )
            drawHomeGear(
                Offset(size.width * 0.34f, size.height * 0.39f),
                unit * 0.075f,
                angle * 1.8f,
                BrassDark.copy(alpha = 0.74f),
            )
            drawCircle(TealGlow.copy(alpha = 0.26f), unit * 0.18f, c, style = Stroke(3.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.radialGradient(listOf(TealSurface.copy(alpha = 0.74f), Recess)))
                .border(1.dp, Brass.copy(alpha = 0.58f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("2048", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
                Text("CORE", style = MaterialTheme.typography.labelSmall, color = TealGlow)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHomeGear(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
) {
    rotate(angle, pivot = center) {
        repeat(12) { index ->
            rotate(index * 30f, pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - radius * 0.78f),
                    end = Offset(center.x, center.y - radius * 1.04f),
                    strokeWidth = radius * 0.14f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(color, radius * 0.82f, center, style = Stroke(radius * 0.17f))
    }
}

@Composable
private fun HomeStatusRail(
    bestScore: Int,
    workshopLevel: Int,
    gems: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.52f))
            .border(1.dp, BrassDark.copy(alpha = 0.30f), shape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeMetric("РЕКОРД", bestScore.toString(), Modifier.weight(1f), BrassBright)
        HomeMetricDivider()
        HomeMetric("МАСТЕРСКАЯ", "LV $workshopLevel", Modifier.weight(1f), TextWarm)
        HomeMetricDivider()
        HomeMetric("ГЕМЫ", gems.toString(), Modifier.weight(1f), TealGlow)
    }
}

@Composable
private fun HomeMetricDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.07f)),
    )
}

@Composable
private fun HomeMetric(
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
private fun HomeEntryCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TealGlow,
) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .height(70.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PanelRaised.copy(alpha = 0.56f),
                        Panel.copy(alpha = 0.72f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.20f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Recess.copy(alpha = 0.52f))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, style = MaterialTheme.typography.labelLarge, color = accent, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextWarm, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        }
    }
}
