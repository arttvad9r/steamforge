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
    val highestDailyStreak: Int = 0,
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
        highestDailyStreak = highestDailyStreak,
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
    /** Daily bonus выдаётся атомарно при claimDailyChallenge; здесь оставлен для совместимости конфигурации. */
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
    val workshopPartsBase: Int = 4,
    val workshopPartsMergeDivisor: Int = 12,
    val workshopPartsMaxMergeBonus: Int = 10,
    val workshopPartsPerMaxTileLevel: Int = 1,
    val workshopPartsWinBonus: Int = 4,
    val workshopCoreUpgradeCosts: List<Int> = listOf(20, 35, 55, 80),
) {
    fun pressureGainForMerge(mergedLevel: Int): Int = pressureBaseGain + pressureGainPerLevel * (mergedLevel - 1)
    fun levelUpGems(newLevel: Int): Int = levelUpGemsBase + newLevel * levelUpGemsPerLevel
    fun dailyRewardGems(day: Int): Int = dailyRewardGemsBase + day * dailyRewardGemsStep
}

data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpToNext: Int) {
    val fraction: Float
        get() = if (xpToNext <= 0) 1f else xpIntoLevel.toFloat() / xpToNext
}

enum class WorkshopMechanism(
    val title: String,
    val shortTitle: String,
) {
    CORE("МЕХАНИЧЕСКОЕ ЯДРО", "ЯДРО"),
    PRESSURE_GENERATOR("ГЕНЕРАТОР ДАВЛЕНИЯ", "ГЕНЕРАТОР"),
    GEAR_PRESS("ШЕСТЕРЁНОЧНЫЙ ПРЕСС", "ПРЕСС"),
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
        // Daily completion bonus is granted exactly once by the repository's atomic daily claim.
        return xp
    }

    fun partsForGame(summary: GameSummary, cfg: ProgressionConfig): Int {
        val divisor = cfg.workshopPartsMergeDivisor.coerceAtLeast(1)
        val mergeBonus = (summary.merges.coerceAtLeast(0) / divisor)
            .coerceAtMost(cfg.workshopPartsMaxMergeBonus.coerceAtLeast(0))
        val tileBonus = summary.maxTileLevel.coerceAtLeast(0) * cfg.workshopPartsPerMaxTileLevel.coerceAtLeast(0)
        val winBonus = if (summary.won) cfg.workshopPartsWinBonus.coerceAtLeast(0) else 0
        return (cfg.workshopPartsBase.coerceAtLeast(0) + mergeBonus + tileBonus + winBonus)
            .coerceAtLeast(1)
    }

    fun maxMechanismStage(cfg: ProgressionConfig): Int = cfg.workshopCoreUpgradeCosts.size

    fun maxCoreStage(cfg: ProgressionConfig): Int = maxMechanismStage(cfg)

    fun normalizedMechanismStage(stage: Int, cfg: ProgressionConfig): Int =
        stage.coerceIn(0, maxMechanismStage(cfg))

    fun normalizedCoreStage(stage: Int, cfg: ProgressionConfig): Int =
        normalizedMechanismStage(stage, cfg)

    fun mechanismStageLabel(stage: Int, cfg: ProgressionConfig): String = when (normalizedMechanismStage(stage, cfg)) {
        0 -> "СЛОМАНО"
        1 -> "КАРКАС"
        2 -> "МЕХАНИЗМЫ"
        3 -> "РАБОТАЕТ"
        else -> "УСИЛЕНО"
    }

    fun coreStageLabel(stage: Int, cfg: ProgressionConfig): String = mechanismStageLabel(stage, cfg)

    fun mechanismUpgradeCost(stage: Int, cfg: ProgressionConfig): Int? =
        cfg.workshopCoreUpgradeCosts.getOrNull(normalizedMechanismStage(stage, cfg))

    fun coreUpgradeCost(stage: Int, cfg: ProgressionConfig): Int? = mechanismUpgradeCost(stage, cfg)

    fun mechanismStage(progress: PlayerProgress, mechanism: WorkshopMechanism, cfg: ProgressionConfig): Int =
        normalizedMechanismStage(
            when (mechanism) {
                WorkshopMechanism.CORE -> progress.workshopCoreStage
                WorkshopMechanism.PRESSURE_GENERATOR -> progress.workshopPressureStage
                WorkshopMechanism.GEAR_PRESS -> progress.workshopGearPressStage
            },
            cfg,
        )

    fun canUpgradeMechanism(progress: PlayerProgress, mechanism: WorkshopMechanism, cfg: ProgressionConfig): Boolean {
        val cost = mechanismUpgradeCost(mechanismStage(progress, mechanism, cfg), cfg) ?: return false
        return progress.workshopParts >= cost
    }

    fun canUpgradeCore(parts: Int, stage: Int, cfg: ProgressionConfig): Boolean {
        val cost = coreUpgradeCost(stage, cfg) ?: return false
        return parts >= cost
    }

    fun upgradeMechanism(
        progress: PlayerProgress,
        mechanism: WorkshopMechanism,
        cfg: ProgressionConfig,
    ): PlayerProgress {
        val rawStage = when (mechanism) {
            WorkshopMechanism.CORE -> progress.workshopCoreStage
            WorkshopMechanism.PRESSURE_GENERATOR -> progress.workshopPressureStage
            WorkshopMechanism.GEAR_PRESS -> progress.workshopGearPressStage
        }
        val stage = normalizedMechanismStage(rawStage, cfg)
        val cost = mechanismUpgradeCost(stage, cfg)
        if (cost == null || progress.workshopParts < cost) {
            return if (rawStage == stage) progress else progress.withMechanismStage(mechanism, stage)
        }
        return progress
            .copy(workshopParts = progress.workshopParts - cost)
            .withMechanismStage(mechanism, stage + 1)
    }

    fun upgradeCore(progress: PlayerProgress, cfg: ProgressionConfig): PlayerProgress =
        upgradeMechanism(progress, WorkshopMechanism.CORE, cfg)

    private fun PlayerProgress.withMechanismStage(
        mechanism: WorkshopMechanism,
        stage: Int,
    ): PlayerProgress = when (mechanism) {
        WorkshopMechanism.CORE -> copy(workshopCoreStage = stage)
        WorkshopMechanism.PRESSURE_GENERATOR -> copy(workshopPressureStage = stage)
        WorkshopMechanism.GEAR_PRESS -> copy(workshopGearPressStage = stage)
    }
}

