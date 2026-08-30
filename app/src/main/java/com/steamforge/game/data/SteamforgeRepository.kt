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
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "steamforge")

/**
 * Единая точка локального persistence поверх Preferences DataStore.
 * UI/persistence разделены: ViewModel общается только через этот класс.
 * Все наградные операции атомарны внутри одного dataStore.edit.
 */
class SteamforgeRepository(private val context: Context) : DataRepo {

    private object Keys {
        val game = stringPreferencesKey("game_save")
        val finishedGame = stringPreferencesKey("finished_game")
        val gems = intPreferencesKey("gems")
        val totalXp = intPreferencesKey("total_xp")
        val bestScore = intPreferencesKey("best_score")
        // stats
        val gamesPlayed = intPreferencesKey("stat_games")
        val totalScore = longPreferencesKey("stat_total_score")
        val maxTileLevel = intPreferencesKey("stat_max_tile")
        val totalMerges = intPreferencesKey("stat_merges")
        val maxMergesInOneMove = intPreferencesKey("stat_combo")
        val overdrives = intPreferencesKey("stat_overdrives")
        val undos = intPreferencesKey("stat_undos")
        val dailyCompleted = intPreferencesKey("stat_daily")
        val gemsEarned = longPreferencesKey("stat_gems_earned")
        // achievements / cosmetics
        val achievements = stringSetPreferencesKey("achievements")
        val achievementDays = stringSetPreferencesKey("achievement_days")
        val cosmetics = stringSetPreferencesKey("cosmetics")
        // daily
        val dailyChallengeDay = longPreferencesKey("daily_challenge_day")
        val dailyChallengeDone = booleanPreferencesKey("daily_challenge_done")
        val dailyRewardDay = longPreferencesKey("daily_reward_day")
        val dailyRewardStreak = intPreferencesKey("daily_reward_streak")
        // settings
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val animationsEnabled = booleanPreferencesKey("animations_enabled")
        // privacy: null (ключ отсутствует) = выбор ещё не сделан
        val analyticsConsent = booleanPreferencesKey("analytics_consent")
    }

    override val progress: Flow<PlayerProgress> = context.dataStore.data.map(::mapProgress)

    override val savedGame: Flow<SavedGame?> = context.dataStore.data.map { prefs ->
        prefs[Keys.game]?.let { raw -> GameSaveCodec.decode(raw) }
    }

    override val finishedGame: Flow<FinishedGameRecord?> = context.dataStore.data.map { prefs ->
        prefs[Keys.finishedGame]?.let { raw -> FinishedGameCodec.decode(raw) }
    }

    override suspend fun saveGame(state: SavedGame) {
        context.dataStore.edit { it[Keys.game] = GameSaveCodec.encode(state) }
    }

    override suspend fun clearGame() {
        context.dataStore.edit { it.remove(Keys.game) }
    }

    /** Атомарное обновление прогресса поверх актуального значения. */
    override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
        context.dataStore.edit { prefs -> writeProgress(prefs, block(mapProgress(prefs))) }
    }

    override suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, com.steamforge.game.progression.FinishEffects>,
    ) {
        context.dataStore.edit { prefs ->
            val (updated, effects) = finisher(mapProgress(prefs))
            prefs[Keys.finishedGame] = FinishedGameCodec.encode(record.withEffects(effects))
            prefs.remove(Keys.game)
            writeProgress(prefs, updated)
        }
    }

    /** Одна DataStore-транзакция: защита от повторной выдачи, даже если callback придёт повторно. */
    override suspend fun claimDoubleReward(gameResultId: String, gems: Int): Boolean {
        if (gems <= 0) return false
        var granted = false
        context.dataStore.edit { prefs ->
            val record = prefs[Keys.finishedGame]?.let { FinishedGameCodec.decode(it) }
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

    override suspend fun clearFinishedGame() {
        context.dataStore.edit { it.remove(Keys.finishedGame) }
    }

    /** Полный сброс прогресса. Вызывается только после подтверждения в Settings. */
    override suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun mapProgress(prefs: Preferences): PlayerProgress = PlayerProgress(
        gems = prefs[Keys.gems] ?: 0,
        totalXp = prefs[Keys.totalXp] ?: 0,
        bestScore = prefs[Keys.bestScore] ?: 0,
        stats = PlayerStats(
            gamesPlayed = prefs[Keys.gamesPlayed] ?: 0,
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
        soundEnabled = prefs[Keys.soundEnabled] ?: true,
        hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
        animationsEnabled = prefs[Keys.animationsEnabled] ?: true,
        analyticsConsent = prefs[Keys.analyticsConsent],
    )

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
        prefs[Keys.soundEnabled] = p.soundEnabled
        prefs[Keys.hapticsEnabled] = p.hapticsEnabled
        prefs[Keys.animationsEnabled] = p.animationsEnabled
        if (p.analyticsConsent != null) {
            prefs[Keys.analyticsConsent] = p.analyticsConsent
        } else {
            prefs.remove(Keys.analyticsConsent)
        }
    }
}
