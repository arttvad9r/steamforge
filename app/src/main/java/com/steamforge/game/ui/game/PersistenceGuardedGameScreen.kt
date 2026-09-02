package com.steamforge.game.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamDecisionDialog

@Composable
fun PersistenceGuardedGameScreen(
    vm: GameViewModel,
    sfx: SfxPlayer,
    ads: AdsManager,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val terminalWritePending = ui.finishPersistenceInProgress || ui.finishPersistenceFailed

    GameScreen(
        vm = vm,
        sfx = sfx,
        ads = ads,
        onExit = {
            if (!terminalWritePending) onExit()
        },
        modifier = modifier,
    )

    if (terminalWritePending) {
        SteamDecisionDialog(
            title = if (ui.finishPersistenceFailed) "РЕЗУЛЬТАТ НЕ СОХРАНЁН" else "СОХРАНЯЕМ РЕЗУЛЬТАТ",
            onDismissRequest = { },
            body = {
                Column {
                    Text(
                        text = if (ui.finishPersistenceFailed) {
                            "Хранилище устройства не приняло финальную запись. Награда ещё не начислена. " +
                                "Освободите немного места и повторите сохранение — будет использован тот же результат без двойного начисления."
                        } else {
                            "Фиксируем результат и награду одной атомарной записью."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "До завершения записи выход и новая партия заблокированы.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            actions = {
                if (ui.finishPersistenceFailed) {
                    SteamButton(
                        text = "ПОВТОРИТЬ СОХРАНЕНИЕ",
                        onClick = vm::retryFinishPersistence,
                        modifier = Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Teal,
                    )
                } else {
                    Text(
                        "СОХРАНЯЕМ…",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TealGlow,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}
