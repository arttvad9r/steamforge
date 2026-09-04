package com.steamforge.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.ContractCounters
import com.steamforge.game.progression.ContractLedger
import com.steamforge.game.progression.DailyContracts
import com.steamforge.game.progression.GameEvent
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.PlayerStats
import com.steamforge.game.progression.Reward
import com.steamforge.game.progression.RewardSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "steamforge")

/**
 * Единая точка локального persistence поверх Preferences DataStore.
 * Все наградные операции, которым нужна идемпотентность, выполняются внутри одного dataStore.edit.
 */
class SteamforgeRepository(private val context: Context) : DataRepo {

    private object Keys {
        val game = stringPreferencesKey("game_save")
        val finishedGame = stringPreferencesKey("finished_game")
        val gems = intPreferencesKey("gems")
        val totalXp = intPreferencesKey("total_xp")
        val bestScore = intPreferencesKey("best_score")
        val workshopParts = intPreferencesKey("workshop_parts")
        val workshopCoreStage = intPreferencesKey("workshop_core_stage")
        val workshopPressureStage = intPreferencesKey("workshop_pressure_stage")
        val workshopGearPressStage = intPreferencesKey("workshop_gear_press_stage")
        val blueprintPieces = stringSetPreferencesKey("blueprint_pieces")
        val gamesPlayed = intPreferencesKey("stat_games")
        val totalScore = longPreferencesKey("stat_total_score")
        val maxTileLevel = intPreferencesKey("stat_max_tile")
        val totalMerges = intPreferencesKey("stat_merges")
        val maxMergesInOneMove = intPreferencesKey("stat_combo")
        val overdrives = intPreferencesKey("stat_overdrives")
        val undos = intPreferencesKey("stat_undos")
        val dailyCompleted = intPreferencesKey("stat_daily")
        val highestDailyStreak = intPreferencesKey("stat_highest_daily_streak")
        val gemsEarned = longPreferencesKey("stat_gems_earned")
        val achievements = stringSetPreferencesKey("achievements")
        val achievementDays = stringSetPreferencesKey("achievement_days")
        val cosmetics = stringSetPreferencesKey("cosmetics")
        val dailyChallengeDay = longPreferencesKey("daily_challenge_day")
        val dailyChallengeDone = booleanPreferencesKey("daily_challenge_done")
        val dailyRewardDay = longPreferencesKey("daily_reward_day")
        val dailyRewardStreak = intPreferencesKey("daily_reward_streak")

        val contractDay = longPreferencesKey("contract_day")
        val contractScore = intPreferencesKey("contract_score")
        val contractBestRunScore = intPreferencesKey("contract_best_run_score")
        val contractMerges = intPreferencesKey("contract_merges")
        val contractMoves = intPreferencesKey("contract_moves")
        val contractRuns = intPreferencesKey("contract_runs")
        val contractMaxTile = intPreferencesKey("contract_max_tile")
        val contractMaxCombo = intPreferencesKey("contract_max_combo")
        val contractOverdrives = intPreferencesKey("contract_overdrives")
        val contractMadeTiles = stringSetPreferencesKey("contract_made_tiles")
        val contractClaimed = stringSetPreferencesKey("contract_claimed")
        val contractActiveSeed = longPreferencesKey("contract_active_seed")
        val contractActiveScore = intPreferencesKey("contract_active_score")
        val contractActiveBestRunScore = intPreferencesKey("contract_active_best_run_score")
        val contractActiveMerges = intPreferencesKey("contract_active_merges")
        val contractActiveMoves = intPreferencesKey("contract_active_moves")
        val contractActiveMaxTile = intPreferencesKey("contract_active_max_tile")
        val contractActiveMaxCombo = intPreferencesKey("contract_active_max_combo")
        val contractActiveOverdrives = intPreferencesKey("contract_active_overdrives")
        val contractActiveMadeTiles = stringSetPreferencesKey("contract_active_made_tiles")

        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val animationsEnabled = booleanPreferencesKey("animations_enabled")
        val analyticsConsent = booleanPreferencesKey("analytics_consent")
    }

    override val progress: Flow<PlayerProgress> = context.dataStore.data.map(::mapProgress)

