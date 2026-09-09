package com.steamforge.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.GameRunMode
import com.steamforge.game.GameRunPolicies
import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameRules
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.ReplayableRandom
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.GameSaveCodec
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.WeeklyChallenge
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.progression.WeeklyRunRecorder
import com.steamforge.game.progression.WeeklyRunSubmission
import com.steamforge.game.progression.applyGameFinished
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val state: GameState = GameState(),
    val best: Int = 0,
    val gems: Int = 0,
    val pressure: Int = 0,
    val overdriveRemaining: Int = 0,
    val canUndo: Boolean = false,
    val freeUndosLeft: Int = 0,
    val finished: Boolean = false,
    val effects: FinishEffects? = null,
    val daily: DailyChallenge? = null,
    val dailySatisfied: Boolean = false,
    val winCelebrated: Boolean = false,
    val winBannerShown: Boolean = false,
    val removingMode: Boolean = false,
    val lastResult: MoveResult? = null,
    val previousTiles: List<Tile> = emptyList(),
    val mergesTotal: Int = 0,
    val maxMergesInOneMove: Int = 0,
    val overdrivesSession: Int = 0,
    val undosSession: Int = 0,
    val highMergesSession: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsActive: Boolean = true,
    val finishPersistenceInProgress: Boolean = false,
    val finishPersistenceFailed: Boolean = false,
)

