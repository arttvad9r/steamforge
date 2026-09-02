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
import com.steamforge.game.core.ReplayableRandom
import com.steamforge.game.core.Tile
import com.steamforge.game.data.DataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.GameSaveCodec
import com.steamforge.game.data.SavedGame
import com.steamforge.game.data.rewardedBonus
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.RewardedWorkshopBonus
import com.steamforge.game.progression.TileMilestone
import com.steamforge.game.progression.TileMilestones
import com.steamforge.game.progression.WeeklyChallenge
import com.steamforge.game.progression.applyGameFinished
import java.io.IOException
import java.util.UUID
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
    val weekly: WeeklyChallenge? = null,
    val weeklySubmissionAccepted: Boolean? = null,
    val tileMilestone: TileMilestone? = null,
    val winCelebrated: Boolean = false,
    val winBannerShown: Boolean = false,
    val removingMode: Boolean = false,
    val gameResultId: String? = null,
    val rewardDoubled: Boolean = false,
    val rewardedBonus: RewardedWorkshopBonus? = null,
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
    private val weeklyChallenge: WeeklyChallenge? = null,
    private val seedProvider: () -> Long = { System.currentTimeMillis() },
    private val savedGameProvider: suspend () -> SavedGame? = { repo.savedGame.first() },
    private val systemAnimationsEnabled: Boolean = true,
    private val ads: AdsManager? = null,
) : ViewModel() {

    init {
        require(!(dailyMode && weeklyChallenge != null)) { "Daily and weekly modes are mutually exclusive" }
    }

    private val engine = GameEngine()
    private val competitiveMode = weeklyChallenge != null
    private val daily = if (dailyMode) dailyProvider() else null
    private var sessionSeed: Long? = when {
        competitiveMode -> weeklyChallenge?.seed
        dailyMode -> daily?.seed
        else -> null
    }
    private var rng = ReplayableRandom(
        when {
            competitiveMode -> weeklyChallenge?.seed ?: 0L
            dailyMode -> daily?.seed ?: 0L
            else -> seedProvider()
        },
    )
    private var dailyCompletedToday = false
    private var knownMaxTileLevel = 0
    private val weeklyMoves = mutableListOf<Move>()

    private val writesScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var finishStarted = false
    private var discardFinishedRecord = false
    private var saveIoFailureActive = false

    private val _ui = MutableStateFlow(
        GameUiState(
            freeUndosLeft = if (competitiveMode) 0 else cfg.freeUndosPerGame,
            daily = daily,
            weekly = weeklyChallenge,
            winBannerShown = competitiveMode,
        ),
    )
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

    init {
        viewModelScope.launch {
            if (competitiveMode) {
                newGameInternal()
            } else {
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
            }

            repo.progress.collect { p ->
                val completedToday = dailyMode &&
                    p.dailyChallengeDay == LocalDay.todayEpochDay() &&
                    p.dailyChallengeDone
                dailyCompletedToday = completedToday
                knownMaxTileLevel = maxOf(knownMaxTileLevel, p.stats.maxTileLevel)
                val modeBest = if (competitiveMode && p.weekly.challengeId == weeklyChallenge?.id) {
                    p.weekly.bestScore
                } else {
                    p.bestScore
                }
                _ui.update { s ->
                    s.copy(
                        gems = p.gems,
                        best = modeBest,
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
                rewardedBonus = record.rewardedBonus().takeIf { bonus -> record.rewardedClaimed && bonus.xpGained > 0 },
                effects = record.toEffects(),
                state = restoredState?.state ?: GameState(score = record.score),
                winCelebrated = record.maxTileLevel >= GameRules().winLevel,
                freeUndosLeft = cfg.freeUndosPerGame,
            )
        }
        if (!record.rewardedClaimed && record.xpGained > 0) {
            analytics.logEvent("rewarded_offered", mapOf("bonus" to "workshop_xp"))
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
        if (s.finished || s.removingMode || finishStarted) return
        val snapshot = if (!competitiveMode) {
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

        val multiplier = if (s.overdriveRemaining > 0) cfg.overdriveMultiplier else 1
        val result = engine.applyMove(s.state, move, rng, multiplier)
        if (!result.moved) return
        if (competitiveMode) weeklyMoves += move

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
                canUndo = !competitiveMode,
            )
        }
        undoSnapshot = snapshot

        maybeRevealTileMilestone(result)
        if (daily != null && !_ui.value.dailySatisfied) checkDailyGoal(result.state)
        if (result.state.status == GameStatus.GAME_OVER) finishGame() else persistGame()
    }

    fun undo() {
        if (competitiveMode) return
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
        analytics.logEvent(
            "undo_used",
            mapOf(
                "paid" to paidUndo,
                "cost_gems" to if (paidUndo) cfg.undoGemsCost else 0,
                "gem_balance" to _ui.value.gems,
            ),
        )
        persistGame()
    }

    fun toggleRemovingMode() {
        if (competitiveMode) return
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
        if (competitiveMode) return false
        val s = _ui.value
        return !finishStarted &&
            !s.finished &&
            s.removingMode &&
            tile.level in 1..cfg.wrenchMaxTileLevel &&
            s.gems >= cfg.wrenchGemsCost
    }

    fun removeTile(tile: Tile) {
        if (competitiveMode) return
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
        analytics.logEvent(
            "powerup_used",
            mapOf(
                "type" to "wrench",
                "tile_level" to tile.level,
                "cost_gems" to cfg.wrenchGemsCost,
                "gem_balance" to _ui.value.gems,
            ),
        )
        persistGame()
    }

    fun restart() {
        if (finishStarted && !_ui.value.finished) return
        if (!competitiveMode && _ui.value.finished) writesScope.launch { repo.clearFinishedGame() }
        newGameInternal()
    }

    fun exit() {
        if (competitiveMode) return
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

    fun dismissTileMilestone() {
        val milestone = _ui.value.tileMilestone
        _ui.update {
            it.copy(
                tileMilestone = null,
                winBannerShown = it.winBannerShown || milestone?.level == GameRules().winLevel,
            )
        }
    }

    private fun maybeRevealTileMilestone(result: MoveResult) {
        if (competitiveMode) return
        val newMaxLevel = result.merges.maxOfOrNull { it.tile.level } ?: return
        val milestone = TileMilestones.newlyReached(knownMaxTileLevel, newMaxLevel) ?: return
        knownMaxTileLevel = maxOf(knownMaxTileLevel, newMaxLevel)
        _ui.update { it.copy(tileMilestone = milestone) }
        analytics.logEvent(
            "tile_milestone_unlocked",
            mapOf("level" to milestone.level, "value" to milestone.value),
        )
        writesScope.launch {
            repo.updateProgress { progress ->
                if (progress.stats.maxTileLevel >= newMaxLevel) {
                    progress
                } else {
                    progress.copy(stats = progress.stats.copy(maxTileLevel = newMaxLevel))
                }
            }
        }
    }

    private fun newGameInternal() {
        finishStarted = false
        discardFinishedRecord = false
        sessionSeed = when {
            competitiveMode -> weeklyChallenge?.seed
            dailyMode -> daily?.seed
            else -> seedProvider()
        }
        rng = ReplayableRandom(sessionSeed ?: 0L)
        weeklyMoves.clear()
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
                rewardedBonus = null,
                effects = null,
                weeklySubmissionAccepted = null,
                tileMilestone = null,
                freeUndosLeft = if (competitiveMode) 0 else cfg.freeUndosPerGame,
                winBannerShown = competitiveMode,
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
        when {
            competitiveMode -> analytics.logEvent("weekly_started", mapOf("challenge_id" to weeklyChallenge?.id.orEmpty()))
            dailyMode -> analytics.logEvent("daily_started", daily?.let { mapOf("daily_type" to it.type.name) } ?: emptyMap())
            else -> analytics.logEvent("game_started")
        }
        if (!dailyMode && !competitiveMode) persistGame()
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
                analytics.logEvent(
                    "daily_completed",
                    mapOf(
                        "type" to challenge.type.name,
                        "reward_gems" to challenge.rewardGems,
                        "bonus_xp" to challenge.bonusXp,
                    ),
                )
            }
        }
    }

    private fun finishGame() {
        if (competitiveMode) {
            finishWeeklyGame()
            return
        }

        val s = _ui.value
        if (s.finished || finishStarted) return
        finishStarted = true
        discardFinishedRecord = false
        _ui.update { it.copy(canUndo = false, removingMode = false) }

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
        writesScope.launch {
            var eff: FinishEffects? = null
            var finalGemBalance = s.gems
            repo.applyGameFinish(record) { latest ->
                val (updated, e) = applyGameFinished(latest, summary, cfg)
                eff = e
                val withAchievementDays = updated.copy(
                    achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to today },
                )
                finalGemBalance = withAchievementDays.gems
                withAchievementDays to e
            }
            if (discardFinishedRecord) repo.clearFinishedGame()
            eff?.let { effects ->
                effects.levelUps.forEach { analytics.logEvent("workshop_level_up", mapOf("level" to it)) }
                effects.newAchievements.forEach { analytics.logEvent("achievement_unlocked", mapOf("id" to it.id)) }
            }
            _ui.update { it.copy(finished = true, effects = eff, gameResultId = resultId, removingMode = false) }
            ads?.onGameFinished()
            if (ads?.rewardedReady?.value == true && (eff?.xpGained ?: 0) > 0) {
                analytics.logEvent("rewarded_offered", mapOf("bonus" to "workshop_xp"))
            }
            analytics.logEvent(
                "game_finished",
                mapOf(
                    "score" to summary.score,
                    "max_tile" to (1 shl summary.maxTileLevel),
                    "moves" to summary.moves,
                    "daily" to summary.daily,
                    "xp_gained" to (eff?.xpGained ?: 0),
                    "gems_gained" to (eff?.gemsGained ?: 0),
                    "gem_balance" to finalGemBalance,
                ),
            )
        }
    }

    private fun finishWeeklyGame() {
        val challenge = weeklyChallenge ?: return
        val s = _ui.value
        if (s.finished || finishStarted) return
        finishStarted = true
        _ui.update { it.copy(canUndo = false, removingMode = false) }
        val replay = weeklyMoves.toList()
        val previousBest = s.best

        writesScope.launch {
            val verified = repo.submitWeeklyChallenge(challenge, replay)
            val accepted = verified != null
            val newBest = verified?.score?.let { it > previousBest } == true
            _ui.update {
                it.copy(
                    finished = true,
                    weeklySubmissionAccepted = accepted,
                    effects = FinishEffects(newBest = newBest),
                )
            }
            analytics.logEvent(
                if (accepted) "weekly_finished" else "weekly_verification_failed",
                mapOf(
                    "challenge_id" to challenge.id,
                    "score" to (verified?.score ?: s.state.score),
                    "moves" to replay.size,
                ),
            )
        }
    }

    fun grantDoubleReward() {
        if (competitiveMode) return
        val s = _ui.value
        val eff = s.effects ?: return
        val id = s.gameResultId ?: return
        if (s.rewardDoubled || eff.xpGained <= 0) return
        writesScope.launch {
            val bonus = repo.claimDoubleReward(id, cfg)
            if (bonus != null) {
                _ui.update { it.copy(rewardDoubled = true, rewardedBonus = bonus) }
                bonus.levelUps.forEach { level ->
                    analytics.logEvent("workshop_level_up", mapOf("level" to level, "source" to "rewarded"))
                }
                analytics.logEvent(
                    "rewarded_workshop_xp_granted",
                    mapOf(
                        "xp" to bonus.xpGained,
                        "gems_gained" to bonus.gemsGained,
                        "level_ups" to bonus.levelUps.size,
                    ),
                )
            }
        }
    }

    private fun persistGame() {
        if (dailyMode || competitiveMode || finishStarted) return
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
