package com.steamforge.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
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
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _ui = MutableStateFlow(GameUiState(freeUndosLeft = cfg.freeUndosPerGame, daily = daily))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var undoSnapshot: UndoSnapshot? = null

    private data class UndoSnapshot(val state: GameState, val pressure: Int, val overdriveRemaining: Int)

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
                gameResultId = record.id,
                rewardDoubled = record.rewardedClaimed,
                effects = record.toEffects(),
                state = restoredState?.state ?: GameState(score = record.score),
                winCelebrated = record.maxTileLevel >= GameRules().winLevel,
                freeUndosLeft = cfg.freeUndosPerGame,
            )
        }
        if (!record.rewardedClaimed && record.gemsGained > 0) {
            analytics.logEvent("rewarded_offered")
        }
    }

    private fun FinishedGameRecord.toEffects() = FinishEffects(
        xpGained = xpGained,
        gemsGained = gemsGained,
        levelUps = levelUps,
        newAchievements = newAchievementIds.mapNotNull { Achievements.byId(it) },
        newBest = newBest,
    )

    fun onMove(move: Move) {
        val s = _ui.value
        if (s.finished || s.removingMode) return
        val snapshot = UndoSnapshot(s.state, s.pressure, s.overdriveRemaining)
        val multiplier = if (s.overdriveRemaining > 0) cfg.overdriveMultiplier else 1
        val result = engine.applyMove(s.state, move, rng, multiplier)
        if (!result.moved) return

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
        if (s.finished) return
        if (s.freeUndosLeft > 0) {
            _ui.update { it.copy(freeUndosLeft = it.freeUndosLeft - 1) }
        } else if (s.gems >= cfg.undoGemsCost) {
            writesScope.launch { repo.updateProgress { p -> p.copy(gems = p.gems - cfg.undoGemsCost) } }
        } else {
            return
        }
        _ui.update {
            it.copy(
                state = snap.state,
                pressure = snap.pressure,
                overdriveRemaining = snap.overdriveRemaining,
                lastResult = null,
                previousTiles = emptyList(),
                canUndo = false,
                undosSession = it.undosSession + 1,
            )
        }
        undoSnapshot = null
        analytics.logEvent("undo_used")
        persistGame()
    }

    fun toggleRemovingMode() {
        _ui.update { it.copy(removingMode = !it.removingMode) }
    }

    fun canRemoveTile(tile: Tile): Boolean =
        tile.level in 1..cfg.wrenchMaxTileLevel && _ui.value.gems >= cfg.wrenchGemsCost

    fun removeTile(tile: Tile) {
        val s = _ui.value
        if (!canRemoveTile(tile)) return
        val tiles = s.state.tiles.filterNot { it.id == tile.id }
        if (tiles.size == s.state.tiles.size) return
        writesScope.launch { repo.updateProgress { p -> p.copy(gems = p.gems - cfg.wrenchGemsCost) } }
        _ui.update {
            it.copy(
                state = s.state.copy(tiles = tiles, status = GameStatus.PLAYING),
                removingMode = false,
                canUndo = false,
            )
        }
        undoSnapshot = null
        analytics.logEvent("powerup_used", mapOf("type" to "wrench", "tile_level" to tile.level))
        persistGame()
    }

    /** Новая партия после результата. Активная незавершённая партия при прямом вызове просто заменяется. */
    fun restart() {
        if (_ui.value.finished) writesScope.launch { repo.clearFinishedGame() }
        newGameInternal()
    }

    /**
     * Выход не является завершением партии и не выдаёт XP. Обычная партия сохраняется для продолжения,
     * Daily-попытка просто закрывается. Завершённый overlay при выходе удаляется из persistence.
     */
    fun exit() {
        if (_ui.value.finished) {
            writesScope.launch { repo.clearFinishedGame() }
        } else if (!dailyMode) {
            persistGame()
        }
    }

    fun markWinBannerShown() {
        _ui.update { it.copy(winBannerShown = true) }
    }

    private fun newGameInternal() {
        finishStarted = false
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
                ),
            ),
        )
        writesScope.launch {
            var eff: FinishEffects? = null
            repo.applyGameFinish(record) { latest ->
                val (updated, e) = applyGameFinished(latest, summary, cfg)
                eff = e
                updated.copy(
                    achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to today },
                ) to e
            }
            eff?.let { effects ->
                effects.levelUps.forEach { analytics.logEvent("workshop_level_up", mapOf("level" to it)) }
                effects.newAchievements.forEach { analytics.logEvent("achievement_unlocked", mapOf("id" to it.id)) }
            }
            _ui.update { it.copy(finished = true, effects = eff, gameResultId = resultId, removingMode = false) }
            ads?.onGameFinished()
            if (ads?.rewardedReady?.value == true && (eff?.gemsGained ?: 0) > 0) {
                analytics.logEvent("rewarded_offered")
            }
            analytics.logEvent(
                "game_finished",
                mapOf(
                    "score" to summary.score,
                    "max_tile" to (1 shl summary.maxTileLevel),
                    "moves" to summary.moves,
                    "daily" to summary.daily,
                ),
            )
        }
    }

    fun grantDoubleReward() {
        val s = _ui.value
        val eff = s.effects ?: return
        val id = s.gameResultId ?: return
        if (s.rewardDoubled || eff.gemsGained <= 0) return
        writesScope.launch {
            val granted = repo.claimDoubleReward(id, eff.gemsGained)
            if (granted) _ui.update { it.copy(rewardDoubled = true) }
        }
    }

    private fun persistGame() {
        if (dailyMode) return
        val s = _ui.value
        writesScope.launch {
            repo.saveGame(
                SavedGame(
                    state = s.state,
                    seed = sessionSeed,
                    pressure = s.pressure,
                    overdriveRemaining = s.overdriveRemaining,
                    freeUndosLeft = s.freeUndosLeft,
                    rngDraws = rng.draws,
                ),
            )
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
