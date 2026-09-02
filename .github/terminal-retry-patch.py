from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    return text.replace(old, new, 1)


vm_path = Path("app/src/main/java/com/steamforge/game/ui/game/GameViewModel.kt")
vm = vm_path.read_text()
vm = replace_once(
    vm,
    "    val finished: Boolean = false,\n    val effects: FinishEffects? = null,",
    "    val finished: Boolean = false,\n    val finishPersistenceFailed: Boolean = false,\n    val finishPersistenceRetrying: Boolean = false,\n    val effects: FinishEffects? = null,",
    "ui finish persistence state",
)
vm = replace_once(
    vm,
    "    private var finishStarted = false\n    private var discardFinishedRecord = false\n    private var saveIoFailureActive = false\n\n    private val _ui = MutableStateFlow(",
    """    private var finishStarted = false
    private var discardFinishedRecord = false
    private var saveIoFailureActive = false
    private var pendingFinish: PendingFinish? = null
    private var finishCommitInFlight = false
    private var finishIoFailureActive = false

    private data class PendingFinish(
        val summary: GameSummary,
        val day: Long,
        val startingGemBalance: Int,
        val record: FinishedGameRecord,
    )

    private val _ui = MutableStateFlow(""",
    "pending finish state",
)
vm = replace_once(
    vm,
    "        finishStarted = false\n        discardFinishedRecord = false\n        sessionSeed = when {",
    """        finishStarted = false
        discardFinishedRecord = false
        pendingFinish = null
        finishCommitInFlight = false
        finishIoFailureActive = false
        sessionSeed = when {""",
    "new game pending reset",
)
vm = replace_once(
    vm,
    "                finished = false,\n                gameResultId = null,",
    "                finished = false,\n                finishPersistenceFailed = false,\n                finishPersistenceRetrying = false,\n                gameResultId = null,",
    "new game ui reset",
)

start = vm.index("    private fun finishGame() {")
end = vm.index("    private fun finishWeeklyGame() {")
replacement = """    private fun finishGame() {
        if (competitiveMode) {
            finishWeeklyGame()
            return
        }

        val s = _ui.value
        if (s.finished || finishStarted) return
        finishStarted = true
        discardFinishedRecord = false
        _ui.update {
            it.copy(
                canUndo = false,
                removingMode = false,
                finishPersistenceFailed = false,
                finishPersistenceRetrying = false,
            )
        }

        val summary = GameSummary(
            score = s.state.score,
            maxTileLevel = s.state.maxLevel,
            moves = s.state.moves,
            merges = s.mergesTotal,
            maxMergesInOneMove = s.maxMergesInOneMove,
            overdrives = s.overdrivesSession,
            undos = s.undosSession,
            won = s.state.won,
            daily = dailyMode,
        )
        val today = LocalDay.todayEpochDay()
        val resultId = "fg-" + UUID.randomUUID().toString()
        val record = FinishedGameRecord(
            id = resultId,
            day = today,
            daily = dailyMode,
            score = summary.score,
            maxTileLevel = summary.maxTileLevel,
            state = GameSaveCodec.encode(
                SavedGame(
                    state = s.state,
                    seed = sessionSeed,
                    pressure = s.pressure,
                    overdriveRemaining = s.overdriveRemaining,
                    freeUndosLeft = s.freeUndosLeft,
                    rngDraws = rng.draws,
                    mergesTotal = s.mergesTotal,
                    maxMergesInOneMove = s.maxMergesInOneMove,
                    overdrivesSession = s.overdrivesSession,
                    undosSession = s.undosSession,
                    highMergesSession = s.highMergesSession,
                ),
            ),
        )
        val pending = PendingFinish(
            summary = summary,
            day = today,
            startingGemBalance = s.gems,
            record = record,
        )
        pendingFinish = pending
        commitPendingFinish(pending)
    }

    fun retryFinishPersistence() {
        if (competitiveMode) return
        val pending = pendingFinish ?: return
        val state = _ui.value
        if (!finishStarted || state.finished || !state.finishPersistenceFailed || finishCommitInFlight) return
        _ui.update { it.copy(finishPersistenceRetrying = true) }
        analytics.logEvent("game_finish_save_retry")
        commitPendingFinish(pending)
    }

    private fun commitPendingFinish(pending: PendingFinish) {
        if (finishCommitInFlight) return
        finishCommitInFlight = true
        writesScope.launch {
            var eff: FinishEffects? = null
            var finalGemBalance = pending.startingGemBalance
            try {
                repo.applyGameFinish(pending.record) { latest ->
                    val (updated, e) = applyGameFinished(latest, pending.summary, cfg)
                    eff = e
                    val withAchievementDays = updated.copy(
                        achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to pending.day },
                    )
                    finalGemBalance = withAchievementDays.gems
                    withAchievementDays to e
                }
            } catch (_: IOException) {
                finishCommitInFlight = false
                _ui.update {
                    it.copy(
                        finishPersistenceFailed = true,
                        finishPersistenceRetrying = false,
                    )
                }
                if (!finishIoFailureActive) {
                    finishIoFailureActive = true
                    analytics.logEvent("game_finish_save_failed", mapOf("reason" to "io"))
                }
                return@launch
            }

            finishCommitInFlight = false
            pendingFinish = null
            if (finishIoFailureActive) {
                finishIoFailureActive = false
                analytics.logEvent("game_finish_save_recovered")
            }
            if (discardFinishedRecord) repo.clearFinishedGame()
            eff?.let { effects ->
                effects.levelUps.forEach { analytics.logEvent("workshop_level_up", mapOf("level" to it)) }
                effects.newAchievements.forEach { analytics.logEvent("achievement_unlocked", mapOf("id" to it.id)) }
            }
            _ui.update {
                it.copy(
                    finished = true,
                    finishPersistenceFailed = false,
                    finishPersistenceRetrying = false,
                    effects = eff,
                    gameResultId = pending.record.id,
                    removingMode = false,
                )
            }
            ads?.onGameFinished()
            if (ads?.rewardedReady?.value == true && (eff?.xpGained ?: 0) > 0) {
                analytics.logEvent("rewarded_offered", mapOf("bonus" to "workshop_xp"))
            }
            analytics.logEvent(
                "game_finished",
                mapOf(
                    "score" to pending.summary.score,
                    "max_tile" to (1 shl pending.summary.maxTileLevel),
                    "moves" to pending.summary.moves,
                    "daily" to pending.summary.daily,
                    "xp_gained" to (eff?.xpGained ?: 0),
                    "gems_gained" to (eff?.gemsGained ?: 0),
                    "gem_balance" to finalGemBalance,
                ),
            )
        }
    }

"""
vm = vm[:start] + replacement + vm[end:]
vm_path.write_text(vm)

