package com.steamforge.game.progression

import java.util.Calendar
import kotlin.random.Random

enum class DailyGoalType { REACH_TILE, REACH_SCORE, HIGH_MERGES }

data class DailyConfig(
    val gemReward: Int = 15,
    val bonusXp: Int = 60,
)

/**
 * Ежедневное испытание без backend: одна календарная дата -> один и тот же челлендж.
 * Детерминированный вывод из epochDay.
 */
data class DailyChallenge(
    val epochDay: Long,
    val type: DailyGoalType,
    /** REACH_TILE: значение плитки (128..512); REACH_SCORE: очки; HIGH_MERGES: количество объединений. */
    val target: Int,
    /** Минимальный уровень объединения, учитываемый для HIGH_MERGES. */
    val mergeLevel: Int,
    val seed: Long,
    val rewardGems: Int,
    val bonusXp: Int,
) {
    fun isSatisfied(maxTileValue: Int, score: Int, highMerges: Int): Boolean = when (type) {
        DailyGoalType.REACH_TILE -> maxTileValue >= target
        DailyGoalType.REACH_SCORE -> score >= target
        DailyGoalType.HIGH_MERGES -> highMerges >= target
    }
}

object DailyChallenges {

    fun forEpochDay(epochDay: Long, cfg: DailyConfig = DailyConfig()): DailyChallenge {
        val rng = Random(epochDay * 6364136223846793005L + 1442695040888963407L)
        val type = DailyGoalType.entries[rng.nextInt(DailyGoalType.entries.size)]
        val target = when (type) {
            DailyGoalType.REACH_TILE -> intArrayOf(128, 256, 256, 512)[rng.nextInt(4)]
            DailyGoalType.REACH_SCORE -> 300 + rng.nextInt(8) * 100
            DailyGoalType.HIGH_MERGES -> 2 + rng.nextInt(3)
        }
        return DailyChallenge(
            epochDay = epochDay,
            type = type,
            target = target,
            mergeLevel = 6,
            seed = epochDay * 1_000_003L + 4242L,
            rewardGems = cfg.gemReward,
            bonusXp = cfg.bonusXp,
        )
    }
}

/** Локальный день без java.time (minSdk 24, без desugaring). */
object LocalDay {
    private const val MS_PER_DAY = 86_400_000L

    fun todayEpochDay(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        val offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return (nowMs + offset) / MS_PER_DAY
    }

    fun epochDayOf(year: Int, month1: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month1 - 1, day, 12, 0, 0)
        }
        val offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return (cal.timeInMillis + offset) / MS_PER_DAY
    }
}
