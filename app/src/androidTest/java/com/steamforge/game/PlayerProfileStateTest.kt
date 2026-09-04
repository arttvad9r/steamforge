package com.steamforge.game

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steamforge.game.progression.BlueprintCollections
import com.steamforge.game.progression.PermanentProfile
import com.steamforge.game.progression.PlayerStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Validation-only state injector for Stage 10 Profile visual captures. */
@RunWith(AndroidJUnit4::class)
class PlayerProfileStateTest {

    @Test
    fun installDensePermanentProfileState() {
        val app = ApplicationProvider.getApplicationContext<SteamforgeApp>()
        val allPieces = BlueprintCollections.steamEngine.pieces.map { it.id }.toSet()

        val persisted = runBlocking {
            app.container.repo.updateProgress { progress ->
                progress.copy(
                    analyticsConsent = false,
                    totalXp = 50_000,
                    bestScore = 87_654_321,
                    stats = PlayerStats(
                        gamesPlayed = 12_345,
                        bestScore = 87_654_321,
                        totalScore = 987_654_321L,
                        maxTileLevel = 11,
                        totalMerges = 543_210,
                        maxMergesInOneMove = 12,
                        overdrives = 1_234,
                        undos = 321,
                        dailyCompleted = 456,
                        highestDailyStreak = 123,
                        gemsEarned = 765_432L,
                    ),
                    workshopCoreStage = 4,
                    workshopPressureStage = 3,
                    workshopGearPressStage = 2,
                    blueprintPieces = allPieces,
                    unlockedAchievements = setOf("profile_a", "profile_b", "profile_c", "profile_d", "profile_e"),
                )
            }
            app.container.repo.progress.first()
        }

        val snapshot = PermanentProfile.snapshot(persisted)
        assertEquals(12_345, snapshot.gamesPlayed)
        assertEquals(987_654_321L, snapshot.totalScore)
        assertEquals(123, snapshot.highestDailyStreak)
        assertEquals(1, snapshot.collectionsCompleted)
        assertEquals(9, snapshot.workshopStagesCompleted)
    }
}
