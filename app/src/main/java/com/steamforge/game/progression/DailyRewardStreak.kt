package com.steamforge.game.progression

/**
 * Returns the streak that should carry into today's daily reward state.
 *
 * Claim today/yesterday or one missed calendar day keeps the stored streak. Two or more missed days
 * reset it. Keeping this rule in progression prevents Home and Workshop from presenting different
 * streak values for the same persisted progress.
 */
fun continuingDailyRewardStreak(
    lastClaimDay: Long,
    storedStreak: Int,
    today: Long,
): Int {
    val safeStreak = storedStreak.coerceAtLeast(0)
    if (safeStreak == 0) return 0
    val daysSinceClaim = today - lastClaimDay
    return if (daysSinceClaim in 0L..2L) safeStreak else 0
}
