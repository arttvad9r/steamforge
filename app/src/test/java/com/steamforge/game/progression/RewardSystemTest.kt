package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardSystemTest {

    @Test
    fun `applies workshop parts and legacy gems through one system`() {
        val start = PlayerProgress(
            workshopParts = 7,
            gems = 3,
            stats = PlayerStats(gemsEarned = 10),
        )

        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.WorkshopParts(5),
            Reward.Gems(4),
        )

        assertEquals(12, updated.workshopParts)
        assertEquals(7, updated.gems)
        assertEquals(14L, updated.stats.gemsEarned)
        assertEquals(5, receipt.workshopParts)
        assertEquals(4, receipt.gems)
    }

    @Test
    fun `rejects zero negative and empty reward payloads without mutating counters`() {
        val start = PlayerProgress(
            workshopParts = 8,
            gems = 9,
            stats = PlayerStats(gemsEarned = 17),
        )
        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.WorkshopParts(0),
            Reward.Gems(0),
            Reward.WorkshopParts(-50),
            Reward.Gems(-50),
            Reward.BlueprintPiece("   "),
            Reward.CosmeticUnlock(""),
        )

        assertEquals(start, updated)
        assertEquals(17L, updated.stats.gemsEarned)
        assertTrue(receipt.isEmpty)
    }

    @Test
    fun `blueprint and cosmetic rewards are idempotent at profile level`() {
        val start = PlayerProgress(
            blueprintPieces = setOf("boiler"),
            unlockedCosmetics = setOf("gold_gauge"),
        )
        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.BlueprintPiece("boiler"),
            Reward.BlueprintPiece("piston"),
            Reward.CosmeticUnlock("gold_gauge"),
            Reward.CosmeticUnlock("core_trim"),
        )

        assertEquals(setOf("boiler", "piston"), updated.blueprintPieces)
        assertEquals(setOf("gold_gauge", "core_trim"), updated.unlockedCosmetics)
        assertEquals(setOf("piston"), receipt.blueprintPieces)
        assertEquals(setOf("core_trim"), receipt.cosmetics)
        assertFalse(receipt.isEmpty)
    }

    @Test
    fun `numeric rewards saturate instead of overflowing`() {
        val start = PlayerProgress(
            workshopParts = Int.MAX_VALUE - 2,
            gems = Int.MAX_VALUE - 1,
            stats = PlayerStats(gemsEarned = Long.MAX_VALUE - 1),
        )
        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.WorkshopParts(50),
            Reward.Gems(50),
        )

        assertEquals(Int.MAX_VALUE, updated.workshopParts)
        assertEquals(Int.MAX_VALUE, updated.gems)
        assertEquals(Long.MAX_VALUE, updated.stats.gemsEarned)
        assertEquals(2, receipt.workshopParts)
        assertEquals(1, receipt.gems)
    }

    @Test
    fun `receipt only reports requested reward when recovering negative persisted counters`() {
        val start = PlayerProgress(
            workshopParts = -7,
            gems = -9,
            stats = PlayerStats(gemsEarned = -11),
        )

        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.WorkshopParts(3),
            Reward.Gems(4),
        )

        assertEquals(3, updated.workshopParts)
        assertEquals(4, updated.gems)
        assertEquals(4L, updated.stats.gemsEarned)
        assertEquals(3, receipt.workshopParts)
        assertEquals(4, receipt.gems)
    }

    @Test
    fun `fully saturated numeric rewards produce an empty receipt without overflow`() {
        val start = PlayerProgress(
            workshopParts = Int.MAX_VALUE,
            gems = Int.MAX_VALUE,
            stats = PlayerStats(gemsEarned = Long.MAX_VALUE),
        )

        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.WorkshopParts(1),
            Reward.Gems(1),
        )

        assertEquals(start, updated)
        assertTrue(receipt.isEmpty)
    }
}