    override val savedGame: Flow<SavedGame?> = context.dataStore.data.map { prefs ->
        prefs[Keys.game]?.let(GameSaveCodec::decode)
    }

    override val finishedGame: Flow<FinishedGameRecord?> = context.dataStore.data.map { prefs ->
        prefs[Keys.finishedGame]?.let(FinishedGameCodec::decode)
    }

    override suspend fun saveGame(state: SavedGame) {
        context.dataStore.edit { prefs ->
            val day = LocalDay.todayEpochDay()
            val previousSaved = prefs[Keys.game]?.let(GameSaveCodec::decode)
            val runSeed = state.seed
            prefs[Keys.game] = GameSaveCodec.encode(state)
            if (runSeed == null) return@edit

            val base = contractBaseForDay(mapProgress(prefs), day, previousSaved)
            val updated = DailyContracts.recordLiveSnapshot(
                progress = base,
                day = day,
                runSeed = runSeed,
                snapshot = contractSnapshot(base, state, previousSaved),
            )
            writeProgress(prefs, updated)
        }
    }

    override suspend fun saveGameWithContractProgress(state: SavedGame, day: Long) {
        context.dataStore.edit { prefs ->
            val previousSaved = prefs[Keys.game]?.let(GameSaveCodec::decode)
            val runSeed = state.seed
            prefs[Keys.game] = GameSaveCodec.encode(state)
            if (runSeed == null) return@edit

            val base = contractBaseForDay(mapProgress(prefs), day, previousSaved)
            val updated = DailyContracts.recordLiveSnapshot(
                progress = base,
                day = day,
                runSeed = runSeed,
                snapshot = contractSnapshot(base, state, previousSaved),
            )
            writeProgress(prefs, updated)
        }
    }

    override suspend fun clearGame() {
        context.dataStore.edit { it.remove(Keys.game) }
    }

