package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val cfg = ProgressionConfig()

    @Test
    fun `pressure gain grows with merge level`() {
        val low = cfg.pressureGainForMerge(1)
        val mid = cfg.pressureGainForMerge(5)
        val high = cfg.pressureGainForMerge(11)
        assertTrue(low < mid && mid < high)
        assertEquals(4, low)
        assertEquals(4 + 3 * 10, high)
    }

    @Test
    fun `overdrive config is sane`() {
        assertTrue(cfg.overdriveMerges > 0)
        assertEquals(2, cfg.overdriveMultiplier)
        assertEquals(100, cfg.pressureMax)
    }

    @Test
    fun `xp for game combines score level and win while daily bonus is claimed separately`() {
        val base = WorkshopProgression.xpForGame(GameSummary(score = 2000, maxTileLevel = 8), cfg)
        assertEquals(2000 / 20 + 8 * 10, base)
        val won = WorkshopProgression.xpForGame(GameSummary(score = 2000, maxTileLevel = 8, won = true), cfg)
        assertEquals(base + cfg.winBonusXp, won)
        val daily = WorkshopProgression.xpForGame(GameSummary(score = 2000, maxTileLevel = 8, daily = true), cfg)
        assertEquals(base, daily)
    }

    @Test
    fun `level progression is monotonic and thresholds grow`() {
        val l1 = WorkshopProgression.levelInfo(0, cfg)
        assertEquals(1, l1.level)
        val need1 = WorkshopProgression.xpToNext(1, cfg)
        val need2 = WorkshopProgression.xpToNext(2, cfg)
        assertTrue(need2 > need1)
        val l2 = WorkshopProgression.levelInfo(need1, cfg)
        assertEquals(2, l2.level)
        assertEquals(0, l2.xpIntoLevel)
        val l4 = WorkshopProgression.levelInfo(need1 + need2 + need3(), cfg)
        assertEquals(4, l4.level)
    }

    private fun need3() = WorkshopProgression.xpToNext(3, cfg)

    @Test
    fun `level up gems grow with level`() {
        assertTrue(cfg.levelUpGems(3) > cfg.levelUpGems(2))
    }

    @Test
    fun `game finish updates stats xp and best`() {
        val stats = PlayerStats(gamesPlayed = 3, bestScore = 500)
        val (progress, effects) = applyGameFinished(
            progress = PlayerProgress(gems = 10, totalXp = 0, bestScore = 500, stats = stats),
            summary = GameSummary(score = 1200, maxTileLevel = 7, moves = 100, merges = 80, maxMergesInOneMove = 2),
            cfg = cfg,
        )
        assertEquals(4, progress.stats.gamesPlayed)
        assertEquals(1200, progress.bestScore)
        assertTrue(effects.xpGained > 0)
        assertTrue(effects.newBest)
        assertTrue(progress.gems > 10 || effects.gemsGained >= 0)
    }

    @Test
    fun `game finish unlocks first merge achievement and awards gems`() {
        val (progress, effects) = applyGameFinished(
            progress = PlayerProgress(),
            summary = GameSummary(score = 100, maxTileLevel = 2, merges = 10, maxMergesInOneMove = 1),
            cfg = cfg,
        )
        assertTrue("merge_1" in progress.unlockedAchievements)
        assertEquals(3, effects.newAchievements.first { it.id == "merge_1" }.gemReward)
        assertTrue(effects.gemsGained >= 3)
    }

    @Test
    fun `achievements unlock only once`() {
        val (p1, _) = applyGameFinished(PlayerProgress(), GameSummary(merges = 5), cfg)
        val (p2, e2) = applyGameFinished(p1, GameSummary(merges = 5), cfg)
        assertFalse("merge_1" in e2.newAchievements.map { it.id })
        assertEquals(p1.unlockedAchievements, p2.unlockedAchievements)
    }

    @Test
    fun `level up grants gems and reports levels`() {
        val need = WorkshopProgression.xpToNext(1, cfg) + WorkshopProgression.xpToNext(2, cfg)
        val summary = GameSummary(score = need * cfg.xpScoreDivisor, maxTileLevel = 0)
        val (_, effects) = applyGameFinished(
            PlayerProgress(unlockedAchievements = Achievements.all.map { it.id }.toSet()), summary, cfg,
        )
        assertTrue(effects.newAchievements.isEmpty())
        assertEquals(listOf(2, 3), effects.levelUps)
        assertEquals(cfg.levelUpGems(2) + cfg.levelUpGems(3), effects.gemsGained)
    }

    @Test
    fun `same day gives same challenge`() {
        val a = DailyChallenges.forEpochDay(20600)
        val b = DailyChallenges.forEpochDay(20600)
        assertEquals(a, b)
    }

    @Test
    fun `different days usually differ and seeds are valid`() {
        val a = DailyChallenges.forEpochDay(20600)
        val b = DailyChallenges.forEpochDay(20601)
        assertNotEquals(a.seed, b.seed)
        assertTrue(a.seed > 0)
    }

    @Test
    fun `daily goals in expected ranges`() {
        for (day in 20500L..20560L) {
            val c = DailyChallenges.forEpochDay(day)
            when (c.type) {
                DailyGoalType.REACH_TILE -> assertTrue(c.target in setOf(128, 256, 512))
                DailyGoalType.REACH_SCORE -> assertTrue(c.target in 300..1000)
                DailyGoalType.HIGH_MERGES -> assertTrue(c.target in 2..4)
            }
        }
    }

    @Test
    fun `goal satisfaction check`() {
        val c = DailyChallenges.forEpochDay(20601).copy(type = DailyGoalType.REACH_SCORE, target = 500)
        assertTrue(c.isSatisfied(maxTileValue = 8, score = 500, highMerges = 0))
        assertFalse(c.isSatisfied(maxTileValue = 8, score = 499, highMerges = 0))
    }

    @Test
    fun `local day is stable within a day`() {
        val d = LocalDay.epochDayOf(2026, 8, 30)
        assertEquals(d, LocalDay.epochDayOf(2026, 8, 30))
        assertNotEquals(d, LocalDay.epochDayOf(2026, 8, 29))
    }

    @Test
    fun `at least 15 achievements defined`() {
        assertTrue(Achievements.all.size >= 15)
        assertTrue(Achievements.all.none { it.title.isBlank() })
        assertTrue(Achievements.all.all { it.gemReward >= 0 })
    }

    @Test
    fun `hidden achievement unlocks on gems earned`() {
        val stats = PlayerStats(gemsEarned = 500)
        val unlocked = Achievements.newlyUnlocked(stats, emptySet()).map { it.id }
        assertTrue("gems_500" in unlocked)
        val early = Achievements.newlyUnlocked(PlayerStats(gemsEarned = 100), emptySet()).map { it.id }
        assertFalse("gems_500" in early)
    }

    @Test
    fun `progress achievements report capped progress`() {
        val def = Achievements.byId("games_50")!!
        assertEquals(7, def.progressOf(PlayerStats(gamesPlayed = 7)))
        assertEquals(50, def.progressOf(PlayerStats(gamesPlayed = 90)))
    }
}
