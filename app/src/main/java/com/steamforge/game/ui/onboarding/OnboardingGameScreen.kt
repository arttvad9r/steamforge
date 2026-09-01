package com.steamforge.game.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.sound.Sfx
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamPanel
import com.steamforge.game.ui.game.BoardView
import com.steamforge.game.ui.game.GameViewModel

/**
 * Первый экран — настоящий board, а не slideshow. Никаких Event/Collection/Daily/offer CTA до первого
 * meaningful progress. Партия сохраняется обычным GameViewModel и после onboarding может быть продолжена.
 */
@Composable
fun OnboardingGameScreen(
    vm: GameViewModel,
    sfx: SfxPlayer,
    onOpenWorkshop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var previousOverdrive by remember { mutableIntStateOf(0) }

    LaunchedEffect(ui.lastResult) {
        val result = ui.lastResult ?: return@LaunchedEffect
        if (result.merges.isNotEmpty()) {
            val maxLevel = result.merges.maxOf { it.tile.level }
            sfx.play(
                when {
                    maxLevel >= 8 -> Sfx.MERGE_HIGH
                    maxLevel >= 4 -> Sfx.MERGE_MID
                    else -> Sfx.MERGE_LOW
                },
            )
            if (ui.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            sfx.play(Sfx.MOVE)
        }
    }
    LaunchedEffect(ui.overdriveRemaining) {
        if (ui.overdriveRemaining > 0 && previousOverdrive == 0) sfx.play(Sfx.OVERDRIVE)
        previousOverdrive = ui.overdriveRemaining
    }

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Text("STEAMFORGE", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
            Text("ПЕРВЫЙ ЗАПУСК", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.height(10.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = ui.mergesTotal > 0,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("СЧЁТ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(ui.state.score.toString(), style = MaterialTheme.typography.titleLarge, color = BrassBright)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ДАВЛЕНИЕ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            if (ui.overdriveRemaining > 0) "OVERDRIVE" else "${ui.pressure}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (ui.overdriveRemaining > 0) TealGlow else TextWarm,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ХОД", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(ui.state.moves.toString(), style = MaterialTheme.typography.titleLarge, color = TextWarm)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            BoardView(
                state = ui.state,
                lastResult = ui.lastResult,
                previousTiles = ui.previousTiles,
                animationsActive = ui.animationsActive,
                removingMode = false,
                canRemove = { false },
                onTileClick = { },
                onSwipe = vm::onMove,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Spacer(Modifier.height(10.dp))

            when {
                ui.mergesTotal > 0 -> {
                    SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                        Text(
                            "ПЕРВЫЙ МЕХАНИЗМ СОБРАН",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TealGlow,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Объединения дают очки и повышают давление. Теперь можно увидеть, куда уходит прогресс между партиями.",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        SteamButton(
                            text = "ОТКРЫТЬ МАСТЕРСКУЮ",
                            onClick = onOpenWorkshop,
                            modifier = Modifier.fillMaxWidth(),
                            style = SteamButtonStyle.Teal,
                        )
                    }
                }
                ui.finished -> {
                    SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                        Text("Попробуйте ещё раз и соедините две одинаковые детали.", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        SteamButton("НАЧАТЬ СНОВА", vm::restart, Modifier.fillMaxWidth(), style = SteamButtonStyle.Teal)
                    }
                }
                else -> {
                    SteamPanel(Modifier.fillMaxWidth()) {
                        Text(
                            if (ui.state.moves == 0) "Свайпните по полю в любую сторону." else "Соедините две одинаковые детали.",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (ui.state.moves == 0) BrassBright else TextWarm,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
