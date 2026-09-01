package com.steamforge.game.ui.onboarding

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.StatPlate
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamPanel
import com.steamforge.game.ui.workshop.WorkshopViewModel

@Composable
fun OnboardingWorkshopScreen(
    vm: WorkshopViewModel,
    onOpenContracts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))
            Text("STEAMFORGE", style = MaterialTheme.typography.labelLarge, color = BrassBright)
            Text("МАСТЕРСКАЯ ОТКРЫТА", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
            Text(
                "Партии развивают мастерскую даже после того, как доска очищена.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                Text("ПОСТОЯННЫЙ ПРОГРЕСС", style = MaterialTheme.typography.labelLarge, color = TealGlow, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatPlate("УРОВЕНЬ", ui.level.toString(), Modifier.weight(1f), BrassBright)
                    Spacer(Modifier.width(8.dp))
                    StatPlate("XP", "${ui.levelInfo.xpIntoLevel}/${ui.levelInfo.xpToNext}", Modifier.weight(1f), TealGlow)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatPlate("РЕКОРД", ui.bestScore.toString(), Modifier.weight(1f), TextWarm)
                    Spacer(Modifier.width(8.dp))
                    StatPlate("ПАРТИЙ", ui.gamesPlayed.toString(), Modifier.weight(1f), TextWarm)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Счёт и крупные детали дают XP. Новые уровни и чертежи постепенно меняют мастерскую, но сама 2048-механика остаётся прежней.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))

            SteamPanel(Modifier.fillMaxWidth()) {
                Text("СЛЕДУЮЩИЙ ШАГ", style = MaterialTheme.typography.labelLarge, color = BrassBright)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Контракты дают короткие цели для обычных партий. Первый набор уже готов — никаких отдельных правил учить не нужно.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(10.dp))
                SteamButton(
                    text = "ПОСМОТРЕТЬ КОНТРАКТЫ",
                    onClick = onOpenContracts,
                    modifier = Modifier.fillMaxWidth(),
                    style = SteamButtonStyle.Brass,
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
