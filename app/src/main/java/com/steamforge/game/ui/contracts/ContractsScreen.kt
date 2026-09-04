package com.steamforge.game.ui.contracts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.progression.ContractReward
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop

@Composable
fun ContractsScreen(
    vm: ContractsViewModel,
    onBack: () -> Unit,
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
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            ContractsHeader(workshopParts = ui.workshopParts, onBack = onBack)
            Spacer(Modifier.height(14.dp))

            TodaySummary(completed = ui.completed, total = ui.items.size.coerceAtLeast(1))
            Spacer(Modifier.height(12.dp))

            ui.items.forEachIndexed { index, item ->
                ContractRow(item = item, onClaim = { vm.claim(item.def.id) })
                if (index != ui.items.lastIndex) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Прогресс контрактов сохраняется автоматически.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ContractsHeader(workshopParts: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrassRoundButton("←", "Назад", onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("КОНТРАКТЫ", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
            Text(
                "Ежедневные задачи мастерской",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        CompactWorkshopParts(workshopParts)
    }
}

@Composable
private fun TodaySummary(completed: Int, total: Int) {
    val shape = RoundedCornerShape(13.dp)
    val fraction = completed.toFloat() / total.coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.50f))
            .border(1.dp, BrassDark.copy(alpha = 0.28f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "Контракты сегодня: выполнено $completed из $total"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("СЕГОДНЯ", style = MaterialTheme.typography.labelLarge, color = BrassBright)
                Text(
                    if (completed >= total) "Все контракты выполнены" else "Выполняются в обычных партиях",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "$completed/$total",
                style = MaterialTheme.typography.titleLarge,
                color = if (completed >= total) TealGlow else TextWarm,
            )
        }
        Spacer(Modifier.height(9.dp))
        ContractProgress(fraction = fraction, modifier = Modifier.fillMaxWidth(), strong = completed >= total)
    }
}

@Composable
private fun ContractRow(item: ContractItemUi, onClaim: () -> Unit) {
    val accent = when {
        item.claimed -> TealGlow.copy(alpha = 0.62f)
        item.complete -> TealGlow
        else -> BrassBright
    }
    val shape = RoundedCornerShape(13.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = if (item.complete && !item.claimed) 0.66f else 0.48f))
            .border(
                1.dp,
                accent.copy(alpha = if (item.complete && !item.claimed) 0.34f else 0.16f),
                shape,
            )
            .padding(horizontal = 11.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "${item.def.title}: ${item.progress} из ${item.def.target}. ${rewardDescription(item.def.reward)}"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContractBadge(icon = contractIcon(item), accent = accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.def.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWarm,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    item.def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    rewardLabel(item.def.reward),
                    style = MaterialTheme.typography.labelLarge,
                    color = rewardAccent(item.def.reward),
                    maxLines = 1,
                )
                Text(
                    when {
                        item.claimed -> "ПОЛУЧЕНО"
                        item.complete -> "ГОТОВО"
                        else -> "НАГРАДА"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.complete) accent else TextMuted,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContractProgress(item.fraction, Modifier.weight(1f), strong = item.complete)
            Spacer(Modifier.width(9.dp))
            Text(
                if (item.claimed) "ГОТОВО" else "${item.progress}/${item.def.target}",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.complete) TealGlow else TextMuted,
                maxLines = 1,
            )
        }

        if (item.complete && !item.claimed) {
            Spacer(Modifier.height(9.dp))
            ContractClaimAction(reward = item.def.reward, onClaim = onClaim)
        }
    }
}

@Composable
private fun ContractBadge(icon: String, accent: Color) {
    val longBadge = icon.length > 2
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(if (longBadge) 46.dp else 42.dp)
            .clip(shape)
            .background(Recess.copy(alpha = 0.66f))
            .border(1.dp, accent.copy(alpha = 0.24f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            icon,
            style = if (longBadge) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
            color = accent,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ContractClaimAction(reward: ContractReward, onClaim: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(TealGlow.copy(alpha = 0.14f))
            .border(1.dp, TealGlow.copy(alpha = 0.46f), shape)
            .clickable(onClick = onClaim)
            .semantics {
                role = Role.Button
                contentDescription = "Получить. ${rewardDescription(reward)}"
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("ПОЛУЧИТЬ", style = MaterialTheme.typography.labelLarge, color = TealGlow)
        Spacer(Modifier.weight(1f))
        Text(rewardLabel(reward), style = MaterialTheme.typography.labelLarge, color = TextWarm, maxLines = 1)
    }
}

@Composable
private fun ContractProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(7.dp)
            .clip(shape)
            .background(Recess)
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(shape)
                .background(TealGlow.copy(alpha = if (strong) 0.90f else 0.68f)),
        )
    }
}

@Composable
private fun CompactWorkshopParts(parts: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel.copy(alpha = 0.66f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .semantics { contentDescription = "Детали мастерской: $parts" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⚙", color = BrassBright, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(5.dp))
        Text(parts.toString(), color = TextWarm, style = MaterialTheme.typography.labelLarge)
    }
}

private fun rewardLabel(reward: ContractReward): String = when (reward) {
    is ContractReward.WorkshopParts -> "⚙ ${reward.amount}"
    is ContractReward.BlueprintPiece -> "▧ ЧЕРТЁЖ"
}

private fun rewardDescription(reward: ContractReward): String = when (reward) {
    is ContractReward.WorkshopParts -> "Награда ${reward.amount} деталей мастерской"
    is ContractReward.BlueprintPiece -> "Награда: фрагмент чертежа"
}

private fun rewardAccent(reward: ContractReward): Color = when (reward) {
    is ContractReward.WorkshopParts -> BrassBright
    is ContractReward.BlueprintPiece -> TealGlow
}

private fun contractIcon(item: ContractItemUi): String = when (item.def.type) {
    com.steamforge.game.progression.ContractType.MAKE_TILE -> "2048"
    com.steamforge.game.progression.ContractType.MERGE_COUNT -> "⇄"
    com.steamforge.game.progression.ContractType.SCORE -> "★"
    com.steamforge.game.progression.ContractType.PLAY_RUNS -> "▶"
    com.steamforge.game.progression.ContractType.SURVIVE_MOVES -> "↟"
    com.steamforge.game.progression.ContractType.OVERDRIVE -> "⚡"
}
