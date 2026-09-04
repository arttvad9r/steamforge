package com.steamforge.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.analytics.GameMoveAnalytics
import com.steamforge.game.analytics.log
import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameRules
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.GameSaveCodec
import com.steamforge.game.data.SavedGame
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.applyGameFinished
import java.io.IOException
import java.util.UUID
import kotlin.random.Random
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
    val gameResultId: String? = null,
    val rewardDoubled: Boolean = false,
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
    private val analytics: Analytics,
    private val cfg: ProgressionConfig = ProgressionConfig(),
    private val dailyMode: Boolean = false,
    private val dailyProvider: () -> DailyChallenge? = { null },
    private val seedProvider: () -> Long = { System.currentTimeMillis() },
    private val savedGameProvider: suspend () -> SavedGame? = { repo.savedGame.first() },
    private val systemAnimationsEnabled: Boolean = true,
    private val ads: AdsManager? = null,
) : ViewModel() {

    private val engine = GameEngine()
    private val daily = if (dailyMode) dailyProvider() else null
    private var sessionSeed: Long? = if (dailyMode) daily?.seed else null
    private var rng = ReplayableRandom(if (dailyMode) daily?.seed ?: 0L else seedProvider())
    private var dailyCompletedToday = false

    private val writesScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var finishStarted = false
    private var discardFinishedRecord = false
    private var saveIoFailureActive = false
    private var pendingFinish: PendingFinish? = null
    private var finishWriteInFlight = false
    private var finishPersistenceHadIoFailure = false
    private var rewardedOfferLoggedResultId: String? = null

    private val _ui = MutableStateFlow(GameUiState(freeUndosLeft = cfg.freeUndosPerGame, daily = daily))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

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
            if (record != null && record.day == LocalDay.todayEpochDay() && record.daily == dailyMode) {
                restoreFinished(record)
            } else {
                val restored = runCatching { savedGameProvider() }.getOrNull()
                if (!dailyMode && restored != null) {
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
        ads?.let { manager ->
            viewModelScope.launch {
                manager.rewardedReady.collect { ready ->
                    if (ready) logRewardedOfferIfVisible()
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
                gameResultId = record.id,
                rewardDoubled = record.rewardedClaimed,
                effects = record.toEffects(),
                state = restoredState?.state ?: GameState(score = record.score),
                winCelebrated = record.maxTileLevel >= GameRules().winLevel,
                freeUndosLeft = cfg.freeUndosPerGame,
                finishPersistenceInProgress = false,
                finishPersistenceFailed = false,
            )
        }
        logRewardedOfferIfVisible()
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
        if (s.finished || s.removingMode || finishStarted) return
        val snapshot = UndoSnapshot(
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
        val multiplier = if (s.overdriveRemaining > 0) cfg.overdriveMultiplier else 1
        val result = engine.applyMove(s.state, move, rng, multiplier)
        if (!result.moved) return

        GameMoveAnalytics.eventsFor(
            result = result,
            previousMaxLevel = s.state.maxLevel,
            daily = dailyMode,
        ).forEach { event -> analytics.log(event) }

        var pressure = s.pressure
        var overdrive = s.overdriveRemaining
        var overdrives = s.overdrivesSession

        if (overdrive > 0) {
            overdrive = (overdrive - result.merges.size).coerceAtLeast(0)
        } else {
            pressure += result.merges.sumOf { cfg.pressureGainForMerge(it.tile.level) }
            if (pressure >= cfg.pressureMax) {
                pressure = 0
                overdrive = cfg.overdriveMerges
                overdrives++
                analytics.logEvent("overdrive_activated")
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
                canUndo = true,
            )
        }
        undoSnapshot = snapshot

        if (daily != null && !_ui.value.dailySatisfied) checkDailyGoal(result.state)
        if (result.state.status == GameStatus.GAME_OVER) finishGame() else persistGame()
    }

    fun undo() {
        val s = _ui.value
        val snap = undoSnapshot ?: return
        if (s.finished || s.removingMode || finishStarted) return
        val paidUndo = s.freeUndosLeft <= 0
        if (!paidUndo) {
            _ui.update { it.copy(freeUndosLeft = it.freeUndosLeft - 1) }
        } else if (s.gems >= cfg.undoGemsCost) {
            writesScope.launch {
                repo.updateProgress { p -> p.copy(gems = (p.gems - cfg.undoGemsCost).coerceAtLeast(0)) }
            }
        } else {
            return
        }
        rng = ReplayableRandom(sessionSeed ?: 0L, snap.rngDraws)
        _ui.update {
            it.copy(
                state = snap.state,
                gems = if (paidUndo) (it.gems - cfg.undoGemsCost).coerceAtLeast(0) else it.gems,
                pressure = snap.pressure,
                overdriveRemaining = snap.overdriveRemaining,
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
        undoSnapshot = null
        analytics.logEvent("undo_used")
        persistGame()
    }

    fun toggleRemovingMode() {
        val s = _ui.value
        if (finishStarted || s.finished) return
        if (s.removingMode) {
            _ui.update { it.copy(removingMode = false) }
            return
        }
        if (s.gems < cfg.wrenchGemsCost) return
        _ui.update { it.copy(removingMode = true) }
    }

    fun canRemoveTile(tile: Tile): Boolean {
        val s = _ui.value
        return !finishStarted &&
            !s.finished &&
            s.removingMode &&
            tile.level in 1..cfg.wrenchMaxTileLevel &&
            s.gems >= cfg.wrenchGemsCost
    }

    fun removeTile(tile: Tile) {
        val s = _ui.value
        if (finishStarted || s.finished || !s.removingMode) return
        if (!canRemoveTile(tile)) return
        val tiles = s.state.tiles.filterNot { it.id == tile.id }
        if (tiles.size == s.state.tiles.size) return
        writesScope.launch {
            repo.updateProgress { p -> p.copy(gems = (p.gems - cfg.wrenchGemsCost).coerceAtLeast(0)) }
        }
        _ui.update {
            it.copy(
                state = s.state.copy(tiles = tiles, status = GameStatus.PLAYING),
                gems = (it.gems - cfg.wrenchGemsCost).coerceAtLeast(0),
                removingMode = false,
                canUndo = false,
            )
        }
        undoSnapshot = null
        analytics.logEvent("powerup_used", mapOf("type" to "wrench", "tile_level" to tile.level))
        persistGame()
    }

    fun restart() {
        if (finishStarted && !_ui.value.finished) return
        if (_ui.value.finished) writesScope.launch { repo.clearFinishedGame() }
        newGameInternal()
    }

    /**
     * Выход не является завершением партии и не выдаёт XP. Обычная партия сохраняется для продолжения,
     * Daily-попытка просто закрывается. Если Game Over уже фиксируется, результат удалится после транзакции.
     */
    fun exit() {
        if (_ui.value.finishPersistenceInProgress || _ui.value.finishPersistenceFailed) return
        if (_ui.value.finished || finishStarted) {
            discardFinishedRecord = true
            if (_ui.value.finished) writesScope.launch { repo.clearFinishedGame() }
        } else if (!dailyMode) {
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
        finishPersistenceHadIoFailure = false
        rewardedOfferLoggedResultId = null
        sessionSeed = if (dailyMode) daily?.seed else seedProvider()
        rng = ReplayableRandom(sessionSeed ?: 0L)
        undoSnapshot = null
        val state = engine.newGame(rng = rng)
        _ui.update {
            it.copy(
                state = state,
                pressure = 0,
                overdriveRemaining = 0,
                canUndo = false,
                finished = false,
                gameResultId = null,
                rewardDoubled = false,
                effects = null,
                freeUndosLeft = cfg.freeUndosPerGame,
                winBannerShown = false,
                lastResult = null,
                previousTiles = emptyList(),
                mergesTotal = 0,
                maxMergesInOneMove = 0,
                overdrivesSession = 0,
                undosSession = 0,
                highMergesSession = 0,
                dailySatisfied = dailyMode && dailyCompletedToday,
                finishPersistenceInProgress = false,
                finishPersistenceFailed = false,
            )
        }
        analytics.logEvent(
            if (dailyMode) "daily_started" else "game_started",
            daily?.let { mapOf("daily_type" to it.type.name) } ?: emptyMap(),
        )
        if (!dailyMode) persistGame()
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
        val today = LocalDay.todayEpochDay()
        writesScope.launch {
            val granted = repo.claimDailyChallenge(
                day = today,
                rewardGems = challenge.rewardGems,
                bonusXp = challenge.bonusXp,
            )
            if (granted) {
                dailyCompletedToday = true
                analytics.logEvent("daily_completed", mapOf("type" to challenge.type.name))
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
                finishPersistenceInProgress = true,
                finishPersistenceFailed = false,
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
        val record = FinishedGameRecord(
            id = "fg-" + UUID.randomUUID().toString(),
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
        pendingFinish = PendingFinish(record = record, summary = summary, day = today)
        persistPendingFinish()
    }

    fun retryFinishPersistence() {
        if (pendingFinish == null || finishWriteInFlight || !_ui.value.finishPersistenceFailed) return
        analytics.logEvent("game_finish_save_retry")
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
                val committedProgress = repo.progress.first()
                if (discardFinishedRecord) repo.clearFinishedGame()

                finishWriteInFlight = false
                pendingFinish = null
                val recovered = finishPersistenceHadIoFailure
                finishPersistenceHadIoFailure = false

                eff?.let { effects ->
                    effects.levelUps.forEach { analytics.logEvent("workshop_level_up", mapOf("level" to it)) }
                    effects.newAchievements.forEach { analytics.logEvent("achievement_unlocked", mapOf("id" to it.id)) }
                    if (effects.workshopPartsGained > 0) {
                        analytics.log(
                            AnalyticsEvents.resourceEarned(
                                resourceType = "workshop_parts",
                                source = "game_finish",
                                amount = effects.workshopPartsGained,
                                balanceAfter = committedProgress.workshopParts,
                            ),
                        )
                    }
                }
                _ui.update {
                    it.copy(
                        finished = true,
                        effects = eff,
                        gameResultId = pending.record.id,
                        removingMode = false,
                        finishPersistenceInProgress = false,
                        finishPersistenceFailed = false,
                    )
                }
                if (recovered) analytics.logEvent("game_finish_save_recovered")
                ads?.onGameFinished()
                logRewardedOfferIfVisible()
                analytics.logEvent(
                    "game_finished",
                    mapOf(
                        "score" to pending.summary.score,
                        "max_tile" to (1 shl pending.summary.maxTileLevel),
                        "moves" to pending.summary.moves,
                        "daily" to pending.summary.daily,
                    ),
                )
            } catch (_: IOException) {
                finishWriteInFlight = false
                _ui.update {
                    it.copy(
                        finished = false,
                        effects = null,
                        gameResultId = null,
                        finishPersistenceInProgress = false,
                        finishPersistenceFailed = true,
                    )
                }
                if (!finishPersistenceHadIoFailure) {
                    finishPersistenceHadIoFailure = true
                    analytics.logEvent("game_finish_save_failed", mapOf("reason" to "io"))
                }
            }
        }
    }

    private fun logRewardedOfferIfVisible() {
        val s = _ui.value
        val resultId = s.gameResultId ?: return
        val rewardAmount = s.effects?.gemsGained ?: return
        if (!s.finished || s.rewardDoubled || rewardAmount <= 0) return
        if (ads?.rewardedReady?.value != true) return
        if (rewardedOfferLoggedResultId == resultId) return
        rewardedOfferLoggedResultId = resultId
        analytics.log(
            AnalyticsEvents.rewardedOfferShown(
                placement = "post_run_result",
                rewardType = "gems",
                rewardAmount = rewardAmount,
                daily = dailyMode,
            ),
        )
    }

    fun grantDoubleReward() {
        val s = _ui.value
        val eff = s.effects ?: return
        val id = s.gameResultId ?: return
        if (s.rewardDoubled || eff.gemsGained <= 0) return
        writesScope.launch {
            val granted = repo.claimDoubleReward(id, eff.gemsGained)
            if (granted) {
                _ui.update { it.copy(rewardDoubled = true) }
                analytics.log(
                    AnalyticsEvents.rewardedCompleted(
                        placement = "post_run_result",
                        rewardType = "gems",
                        rewardAmount = eff.gemsGained,
                        daily = dailyMode,
                    ),
                )
            }
        }
    }

    private fun persistGame() {
        if (dailyMode || finishStarted) return
        val s = _ui.value
        val snapshot = SavedGame(
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
        )
        writesScope.launch {
            try {
                repo.saveGame(snapshot)
                if (saveIoFailureActive) {
                    saveIoFailureActive = false
                    analytics.logEvent("run_save_recovered")
                }
            } catch (_: IOException) {
                if (!saveIoFailureActive) {
                    saveIoFailureActive = true
                    analytics.logEvent("run_save_failed", mapOf("reason" to "io"))
                }
            }
        }
    }
}

/**
 * Маленький детерминированный PRNG с сериализуемой позицией. Random.nextInt/nextDouble строятся поверх
 * nextBits, поэтому одного счётчика draws достаточно для точного продолжения последовательности.
 */
private class ReplayableRandom(
    private val seed: Long,
    initialDraws: Long = 0L,
) : Random() {
    var draws: Long = initialDraws.coerceIn(0L, 1_000_000L)
        private set

    override fun nextBits(bitCount: Int): Int {
        require(bitCount in 0..32)
        if (bitCount == 0) return 0
        val index = draws++
        var z = seed + GOLDEN_GAMMA * (index + 1L)
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        z = z xor (z ushr 31)
        return (z ushr (64 - bitCount)).toInt()
    }

    private companion object {
        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_1 = -4658895280553007687L
        const val MIX_2 = -7723592293110705685L
    }
}