data class FinishEffects(
    val xpGained: Int = 0,
    val gemsGained: Int = 0,
    val workshopPartsGained: Int = 0,
    val levelUps: List<Int> = emptyList(),
    val newAchievements: List<AchievementDef> = emptyList(),
    val newBest: Boolean = false,
)

fun applyGameFinished(
    progress: PlayerProgress,
    summary: GameSummary,
    cfg: ProgressionConfig,
): Pair<PlayerProgress, FinishEffects> {
    val xpGained = WorkshopProgression.xpForGame(summary, cfg)
    val partsReward = WorkshopProgression.partsForGame(summary, cfg)
    val newXp = progress.totalXp + xpGained

    val before = WorkshopProgression.levelInfo(progress.totalXp, cfg).level
    val after = WorkshopProgression.levelInfo(newXp, cfg).level
    val levelUps = (before + 1..after).toList()
    val levelGems = levelUps.sumOf { cfg.levelUpGems(it) }

    val newStats = progress.stats.mergedWith(summary)
    val candidates = Achievements.newlyUnlocked(newStats, progress.unlockedAchievements)
    val achievementGems = candidates.sumOf { it.gemReward }

    val baseProgress = progress.copy(
        totalXp = newXp,
        bestScore = maxOf(progress.bestScore, summary.score),
        stats = newStats,
        unlockedAchievements = progress.unlockedAchievements + candidates.map { it.id }.toSet(),
    )
    val (newProgress, receipt) = RewardSystem.apply(
        baseProgress,
        Reward.WorkshopParts(partsReward),
        Reward.Gems(levelGems + achievementGems),
    )
    val effects = FinishEffects(
        xpGained = xpGained,
        gemsGained = receipt.gems,
        workshopPartsGained = receipt.workshopParts,
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
    val dailyChallengeDay: Long = -1L,
    val dailyChallengeDone: Boolean = false,
    val dailyRewardDay: Long = -1L,
    val dailyRewardStreak: Int = 0,
    val contracts: ContractLedger = ContractLedger(),
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val analyticsConsent: Boolean? = null,
    val workshopParts: Int = 0,
    val workshopCoreStage: Int = 0,
    val workshopPressureStage: Int = 0,
    val workshopGearPressStage: Int = 0,
    val blueprintPieces: Set<String> = emptySet(),
) {
    fun levelInfo(cfg: ProgressionConfig): LevelInfo = WorkshopProgression.levelInfo(totalXp, cfg)
}
