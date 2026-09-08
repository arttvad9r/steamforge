package com.steamforge.game.ui.blueprints

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.workshop.SteamEngineBlueprintModule

@Composable
fun BlueprintsScreen(
    vm: BlueprintsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    BlueprintsContent(ui = ui, onBack = onBack, modifier = modifier)
}

@Composable
internal fun BlueprintsContent(
    ui: BlueprintsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collection = BlueprintCollections.steamEngine
    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Чертежи", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("Инженерная коллекция", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))

            if (!ui.loaded) {
                Text("ЗАГРУЗКА…", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            SteamEngineBlueprintModule(
                piecesOwned = ui.steamEngineOwned,
                piecesTotal = ui.steamEngineTotal,
                unlocked = ui.steamEngineComplete,
                animationsEnabled = ui.animationsEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            Text(
                "КОМПЛЕКТ ДЕТАЛЕЙ",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = BrassBright.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(6.dp))
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Panel.copy(alpha = 0.62f))
                    .border(1.dp, BrassDark.copy(alpha = 0.34f), shape)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .semantics {
                        contentDescription = "Детали Steam Engine: собрано ${ui.steamEngineOwned} из ${ui.steamEngineTotal}"
                    },
            ) {
                collection.pieces.forEachIndexed { index, piece ->
                    val owned = piece.id in ui.ownedPieceIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (owned) "✓" else "○",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (owned) TealGlow else BrassDark,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            piece.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (owned) TextWarm else TextMuted,
                        )
                        Text(
                            if (owned) "НАЙДЕНО" else "НЕ НАЙДЕНО",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (owned) TealGlow else TextMuted,
                        )
                    }
                    if (index != collection.pieces.lastIndex) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.055f)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Части чертежа можно получать за контракты. Собранный комплект остаётся в мастерской.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