screen_path = Path("app/src/main/java/com/steamforge/game/ui/game/GameScreen.kt")
screen = screen_path.read_text()
screen = replace_once(
    screen,
    """    fun leave() {
        if (!exitHandled) {""",
    """    fun leave() {
        if (ui.finishPersistenceFailed) return
        if (!exitHandled) {""",
    "block exit while terminal save failed",
)
screen = replace_once(
    screen,
    """    if (ui.finished) {
        if (weeklyMode) {""",
    """    if (ui.finishPersistenceFailed) {
        FinishPersistenceRecoveryOverlay(
            retrying = ui.finishPersistenceRetrying,
            onRetry = vm::retryFinishPersistence,
        )
    } else if (ui.finished) {
        if (weeklyMode) {""",
    "terminal recovery overlay branch",
)
marker = "@Composable\nprivate fun WeeklyResultOverlay("
if screen.count(marker) != 1:
    raise SystemExit("weekly overlay marker mismatch")
recovery = """@Composable
private fun FinishPersistenceRecoveryOverlay(retrying: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.84f)).padding(20.dp), contentAlignment = Alignment.Center) {
        SteamPanel(Modifier.fillMaxWidth().widthIn(max = 500.dp), highlighted = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("РЕЗУЛЬТАТ НЕ СОХРАНЁН", style = MaterialTheme.typography.headlineSmall, color = BrassBright, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Хранилище устройства не приняло финальную запись. Партия остаётся на экране, а награда пока не начислена.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                    Text(
                        "Освободите немного места и повторите сохранение. Steamforge повторит тот же результат без повторного начисления.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWarm,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SteamButton(
                    if (retrying) "СОХРАНЯЕМ…" else "ПОВТОРИТЬ СОХРАНЕНИЕ",
                    if (retrying) ({}) else onRetry,
                    Modifier.fillMaxWidth(),
                    style = SteamButtonStyle.Teal,
                )
                Spacer(Modifier.height(6.dp))
                Text("До успешной записи выход из результата заблокирован.", style = MaterialTheme.typography.labelMedium, color = TextMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

"""
screen = screen.replace(marker, recovery + marker, 1)
screen_path.write_text(screen)

