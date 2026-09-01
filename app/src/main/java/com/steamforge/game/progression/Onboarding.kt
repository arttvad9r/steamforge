package com.steamforge.game.progression

/**
 * Onboarding не является отдельным tutorial-mode core: этапы управляют только app shell. Первая партия
 * остаётся настоящей, deterministic и сохраняемой тем же GameViewModel.
 */
object Onboarding {
    const val CORE = 0
    const val WORKSHOP = 1
    const val COMPLETE = 2

    fun normalize(step: Int): Int = step.coerceIn(CORE, COMPLETE)

    /**
     * Старые установки не должны внезапно попадать в onboarding после обновления. Если отдельного
     * onboarding-ключа ещё нет, любой реальный след предыдущей игры считается достаточным для COMPLETE.
     */
    fun resolveInitialStep(storedStep: Int?, hasLegacyProgress: Boolean): Int = when {
        storedStep != null -> normalize(storedStep)
        hasLegacyProgress -> COMPLETE
        else -> CORE
    }

    fun hasLegacyProgress(progress: PlayerProgress, hasSavedGame: Boolean): Boolean =
        hasSavedGame ||
            progress.bestScore > 0 ||
            progress.totalXp > 0 ||
            progress.stats.gamesPlayed > 0 ||
            progress.stats.totalMerges > 0 ||
            progress.dailyRewardDay >= 0 ||
            progress.unlockedAchievements.isNotEmpty() ||
            progress.blueprintPieces.isNotEmpty() ||
            progress.contracts.day >= 0 ||
            progress.weekly.challengeId.isNotBlank() ||
            progress.liveOps.eventId.isNotBlank()
}
