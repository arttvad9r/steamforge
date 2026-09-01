package com.steamforge.game.progression

/** Накопительная статистика игрока — источник для достижений и аналитики. */
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val bestScore: Int = 0,
    val totalScore: Long = 0L,
    val maxTileLevel: Int = 0,
    val totalMerges: Int = 0,
    val maxMergesInOneMove: Int = 0,
    val overdrives: Int = 0,
    val undos: Int = 0,
    val dailyCompleted: Int = 0,
    val gemsEarned: Long = 0L,
) {
    fun mergedWith(summary: GameSummary): PlayerStats = PlayerStats(
        gamesPlayed = gamesPlayed + 1,
        bestScore = maxOf(bestScore, summary.score),
        totalScore = totalScore + summary.score,
        maxTileLevel = maxOf(maxTileLevel, summary.maxTileLevel),
        totalMerges = totalMerges + summary.merges,
        maxMergesInOneMove = maxOf(maxMergesInOneMove, summary.maxMergesInOneMove),
        overdrives = overdrives + summary.overdrives,
        undos = undos + summary.undos,
        dailyCompleted = dailyCompleted,
        gemsEarned = gemsEarned,
    )
}

data class GameSummary(
    val score: Int = 0,
    val maxTileLevel: Int = 0,
    val moves: Int = 0,
    val merges: Int = 0,
    val maxMergesInOneMove: Int = 0,
    val overdrives: Int = 0,
    val undos: Int = 0,
    val won: Boolean = false,
    val daily: Boolean = false,
)

data class ProgressionConfig(
    val pressureMax: Int = 100,
    val pressureBaseGain: Int = 4,
    val pressureGainPerLevel: Int = 3,
    val overdriveMerges: Int = 4,
    val overdriveMultiplier: Int = 2,
    val xpScoreDivisor: Int = 20,
    val xpPerMaxTileLevel: Int = 10,
    val winBonusXp: Int = 60,
    val dailyBonusXp: Int = 60,
    val baseXpToLevel: Int = 120,
    val xpGrowthPerLevel: Int = 40,
    val levelUpGemsBase: Int = 10,
    val levelUpGemsPerLevel: Int = 2,
    val freeUndosPerGame: Int = 2,
    val undoGemsCost: Int = 5,
    val wrenchGemsCost: Int = 10,
    val wrenchMaxTileLevel: Int = 4,
    val dailyRewardCycle: Int = 7,
    val dailyRewardGemsBase: Int = 5,
    val dailyRewardGemsStep: Int = 3,
) {
    fun pressureGainForMerge(mergedLevel: Int): Int = pressureBaseGain + pressureGainPerLevel * (mergedLevel - 1)
    fun levelUpGems(newLevel: Int): Int = levelUpGemsBase + newLevel * levelUpGemsPerLevel
    fun dailyRewardGems(day: Int): Int =
        dailyRewardGemsBase + (day.coerceAtLeast(1) - 1) * dailyRewardGemsStep
}

data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpToNext: Int) {
    val fraction: Float
        get() = if (xpToNext <= 0) 1f else xpIntoLevel.toFloat() / xpToNext
}

object WorkshopProgression {
    fun xpToNext(level: Int, cfg: ProgressionConfig): Int =
        cfg.baseXpToLevel + (level - 1) * cfg.xpGrowthPerLevel

    fun levelInfo(totalXp: Int, cfg: ProgressionConfig): LevelInfo {
        var level = 1
        var rest = totalXp
        while (rest >= xpToNext(level, cfg)) {
            rest -= xpToNext(level, cfg)
            level++
        }
        return LevelInfo(level, rest, xpToNext(level, cfg))
    }

    fun xpForGame(summary: GameSummary, cfg: ProgressionConfig): Int {
        var xp = summary.score / cfg.xpScoreDivisor + summary.maxTileLevel * cfg.xpPerMaxTileLevel
        if (summary.won) xp += cfg.winBonusXp
        return xp
    }
}

data class FinishEffects(
    val xpGained: Int = 0,
    val gemsGained: Int = 0,
    val levelUps: List<Int> = emptyList(),
    val newAchievements: List<AchievementDef> = emptyList(),
    val newBest: Boolean = false,
)

