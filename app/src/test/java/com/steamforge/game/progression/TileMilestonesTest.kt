package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileMilestonesTest {

    @Test
    fun `lower tiles do not interrupt gameplay`() {
        assertNull(TileMilestones.newlyReached(previousMaxLevel = 5, newMaxLevel = 5))
    }

    @Test
    fun `64 is the first premium reveal`() {
        val milestone = TileMilestones.newlyReached(previousMaxLevel = 5, newMaxLevel = 6)
        assertEquals(64, milestone?.value)
        assertEquals("PRESSURE VALVE", milestone?.title)
    }

    @Test
    fun `already discovered milestone is not repeated`() {
        assertNull(TileMilestones.newlyReached(previousMaxLevel = 8, newMaxLevel = 8))
    }

    @Test
    fun `crossing multiple thresholds reveals only the highest one`() {
        val milestone = TileMilestones.newlyReached(previousMaxLevel = 5, newMaxLevel = 9)
        assertEquals(512, milestone?.value)
    }

    @Test
    fun `2048 remains the final mechanical core reveal`() {
        val milestone = TileMilestones.newlyReached(previousMaxLevel = 10, newMaxLevel = 11)
        assertEquals(2048, milestone?.value)
        assertEquals("MECHANICAL CORE", milestone?.title)
    }

    @Test
    fun `levels above 2048 do not invent unsupported milestone content`() {
        assertNull(TileMilestones.newlyReached(previousMaxLevel = 11, newMaxLevel = 12))
    }
}
