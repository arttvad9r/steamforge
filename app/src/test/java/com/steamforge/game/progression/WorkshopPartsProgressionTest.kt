package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkshopPartsProgressionTest {

    private val cfg = ProgressionConfig()

    @Test
    fun `finished game earns deterministic workshop parts`() {
        val summary = GameSummary(
            score = 2400,
            maxTileLevel = 7,
            merges = 24,
            won = true,
        )

        assertEquals(17, WorkshopProgression.partsForGame(summary, cfg))

        val (progress, _) = applyGameFinished(
            progress = PlayerProgress(workshopParts = 6),
            summary = summary,
            cfg = cfg,
        )
        assertEquals(23, progress.workshopParts)
    }

    @Test
    fun `workshop parts reward is positive and merge bonus is capped`() {
        val small = WorkshopProgression.partsForGame(
            GameSummary(maxTileLevel = 2, merges = 0),
            cfg,
        )
        val large = WorkshopProgression.partsForGame(
            GameSummary(maxTileLevel = 8, merges = 50_000),
            cfg,
        )

        assertTrue(small > 0)
        assertTrue(large > small)
        assertEquals(
            cfg.workshopPartsBase + cfg.workshopPartsMaxMergeBonus +
                8 * cfg.workshopPartsPerMaxTileLevel,
            large,
        )
    }

    @Test
    fun `core upgrade refuses overspend and deducts exact cost`() {
        val cost = WorkshopProgression.coreUpgradeCost(0, cfg)!!
        val poor = PlayerProgress(workshopParts = cost - 1)

        assertFalse(WorkshopProgression.canUpgradeCore(poor.workshopParts, poor.workshopCoreStage, cfg))
        assertEquals(poor, WorkshopProgression.upgradeCore(poor, cfg))

        val ready = poor.copy(workshopParts = cost + 5)
        val upgraded = WorkshopProgression.upgradeCore(ready, cfg)

        assertEquals(1, upgraded.workshopCoreStage)
        assertEquals(5, upgraded.workshopParts)
    }

    @Test
    fun `core has five visual states and stops at enhanced`() {
        var progress = PlayerProgress(workshopParts = cfg.workshopCoreUpgradeCosts.sum())

        assertEquals("СЛОМАНО", WorkshopProgression.coreStageLabel(0, cfg))
        assertEquals("КАРКАС", WorkshopProgression.coreStageLabel(1, cfg))
        assertEquals("МЕХАНИЗМЫ", WorkshopProgression.coreStageLabel(2, cfg))
        assertEquals("РАБОТАЕТ", WorkshopProgression.coreStageLabel(3, cfg))
        assertEquals("УСИЛЕНО", WorkshopProgression.coreStageLabel(4, cfg))

        repeat(cfg.workshopCoreUpgradeCosts.size) {
            assertTrue(WorkshopProgression.canUpgradeCore(progress.workshopParts, progress.workshopCoreStage, cfg))
            progress = WorkshopProgression.upgradeCore(progress, cfg)
        }

        assertEquals(4, progress.workshopCoreStage)
        assertEquals(0, progress.workshopParts)
        assertNull(WorkshopProgression.coreUpgradeCost(progress.workshopCoreStage, cfg))
        assertFalse(WorkshopProgression.canUpgradeCore(progress.workshopParts, progress.workshopCoreStage, cfg))
        assertEquals(progress, WorkshopProgression.upgradeCore(progress, cfg))
    }
}
