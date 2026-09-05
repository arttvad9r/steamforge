package com.steamforge.game.progression

enum class WeeklyRuleType {
    STANDARD_SCORE_ATTACK,
}

data class WeeklyRules(
    val type: WeeklyRuleType = WeeklyRuleType.STANDARD_SCORE_ATTACK,
    /** Account-owned undo resources must not affect competitive runs. */
    val allowUndo: Boolean = false,
    /** Account-owned wrench resources must not affect competitive runs. */
    val allowWrench: Boolean = false,
    /**
     * V1 competitive replay stays on the pure GameEngine score path. Pressure/Overdrive multipliers
     * are therefore disabled until they are explicitly represented in the replay protocol.
     */
    val allowOverdrive: Boolean = false,
)

data class WeeklyChallenge(
    val challengeId: String,
    val startEpochDay: Long,
    /** Exclusive UTC-calendar day boundary. */
    val endEpochDayExclusive: Long,
    val seed: Long,
    val rules: WeeklyRules,
)

/**
 * Global deterministic weekly challenge definition.
 *
 * Weeks are aligned Monday 00:00 UTC -> Monday 00:00 UTC. Every client therefore switches to the
 * same challenge id and seed at the same instant, and the existing replayable RNG path can later run
 * the same spawn sequence without changing Game Core.
 */
object WeeklyChallenges {
    const val DAYS_PER_WEEK = 7L
    private const val MS_PER_DAY = 86_400_000L
    private const val MONDAY_ALIGNMENT_FROM_EPOCH = 3L
    private const val SEED_MULTIPLIER = 6_364_136_223_846_793_005L
    private const val SEED_INCREMENT = 1_442_695_040_888_963_407L

    fun utcEpochDay(nowMs: Long = System.currentTimeMillis()): Long =
        Math.floorDiv(nowMs, MS_PER_DAY)

    fun forUtcMillis(nowMs: Long = System.currentTimeMillis()): WeeklyChallenge =
        forEpochDay(utcEpochDay(nowMs))

    fun weekStartEpochDay(epochDay: Long): Long =
        epochDay - Math.floorMod(epochDay + MONDAY_ALIGNMENT_FROM_EPOCH, DAYS_PER_WEEK)

    fun forEpochDay(epochDay: Long): WeeklyChallenge {
        val start = weekStartEpochDay(epochDay)
        val weekIndex = Math.floorDiv(start + MONDAY_ALIGNMENT_FROM_EPOCH, DAYS_PER_WEEK)
        return WeeklyChallenge(
            challengeId = "weekly-$start",
            startEpochDay = start,
            endEpochDayExclusive = start + DAYS_PER_WEEK,
            seed = weekIndex * SEED_MULTIPLIER + SEED_INCREMENT,
            rules = WeeklyRules(),
        )
    }
}
