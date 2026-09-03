package com.steamforge.game.ui.contracts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamPanel

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("КОНТРАКТЫ", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text(
                        "3 задания · обновляются каждый день",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
                CompactGems(ui.gems)
            }
            Spacer(Modifier.height(14.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("СЕГОДНЯ", style = MaterialTheme.typography.labelLarge, color = BrassBright)
                        Text(
                            "Прогресс сохраняется вместе с партией",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                    Text(
                        "${ui.completed}/3",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (ui.completed == 3) TealGlow else TextWarm,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            ui.items.forEachIndexed { index, item ->
                ContractCard(item = item, onClaim = { vm.claim(item.def.id) })
                if (index != ui.items.lastIndex) Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Незавершённая обычная партия учитывается через тот же autosave, поэтому прогресс не теряется после закрытия приложения.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ContractCard(item: ContractItemUi, onClaim: () -> Unit) {
    val accent = when {
        item.claimed -> TealGlow.copy(alpha = 0.68f)
        item.complete -> TealGlow
        else -> BrassBright
    }
    SteamPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${item.def.title}: ${item.progress} из ${item.def.target}"
            },
        highlighted = item.complete && !item.claimed,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Recess.copy(alpha = 0.76f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(contractIcon(item), style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(item.def.title, style = MaterialTheme.typography.titleMedium, color = TextWarm)
                Text(item.def.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            Spacer(Modifier.width(8.dp))
            Text("◆ ${item.def.rewardGems}", style = MaterialTheme.typography.labelLarge, color = TealGlow)
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContractProgress(item.fraction, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(
                if (item.claimed) "ГОТОВО" else "${item.progress}/${item.def.target}",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.complete) TealGlow else TextMuted,
            )
        }

        if (item.complete && !item.claimed) {
            Spacer(Modifier.height(10.dp))
            SteamButton(
                text = "ПОЛУЧИТЬ ◆ ${item.def.rewardGems}",
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
            )
        }
    }
}

@Composable
private fun ContractProgress(fraction: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(shape)
            .background(Recess)
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(shape)
                .background(TealGlow.copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun CompactGems(gems: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel.copy(alpha = 0.76f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("◆", color = TealGlow, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(5.dp))
        Text(gems.toString(), color = TextWarm, style = MaterialTheme.typography.labelLarge)
    }
}

private fun contractIcon(item: ContractItemUi): String = when (item.def.type) {
    com.steamforge.game.progression.ContractType.MAKE_TILE -> "2048"
    com.steamforge.game.progression.ContractType.MERGE_COUNT -> "⇄"
    com.steamforge.game.progression.ContractType.SCORE -> "★"
    com.steamforge.game.progression.ContractType.PLAY_RUNS -> "▶"
    com.steamforge.game.progression.ContractType.SURVIVE_MOVES -> "↟"
    com.steamforge.game.progression.ContractType.OVERDRIVE -> "⚡"
}
