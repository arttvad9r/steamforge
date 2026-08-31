package com.steamforge.game.ui.achievements

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamLogoHeader
import com.steamforge.game.ui.components.SteamPanel
import com.steamforge.game.ui.components.SteamSectionTitle

@Composable
fun AchievementsScreen(
    vm: AchievementsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val achievementItems by vm.ui.collectAsStateWithLifecycle()
    val unlocked = achievementItems.count { it.unlocked }

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            SteamLogoHeader(
                compact = true,
                leading = { BrassRoundButton("←", "Назад", onBack) },
            )
            Spacer(Modifier.height(10.dp))
            SteamSectionTitle("ДОСТИЖЕНИЯ")
            Spacer(Modifier.height(10.dp))
            SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TealSurface)
                            .border(1.dp, TealGlow, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("★", color = BrassBright, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ПРОГРЕСС ДОСТИЖЕНИЙ", style = MaterialTheme.typography.labelLarge, color = TextWarm)
                        Spacer(Modifier.height(6.dp))
                        ProgressLine(unlocked, achievementItems.size.coerceAtLeast(1))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("$unlocked / ${achievementItems.size}", style = MaterialTheme.typography.titleLarge, color = BrassBright)
                }
            }
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                items(achievementItems, key = { it.def.id }) { item ->
                    AchievementRow(item)
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementUi) {
    val unlocked = item.unlocked
    val hidden = item.def.hidden && !unlocked
    val iconText = achievementIcon(item.def.id, hidden)

    SteamPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${if (hidden) "Скрытое достижение" else item.def.title}: ${if (unlocked) "разблокировано" else "заблокировано"}"
            },
        highlighted = unlocked,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.verticalGradient(
                            if (unlocked) listOf(TealSurface, Color(0xFF18332E)) else listOf(Color(0xFF30271C), Recess),
                        ),
                    )
                    .border(1.dp, if (unlocked) TealGlow.copy(alpha = 0.75f) else Brass.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconText, style = MaterialTheme.typography.titleLarge, color = if (unlocked) BrassBright else TextMuted)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (hidden) "???" else item.def.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (unlocked) BrassBright else TextWarm,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (hidden) "Это достижение пока скрыто" else item.def.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!unlocked && item.def.maxProgress > 1) {
                    Spacer(Modifier.height(6.dp))
                    ProgressLine(item.progress, item.def.maxProgress)
                }
                if (item.unlockDate != null) {
                    Spacer(Modifier.height(3.dp))
                    Text("Открыто: ${item.unlockDate}", color = TealGlow, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("◆ ${item.def.gemReward}", color = if (unlocked) TealGlow else TextMuted, style = MaterialTheme.typography.labelLarge)
                if (!unlocked) Text("⊘", color = Brass, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun achievementIcon(id: String, hidden: Boolean): String {
    if (hidden) return "?"
    return when {
        id.startsWith("tile_") -> id.removePrefix("tile_")
        id.startsWith("score_") -> "★"
        id.startsWith("games_") -> "▣"
        id.startsWith("overdrive") -> "⚡"
        id.startsWith("combo") -> "✦"
        id.startsWith("undo") -> "↶"
        id.startsWith("daily") -> "☼"
        id.startsWith("gems") -> "◆"
        else -> "⚙"
    }
}

@Composable
private fun ProgressLine(progress: Int, max: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Recess)
            .border(1.dp, Brass.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((progress.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f))
                .height(7.dp)
                .background(Brush.horizontalGradient(listOf(TealSurface, TealGlow))),
        )
    }
}
