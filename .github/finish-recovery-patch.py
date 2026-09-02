from pathlib import Path


def once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)

vm_path = Path('app/src/main/java/com/steamforge/game/ui/game/GameViewModel.kt')
vm = vm_path.read_text()
vm = once(
    vm,
    '''    fun exit() {
        if (competitiveMode) return
        if (_ui.value.finished || finishStarted) {''',
    '''    fun exit() {
        if (competitiveMode) return
        if (_ui.value.finishPersistenceInProgress || _ui.value.finishPersistenceFailed) return
        if (_ui.value.finished || finishStarted) {''',
    'block exit during terminal persistence',
)
vm = once(
    vm,
    '''    fun retryFinishPersistence() {
        if (competitiveMode || pendingFinish == null || finishWriteInFlight || !_ui.value.finishPersistenceFailed) return
        persistPendingFinish()
    }''',
    '''    fun retryFinishPersistence() {
        if (competitiveMode || pendingFinish == null || finishWriteInFlight || !_ui.value.finishPersistenceFailed) return
        analytics.logEvent("game_finish_save_retry")
        persistPendingFinish()
    }''',
    'retry analytics',
)
vm_path.write_text(vm)

screen_path = Path('app/src/main/java/com/steamforge/game/ui/game/GameScreen.kt')
screen = screen_path.read_text()
screen = once(
    screen,
    '''    fun leave() {
        if (!exitHandled) {''',
    '''    fun leave() {
        if (ui.finishPersistenceInProgress || ui.finishPersistenceFailed) return
        if (!exitHandled) {''',
    'block screen exit during terminal persistence',
)
screen = once(
    screen,
    '''    if (ui.finished) {
        if (weeklyMode) {''',
    '''    if (ui.finishPersistenceInProgress || ui.finishPersistenceFailed) {
        FinishPersistenceRecoveryOverlay(
            inProgress = ui.finishPersistenceInProgress,
            failed = ui.finishPersistenceFailed,
            onRetry = vm::retryFinishPersistence,
        )
    } else if (ui.finished) {
        if (weeklyMode) {''',
    'show finish persistence overlay',
)
marker = '@Composable\nprivate fun WeeklyResultOverlay('
if screen.count(marker) != 1:
    raise SystemExit('weekly overlay marker mismatch')
overlay = '''@Composable
private fun FinishPersistenceRecoveryOverlay(
    inProgress: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.84f)).padding(20.dp), contentAlignment = Alignment.Center) {
        SteamPanel(Modifier.fillMaxWidth().widthIn(max = 500.dp), highlighted = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (failed) "РЕЗУЛЬТАТ НЕ СОХРАНЁН" else "СОХРАНЯЕМ РЕЗУЛЬТАТ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrassBright,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (failed) {
                        "Хранилище устройства не приняло финальную запись. Награда ещё не начислена, результат остаётся защищённым на этом экране."
                    } else {
                        "Фиксируем результат и награду одной атомарной записью."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                if (failed) {
                    Spacer(Modifier.height(10.dp))
                    SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                        Text(
                            "Освободите немного места и повторите сохранение. Будет повторён тот же результат без двойного начисления.",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWarm,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SteamButton(
                        "ПОВТОРИТЬ СОХРАНЕНИЕ",
                        onRetry,
                        Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Teal,
                    )
                } else if (inProgress) {
                    Spacer(Modifier.height(10.dp))
                    Text("СОХРАНЯЕМ…", style = MaterialTheme.typography.labelLarge, color = TealGlow)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "До завершения записи выход из результата заблокирован.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

'''
screen = screen.replace(marker, overlay + marker, 1)
screen_path.write_text(screen)

test_path = Path('app/src/test/java/com/steamforge/game/ui/game/FinishPersistenceRetryTest.kt')
test = test_path.read_text()
test = once(
    test,
    '''        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertEquals(GameStatus.GAME_OVER, model.ui.value.state.status)''',
    '''        model.onMove(Move.LEFT)
        assertTrue(model.ui.value.finishPersistenceInProgress)
        model.exit()
        advanceUntilIdle()

        assertEquals(GameStatus.GAME_OVER, model.ui.value.state.status)''',
    'exercise exit race',
)
test = once(
    test,
    '''        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })

        model.retryFinishPersistence()''',
    '''        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_retry" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })

        model.retryFinishPersistence()''',
    'retry analytics assertion',
)
test_path.write_text(test)
