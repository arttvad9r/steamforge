package com.steamforge.game.progression

data class DailyRewardPlan(
    val canClaim: Boolean,
    val rewardDay: Int,
    val visibleStreak: Int,
    val usesGrace: Boolean,
    val graceUsedAfterClaim: Boolean,
)

object ReturnLoop {
    fun dailyRewardPlan(
        lastClaimDay: Long,
        streakDay: Int,
        graceUsed: Boolean,
        today: Long,
        cycleDays: Int,
    ): DailyRewardPlan {
        require(cycleDays > 0)
        val safeStreak = streakDay.coerceIn(0, cycleDays)
        if (lastClaimDay == today) {
            return DailyRewardPlan(
                canClaim = false,
                rewardDay = safeStreak.coerceAtLeast(1),
                visibleStreak = safeStreak,
                usesGrace = false,
                graceUsedAfterClaim = graceUsed,
            )
        }

        val gap = if (lastClaimDay < 0L) Long.MAX_VALUE else today - lastClaimDay
        val continuesNormally = gap == 1L
        val canUseGrace = gap == 2L && !graceUsed
        val continues = continuesNormally || canUseGrace
        val base = if (continues) safeStreak else 0
        val rewardDay = (base % cycleDays) + 1
        val graceAfter = when {
            canUseGrace -> true
            continuesNormally -> false
            else -> false
        }
        return DailyRewardPlan(
            canClaim = true,
            rewardDay = rewardDay,
            visibleStreak = if (continues) safeStreak else 0,
            usesGrace = canUseGrace,
            graceUsedAfterClaim = graceAfter,
        )
    }

    fun visibleStreak(
        lastClaimDay: Long,
        streakDay: Int,
        graceUsed: Boolean,
        today: Long,
        cycleDays: Int,
    ): Int {
        if (lastClaimDay < 0L) return 0
        val safeStreak = streakDay.coerceIn(0, cycleDays)
        val gap = today - lastClaimDay
        return when {
            gap <= 1L -> safeStreak
            gap == 2L && !graceUsed -> safeStreak
            else -> 0
        }
    }
}
