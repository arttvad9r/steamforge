package com.steamforge.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamDecisionDialog
import kotlinx.coroutines.delay

private const val TERMINAL_WRITE_DIALOG_DELAY_MS = 350L

internal enum class FirstRunOnboardingPhase {
    NONE,
    SWIPE,
    MERGE,
}

internal fun firstRunOnboardingPhase(
    isFirstGame: Boolean,
    moves: Int,
    merges: Int,
    finished: Boolean,
    removingMode: Boolean,
): FirstRunOnboardingPhase {
    if (!isFirstGame || finished || removingMode) return FirstRunOnboardingPhase.NONE
    if (moves <= 0) return FirstRunOnboardingPhase.SWIPE
    if (merges <= 0) return FirstRunOnboardingPhase.MERGE
    return FirstRunOnboardingPhase.NONE
}

internal fun terminalPersistenceDialogVisible(
    inProgress: Boolean,
    failed: Boolean,
    delayElapsed: Boolean,
): Boolean = failed || (inProgress && delayElapsed)

@Composable
fun PersistenceGuardedGameScreen(
    vm: GameViewModel,
    sfx: SfxPlayer,
    isFirstGame: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val terminalWritePending = ui.finishPersistenceInProgress || ui.finishPersistenceFailed
    var terminalWriteDelayElapsed by remember { mutableStateOf(false) }
    val onboardingPhase = firstRunOnboardingPhase(
        isFirstGame = isFirstGame,
        moves = ui.state.moves,
        merges = ui.mergesTotal,
        finished = ui.finished,
        removingMode = ui.removingMode,
    )

    LaunchedEffect(ui.finishPersistenceInProgress, ui.finishPersistenceFailed) {
        when {
            ui.finishPersistenceFailed -> terminalWriteDelayElapsed = true
            ui.finishPersistenceInProgress -> {
                terminalWriteDelayElapsed = false
                delay(TERMINAL_WRITE_DIALOG_DELAY_MS)
                terminalWriteDelayElapsed = vm.ui.value.finishPersistenceInProgress &&
                    !vm.ui.value.finishPersistenceFailed
            }
            else -> terminalWriteDelayElapsed = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactLandscape = maxWidth > maxHeight && maxHeight < 600.dp

        GameScreen(
            vm = vm,
            sfx = sfx,
            onExit = {
                if (!terminalWritePending) onExit()
            },
            modifier = modifier,
        )

        // The gameplay screen already carries the permanent swipe instruction. Avoid repeating it as a second card.
        // After the first accepted move, keep only one small transient merge cue until the first merge succeeds.
        if (onboardingPhase == FirstRunOnboardingPhase.MERGE && !terminalWritePending) {
            FirstRunOnboardingHint(
                phase = onboardingPhase,
                modifier = Modifier
                    .align(if (compactLandscape) Alignment.BottomEnd else Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .widthIn(max = if (compactLandscape) 250.dp else 310.dp),
            )
        }
    }

    if (
        terminalPersistenceDialogVisible(
            inProgress = ui.finishPersistenceInProgress,
            failed = ui.finishPersistenceFailed,
            delayElapsed = terminalWriteDelayElapsed,
        )
    ) {
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

@Composable
private fun FirstRunOnboardingHint(
    phase: FirstRunOnboardingPhase,
    modifier: Modifier = Modifier,
) {
    val title: String
    val body: String
    val accent = when (phase) {
        FirstRunOnboardingPhase.SWIPE -> {
            title = "СВАЙПНИ ПО ПОЛЮ"
            body = "Сдвинь детали в любую сторону"
            BrassBright
        }
        FirstRunOnboardingPhase.MERGE -> {
            title = "СОЕДИНИ ОДИНАКОВЫЕ"
            body = "Две одинаковые детали образуют более сильную"
            TealGlow
        }
        FirstRunOnboardingPhase.NONE -> return
    }
    val shape = RoundedCornerShape(99.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Recess.copy(alpha = 0.78f))
            .border(1.dp, accent.copy(alpha = 0.28f), shape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .semantics { contentDescription = "$title. $body" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
