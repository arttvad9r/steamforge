package com.steamforge.game.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.PanelRaised
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.SeasonalVisuals
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SeasonalBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamPanel

@Composable
fun EventScreen(
    vm: EventViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val visual = SeasonalVisuals.resolve(ui.event.theme)
    val theme = ui.event.theme
    val next = ui.nextTarget
    val progress = if (next == null) 1f else (ui.points.toFloat() / next).coerceIn(0f, 1f)

    SeasonalBackdrop(theme = visual, modifier = modifier) {
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(theme.title, style = MaterialTheme.typography.headlineSmall, color = visual.accent)
                    Text(theme.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Text("${ui.daysRemaining} дн.", style = MaterialTheme.typography.labelLarge, color = visual.secondary)
            }
            Spacer(Modifier.height(12.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    visual.accent.copy(alpha = 0.23f),
                                    PanelRaised,
                                    Recess,
                                ),
                            ),
                        )
                        .border(1.dp, visual.accent.copy(alpha = 0.36f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(theme.scoreLabel, style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        Text(ui.points.toString(), style = MaterialTheme.typography.displaySmall, color = visual.accent)
                        Text(
                            if (next == null) "ВСЕ РУБЕЖИ ДОСТИГНУТЫ" else "СЛЕДУЮЩИЙ РУБЕЖ · $next",
                            style = MaterialTheme.typography.labelMedium,
                            color = visual.secondary,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Recess),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(9.dp)
                            .background(Brush.horizontalGradient(listOf(visual.accent, visual.secondary))),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    theme.rulesText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                SteamButton(
                    text = "ИГРАТЬ",
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth(),
                    style = SteamButtonStyle.Teal,
                    icon = "▶",
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(theme.milestonesTitle, style = MaterialTheme.typography.labelLarge, color = visual.secondary, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))

            ui.milestones.forEach { item ->
                val milestone = item.definition
                SteamPanel(
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = item.claimable,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(visual.accent.copy(alpha = if (item.claimed) 0.08f else 0.15f))
                                .border(1.dp, visual.accent.copy(alpha = 0.36f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (item.claimed) "✓" else milestone.targetPoints.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (item.claimed) TealGlow else visual.accent,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${milestone.targetPoints} ${theme.milestoneUnit}", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                            val rewardText = buildString {
                                if (milestone.reward.gems > 0) append("+${milestone.reward.gems} гемов")
                                if (milestone.reward.blueprintPieces > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("деталь чертежа")
                                }
                                milestone.reward.cosmeticId?.let {
                                    if (isNotEmpty()) append(" · ")
                                    append("косметика")
                                }
                            }
                            Text(rewardText, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                    if (item.claimable) {
                        Spacer(Modifier.height(8.dp))
                        SteamButton(
                            text = "ПОЛУЧИТЬ",
                            onClick = { vm.claim(milestone.id) },
                            modifier = Modifier.fillMaxWidth(),
                            style = SteamButtonStyle.Brass,
                        )
                    } else if (item.claimed) {
                        Spacer(Modifier.height(6.dp))
                        Text("ПОЛУЧЕНО", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, color = TealGlow, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
