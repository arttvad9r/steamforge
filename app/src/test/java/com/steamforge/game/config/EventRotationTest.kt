package com.steamforge.game.config

import com.steamforge.game.progression.EventRunCounters
import com.steamforge.game.progression.LiveOpsProgression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EventRotationTest {

    @Test
    fun `local fallback rotates foundry and calibration on adjacent weeks`() {
        val foundryDay = 25_000L
        val calibrationDay = foundryDay + 7L
        val foundry = LocalDefaultConfig.value.activeEvent(foundryDay)!!
        val calibration = LocalDefaultConfig.value.activeEvent(calibrationDay)!!
        val foundryAgain = LocalDefaultConfig.value.activeEvent(foundryDay + 14L)!!

        assertEquals("foundry", foundry.theme.id)
        assertEquals("calibration", calibration.theme.id)
        assertNotEquals(foundry.id, calibration.id)
        assertEquals(foundry.theme.id, foundryAgain.theme.id)
        assertEquals(foundry.scoringRule, foundryAgain.scoringRule)
    }

    @Test
    fun `calibration uses score based points without changing game score`() {
        val event = LocalDefaultConfig.value.activeEvent(25_007L)!!
        assertEquals("calibration", event.theme.id)

        assertEquals(
            0,
            LiveOpsProgression.pointsFor(event.scoringRule, EventRunCounters(score = 249)),
        )
        assertEquals(
            1,
            LiveOpsProgression.pointsFor(event.scoringRule, EventRunCounters(score = 250)),
        )
        assertEquals(
            4,
            LiveOpsProgression.pointsFor(event.scoringRule, EventRunCounters(score = 1_249)),
        )
    }

    @Test
    fun `empty rotation keeps legacy single fallback behavior`() {
        val config = RemoteGameConfig(
            fallbackEvent = LocalDefaultConfig.foundryTemplate,
            fallbackEventRotation = emptyList(),
        )
        val event = config.activeEvent(25_007L)!!

        assertEquals("foundry", event.theme.id)
    }
}
