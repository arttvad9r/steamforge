package com.steamforge.game.progression

/** Compact immutable view of the permanent statistics required by Stage 10. */
data class PermanentProfileSnapshot(
    val level: Int,
    val gamesPlayed: Int,
    val totalScore: Long,
    val bestScore: Int,
    val highestTile: Int,
    val totalMerges: Int,
    val largestCombo: Int,
    val highestDailyStreak: Int,
    val collectionsCompleted: Int,
    val collectionsTotal: Int,
    val workshopStagesCompleted: Int,
    val workshopStagesTotal: Int,
    val achievementsUnlocked: Int,
) {
    val workshopFraction: Float
        get() = if (workshopStagesTotal <= 0) 0f else
            (workshopStagesCompleted.toFloat() / workshopStagesTotal).coerceIn(0f, 1f)
}

object PermanentProfile {
    fun snapshot(
        progress: PlayerProgress,
        cfg: ProgressionConfig = ProgressionConfig(),
    ): PermanentProfileSnapshot {
        val collectionsTotal = BlueprintCollections.all.size
        val collectionsCompleted = BlueprintCollections.all.count { collection ->
            BlueprintCollections.isComplete(collection, progress.blueprintPieces)
        }
        val maxStage = WorkshopProgression.maxMechanismStage(cfg)
        val workshopStagesTotal = WorkshopMechanism.entries.size * maxStage
        val workshopStagesCompleted = WorkshopMechanism.entries.sumOf { mechanism ->
            WorkshopProgression.mechanismStage(progress, mechanism, cfg)
        }
        return PermanentProfileSnapshot(
            level = progress.levelInfo(cfg).level,
            gamesPlayed = progress.stats.gamesPlayed.coerceAtLeast(0),
            totalScore = progress.stats.totalScore.coerceAtLeast(0L),
            bestScore = maxOf(progress.bestScore, progress.stats.bestScore).coerceAtLeast(0),
            highestTile = tileValue(progress.stats.maxTileLevel),
            totalMerges = progress.stats.totalMerges.coerceAtLeast(0),
            largestCombo = progress.stats.maxMergesInOneMove.coerceAtLeast(0),
            // Before Stage 10 dailyRewardStreak persisted the 1–7 cycle position. It is still a valid
            // lower bound for the player's historical streak and keeps existing installs migration-safe.
            highestDailyStreak = maxOf(progress.stats.highestDailyStreak, progress.dailyRewardStreak)
                .coerceAtLeast(0),
            collectionsCompleted = collectionsCompleted,
            collectionsTotal = collectionsTotal,
            workshopStagesCompleted = workshopStagesCompleted,
            workshopStagesTotal = workshopStagesTotal,
            achievementsUnlocked = progress.unlockedAchievements.size,
        )
    }

    private fun tileValue(level: Int): Int = when {
        level <= 0 -> 0
        level >= 30 -> 1 shl 30
        else -> 1 shl level
    }
}
