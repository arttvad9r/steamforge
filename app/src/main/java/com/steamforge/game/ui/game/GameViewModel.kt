package com.steamforge.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.GameRules
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

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
    /** Уникальный id завершённой партии: ключ идемпотентности rewarded-награды. */
    val gameResultId: String? = null,
    val rewardDoubled: Boolean = false,
    // данные для анимаций UI
    val lastResult: MoveResult? = null,
    val previousTiles: List<Tile> = emptyList(),
    // сессия (для summary)
    val mergesTotal: Int = 0,
    val maxMergesInOneMove: Int = 0,
    val overdrivesSession: Int = 0,
    val undosSession: Int = 0,
    val highMergesSession: Int = 0,
    // настройки фидбека
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsActive: Boolean = true,
)

/**
 * Связывает чистый GameEngine с мета-системами и persistence.
 * Overdrive/pressure живёт здесь, а не в движке — механику Overdrive можно менять без GameEngine.
 */
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
    private var rng: Random = Random(if (dailyMode) daily?.seed ?: 0L else seedProvider())

    /** Seed текущей партии: персистится вместе с доской, чтобы спавны после restore были той же партии. */
    private var sessionSeed: Long? = if (dailyMode) daily?.seed else null

    /**
     * Запись результата партии должна пережить уход с экрана (навигация отменяет viewModelScope),
     * поэтому все записи репозитория идут в независимом scope на main.
     */
    private val writesScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var finishStarted = false

    private val _ui = MutableStateFlow(GameUiState(freeUndosLeft = cfg.freeUndosPerGame, daily = daily))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    /** Снапшот для undo: одноуровневая отмена. */
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
                    rng = Random(sessionSeed ?: 0L)
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
                _ui.update { s ->
                    s.copy(
                        gems = p.gems,
                        best = p.bestScore,
                        soundEnabled = p.soundEnabled,
                        hapticsEnabled = p.hapticsEnabled,
                        animationsActive = p.animationsEnabled && systemAnimationsEnabled,
                    )
                }
            }
        }
    }

    /** Process death на экране результата: overlay показывается заново, награда идемпотентна по id. */
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
            // Во время Overdrive давление заморожено, счётчик тратится на объединения
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

        if (daily != null && !_ui.value.dailySatisfied) {
            checkDailyGoal(result.state)
        }
        if (result.state.status == GameStatus.GAME_OVER) {
            finishGame(discard = false)
        } else {
            persistGame()
        }
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

    /** Wrench: удалить плитку низкого уровня за гемы. Доступна и в game over как спасение. */
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

    fun restart() {
        finishIfNeeded(discard = true)
        newGameInternal()
    }

    /** Выход с экрана: завершаем партию, чтобы засчитать XP/достижения; overlay не переносится на следующее посещение. */
    fun exit() {
        finishIfNeeded(discard = true)
    }

    fun markWinBannerShown() {
        _ui.update { it.copy(winBannerShown = true) }
    }

    private fun finishIfNeeded(discard: Boolean) {
        val s = _ui.value
        if (s.finished) {
            // overlay уже показан: выход/рестарт только выбрасывает запись
            if (discard) writesScope.launch { repo.clearFinishedGame() }
            return
        }
        finishGame(discard)
    }

    private fun newGameInternal() {
        sessionSeed = if (dailyMode) daily?.seed else seedProvider()
        rng = Random(sessionSeed ?: 0L)
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
            repo.updateProgress { p ->
                val baseStats = p.stats.copy(dailyCompleted = p.stats.dailyCompleted + 1)
                val unlocked = Achievements.newlyUnlocked(baseStats, p.unlockedAchievements)
                val unlockedGems = unlocked.sumOf { it.gemReward }
                p.copy(
                    dailyChallengeDay = today,
                    dailyChallengeDone = true,
                    gems = p.gems + challenge.rewardGems + unlockedGems,
                    totalXp = p.totalXp + challenge.bonusXp,
                    stats = baseStats.copy(gemsEarned = baseStats.gemsEarned + challenge.rewardGems + unlockedGems),
                    unlockedAchievements = p.unlockedAchievements + unlocked.map { it.id }.toSet(),
                    achievementDays = p.achievementDays + unlocked.associate { it.id to today },
                )
            }
        }
        analytics.logEvent("daily_completed", mapOf("type" to challenge.type.name))
    }

    /**
     * Завершение партии. Прогресс-награды и запись результата пишутся в одной DataStore-транзакции;
     * [discard] = выход/рестарт сразу после финиша: запись не нужна, overlay не восстановится.
     */
    private fun finishGame(discard: Boolean) {
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
            newBest = summary.score > s.best,
            state = GameSaveCodec.encode(
                SavedGame(s.state, sessionSeed, s.pressure, s.overdriveRemaining, s.freeUndosLeft),
            ),
        )
        writesScope.launch {
            var eff: FinishEffects? = null
            repo.applyGameFinish(record) { latest ->
                val (updated, e) = applyGameFinished(latest, summary, cfg)
                eff = e
                updated.copy(achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to today }) to e
            }
            eff?.let { effects ->
                effects.levelUps.forEach { analytics.logEvent("workshop_level_up", mapOf("level" to it)) }
                effects.newAchievements.forEach { analytics.logEvent("achievement_unlocked", mapOf("id" to it.id)) }
            }
            if (discard) repo.clearFinishedGame()
            _ui.update { it.copy(finished = true, effects = eff, gameResultId = resultId, removingMode = false) }
            ads?.onGameFinished()
            if (ads?.rewardedReady == true && (eff?.gemsGained ?: 0) > 0) {
                analytics.logEvent("rewarded_offered")
            }
            analytics.logEvent(
                "game_finished",
                mapOf(
                    "score" to summary.score,
                    "max_tile" to (1 shl summary.maxTileLevel),
                    "moves" to summary.moves,
                    "daily" to dailyMode,
                ),
            )
        }
        _ui.update { it.copy(removingMode = false) }
    }

    /**
     * Удвоение награды за партию после подтверждённого rewarded callback.
     * Идемпотентность держится на persisted FinishedGameRecord (репозиторий, атомарный claim),
     * а не на in-memory флаге: повторный callback / process death / повторный вход не выдают гемы второй раз.
     */
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
                ),
            )
        }
    }
}