    override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
        context.dataStore.edit { prefs -> writeProgress(prefs, block(mapProgress(prefs))) }
    }

    override suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, com.steamforge.game.progression.FinishEffects>,
    ) {
        context.dataStore.edit { prefs ->
            val existingFinished = prefs[Keys.finishedGame]?.let(FinishedGameCodec::decode)
            if (existingFinished?.id == record.id) {
                prefs.remove(Keys.game)
                return@edit
            }

            val previousSaved = prefs[Keys.game]?.let(GameSaveCodec::decode)
            val finalSaved = GameSaveCodec.decode(record.state)
            val baseProgress = contractBaseForDay(mapProgress(prefs), record.day, previousSaved)
            val base = if (finalSaved?.seed != null) {
                DailyContracts.recordFinishedRun(
                    progress = baseProgress,
                    day = record.day,
                    runSeed = finalSaved.seed,
                    snapshot = contractSnapshot(baseProgress, finalSaved, previousSaved),
                )
            } else {
                baseProgress
            }
            val (updated, effects) = finisher(base)
            prefs[Keys.finishedGame] = FinishedGameCodec.encode(record.withEffects(effects))
            prefs.remove(Keys.game)
            writeProgress(prefs, updated)
        }
    }

    override suspend fun applyGameFinishWithContractProgress(
        record: FinishedGameRecord,
        summary: GameSummary,
        day: Long,
        runSeed: Long,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, com.steamforge.game.progression.FinishEffects>,
    ) {
        context.dataStore.edit { prefs ->
            val existingFinished = prefs[Keys.finishedGame]?.let(FinishedGameCodec::decode)
            if (existingFinished?.id == record.id) {
                prefs.remove(Keys.game)
                return@edit
            }

            val previousSaved = prefs[Keys.game]?.let(GameSaveCodec::decode)
            val base = contractBaseForDay(mapProgress(prefs), day, previousSaved)
            val finalSaved = GameSaveCodec.decode(record.state)
            val finalSnapshot = if (finalSaved?.seed == runSeed) {
                contractSnapshot(base, finalSaved, previousSaved)
            } else {
                ContractCounters.fromSummary(summary)
            }
            val withContracts = DailyContracts.recordFinishedRun(
                progress = base,
                day = day,
                runSeed = runSeed,
                snapshot = finalSnapshot,
            )
            val (updated, effects) = finisher(withContracts)
            prefs[Keys.finishedGame] = FinishedGameCodec.encode(record.withEffects(effects))
            prefs.remove(Keys.game)
            writeProgress(prefs, updated)
        }
    }

    override suspend fun claimDoubleReward(gameResultId: String, gems: Int): Boolean {
        if (gems <= 0) return false
        var granted = false
        context.dataStore.edit { prefs ->
            val record = prefs[Keys.finishedGame]?.let(FinishedGameCodec::decode)
            if (record != null && record.id == gameResultId && !record.rewardedClaimed) {
                val progress = mapProgress(prefs)
                val (updated, _) = RewardSystem.apply(progress, Reward.Gems(gems))
                prefs[Keys.finishedGame] = FinishedGameCodec.encode(record.copy(rewardedClaimed = true))
                writeProgress(prefs, updated)
                granted = true
            }
        }
        return granted
    }

    override suspend fun claimDailyChallenge(day: Long, rewardGems: Int, bonusXp: Int): Boolean {
        if (rewardGems < 0 || bonusXp < 0) return false
        var granted = false
        context.dataStore.edit { prefs ->
            val progress = mapProgress(prefs)
            if (progress.dailyChallengeDay == day && progress.dailyChallengeDone) return@edit

            val baseStats = progress.stats.copy(dailyCompleted = progress.stats.dailyCompleted + 1)
            val unlocked = Achievements.newlyUnlocked(baseStats, progress.unlockedAchievements)
            val base = progress.copy(
                dailyChallengeDay = day,
                dailyChallengeDone = true,
                totalXp = (progress.totalXp.toLong() + bonusXp.toLong())
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt(),
                stats = baseStats,
                unlockedAchievements = progress.unlockedAchievements + unlocked.map { it.id }.toSet(),
                achievementDays = progress.achievementDays + unlocked.associate { it.id to day },
            )
            val (updated, _) = RewardSystem.apply(
                base,
                Reward.Gems(rewardGems),
                Reward.Gems(unlocked.sumOf { it.gemReward }),
            )
            writeProgress(prefs, updated)
            granted = true
        }
        return granted
    }

    override suspend fun claimContract(day: Long, contractId: String): Boolean {
        var granted = false
        context.dataStore.edit { prefs ->
            val progress = mapProgress(prefs)
            val updated = DailyContracts.claim(progress, day, contractId)
            if (updated == progress) return@edit
            writeProgress(prefs, updated)
            granted = true
        }
        return granted
    }

    override suspend fun clearFinishedGame() {
        context.dataStore.edit { it.remove(Keys.finishedGame) }
    }

    /**
     * Сбрасывает только игровые данные. Privacy-выбор и пользовательские настройки сохраняются,
     * поэтому reset progression не возвращает приложение в промежуточное consent-состояние.
     */
    override suspend fun resetGameProgress() {
        context.dataStore.edit { prefs ->
            val sound = prefs[Keys.soundEnabled]
            val haptics = prefs[Keys.hapticsEnabled]
            val animations = prefs[Keys.animationsEnabled]
            val consent = prefs[Keys.analyticsConsent]
            prefs.clear()
            if (sound != null) prefs[Keys.soundEnabled] = sound
            if (haptics != null) prefs[Keys.hapticsEnabled] = haptics
            if (animations != null) prefs[Keys.animationsEnabled] = animations
            if (consent != null) prefs[Keys.analyticsConsent] = consent
        }
    }

    private fun mapProgress(prefs: Preferences): PlayerProgress {
        val persistedBest = prefs[Keys.bestScore] ?: 0
        return PlayerProgress(
            gems = prefs[Keys.gems] ?: 0,
            totalXp = prefs[Keys.totalXp] ?: 0,
            bestScore = persistedBest,
            workshopParts = prefs[Keys.workshopParts] ?: 0,
            workshopCoreStage = prefs[Keys.workshopCoreStage] ?: 0,
            workshopPressureStage = prefs[Keys.workshopPressureStage] ?: 0,
            workshopGearPressStage = prefs[Keys.workshopGearPressStage] ?: 0,
            blueprintPieces = prefs[Keys.blueprintPieces] ?: emptySet(),
            stats = PlayerStats(
                gamesPlayed = prefs[Keys.gamesPlayed] ?: 0,
                bestScore = persistedBest,
                totalScore = prefs[Keys.totalScore] ?: 0L,
                maxTileLevel = prefs[Keys.maxTileLevel] ?: 0,
                totalMerges = prefs[Keys.totalMerges] ?: 0,
                maxMergesInOneMove = prefs[Keys.maxMergesInOneMove] ?: 0,
                overdrives = prefs[Keys.overdrives] ?: 0,
                undos = prefs[Keys.undos] ?: 0,
                dailyCompleted = prefs[Keys.dailyCompleted] ?: 0,
                highestDailyStreak = prefs[Keys.highestDailyStreak] ?: 0,
                gemsEarned = prefs[Keys.gemsEarned] ?: 0L,
            ),
            unlockedAchievements = prefs[Keys.achievements] ?: emptySet(),
            achievementDays = (prefs[Keys.achievementDays] ?: emptySet())
                .mapNotNull { entry ->
                    val i = entry.indexOf(':')
                    if (i <= 0) null else entry.take(i) to (entry.substring(i + 1).toLongOrNull() ?: 0L)
                }
                .toMap(),
            unlockedCosmetics = prefs[Keys.cosmetics] ?: emptySet(),
            dailyChallengeDay = prefs[Keys.dailyChallengeDay] ?: -1L,
            dailyChallengeDone = prefs[Keys.dailyChallengeDone] ?: false,
            dailyRewardDay = prefs[Keys.dailyRewardDay] ?: -1L,
            dailyRewardStreak = prefs[Keys.dailyRewardStreak] ?: 0,
            contracts = ContractLedger(
                day = prefs[Keys.contractDay] ?: -1L,
                totals = ContractCounters(
                    score = prefs[Keys.contractScore] ?: 0,
                    bestRunScore = prefs[Keys.contractBestRunScore] ?: 0,
                    merges = prefs[Keys.contractMerges] ?: 0,
                    moves = prefs[Keys.contractMoves] ?: 0,
                    runs = prefs[Keys.contractRuns] ?: 0,
                    maxTileLevel = prefs[Keys.contractMaxTile] ?: 0,
                    maxCombo = prefs[Keys.contractMaxCombo] ?: 0,
                    overdrives = prefs[Keys.contractOverdrives] ?: 0,
                    madeTilesByLevel = decodeContractTileCounts(prefs[Keys.contractMadeTiles] ?: emptySet()),
                ),
                claimedIds = prefs[Keys.contractClaimed] ?: emptySet(),
                activeRunSeed = prefs[Keys.contractActiveSeed],
                activeRun = ContractCounters(
                    score = prefs[Keys.contractActiveScore] ?: 0,
                    bestRunScore = prefs[Keys.contractActiveBestRunScore] ?: 0,
                    merges = prefs[Keys.contractActiveMerges] ?: 0,
                    moves = prefs[Keys.contractActiveMoves] ?: 0,
                    maxTileLevel = prefs[Keys.contractActiveMaxTile] ?: 0,
                    maxCombo = prefs[Keys.contractActiveMaxCombo] ?: 0,
                    overdrives = prefs[Keys.contractActiveOverdrives] ?: 0,
                    madeTilesByLevel = decodeContractTileCounts(prefs[Keys.contractActiveMadeTiles] ?: emptySet()),
                ),
            ),
            soundEnabled = prefs[Keys.soundEnabled] ?: true,
            hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
            animationsEnabled = prefs[Keys.animationsEnabled] ?: true,
            analyticsConsent = prefs[Keys.analyticsConsent],
        )
    }

    private fun writeProgress(prefs: androidx.datastore.preferences.core.MutablePreferences, p: PlayerProgress) {
        prefs[Keys.gems] = p.gems
        prefs[Keys.totalXp] = p.totalXp
        prefs[Keys.bestScore] = p.bestScore
        prefs[Keys.workshopParts] = p.workshopParts
        prefs[Keys.workshopCoreStage] = p.workshopCoreStage
        prefs[Keys.workshopPressureStage] = p.workshopPressureStage
        prefs[Keys.workshopGearPressStage] = p.workshopGearPressStage
        prefs[Keys.blueprintPieces] = p.blueprintPieces
        prefs[Keys.gamesPlayed] = p.stats.gamesPlayed
        prefs[Keys.totalScore] = p.stats.totalScore
        prefs[Keys.maxTileLevel] = p.stats.maxTileLevel
        prefs[Keys.totalMerges] = p.stats.totalMerges
        prefs[Keys.maxMergesInOneMove] = p.stats.maxMergesInOneMove
        prefs[Keys.overdrives] = p.stats.overdrives
        prefs[Keys.undos] = p.stats.undos
        prefs[Keys.dailyCompleted] = p.stats.dailyCompleted
        prefs[Keys.highestDailyStreak] = p.stats.highestDailyStreak
        prefs[Keys.gemsEarned] = p.stats.gemsEarned
        prefs[Keys.achievements] = p.unlockedAchievements
        prefs[Keys.achievementDays] = p.achievementDays.map { (id, day) -> "$id:$day" }.toSet()
        prefs[Keys.cosmetics] = p.unlockedCosmetics
        prefs[Keys.dailyChallengeDay] = p.dailyChallengeDay
        prefs[Keys.dailyChallengeDone] = p.dailyChallengeDone
        prefs[Keys.dailyRewardDay] = p.dailyRewardDay
        prefs[Keys.dailyRewardStreak] = p.dailyRewardStreak

        prefs[Keys.contractDay] = p.contracts.day
        prefs[Keys.contractScore] = p.contracts.totals.score
        prefs[Keys.contractBestRunScore] = p.contracts.totals.bestRunScore
        prefs[Keys.contractMerges] = p.contracts.totals.merges
        prefs[Keys.contractMoves] = p.contracts.totals.moves
        prefs[Keys.contractRuns] = p.contracts.totals.runs
        prefs[Keys.contractMaxTile] = p.contracts.totals.maxTileLevel
        prefs[Keys.contractMaxCombo] = p.contracts.totals.maxCombo
        prefs[Keys.contractOverdrives] = p.contracts.totals.overdrives
        prefs[Keys.contractMadeTiles] = encodeContractTileCounts(p.contracts.totals.madeTilesByLevel)
        prefs[Keys.contractClaimed] = p.contracts.claimedIds
        if (p.contracts.activeRunSeed != null) {
            prefs[Keys.contractActiveSeed] = p.contracts.activeRunSeed
        } else {
            prefs.remove(Keys.contractActiveSeed)
        }
        prefs[Keys.contractActiveScore] = p.contracts.activeRun.score
        prefs[Keys.contractActiveBestRunScore] = p.contracts.activeRun.bestRunScore
        prefs[Keys.contractActiveMerges] = p.contracts.activeRun.merges
        prefs[Keys.contractActiveMoves] = p.contracts.activeRun.moves
        prefs[Keys.contractActiveMaxTile] = p.contracts.activeRun.maxTileLevel
        prefs[Keys.contractActiveMaxCombo] = p.contracts.activeRun.maxCombo
        prefs[Keys.contractActiveOverdrives] = p.contracts.activeRun.overdrives
        prefs[Keys.contractActiveMadeTiles] = encodeContractTileCounts(p.contracts.activeRun.madeTilesByLevel)

        prefs[Keys.soundEnabled] = p.soundEnabled
        prefs[Keys.hapticsEnabled] = p.hapticsEnabled
        prefs[Keys.animationsEnabled] = p.animationsEnabled
        if (p.analyticsConsent != null) prefs[Keys.analyticsConsent] = p.analyticsConsent else prefs.remove(Keys.analyticsConsent)
    }

    /**
     * При смене календарного дня старый прогресс Contracts обнуляется. Если обычная партия уже была
     * сохранена до полуночи, её последний snapshot становится baseline нового дня и не добавляется повторно.
     */
    private fun contractBaseForDay(progress: PlayerProgress, day: Long, previousSaved: SavedGame?): PlayerProgress {
        if (progress.contracts.day == day) return progress
        val savedSeed = previousSaved?.seed
        return progress.copy(
            contracts = ContractLedger(
                day = day,
                activeRunSeed = savedSeed,
                activeRun = if (savedSeed != null) previousSaved.toContractCounters() else ContractCounters(),
            ),
        )
    }

    private fun contractSnapshot(
        progress: PlayerProgress,
        state: SavedGame,
        previousSaved: SavedGame?,
    ): ContractCounters {
        val runSeed = state.seed
        val previousRun = if (runSeed != null && progress.contracts.activeRunSeed == runSeed) {
            progress.contracts.activeRun
        } else {
            ContractCounters()
        }
        val inferred = if (state.state.moves > previousRun.moves) {
            inferMergeCreatedTileEvents(previousSaved, state)
        } else {
            emptyList()
        }
        val madeTiles = previousRun.record(inferred).madeTilesByLevel
        return state.toContractCounters(madeTiles)
    }

    private fun SavedGame.toContractCounters(
        madeTilesByLevel: Map<Int, Int> = emptyMap(),
    ): ContractCounters = ContractCounters(
        score = state.score,
        bestRunScore = state.score,
        merges = mergesTotal,
        moves = state.moves,
        maxTileLevel = state.maxLevel,
        maxCombo = maxMergesInOneMove,
        overdrives = overdrivesSession,
        madeTilesByLevel = madeTilesByLevel,
    )

    private fun SavedGame.toSummary(daily: Boolean): GameSummary = GameSummary(
        score = state.score,
        maxTileLevel = state.maxLevel,
        moves = state.moves,
        merges = mergesTotal,
        maxMergesInOneMove = maxMergesInOneMove,
        overdrives = overdrivesSession,
        undos = undosSession,
        won = state.won,
        daily = daily,
    )
}

