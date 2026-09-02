package com.steamforge.game.data

import com.steamforge.game.progression.Achievements
import com.steamforge.game.progression.Blueprints
import com.steamforge.game.progression.DailyContracts
import com.steamforge.game.progression.FinishEffects
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.ProgressionConfig
import com.steamforge.game.progression.RewardedWorkshopBonus
import com.steamforge.game.progression.applyRewardedWorkshopXp
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

    override suspend fun claimDoubleReward(
        gameResultId: String,
        cfg: ProgressionConfig,
    ): RewardedWorkshopBonus? {
        val record = currentFinished ?: return null
        if (record.id != gameResultId || record.rewardedClaimed || record.xpGained <= 0) return null
        val (updated, bonus) = applyRewardedWorkshopXp(currentProgress, record.xpGained, cfg)
        if (bonus.xpGained <= 0) return null
        currentProgress = updated
        currentFinished = record.withRewardedBonus(bonus)
        return bonus
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

    override suspend fun claimContract(day: Long, contractId: String): Boolean {
        val progress = currentProgress
        val ledger = DailyContracts.normalized(progress.contracts, day)
        val contract = DailyContracts.forEpochDay(day).firstOrNull { it.id == contractId } ?: return false
        if (contract.id in ledger.claimedIds || !DailyContracts.isComplete(contract, ledger)) return false

        val firstContractClaimToday = ledger.claimedIds.isEmpty()
        val piece = if (firstContractClaimToday) {
            Blueprints.nextMissingPiece(
                set = Blueprints.steamEngine,
                owned = progress.blueprintPieces,
                seed = day xor contract.id.hashCode().toLong(),
            )
        } else {
            null
        }
        val pieces = if (piece != null) progress.blueprintPieces + piece.id else progress.blueprintPieces
        val workshopUnlocks = Blueprints.workshopUnlocks(pieces)
        currentProgress = progress.copy(
            gems = progress.gems + contract.rewardGems,
            stats = progress.stats.copy(gemsEarned = progress.stats.gemsEarned + contract.rewardGems),
            blueprintPieces = pieces,
            unlockedCosmetics = progress.unlockedCosmetics + workshopUnlocks,
            contracts = ledger.copy(claimedIds = ledger.claimedIds + contract.id),
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
            analyticsConsent = p.analyticsConsent,
        )
        currentGame = null
        currentFinished = null
    }
}
