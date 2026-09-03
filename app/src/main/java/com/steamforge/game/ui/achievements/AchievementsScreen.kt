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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("КОЛЛЕКЦИЯ", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("Достижения мастерской", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
                Text(
                    "$unlocked/${achievementItems.size}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (unlocked == achievementItems.size && achievementItems.isNotEmpty()) TealGlow else BrassBright,
                )
            }
            Spacer(Modifier.height(12.dp))

            CollectionSummary(unlocked = unlocked, total = achievementItems.size)
            Spacer(Modifier.height(12.dp))

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
private fun CollectionSummary(unlocked: Int, total: Int) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.50f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "Коллекция: открыто $unlocked из $total достижений"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ПРОГРЕСС КОЛЛЕКЦИИ", style = MaterialTheme.typography.labelLarge, color = BrassBright)
                Text(
                    if (total > 0 && unlocked >= total) "Коллекция собрана" else "Открывайте достижения в обычной игре",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (total > 0) "${((unlocked.toFloat() / total) * 100).toInt()}%" else "0%",
                style = MaterialTheme.typography.titleMedium,
                color = if (total > 0 && unlocked >= total) TealGlow else TextWarm,
            )
        }
        Spacer(Modifier.height(9.dp))
        ProgressLine(unlocked, total.coerceAtLeast(1), Modifier.fillMaxWidth())
    }
}

@Composable
private fun AchievementRow(item: AchievementUi) {
    val unlocked = item.unlocked
    val hidden = item.def.hidden && !unlocked
    val iconText = achievementIcon(item.def.id, hidden)
    val shape = RoundedCornerShape(13.dp)
    val accent = if (unlocked) TealGlow else BrassBright

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = if (unlocked) 0.58f else 0.42f))
            .border(
                1.dp,
                accent.copy(alpha = if (unlocked) 0.28f else 0.10f),
                shape,
            )
            .padding(horizontal = 11.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${if (hidden) "Скрытое достижение" else item.def.title}: ${if (unlocked) "разблокировано" else "заблокировано"}"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AchievementBadge(iconText, unlocked)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (hidden) "Скрытое достижение" else item.def.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (unlocked) TextWarm else TextWarm.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hidden) "Условие откроется после выполнения" else item.def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!unlocked && item.def.maxProgress > 1) {
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressLine(item.progress, item.def.maxProgress, Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${item.progress}/${item.def.maxProgress}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                        )
                    }
                }
                if (item.unlockDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Открыто ${item.unlockDate}",
                        color = TealGlow,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "◆ ${item.def.gemReward}",
                    color = if (unlocked) TealGlow else TextMuted,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Text(
                    if (unlocked) "ОТКРЫТО" else "НАГРАДА",
                    color = if (unlocked) TealGlow.copy(alpha = 0.76f) else TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AchievementBadge(icon: String, unlocked: Boolean) {
    val longBadge = icon.length > 2
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .width(if (longBadge) 50.dp else 42.dp)
            .height(42.dp)
            .clip(shape)
            .background(if (unlocked) TealSurface.copy(alpha = 0.62f) else Recess.copy(alpha = 0.66f))
            .border(
                1.dp,
                if (unlocked) TealGlow.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.045f),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            icon,
            style = if (longBadge) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
            color = if (unlocked) BrassBright else TextMuted,
            maxLines = 1,
            softWrap = false,
        )
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
private fun ProgressLine(
    progress: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(Recess),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((progress.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f))
                .height(6.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(TealSurface, TealGlow))),
        )
    }
}
