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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
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
    ContractsContent(
        ui = ui,
        onBack = onBack,
        onClaim = vm::claim,
        modifier = modifier,
    )
}

@Composable
internal fun ContractsContent(
    ui: ContractsUiState,
    onBack: () -> Unit,
    onClaim: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Spacer(Modifier.height(13.dp))

            TodaySummary(
                completed = ui.completed,
                total = ui.items.size.coerceAtLeast(1),
                firstContractOnboarding = ui.firstContractOnboarding,
            )
            Spacer(Modifier.height(12.dp))

            ui.items.forEachIndexed { index, item ->
                ContractRow(item = item, onClaim = { onClaim(item.def.id) })
                if (index != ui.items.lastIndex) Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
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
private fun TodaySummary(
    completed: Int,
    total: Int,
    firstContractOnboarding: Boolean,
) {
    val shape = RoundedCornerShape(14.dp)
    val fraction = completed.toFloat() / total.coerceAtLeast(1)
    val complete = completed >= total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Recess.copy(alpha = 0.72f))
            .border(1.dp, BrassDark.copy(alpha = 0.22f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                contentDescription = "Контракты сегодня: выполнено $completed из $total"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "СМЕНА · СЕГОДНЯ",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (complete) TealGlow else BrassBright,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        firstContractOnboarding -> "Начните с выделенного задания — прогресс идёт в обычной игре"
                        complete -> "Все задания смены выполнены"
                        else -> "Задания выполняются в обычных партиях"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$completed/$total",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (complete) TealGlow else TextWarm,
                )
                Text(
                    if (complete) "ГОТОВО" else "ЗАДАЧ",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (complete) TealGlow else TextMuted,
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        ContractProgress(
            fraction = fraction,
            modifier = Modifier.fillMaxWidth(),
            strong = complete,
        )
    }
}

@Composable
private fun ContractRow(item: ContractItemUi, onClaim: () -> Unit) {
    val statusAccent = when {
        item.claimed -> TealGlow.copy(alpha = 0.58f)
        item.complete -> TealGlow
        item.recommended -> BrassBright
        else -> BrassDark
    }
    val railAlpha = if (item.claimed) 0.42f else 0.78f
    val shape = RoundedCornerShape(14.dp)
    val surfaceAlpha = when {
        item.complete && !item.claimed -> 0.80f
        item.recommended -> 0.74f
        else -> 0.68f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Recess.copy(alpha = surfaceAlpha))
            .drawBehind {
                drawRect(
                    color = statusAccent.copy(alpha = railAlpha),
                    size = Size(4.dp.toPx(), size.height),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape)
            .padding(start = 15.dp, end = 11.dp, top = 10.dp, bottom = 10.dp)
            .semantics {
                contentDescription = buildString {
                    if (item.recommended) append("Рекомендуемый первый контракт. ")
                    append("${item.def.title}: ${item.progress} из ${item.def.target}. ${rewardDescription(item.def.reward)}")
                }
            },
    ) {
        if (item.recommended) {
            Text(
                "ПЕРВЫЙ ШАГ",
                style = MaterialTheme.typography.labelSmall,
                color = BrassBright,
                maxLines = 1,
            )
            Spacer(Modifier.height(5.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ContractBadge(
                icon = contractIcon(item),
                accent = when {
                    item.complete -> TealGlow
                    item.recommended -> BrassBright
                    else -> TextMuted
                },
            )
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
            Spacer(Modifier.width(10.dp))
            RewardBlock(item)
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContractProgress(
                fraction = item.fraction,
                modifier = Modifier.weight(1f),
                strong = item.complete,
            )
            Spacer(Modifier.width(10.dp))
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
private fun RewardBlock(item: ContractItemUi) {
    val reward = item.def.reward
    val accent = rewardAccent(reward)
    Column(horizontalAlignment = Alignment.End) {
        Text(
            rewardLabel(reward),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            maxLines = 1,
        )
        Text(
            when {
                item.claimed -> "ПОЛУЧЕНО"
                item.complete -> "ГОТОВО"
                else -> "НАГРАДА"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (item.complete) TealGlow else TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun ContractBadge(icon: String, accent: Color) {
    val longBadge = icon.length > 2
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(Panel.copy(alpha = 0.44f))
            .border(1.dp, accent.copy(alpha = 0.18f), shape),
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
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(TealGlow.copy(alpha = 0.11f))
            .border(1.dp, TealGlow.copy(alpha = 0.36f), shape)
            .clickable(onClick = onClaim)
            .semantics {
                role = Role.Button
                contentDescription = "Получить. ${rewardDescription(reward)}"
            }
            .padding(horizontal = 11.dp),
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
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(shape)
                .background(TealGlow.copy(alpha = if (strong) 0.92f else 0.72f)),
        )
    }
}

@Composable
private fun CompactWorkshopParts(parts: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel.copy(alpha = 0.56f))
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
    com.steamforge.game.progression.ContractType.MAKE_TILE -> "◆"
    com.steamforge.game.progression.ContractType.REACH_TILE -> "↑"
    com.steamforge.game.progression.ContractType.MERGE_COUNT -> "⇄"
    com.steamforge.game.progression.ContractType.SCORE -> "★"
    com.steamforge.game.progression.ContractType.TOTAL_SCORE -> "Σ"
    com.steamforge.game.progression.ContractType.COMBO_COUNT -> "×N"
    com.steamforge.game.progression.ContractType.PLAY_RUNS -> "▶"
    com.steamforge.game.progression.ContractType.SURVIVE_MOVES -> "↟"
}