test_path = Path("app/src/test/java/com/steamforge/game/ui/game/LowStoragePersistenceTest.kt")
test_path.write_text("""package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.FinishEffects
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LowStoragePersistenceTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingAnalytics : Analytics {
        val names = mutableListOf<String>()

        override fun logEvent(name: String, params: Map<String, Any?>) {
            names += name
        }
    }

    private class FlakySaveRepo(
        private val delegate: FakeDataRepo = FakeDataRepo(),
    ) : DataRepo by delegate {
        var remainingIoFailures: Int = 0
        var remainingFinishIoFailures: Int = 0
        val finishAttemptIds = mutableListOf<String>()

        val currentGame: SavedGame?
            get() = delegate.currentGame
        val currentFinished: FinishedGameRecord?
            get() = delegate.currentFinished
        val currentProgress
            get() = delegate.currentProgress

        override suspend fun saveGame(state: SavedGame) {
            if (remainingIoFailures > 0) {
                remainingIoFailures--
                throw IOException("ENOSPC")
            }
            delegate.saveGame(state)
        }

        override suspend fun applyGameFinish(
            record: FinishedGameRecord,
            finisher: (com.steamforge.game.progression.PlayerProgress) -> Pair<com.steamforge.game.progression.PlayerProgress, FinishEffects>,
        ) {
            finishAttemptIds += record.id
            if (remainingFinishIoFailures > 0) {
                remainingFinishIoFailures--
                throw IOException("ENOSPC")
            }
            delegate.applyGameFinish(record, finisher)
        }
    }

    @Test
    fun `transient save io failure keeps run alive and next autosave recovers`() = runTest(dispatcher) {
        val repo = FlakySaveRepo()
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 73L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()

        val durableBeforeFailure = requireNotNull(repo.currentGame).state
        repo.remainingIoFailures = 1

        val failedSaveState = performOneValidMove(model)
        advanceUntilIdle()

        assertEquals(failedSaveState, model.ui.value.state)
        assertEquals(durableBeforeFailure, requireNotNull(repo.currentGame).state)
        assertTrue("I/O save failure was not surfaced to analytics", "run_save_failed" in analytics.names)

        val recoveredState = performOneValidMove(model)
        advanceUntilIdle()

        assertNotEquals(failedSaveState, recoveredState)
        assertEquals(recoveredState, requireNotNull(repo.currentGame).state)
        assertTrue("save recovery was not surfaced to analytics", "run_save_recovered" in analytics.names)
    }

    @Test
    fun `terminal io failure retries same result and applies finish exactly once`() = runTest(dispatcher) {
        val initialGame = finishingSavedGame(seed = 17L)
        val delegate = FakeDataRepo(initialGame = initialGame)
        val repo = FlakySaveRepo(delegate)
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 17L },
            savedGameProvider = { repo.currentGame },
        )
        advanceUntilIdle()
        val progressBeforeFinish = repo.currentProgress

        repo.remainingFinishIoFailures = 1
        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertFalse(model.ui.value.finished)
        assertTrue(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finishPersistenceRetrying)
        assertEquals(progressBeforeFinish, repo.currentProgress)
        assertNull(repo.currentFinished)
        assertEquals(1, repo.finishAttemptIds.size)
        val resultId = repo.finishAttemptIds.single()
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(0, analytics.names.count { it == "game_finished" })

        model.retryFinishPersistence()
        assertTrue(model.ui.value.finishPersistenceRetrying)
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finishPersistenceRetrying)
        assertEquals(listOf(resultId, resultId), repo.finishAttemptIds)
        assertNotNull(repo.currentFinished)
        assertEquals(resultId, repo.currentFinished?.id)
        assertEquals(progressBeforeFinish.stats.gamesPlayed + 1, repo.currentProgress.stats.gamesPlayed)
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_retry" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })
        assertEquals(1, analytics.names.count { it == "game_finished" })
    }

    private fun finishingSavedGame(seed: Long): SavedGame {
        val levels = listOf(
            1, 1, 3, 4,
            5, 6, 7, 8,
            9, 10, 1, 2,
            3, 4, 5, 6,
        )
        val tiles = levels.mapIndexed { index, level ->
            Tile(
                id = (index + 1).toLong(),
                level = level,
                row = index / 4,
                col = index % 4,
            )
        }
        return SavedGame(
            state = GameState(
                size = 4,
                tiles = tiles,
                score = 128,
                nextTileId = 17L,
                moves = 20,
            ),
            seed = seed,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
            rngDraws = 0L,
        )
    }

    private fun performOneValidMove(model: GameViewModel): GameState {
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = model.ui.value.state
            model.onMove(move)
            if (model.ui.value.state != before) return model.ui.value.state
        }
        error("fixture did not provide a valid move")
    }
}
""")
