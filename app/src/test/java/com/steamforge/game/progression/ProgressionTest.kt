package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val cfg = ProgressionConfig()

    @Test
    fun `level progression advances across thresholds`() {
        assertEquals(1, WorkshopProgression.levelInfo(0, cfg).level)
        assertEquals(2, WorkshopProgression.levelInfo(cfg.baseXpToLevel, cfg).level)
        val level3Xp = cfg.baseXpToLevel + WorkshopProgression.xpToNext(2, cfg)
        assertEquals(3, WorkshopProgression.levelInfo(level3Xp, cfg).level)
    }

    @Test
    fun `game xp uses score tile and win but daily bonus is claimed separately`() {
        val normal = GameSummary(score = 1000, maxTileLevel = 7, won = false, daily = false)
        val daily = normal.copy(daily = true)
        val won = normal.copy(won = true)
        val expectedBase = 1000 / cfg.xpScoreDivisor + 7 * cfg.xpPerMaxTileLevel
        assertEquals(expectedBase, WorkshopProgression.xpForGame(normal, cfg))
        assertEquals(expectedBase, WorkshopProgression.xpForGame(daily, cfg))
        assertEquals(expectedBase + cfg.winBonusXp, WorkshopProgression.xpForGame(won, cfg))
    }

    @Test
    fun `apply finish updates best stats achievements and level rewards`() {
        val summary = GameSummary(
            score = 3000,
            maxTileLevel = 8,
            moves = 100,
            merges = 40,
            maxMergesInOneMove = 3,
            overdrives = 5,
            undos = 10,
            won = false,
        )
        val (updated, effects) = applyGameFinished(PlayerProgress(), summary, cfg)
        assertEquals(1, updated.stats.gamesPlayed)
        assertEquals(3000, updated.bestScore)
        assertEquals(3000, updated.stats.bestScore)
        assertEquals(3000L, updated.stats.totalScore)
        assertEquals(8, updated.stats.maxTileLevel)
        assertEquals(40, updated.stats.totalMerges)
        assertTrue(effects.xpGained > 0)
        assertTrue(updated.unlockedAchievements.isNotEmpty())
        assertTrue(updated.gems >= effects.gemsGained)
    }

    @Test
    fun `pressure gain grows with merge level`() {
        assertTrue(cfg.pressureGainForMerge(6) > cfg.pressureGainForMerge(2))
    }

    @Test
    fun `daily challenge deterministic for epoch day`() {
        val a = DailyChallenges.forEpochDay(12345L)
        val b = DailyChallenges.forEpochDay(12345L)
        assertEquals(a, b)
        val c = DailyChallenges.forEpochDay(12346L)
        assertFalse(a.seed == c.seed)
    }

    @Test
    fun `daily goals evaluate correctly`() {
        val reachTile = DailyChallenge(1, DailyGoalType.REACH_TILE, 128, 6, 1, 15, 60)
        assertFalse(reachTile.isSatisfied(64, 9999, 99))
        assertTrue(reachTile.isSatisfied(128, 0, 0))

        val reachScore = DailyChallenge(1, DailyGoalType.REACH_SCORE, 500, 6, 1, 15, 60)
        assertFalse(reachScore.isSatisfied(2048, 499, 99))
        assertTrue(reachScore.isSatisfied(2, 500, 0))

        val high = DailyChallenge(1, DailyGoalType.HIGH_MERGES, 3, 6, 1, 15, 60)
        assertFalse(high.isSatisfied(2048, 9999, 2))
        assertTrue(high.isSatisfied(2, 0, 3))
    }

    @Test
    fun `achievements unlock once`() {
        val stats = PlayerStats(totalMerges = 1, bestScore = 1000, gamesPlayed = 10)
        val first = Achievements.newlyUnlocked(stats, emptySet())
        assertTrue(first.any { it.id == "merge_1" })
        assertTrue(first.any { it.id == "score_1000" })
        assertTrue(first.any { it.id == "games_10" })
        val second = Achievements.newlyUnlocked(stats, first.map { it.id }.toSet())
        assertFalse(second.any { it.id in first.map { a -> a.id } })
    }

    @Test
    fun `daily reward cycle values grow`() {
        assertTrue(cfg.dailyRewardGems(7) > cfg.dailyRewardGems(1))
    }
}
