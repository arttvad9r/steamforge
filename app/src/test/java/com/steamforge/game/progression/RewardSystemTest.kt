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
    fun `rejects negative and empty reward payloads`() {
        val start = PlayerProgress(workshopParts = 8, gems = 9)
        val (updated, receipt) = RewardSystem.apply(
            start,
            Reward.WorkshopParts(-50),
            Reward.Gems(-50),
            Reward.BlueprintPiece("   "),
            Reward.CosmeticUnlock(""),
        )

        assertEquals(start, updated)
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
}
