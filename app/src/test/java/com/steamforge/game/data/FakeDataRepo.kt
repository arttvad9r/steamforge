package com.steamforge.game.data

import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDataRepo(
    initialProgress: PlayerProgress = PlayerProgress(),
    initialGame: SavedGame? = null,
    initialFinished: FinishedGameRecord? = null,
) : DataRepo {
    private val progressFlow = MutableStateFlow(initialProgress)
    private val gameFlow = MutableStateFlow(initialGame)
    private val finishedFlow = MutableStateFlow(initialFinished)
    private var lastPaidToolOperationId: String? = null

    override val progress: Flow<PlayerProgress> = progressFlow
    override val savedGame: Flow<SavedGame?> = gameFlow
    override val finishedGame: Flow<FinishedGameRecord?> = finishedFlow

    var currentProgress: PlayerProgress
        get() = progressFlow.value
        set(value) { progressFlow.value = value }

    var currentGame: SavedGame?
        get() = gameFlow.value
        set(value) { gameFlow.value = value }

    var currentFinished: FinishedGameRecord?
        get() = finishedFlow.value
        set(value) { finishedFlow.value = value }

    override suspend fun saveGame(state: SavedGame) {
        currentGame = state
    }

    override suspend fun clearGame() {
        currentGame = null
    }

    override suspend fun updateProgress(block: (PlayerProgress) -> PlayerProgress) {
        currentProgress = block(currentProgress)
    }

    override suspend fun applyPaidTool(
        operationId: String,
        expectedGems: Int,
        gemCost: Int,
        activeGame: SavedGame?,
    ): Boolean {
        if (operationId.isBlank() || gemCost <= 0 || expectedGems < gemCost) return false
        if (lastPaidToolOperationId == operationId) return true
        if (currentProgress.gems != expectedGems) return false

        if (activeGame != null) currentGame = activeGame
        currentProgress = currentProgress.copy(gems = expectedGems - gemCost)
        lastPaidToolOperationId = operationId
        return true
    }

    override suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, FinishEffects>,
    ) {
        if (currentFinished?.id == record.id) {
            currentGame = null
            return
        }
        val (updated, effects) = finisher(currentProgress)
        currentProgress = updated
        currentFinished = record.withEffects(effects)
        currentGame = null
    }

    override suspend fun claimDailyChallenge(day: Long, rewardGems: Int, bonusXp: Int): Boolean {
        val p = currentProgress
        if (p.dailyChallengeDay == day && p.dailyChallengeDone) return false
        val baseStats = p.stats.copy(dailyCompleted = p.stats.dailyCompleted + 1)
        val unlocked = Achievements.newlyUnlocked(baseStats, p.unlockedAchievements)
        val unlockedGems = unlocked.sumOf { it.gemReward }
        val totalGems = rewardGems + unlockedGems
        currentProgress = p.copy(
            dailyChallengeDay = day,
            dailyChallengeDone = true,
            gems = p.gems + totalGems,
            totalXp = p.totalXp + bonusXp,
            stats = baseStats.copy(gemsEarned = baseStats.gemsEarned + totalGems),
            unlockedAchievements = p.unlockedAchievements + unlocked.map { it.id }.toSet(),
            achievementDays = p.achievementDays + unlocked.associate { it.id to day },
        )
        return true
    }

    override suspend fun clearFinishedGame() {
        currentFinished = null
    }

    override suspend fun resetGameProgress() {
        val p = currentProgress
        currentProgress = PlayerProgress(
            soundEnabled = p.soundEnabled,
            hapticsEnabled = p.hapticsEnabled,
            animationsEnabled = p.animationsEnabled,
        )
        currentGame = null
        currentFinished = null
    }
}
