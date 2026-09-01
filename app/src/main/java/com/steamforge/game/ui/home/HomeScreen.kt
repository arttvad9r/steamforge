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
import com.steamforge.game.ui.components.SteamPanel

private val EventOrange = Color(0xFFE08A3A)

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onPlay: () -> Unit,
    onWorkshop: () -> Unit,
    onEvent: () -> Unit,
    onWeekly: () -> Unit,
    onContracts: () -> Unit,
    onBlueprints: () -> Unit,
    onDaily: () -> Unit,
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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STEAMFORGE", style = MaterialTheme.typography.displaySmall, color = BrassBright, textAlign = TextAlign.Center)
                    Text("MECHANICAL 2048", style = MaterialTheme.typography.labelLarge, color = TextMuted, textAlign = TextAlign.Center)
                }
                BrassRoundButton("⚙", "Настройки", onSettings, Modifier.align(Alignment.CenterEnd))
            }
            Spacer(Modifier.height(12.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                HomeCoreScene()
                Spacer(Modifier.height(2.dp))
                Text("СОБЕРИТЕ МЕХАНИЧЕСКОЕ ЯДРО", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, color = TextWarm, textAlign = TextAlign.Center)
                Text("Объединяйте детали, развивайте мастерскую и доберитесь до 2048.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = TextMuted, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))

            SteamButton(
                text = if (ui.hasSavedRun) "ПРОДОЛЖИТЬ" else "ИГРАТЬ",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
                icon = "▶",
            )
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                HomeMetric("РЕКОРД", ui.bestScore.toString(), Modifier.weight(1f), BrassBright)
                Spacer(Modifier.width(8.dp))
                HomeMetric("МАСТЕРСКАЯ", "LV ${ui.workshopLevel}", Modifier.weight(1f), TextWarm)
                Spacer(Modifier.width(8.dp))
                HomeMetric("ГЕМЫ", ui.gems.toString(), Modifier.weight(1f), TealGlow)
            }
            Spacer(Modifier.height(14.dp))

            HomeEntryCard("⚒", "Мастерская", "Ядро · уровень ${ui.workshopLevel} · серия ${ui.dailyRewardStreak}", onWorkshop, Modifier.fillMaxWidth(), primary = true)
            Spacer(Modifier.height(8.dp))

            if (ui.eventEnabled) {
                HomeEntryCard(
                    icon = if (ui.eventRewardAvailable) "◆" else "F",
                    title = "Foundry Week",
                    subtitle = when {
                        ui.eventRewardAvailable -> "Награда готова · ${ui.eventPoints} pressure"
                        else -> "${ui.eventPoints} pressure · ${ui.eventDaysRemaining} дн. осталось"
                    },
                    onClick = onEvent,
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (ui.eventRewardAvailable) TealGlow else EventOrange,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (ui.weeklyEnabled) {
                HomeEntryCard(
                    icon = if (ui.weeklyRewardAvailable) "◆" else "W",
                    title = "Недельный турнир",
                    subtitle = when {
                        ui.weeklyRewardAvailable -> "Награда готова · verified best ${ui.weeklyBestScore}"
                        ui.weeklyBestScore > 0 -> "Лучший ${ui.weeklyBestScore} · ${ui.weeklyDaysRemaining} дн. до ротации"
                        else -> "Один seed на неделю · ${ui.weeklyDaysRemaining} дн. осталось"
                    },
                    onClick = onWeekly,
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (ui.weeklyRewardAvailable) TealGlow else BrassBright,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (ui.contractsEnabled) {
                HomeEntryCard("≡", "Контракты", "3 задания на сегодня · награды за обычную игру", onContracts, Modifier.fillMaxWidth(), accent = TealGlow)
                Spacer(Modifier.height(8.dp))
            }

            if (ui.dailyEnabled) {
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
                        icon = "⌁",
                        title = "Чертежи",
                        subtitle = "${ui.blueprintsCollected}/${ui.blueprintsTotal} · Steam Engine",
                        onClick = onBlueprints,
                        modifier = Modifier.weight(1f),
                        accent = if (ui.blueprintsCollected == ui.blueprintsTotal) TealGlow else BrassBright,
                    )
                }
            } else {
                HomeEntryCard(
                    icon = "⌁",
                    title = "Чертежи",
                    subtitle = "${ui.blueprintsCollected}/${ui.blueprintsTotal} · Steam Engine",
                    onClick = onBlueprints,
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (ui.blueprintsCollected == ui.blueprintsTotal) TealGlow else BrassBright,
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

    Box(Modifier.fillMaxWidth().height(184.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val unit = size.minDimension
            drawCircle(TealGlow.copy(alpha = 0.055f), unit * 0.48f, c)
            drawCircle(Brass.copy(alpha = 0.045f), unit * 0.37f, c)
            drawHomeGear(c, unit * 0.27f, angle, Brass.copy(alpha = 0.62f))
            drawHomeGear(Offset(size.width * 0.68f, size.height * 0.66f), unit * 0.105f, -angle * 1.4f, Copper.copy(alpha = 0.66f))
            drawHomeGear(Offset(size.width * 0.34f, size.height * 0.39f), unit * 0.075f, angle * 1.8f, BrassDark.copy(alpha = 0.74f))
            drawCircle(TealGlow.copy(alpha = 0.26f), unit * 0.18f, c, style = Stroke(3.dp.toPx()))
        }
        Box(
            Modifier.size(90.dp).clip(RoundedCornerShape(28.dp)).background(Brush.radialGradient(listOf(TealSurface.copy(alpha = 0.74f), Recess))).border(1.dp, Brass.copy(alpha = 0.58f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("2048", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
                Text("CORE", style = MaterialTheme.typography.labelSmall, color = TealGlow)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHomeGear(center: Offset, radius: Float, angle: Float, color: Color) {
    rotate(angle, pivot = center) {
        repeat(12) { index ->
            rotate(index * 30f, pivot = center) {
                drawLine(color, Offset(center.x, center.y - radius * 0.78f), Offset(center.x, center.y - radius * 1.04f), radius * 0.14f, StrokeCap.Round)
            }
        }
        drawCircle(color, radius * 0.82f, center, style = Stroke(radius * 0.17f))
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier = Modifier, accent: Color = TextWarm) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.clip(shape).background(Panel.copy(alpha = 0.74f)).border(1.dp, Color.White.copy(alpha = 0.055f), shape).padding(horizontal = 8.dp, vertical = 7.dp),
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
    primary: Boolean = false,
    accent: Color = TealGlow,
) {
    val shape = RoundedCornerShape(13.dp)
    val background = if (primary) Brush.verticalGradient(listOf(TealSurface.copy(alpha = 0.64f), PanelRaised, Panel)) else Brush.verticalGradient(listOf(PanelRaised.copy(alpha = 0.76f), Panel))
    Row(
        modifier = modifier
            .height(if (primary) 72.dp else 68.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, accent.copy(alpha = if (primary) 0.52f else 0.24f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics { role = Role.Button; contentDescription = "$title. $subtitle" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(if (primary) 42.dp else 38.dp).clip(RoundedCornerShape(10.dp)).background(Recess.copy(alpha = 0.62f)).border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, style = MaterialTheme.typography.labelLarge, color = accent)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextWarm, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
        }
        if (primary) Text("›", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
    }
}
