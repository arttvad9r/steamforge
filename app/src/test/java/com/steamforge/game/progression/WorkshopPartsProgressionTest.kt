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

    @Test
    fun `selected mechanism upgrades independently and spends exact shared parts`() {
        val cost = WorkshopProgression.mechanismUpgradeCost(0, cfg)!!
        val initial = PlayerProgress(
            workshopParts = cost + 9,
            workshopCoreStage = 2,
            workshopPressureStage = 0,
            workshopGearPressStage = 1,
        )

        val upgraded = WorkshopProgression.upgradeMechanism(
            initial,
            WorkshopMechanism.PRESSURE_GENERATOR,
            cfg,
        )

        assertEquals(2, upgraded.workshopCoreStage)
        assertEquals(1, upgraded.workshopPressureStage)
        assertEquals(1, upgraded.workshopGearPressStage)
        assertEquals(9, upgraded.workshopParts)
    }

    @Test
    fun `all workshop mechanisms share the five stage lifecycle and stop independently`() {
        var progress = PlayerProgress(workshopParts = cfg.workshopCoreUpgradeCosts.sum() * WorkshopMechanism.entries.size)

        WorkshopMechanism.entries.forEach { mechanism ->
            repeat(cfg.workshopCoreUpgradeCosts.size) {
                assertTrue(WorkshopProgression.canUpgradeMechanism(progress, mechanism, cfg))
                progress = WorkshopProgression.upgradeMechanism(progress, mechanism, cfg)
            }
            assertEquals(4, WorkshopProgression.mechanismStage(progress, mechanism, cfg))
            assertFalse(WorkshopProgression.canUpgradeMechanism(progress, mechanism, cfg))
        }

        assertEquals(0, progress.workshopParts)
        assertEquals(4, progress.workshopCoreStage)
        assertEquals(4, progress.workshopPressureStage)
        assertEquals(4, progress.workshopGearPressStage)
    }

    @Test
    fun `out of range mechanism stage is normalized without charging parts`() {
        val corrupted = PlayerProgress(
            workshopParts = 100,
            workshopCoreStage = 2,
            workshopPressureStage = 99,
            workshopGearPressStage = -5,
        )

        val pressure = WorkshopProgression.upgradeMechanism(
            corrupted,
            WorkshopMechanism.PRESSURE_GENERATOR,
            cfg,
        )
        assertEquals(4, pressure.workshopPressureStage)
        assertEquals(100, pressure.workshopParts)

        val press = WorkshopProgression.upgradeMechanism(
            pressure,
            WorkshopMechanism.GEAR_PRESS,
            cfg,
        )
        assertEquals(1, press.workshopGearPressStage)
        assertEquals(80, press.workshopParts)
    }
}
