package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermanentProfileTest {

    @Test
    fun `snapshot exposes permanent stats and derives meta progress`() {
        val pieces = BlueprintCollections.steamEngine.pieces.map { it.id }.toSet()
        val progress = PlayerProgress(
            totalXp = 240,
            bestScore = 12_345,
            stats = PlayerStats(
                gamesPlayed = 42,
                bestScore = 12_000,
                totalScore = 321_456L,
                maxTileLevel = 11,
                totalMerges = 789,
                maxMergesInOneMove = 6,
                highestDailyStreak = 9,
            ),
            workshopCoreStage = 4,
            workshopPressureStage = 2,
            workshopGearPressStage = 1,
            blueprintPieces = pieces,
            unlockedAchievements = setOf("a", "b", "c"),
        )

        val snapshot = PermanentProfile.snapshot(progress)

        assertEquals(42, snapshot.gamesPlayed)
        assertEquals(321_456L, snapshot.totalScore)
        assertEquals(12_345, snapshot.bestScore)
        assertEquals(2048, snapshot.highestTile)
        assertEquals(789, snapshot.totalMerges)
        assertEquals(6, snapshot.largestCombo)
        assertEquals(9, snapshot.highestDailyStreak)
        assertEquals(1, snapshot.collectionsCompleted)
        assertEquals(BlueprintCollections.all.size, snapshot.collectionsTotal)
        assertEquals(7, snapshot.workshopStagesCompleted)
        assertEquals(12, snapshot.workshopStagesTotal)
        assertEquals(3, snapshot.achievementsUnlocked)
        assertTrue(snapshot.workshopFraction in 0f..1f)
    }

    @Test
    fun `snapshot reports incomplete finite collection without copying ownership`() {
        val progress = PlayerProgress(
            blueprintPieces = setOf(BlueprintCollections.steamEngine.pieces.first().id),
        )

        val snapshot = PermanentProfile.snapshot(progress)

        assertEquals(0, snapshot.collectionsCompleted)
        assertEquals(1, snapshot.collectionsTotal)
        assertEquals(0, snapshot.workshopStagesCompleted)
    }

    @Test
    fun `legacy reward cycle streak is historical lower bound`() {
        val progress = PlayerProgress(
            dailyRewardStreak = 7,
            stats = PlayerStats(highestDailyStreak = 0),
        )

        assertEquals(7, PermanentProfile.snapshot(progress).highestDailyStreak)
    }

    @Test
    fun `snapshot normalizes negative legacy statistics`() {
        val progress = PlayerProgress(
            bestScore = -5,
            dailyRewardStreak = -2,
            stats = PlayerStats(
                gamesPlayed = -2,
                bestScore = -10,
                totalScore = -30L,
                maxTileLevel = -1,
                totalMerges = -4,
                maxMergesInOneMove = -3,
                highestDailyStreak = -8,
            ),
        )

        val snapshot = PermanentProfile.snapshot(progress)

        assertEquals(0, snapshot.gamesPlayed)
        assertEquals(0L, snapshot.totalScore)
        assertEquals(0, snapshot.bestScore)
        assertEquals(0, snapshot.highestTile)
        assertEquals(0, snapshot.totalMerges)
        assertEquals(0, snapshot.largestCombo)
        assertEquals(0, snapshot.highestDailyStreak)
    }
}