/**
 * Выводит только merge-created tiles из двух соседних autosave-состояний одного run.
 * Последний новый id всегда принадлежит spawned tile; он намеренно не считается MAKE_TILE.
 * При пропущенном autosave функция возвращает пусто, чтобы лучше недосчитать, чем начислить ложный прогресс.
 */
internal fun inferMergeCreatedTileEvents(previous: SavedGame?, current: SavedGame): List<GameEvent.TileCreated> {
    val currentSeed = current.seed ?: return emptyList()
    if (previous?.seed != currentSeed) return emptyList()
    if (current.state.moves != previous.state.moves + 1) return emptyList()

    val mergeDelta = current.mergesTotal - previous.mergesTotal
    if (mergeDelta <= 0) return emptyList()

    val previousIds = previous.state.tiles.asSequence().map { it.id }.toHashSet()
    val newTiles = current.state.tiles.filter { it.id !in previousIds }
    if (newTiles.size != mergeDelta + 1) return emptyList()

    val spawnedId = newTiles.maxOfOrNull { it.id } ?: return emptyList()
    val mergedTiles = newTiles.filterNot { it.id == spawnedId }
    if (mergedTiles.size != mergeDelta) return emptyList()

    return mergedTiles
        .groupingBy { it.level }
        .eachCount()
        .filterKeys { it in 1..30 }
        .map { (level, count) -> GameEvent.TileCreated(level, count) }
}

internal fun encodeContractTileCounts(counts: Map<Int, Int>): Set<String> = counts
    .asSequence()
    .filter { (level, count) -> level in 1..30 && count > 0 }
    .map { (level, count) -> "$level:${count.coerceAtMost(10_000_000)}" }
    .toSet()

internal fun decodeContractTileCounts(entries: Set<String>): Map<Int, Int> = entries
    .mapNotNull { entry ->
        val separator = entry.indexOf(':')
        if (separator <= 0) return@mapNotNull null
        val level = entry.take(separator).toIntOrNull() ?: return@mapNotNull null
        val count = entry.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
        if (level !in 1..30 || count <= 0) null else level to count.coerceAtMost(10_000_000)
    }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, values) -> values.maxOrNull() ?: 0 }
    .filterValues { it > 0 }