data class RewardedWorkshopBonus(
    val xpGained: Int = 0,
    val gemsGained: Int = 0,
    val levelUps: List<Int> = emptyList(),
)

/**
 * Applies extra Workshop XP without skipping the gem rewards attached to crossed workshop levels.
 * This is intentionally separate from game finish so rewarded completion never replays stats/achievements.
 */
fun applyRewardedWorkshopXp(
    progress: PlayerProgress,
    bonusXp: Int,
    cfg: ProgressionConfig,
): Pair<PlayerProgress, RewardedWorkshopBonus> {
    if (bonusXp <= 0) return progress to RewardedWorkshopBonus()
    val safeXp = bonusXp.coerceAtMost(Int.MAX_VALUE - progress.totalXp)
    if (safeXp <= 0) return progress to RewardedWorkshopBonus()

    val before = WorkshopProgression.levelInfo(progress.totalXp, cfg).level
    val newXp = progress.totalXp + safeXp
    val after = WorkshopProgression.levelInfo(newXp, cfg).level
    val levelUps = (before + 1..after).toList()
    val levelGems = levelUps.sumOf { cfg.levelUpGems(it) }

    return progress.copy(
        totalXp = newXp,
        gems = progress.gems + levelGems,
        stats = progress.stats.copy(gemsEarned = progress.stats.gemsEarned + levelGems),
    ) to RewardedWorkshopBonus(
        xpGained = safeXp,
        gemsGained = levelGems,
        levelUps = levelUps,
    )
}

fun applyGameFinished(
    progress: PlayerProgress,
    summary: GameSummary,
    cfg: ProgressionConfig,
): Pair<PlayerProgress, FinishEffects> {
    val xpGained = WorkshopProgression.xpForGame(summary, cfg)
    val newXp = progress.totalXp + xpGained

    val before = WorkshopProgression.levelInfo(progress.totalXp, cfg).level
    val after = WorkshopProgression.levelInfo(newXp, cfg).level
    val levelUps = (before + 1..after).toList()
    val levelGems = levelUps.sumOf { cfg.levelUpGems(it) }

    val newStats = progress.stats.mergedWith(summary)
    val candidates = Achievements.newlyUnlocked(newStats, progress.unlockedAchievements)
    val achievementGems = candidates.sumOf { it.gemReward }
    val finalStats = newStats.copy(gemsEarned = newStats.gemsEarned + levelGems + achievementGems)

    val newProgress = progress.copy(
        gems = progress.gems + levelGems + achievementGems,
        totalXp = newXp,
        bestScore = maxOf(progress.bestScore, summary.score),
        stats = finalStats,
        unlockedAchievements = progress.unlockedAchievements + candidates.map { it.id }.toSet(),
    )
    val effects = FinishEffects(
        xpGained = xpGained,
        gemsGained = levelGems + achievementGems,
        levelUps = levelUps,
        newAchievements = candidates,
        newBest = summary.score > progress.bestScore,
    )
    return newProgress to effects
}

data class PlayerProgress(
    val gems: Int = 0,
    val totalXp: Int = 0,
    val bestScore: Int = 0,
    val stats: PlayerStats = PlayerStats(),
    val unlockedAchievements: Set<String> = emptySet(),
    val achievementDays: Map<String, Long> = emptyMap(),
    val unlockedCosmetics: Set<String> = emptySet(),
    val blueprintPieces: Set<String> = emptySet(),
    val dailyChallengeDay: Long = -1L,
    val dailyChallengeDone: Boolean = false,
    val dailyRewardDay: Long = -1L,
    val dailyRewardStreak: Int = 0,
    val dailyRewardGraceUsed: Boolean = false,
    val contracts: ContractLedger = ContractLedger(),
    val weekly: WeeklyRecord = WeeklyRecord(),
    val liveOps: LiveOpsLedger = LiveOpsLedger(),
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val analyticsConsent: Boolean? = null,
) {
    fun levelInfo(cfg: ProgressionConfig): LevelInfo = WorkshopProgression.levelInfo(totalXp, cfg)
}
