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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
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
    onCollection: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    HomeContent(
        ui = ui,
        onPlay = onPlay,
        onWorkshop = onWorkshop,
        onContracts = onContracts,
        onDaily = onDaily,
        onCollection = onCollection,
        onSettings = onSettings,
        onProfile = onProfile,
        modifier = modifier,
    )
}

@Composable
internal fun HomeContent(
    ui: HomeUiState,
    onPlay: () -> Unit,
    onWorkshop: () -> Unit,
    onContracts: () -> Unit,
    onDaily: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val compactHeader = LocalConfiguration.current.screenWidthDp < 390
    val visibility = ui.featureVisibility

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
                if (visibility.showCollection) {
                    BrassRoundButton(
                        symbol = "▣",
                        description = "Коллекция",
                        onClick = onCollection,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "STEAMFORGE",
                        style = if (compactHeader) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
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

            if (visibility.showStatusRail) {
                Spacer(Modifier.height(8.dp))
                HomeStatusRail(
                    bestScore = ui.bestScore,
                    workshopLevel = ui.workshopLevel,
                    gems = ui.gems,
                    onProfile = onProfile,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            HomeCoreScene(
                expanded = !visibility.showWorkshop,
                animationsEnabled = ui.animationsEnabled,
            )
            Text(
                "СОБЕРИТЕ МЕХАНИЧЕСКОЕ ЯДРО",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = TextWarm,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (visibility.showWorkshop) {
                    "Объединяйте детали, развивайте мастерскую и доберитесь до 2048."
                } else {
                    "Объединяйте одинаковые детали и доберитесь до 2048."
                },
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

            HomeNavigationDeck(
                visibility = visibility,
                workshopLevel = ui.workshopLevel,
                dailyRewardStreak = ui.dailyRewardStreak,
                dailyDone = ui.dailyDone,
                onWorkshop = onWorkshop,
                onContracts = onContracts,
                onDaily = onDaily,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun HomeCoreScene(
    expanded: Boolean,
    animationsEnabled: Boolean,
) {
    val angle = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "home-core")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
            label = "home-core-angle",
        )
        animated
    } else {
        0f
    }
    val sceneHeight = if (expanded) 310.dp else 184.dp
    val reactorSize = if (expanded) 132.dp else 96.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sceneHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val unit = size.minDimension
            drawCircle(TealGlow.copy(alpha = if (expanded) 0.045f else 0.040f), unit * 0.49f, c)
            drawCircle(Brass.copy(alpha = 0.040f), unit * 0.38f, c)
            drawHomeGear(c, unit * 0.27f, angle, Brass.copy(alpha = 0.54f))
            drawHomeGear(
                Offset(size.width * 0.69f, size.height * 0.65f),
                unit * 0.105f,
                -angle * 1.4f,
                Copper.copy(alpha = 0.58f),
            )
            drawHomeGear(
                Offset(size.width * 0.33f, size.height * 0.39f),
                unit * 0.075f,
                angle * 1.8f,
                BrassDark.copy(alpha = 0.68f),
            )
            drawCircle(BrassDark.copy(alpha = 0.72f), unit * 0.205f, c, style = Stroke(2.dp.toPx()))
            drawCircle(TealGlow.copy(alpha = 0.20f), unit * 0.176f, c, style = Stroke(2.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(reactorSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            TealSurface.copy(alpha = 0.72f),
                            PanelRaised.copy(alpha = 0.94f),
                            Recess,
                        ),
                    ),
                )
                .border(2.dp, BrassDark.copy(alpha = 0.92f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(TealGlow.copy(alpha = 0.10f), Recess)))
                    .border(1.dp, Brass.copy(alpha = 0.66f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "2048",
                        style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                        color = BrassBright,
                    )
                    Text("CORE", style = MaterialTheme.typography.labelSmall, color = TealGlow)
                }
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
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onProfile)
            .semantics {
                role = Role.Button
                contentDescription = "Профиль. Рекорд $bestScore, мастерская уровень $workshopLevel, гемы $gems"
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
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
            .height(28.dp)
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