class GameViewModel(
    private val repo: DataRepo,
    private val cfg: ProgressionConfig = ProgressionConfig(),
    dailyMode: Boolean = false,
    private val runMode: GameRunMode = if (dailyMode) GameRunMode.DAILY else GameRunMode.NORMAL,
    private val dailyProvider: () -> DailyChallenge? = { null },
    private val weeklyProvider: () -> WeeklyChallenge = { WeeklyChallenges.forUtcMillis() },
    private val seedProvider: () -> Long = { System.currentTimeMillis() },
    private val savedGameProvider: suspend () -> SavedGame? = { repo.savedGame.first() },
    private val systemAnimationsEnabled: Boolean = true,
) : ViewModel() {

    private val dailyMode = runMode == GameRunMode.DAILY
    private val engine = GameEngine()
    private val daily = if (dailyMode) dailyProvider() else null
    private val weekly = if (runMode == GameRunMode.WEEKLY) weeklyProvider() else null
    private val policy = weekly?.let { GameRunPolicies.resolve(runMode, it.rules) }
        ?: GameRunPolicies.resolve(runMode)
    private var sessionSeed: Long? = when (runMode) {
        GameRunMode.NORMAL -> null
        GameRunMode.DAILY -> daily?.seed
        GameRunMode.WEEKLY -> weekly?.seed
    }
    private var rng = ReplayableRandom(
        sessionSeed ?: if (runMode == GameRunMode.NORMAL) seedProvider() else 0L,
    )
    private var dailyCompletedToday = false
    private var dailyClaimInFlight = false
    private var paidToolWriteInFlight = false

    private val writesScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var finishStarted = false
    private var discardFinishedRecord = false
    private var pendingFinish: PendingFinish? = null
    private var finishWriteInFlight = false
    private var weeklyRecorder: WeeklyRunRecorder? = null

    private val _ui = MutableStateFlow(
        GameUiState(
            freeUndosLeft = if (policy.allowUndo) cfg.freeUndosPerGame else 0,
            daily = daily,
        ),
    )
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _weeklySubmission = MutableStateFlow<WeeklyRunSubmission?>(null)
    val weeklySubmission: StateFlow<WeeklyRunSubmission?> = _weeklySubmission.asStateFlow()

    private var undoSnapshot: UndoSnapshot? = null

    private data class UndoSnapshot(
        val state: GameState,
        val pressure: Int,
        val overdriveRemaining: Int,
        val rngDraws: Long,
        val mergesTotal: Int,
        val maxMergesInOneMove: Int,
        val overdrivesSession: Int,
        val undosSession: Int,
        val highMergesSession: Int,
    )

    private data class PendingFinish(
        val record: FinishedGameRecord,
        val summary: GameSummary,
        val day: Long,
    )

    init {
        viewModelScope.launch {
            val record = runCatching { repo.finishedGame.first() }.getOrNull()
            if (
                policy.restoreFinishedResult &&
                record != null &&
                record.day == LocalDay.todayEpochDay() &&
                record.daily == dailyMode
            ) {
                restoreFinished(record)
            } else {
                val restored = runCatching { savedGameProvider() }.getOrNull()
                if (policy.persistActiveRun && restored != null) {
                    sessionSeed = restored.seed ?: seedProvider()
                    rng = ReplayableRandom(sessionSeed ?: 0L, restored.rngDraws)
                    _ui.update {
                        it.copy(
                            state = restored.state,
                            pressure = restored.pressure,
                            overdriveRemaining = restored.overdriveRemaining,
                            freeUndosLeft = restored.freeUndosLeft,
                            canUndo = false,
                            winCelebrated = restored.state.won,
                            mergesTotal = restored.mergesTotal,
                            maxMergesInOneMove = restored.maxMergesInOneMove,
                            overdrivesSession = restored.overdrivesSession,
                            undosSession = restored.undosSession,
                            highMergesSession = restored.highMergesSession,
                        )
                    }
                } else {
                    newGameInternal()
                }
            }
            repo.progress.collect { p ->
                val completedToday = dailyMode &&
                    p.dailyChallengeDay == LocalDay.todayEpochDay() &&
                    p.dailyChallengeDone
                dailyCompletedToday = completedToday
                _ui.update { s ->
                    s.copy(
                        gems = p.gems,
                        best = p.bestScore,
                        soundEnabled = p.soundEnabled,
                        hapticsEnabled = p.hapticsEnabled,
                        animationsActive = p.animationsEnabled && systemAnimationsEnabled,
                        dailySatisfied = s.dailySatisfied || completedToday,
                    )
                }
            }
        }
    }

    private fun restoreFinished(record: FinishedGameRecord) {
        sessionSeed = null
        val restoredState = GameSaveCodec.decode(record.state)
        _ui.update {
            it.copy(
                finished = true,
                effects = record.toEffects(),
                state = restoredState?.state ?: GameState(score = record.score),
                winCelebrated = record.maxTileLevel >= GameRules().winLevel,
                freeUndosLeft = if (policy.allowUndo) cfg.freeUndosPerGame else 0,
                finishPersistenceInProgress = false,
                finishPersistenceFailed = false,
            )
        }
    }

    private fun FinishedGameRecord.toEffects() = FinishEffects(
        xpGained = xpGained,
        gemsGained = gemsGained,
        workshopPartsGained = workshopPartsGained,
        levelUps = levelUps,
        newAchievements = newAchievementIds.mapNotNull { Achievements.byId(it) },
        newBest = newBest,
    )

    fun onMove(move: Move) {
        val s = _ui.value
        if (s.finished || s.removingMode || finishStarted || paidToolWriteInFlight) return
        val snapshot = if (policy.allowUndo) {
            UndoSnapshot(
                state = s.state,
                pressure = s.pressure,
                overdriveRemaining = s.overdriveRemaining,
                rngDraws = rng.draws,
                mergesTotal = s.mergesTotal,
                maxMergesInOneMove = s.maxMergesInOneMove,
                overdrivesSession = s.overdrivesSession,
                undosSession = s.undosSession,
                highMergesSession = s.highMergesSession,
            )
        } else {
            null
        }
        val multiplier = if (policy.allowOverdrive && s.overdriveRemaining > 0) {
            cfg.overdriveMultiplier
        } else {
            1
        }
        val result = engine.applyMove(s.state, move, rng, multiplier)
        weeklyRecorder?.record(move, result)
        if (!result.moved) return

        var pressure = if (policy.allowOverdrive) s.pressure else 0
        var overdrive = if (policy.allowOverdrive) s.overdriveRemaining else 0
        var overdrives = if (policy.allowOverdrive) s.overdrivesSession else 0

        if (policy.allowOverdrive) {
            if (overdrive > 0) {
                overdrive = (overdrive - result.merges.size).coerceAtLeast(0)
            } else {
                pressure += result.merges.sumOf { cfg.pressureGainForMerge(it.tile.level) }
                if (pressure >= cfg.pressureMax) {
                    pressure = 0
                    overdrive = cfg.overdriveMerges
                    overdrives++
                }
            }
        }

        val merges = result.merges.size
        val highMerges = result.merges.count { it.tile.level >= 6 }

        _ui.update {
            it.copy(
                state = result.state,
                lastResult = result,
                previousTiles = s.state.tiles,
                pressure = pressure,
                overdriveRemaining = overdrive,
                mergesTotal = it.mergesTotal + merges,
                maxMergesInOneMove = maxOf(it.maxMergesInOneMove, merges),
                overdrivesSession = overdrives,
                highMergesSession = it.highMergesSession + highMerges,
                winCelebrated = it.winCelebrated || result.state.won,
                canUndo = policy.allowUndo,
            )
        }
        undoSnapshot = snapshot

        if (daily != null && !dailyCompletedToday) checkDailyGoal(result.state)
        if (result.state.status == GameStatus.GAME_OVER) finishGame() else persistGame()
    }

    fun undo() {
        if (!policy.allowUndo) return
        val s = _ui.value
        val snap = undoSnapshot ?: return
        if (s.finished || s.removingMode || finishStarted || paidToolWriteInFlight) return
        val paidUndo = s.freeUndosLeft <= 0
        if (paidUndo && s.gems < cfg.undoGemsCost) return

        val applyUndoState: (GameUiState) -> GameUiState = { current ->
            current.copy(
                state = snap.state,
                gems = if (paidUndo) (s.gems - cfg.undoGemsCost).coerceAtLeast(0) else current.gems,
                pressure = snap.pressure,
                overdriveRemaining = snap.overdriveRemaining,
                freeUndosLeft = if (paidUndo) current.freeUndosLeft else (current.freeUndosLeft - 1).coerceAtLeast(0),
                lastResult = null,
                previousTiles = emptyList(),
                canUndo = false,
                mergesTotal = snap.mergesTotal,
                maxMergesInOneMove = snap.maxMergesInOneMove,
                overdrivesSession = snap.overdrivesSession,
                undosSession = snap.undosSession + 1,
                highMergesSession = snap.highMergesSession,
            )
        }

        if (paidUndo) {
            val nextUi = applyUndoState(s)
            val activeGame = if (policy.persistActiveRun) {
                buildSavedGameSnapshot(nextUi, sessionSeed, snap.rngDraws)
            } else {
                null
            }
            persistPaidTool(
                expectedGems = s.gems,
                gemCost = cfg.undoGemsCost,
                activeGame = activeGame,
            ) {
                rng = ReplayableRandom(sessionSeed ?: 0L, snap.rngDraws)
                _ui.update(applyUndoState)
                undoSnapshot = null
            }
            return
        }

        rng = ReplayableRandom(sessionSeed ?: 0L, snap.rngDraws)
        _ui.update(applyUndoState)
        undoSnapshot = null
        persistGame()
    }

    fun toggleRemovingMode() {
        if (!policy.allowWrench) return
        val s = _ui.value
        if (paidToolWriteInFlight || finishStarted || s.finished) return
        if (s.removingMode) {
            _ui.update { it.copy(removingMode = false) }
            return
        }
        if (s.gems < cfg.wrenchGemsCost) return
        _ui.update { it.copy(removingMode = true) }
    }

    fun canRemoveTile(tile: Tile): Boolean {
        if (!policy.allowWrench) return false
        val s = _ui.value
        return !paidToolWriteInFlight &&
            !finishStarted &&
            !s.finished &&
            s.removingMode &&
            tile.level in 1..cfg.wrenchMaxTileLevel &&
            s.gems >= cfg.wrenchGemsCost
    }

    fun removeTile(tile: Tile) {
        if (!policy.allowWrench) return
        val s = _ui.value
        if (paidToolWriteInFlight || finishStarted || s.finished || !s.removingMode) return
        if (!canRemoveTile(tile)) return
        val tiles = s.state.tiles.filterNot { it.id == tile.id }
        if (tiles.size == s.state.tiles.size) return

        val applyRemoval: (GameUiState) -> GameUiState = { current ->
            current.copy(
                state = s.state.copy(tiles = tiles, status = GameStatus.PLAYING),
                gems = (s.gems - cfg.wrenchGemsCost).coerceAtLeast(0),
                removingMode = false,
                canUndo = false,
            )
        }
        val nextUi = applyRemoval(s)
        val activeGame = if (policy.persistActiveRun) {
            buildSavedGameSnapshot(nextUi, sessionSeed, rng.draws)
        } else {
            null
        }
        persistPaidTool(
            expectedGems = s.gems,
            gemCost = cfg.wrenchGemsCost,
            activeGame = activeGame,
        ) {
            _ui.update(applyRemoval)
            undoSnapshot = null
        }
    }

    fun restart() {
        val s = _ui.value
        if (paidToolWriteInFlight || (finishStarted && !s.finished)) return
        if (s.finished && policy.restoreFinishedResult) {
            writesScope.launch { repo.clearFinishedGame() }
        }
        newGameInternal()
    }

    /**
     * Выход не является завершением партии и не выдаёт XP. Только NORMAL-партия сохраняется для
     * продолжения; DAILY/WEEKLY закрываются без active-save. Если persisted finish уже фиксируется,
     * результат удалится после транзакции только для режимов, которые таким результатом владеют.
     */
    fun exit() {
        if (paidToolWriteInFlight || _ui.value.finishPersistenceInProgress || _ui.value.finishPersistenceFailed) return
        if (_ui.value.finished || finishStarted) {
            discardFinishedRecord = true
            if (_ui.value.finished && policy.restoreFinishedResult) {
                writesScope.launch { repo.clearFinishedGame() }
            }
        } else if (policy.persistActiveRun) {
            persistGame()
        }
    }

    fun markWinBannerShown() {
        _ui.update { it.copy(winBannerShown = true) }
    }

    private fun newGameInternal() {
        finishStarted = false
        discardFinishedRecord = false
        pendingFinish = null
        finishWriteInFlight = false
        sessionSeed = when (runMode) {
            GameRunMode.NORMAL -> seedProvider()
            GameRunMode.DAILY -> daily?.seed
            GameRunMode.WEEKLY -> weekly?.seed
        }
        rng = ReplayableRandom(sessionSeed ?: 0L)
        undoSnapshot = null
        weeklyRecorder = weekly?.let { WeeklyRunRecorder(it) }
        _weeklySubmission.value = null
        val state = engine.newGame(rng = rng)
        _ui.update {
            it.copy(
                state = state,
                pressure = 0,
                overdriveRemaining = 0,
                canUndo = false,
                finished = false,
                effects = null,
                freeUndosLeft = if (policy.allowUndo) cfg.freeUndosPerGame else 0,
                dailySatisfied = dailyMode && dailyCompletedToday,
                winCelebrated = false,
                winBannerShown = false,
                lastResult = null,
                previousTiles = emptyList(),
                mergesTotal = 0,
                maxMergesInOneMove = 0,
                overdrivesSession = 0,
                undosSession = 0,
                highMergesSession = 0,
                finishPersistenceInProgress = false,
                finishPersistenceFailed = false,
            )
        }
        persistGame()
    }

    private fun persistPaidTool(
        expectedGems: Int,
        gemCost: Int,
        activeGame: SavedGame?,
        onCommitted: () -> Unit,
    ) {
        if (paidToolWriteInFlight) return
        paidToolWriteInFlight = true
        val operationId = UUID.randomUUID().toString()
        writesScope.launch {
            try {
                for (attempt in 0 until 2) {
                    try {
                        if (
                            repo.applyPaidTool(
                                operationId = operationId,
                                expectedGems = expectedGems,
                                gemCost = gemCost,
                                activeGame = activeGame,
                            )
                        ) {
                            onCommitted()
                        }
                        return@launch
                    } catch (_: IOException) {
                        if (attempt == 1) return@launch
                    }
                }
            } finally {
                paidToolWriteInFlight = false
            }
        }
    }

    private fun checkDailyGoal(state: GameState) {
        val challenge = daily ?: return
        val satisfied = challenge.isSatisfied(
            maxTileValue = 1 shl state.maxLevel,
            score = state.score,
            highMerges = _ui.value.highMergesSession,
        )
        if (!satisfied) return
        _ui.update { it.copy(dailySatisfied = true) }
        if (dailyCompletedToday || dailyClaimInFlight) return

        dailyClaimInFlight = true
        val today = LocalDay.todayEpochDay()
        writesScope.launch {
            try {
                for (attempt in 0 until 2) {
                    try {
                        val granted = repo.claimDailyChallenge(
                            day = today,
                            rewardGems = challenge.rewardGems,
                            bonusXp = challenge.bonusXp,
                        )
                        if (granted) {
                            dailyCompletedToday = true
                            return@launch
                        }

                        val persisted = runCatching { repo.progress.first() }.getOrNull()
                        if (persisted?.dailyChallengeDay == today && persisted.dailyChallengeDone) {
                            dailyCompletedToday = true
                        }
                        return@launch
                    } catch (_: IOException) {
                        if (attempt == 1) return@launch
                    }
                }
            } finally {
                dailyClaimInFlight = false
            }
        }
    }

    private fun finishGame() {
        val s = _ui.value
        if (s.finished || finishStarted) return
        finishStarted = true
        discardFinishedRecord = false
        _ui.update {
            it.copy(
                canUndo = false,
                removingMode = false,
                finishPersistenceInProgress = policy.grantProgressionOnFinish,
                finishPersistenceFailed = false,
            )
        }

        if (!policy.grantProgressionOnFinish) {
            finishCompetitiveGame(s)
            return
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
        val record = FinishedGameRecord(
            id = "fg-" + UUID.randomUUID().toString(),
            day = today,
            daily = dailyMode,
            score = summary.score,
            maxTileLevel = summary.maxTileLevel,
            state = GameSaveCodec.encode(
                buildSavedGameSnapshot(
                    ui = s,
                    sessionSeed = sessionSeed,
                    rngDraws = rng.draws,
                ),
            ),
        )
        pendingFinish = PendingFinish(record = record, summary = summary, day = today)
        persistPendingFinish()
    }

    private fun finishCompetitiveGame(s: GameUiState) {
        pendingFinish = null
        finishWriteInFlight = false
        _weeklySubmission.value = weeklyRecorder?.verifiedSubmission(s.state)
        _ui.update {
            it.copy(
                finished = true,
                effects = null,
                removingMode = false,
                canUndo = false,
                finishPersistenceInProgress = false,
                finishPersistenceFailed = false,
            )
        }
    }

    fun retryFinishPersistence() {
        if (pendingFinish == null || finishWriteInFlight || !_ui.value.finishPersistenceFailed) return
        persistPendingFinish()
    }

    private fun persistPendingFinish() {
        val pending = pendingFinish ?: return
        if (finishWriteInFlight) return
        finishWriteInFlight = true
        _ui.update { it.copy(finishPersistenceInProgress = true, finishPersistenceFailed = false) }

        writesScope.launch {
            try {
                var eff: FinishEffects? = null
                repo.applyGameFinish(pending.record) { latest ->
                    val (updated, e) = applyGameFinished(latest, pending.summary, cfg)
                    eff = e
                    updated.copy(
                        achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to pending.day },
                    ) to e
                }
                val committedRecord = repo.finishedGame.first()?.takeIf { it.id == pending.record.id }
                if (committedRecord != null) eff = committedRecord.toEffects()
                if (discardFinishedRecord) repo.clearFinishedGame()

                finishWriteInFlight = false
                pendingFinish = null
                _ui.update {
                    it.copy(
                        finished = true,
                        effects = eff,
                        removingMode = false,
                        finishPersistenceInProgress = false,
                        finishPersistenceFailed = false,
                    )
                }
            } catch (_: IOException) {
                finishWriteInFlight = false
                _ui.update {
                    it.copy(
                        finished = false,
                        effects = null,
                        finishPersistenceInProgress = false,
                        finishPersistenceFailed = true,
                    )
                }
            }
        }
    }

    private fun persistGame() {
        if (!policy.persistActiveRun || finishStarted) return
        val s = _ui.value
        val snapshot = buildSavedGameSnapshot(
            ui = s,
            sessionSeed = sessionSeed,
            rngDraws = rng.draws,
        )
        writesScope.launch {
            try {
                repo.saveGame(snapshot)
            } catch (_: IOException) {
                // The active game remains playable in memory. A later successful move retries autosave.
            }
        }
    }
}
