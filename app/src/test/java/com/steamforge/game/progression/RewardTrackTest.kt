package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardTrackTest {

    @Test
    fun `foundry event maps to ordered free reward track`() {
        val event = LiveOpsCatalog.foundryWeek(startEpochDay = 100L)
        val track = RewardTrackProgression.forEvent(event)

        assertEquals("${event.id}-reward-track", track.id)
        assertEquals(5, track.levels.size)
        assertEquals(listOf(1, 2, 3, 4, 5), track.levels.map { it.level })
        assertEquals(listOf(100, 250, 500, 900, 1500), track.levels.map { it.progressRequirement })
        assertEquals(event.milestones.map { it.id }, track.levels.map { it.id })
        assertEquals(5, track.levels.first().freeReward.gems)
        assertNull(track.levels.first().premiumReward)
    }

    @Test
    fun `snapshot marks reached unclaimed level claimable and keeps claimed idempotent`() {
        val track = RewardTrackProgression.forEvent(LiveOpsCatalog.foundryWeek(startEpochDay = 100L))
        val firstId = track.levels.first().id
        val snapshot = RewardTrackProgression.snapshot(
            definition = track,
            progress = 300,
            claimedFreeIds = setOf(firstId),
        )

        assertTrue(snapshot.levels[0].freeClaimed)
        assertFalse(snapshot.levels[0].freeClaimable)
        assertFalse(snapshot.levels[1].freeClaimed)
        assertTrue(snapshot.levels[1].freeClaimable)
        assertFalse(snapshot.levels[2].freeClaimable)
        assertEquals(500, snapshot.nextLevel?.progressRequirement)
    }

    @Test
    fun `generic definition supports optional premium reward without enabling a pass`() {
        val premium = RewardTrackReward(cosmeticId = "seasonal-frame")
        val track = RewardTrackDefinition(
            id = "future-season",
            levels = listOf(
                RewardTrackLevel(
                    id = "level-1",
                    level = 1,
                    progressRequirement = 100,
                    freeReward = RewardTrackReward(gems = 5),
                    premiumReward = premium,
                ),
            ),
        )

        assertNotNull(track.levels.single().premiumReward)
        assertEquals("seasonal-frame", track.levels.single().premiumReward?.cosmeticId)
    }
}
