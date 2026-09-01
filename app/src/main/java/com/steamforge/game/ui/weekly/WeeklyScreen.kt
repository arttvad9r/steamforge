package com.steamforge.game.ui.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
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

@Composable
fun WeeklyScreen(
    vm: WeeklyViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("WEEKLY FORGE", style = MaterialTheme.typography.labelLarge, color = TealGlow)
                    Text("Недельное испытание", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                }
                Text(
                    "${ui.daysRemaining} дн.",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrassBright,
                )
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
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    TealSurface.copy(alpha = 0.46f),
                                    PanelRaised.copy(alpha = 0.92f),
                                    Recess,
                                ),
                            ),
                        )
                        .border(1.dp, TealGlow.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ONE SEED", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        Text("7 DAYS", style = MaterialTheme.typography.displaySmall, color = BrassBright)
                        Text(
                            ui.challenge.id.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TealGlow,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Один и тот же seed для всех попыток недели. Отмена и Ключ отключены, а итоговый результат принимается только после полного replay-пересчёта.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeeklyMetric(
                    label = "ЛУЧШИЙ СЧЁТ",
                    value = if (ui.hasVerifiedRun) ui.bestScore.toString() else "—",
                    modifier = Modifier.weight(1f),
                    accent = BrassBright,
                )
                WeeklyMetric(
                    label = "ХОДОВ",
                    value = if (ui.hasVerifiedRun) ui.bestMoveCount.toString() else "—",
                    modifier = Modifier.weight(1f),
                    accent = TextWarm,
                )
                WeeklyMetric(
                    label = "ГЕМЫ",
                    value = ui.gems.toString(),
                    modifier = Modifier.weight(1f),
                    accent = TealGlow,
                )
            }
            Spacer(Modifier.height(12.dp))

            SteamButton(
                text = if (ui.hasVerifiedRun) "УЛУЧШИТЬ РЕЗУЛЬТАТ" else "НАЧАТЬ ЗАБЕГ",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
                icon = "▶",
            )
            Spacer(Modifier.height(12.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(PanelRaised)
                            .border(1.dp, Brass.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("◆", style = MaterialTheme.typography.titleLarge, color = TealGlow)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Недельная награда", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                        Text(
                            "+${ui.challenge.rewardGems} гемов · 1 недостающая деталь Steam Engine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                        Text(
                            "Чертежи ${ui.blueprintsCollected}/${ui.blueprintsTotal}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrassBright,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    ui.rewardClaimed -> Text(
                        "НАГРАДА ПОЛУЧЕНА",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TealGlow,
                        textAlign = TextAlign.Center,
                    )
                    ui.rewardAvailable -> SteamButton(
                        text = "ПОЛУЧИТЬ НАГРАДУ",
                        onClick = vm::claimReward,
                        modifier = Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Brass,
                    )
                    else -> Text(
                        "Завершите хотя бы один verified-забег этой недели.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                Text("РЕЙТИНГ", style = MaterialTheme.typography.labelLarge, color = BrassBright)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Локальный рекорд уже проверяется replay-механизмом. Глобальный percentile и таблица игроков появятся только вместе с серверным leaderboard — сейчас игра не показывает выдуманный ранг.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun WeeklyMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
    }
}
