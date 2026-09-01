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
import com.steamforge.game.progression.GameSummary
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.PlayerStats
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
        val gamesPlayed = intPreferencesKey("stat_games")
        val totalScore = longPreferencesKey("stat_total_score")
        val maxTileLevel = intPreferencesKey("stat_max_tile")
        val totalMerges = intPreferencesKey("stat_merges")
        val maxMergesInOneMove = intPreferencesKey("stat_combo")
        val overdrives = intPreferencesKey("stat_overdrives")
        val undos = intPreferencesKey("stat_undos")
        val dailyCompleted = intPreferencesKey("stat_daily")
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
        val contractMerges = intPreferencesKey("contract_merges")
        val contractMoves = intPreferencesKey("contract_moves")
        val contractRuns = intPreferencesKey("contract_runs")
        val contractMaxTile = intPreferencesKey("contract_max_tile")
        val contractOverdrives = intPreferencesKey("contract_overdrives")
        val contractClaimed = stringSetPreferencesKey("contract_claimed")
        val contractActiveSeed = longPreferencesKey("contract_active_seed")
        val contractActiveScore = intPreferencesKey("contract_active_score")
        val contractActiveMerges = intPreferencesKey("contract_active_merges")
        val contractActiveMoves = intPreferencesKey("contract_active_moves")
        val contractActiveMaxTile = intPreferencesKey("contract_active_max_tile")
        val contractActiveOverdrives = intPreferencesKey("contract_active_overdrives")

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
                snapshot = state.toContractCounters(),
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
                snapshot = state.toContractCounters(),
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
            val previousSaved = prefs[Keys.game]?.let(GameSaveCodec::decode)
            val finalSaved = GameSaveCodec.decode(record.state)
            val baseProgress = contractBaseForDay(mapProgress(prefs), record.day, previousSaved)
            val base = if (finalSaved?.seed != null) {
                DailyContracts.recordFinishedRun(
                    progress = baseProgress,
                    day = record.day,
                    runSeed = finalSaved.seed,
                    summary = finalSaved.toSummary(record.daily),
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
            val previousSaved = prefs[Keys.game]?.let(GameSaveCodec::decode)
            val base = contractBaseForDay(mapProgress(prefs), day, previousSaved)
            val withContracts = DailyContracts.recordFinishedRun(
                progress = base,
                day = day,
                runSeed = runSeed,
                summary = summary,
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
                prefs[Keys.finishedGame] = FinishedGameCodec.encode(record.copy(rewardedClaimed = true))
                val progress = mapProgress(prefs)
                writeProgress(
                    prefs,
                    progress.copy(
                        gems = progress.gems + gems,
                        stats = progress.stats.copy(gemsEarned = progress.stats.gemsEarned + gems),
                    ),
                )
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
            val unlockedGems = unlocked.sumOf { it.gemReward }
            val totalGems = rewardGems + unlockedGems
            val updated = progress.copy(
                dailyChallengeDay = day,
                dailyChallengeDone = true,
                gems = progress.gems + totalGems,
                totalXp = progress.totalXp + bonusXp,
                stats = baseStats.copy(gemsEarned = baseStats.gemsEarned + totalGems),
                unlockedAchievements = progress.unlockedAchievements + unlocked.map { it.id }.toSet(),
                achievementDays = progress.achievementDays + unlocked.associate { it.id to day },
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
            val ledger = DailyContracts.normalized(progress.contracts, day)
            val contract = DailyContracts.forEpochDay(day).firstOrNull { it.id == contractId } ?: return@edit
            if (contract.id in ledger.claimedIds || !DailyContracts.isComplete(contract, ledger)) return@edit

            val reward = contract.rewardGems
            val updated = progress.copy(
                gems = progress.gems + reward,
                stats = progress.stats.copy(gemsEarned = progress.stats.gemsEarned + reward),
                contracts = ledger.copy(claimedIds = ledger.claimedIds + contract.id),
            )
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
                    merges = prefs[Keys.contractMerges] ?: 0,
                    moves = prefs[Keys.contractMoves] ?: 0,
                    runs = prefs[Keys.contractRuns] ?: 0,
                    maxTileLevel = prefs[Keys.contractMaxTile] ?: 0,
                    overdrives = prefs[Keys.contractOverdrives] ?: 0,
                ),
                claimedIds = prefs[Keys.contractClaimed] ?: emptySet(),
                activeRunSeed = prefs[Keys.contractActiveSeed],
                activeRun = ContractCounters(
                    score = prefs[Keys.contractActiveScore] ?: 0,
                    merges = prefs[Keys.contractActiveMerges] ?: 0,
                    moves = prefs[Keys.contractActiveMoves] ?: 0,
                    maxTileLevel = prefs[Keys.contractActiveMaxTile] ?: 0,
                    overdrives = prefs[Keys.contractActiveOverdrives] ?: 0,
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
        prefs[Keys.gamesPlayed] = p.stats.gamesPlayed
        prefs[Keys.totalScore] = p.stats.totalScore
        prefs[Keys.maxTileLevel] = p.stats.maxTileLevel
        prefs[Keys.totalMerges] = p.stats.totalMerges
        prefs[Keys.maxMergesInOneMove] = p.stats.maxMergesInOneMove
        prefs[Keys.overdrives] = p.stats.overdrives
        prefs[Keys.undos] = p.stats.undos
        prefs[Keys.dailyCompleted] = p.stats.dailyCompleted
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
        prefs[Keys.contractMerges] = p.contracts.totals.merges
        prefs[Keys.contractMoves] = p.contracts.totals.moves
        prefs[Keys.contractRuns] = p.contracts.totals.runs
        prefs[Keys.contractMaxTile] = p.contracts.totals.maxTileLevel
        prefs[Keys.contractOverdrives] = p.contracts.totals.overdrives
        prefs[Keys.contractClaimed] = p.contracts.claimedIds
        if (p.contracts.activeRunSeed != null) {
            prefs[Keys.contractActiveSeed] = p.contracts.activeRunSeed
        } else {
            prefs.remove(Keys.contractActiveSeed)
        }
        prefs[Keys.contractActiveScore] = p.contracts.activeRun.score
        prefs[Keys.contractActiveMerges] = p.contracts.activeRun.merges
        prefs[Keys.contractActiveMoves] = p.contracts.activeRun.moves
        prefs[Keys.contractActiveMaxTile] = p.contracts.activeRun.maxTileLevel
        prefs[Keys.contractActiveOverdrives] = p.contracts.activeRun.overdrives

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

    private fun SavedGame.toContractCounters(): ContractCounters = ContractCounters(
        score = state.score,
        merges = mergesTotal,
        moves = state.moves,
        maxTileLevel = state.maxLevel,
        overdrives = overdrivesSession,
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
