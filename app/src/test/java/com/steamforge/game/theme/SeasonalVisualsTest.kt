package com.steamforge.game.theme

import com.steamforge.game.progression.EventTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeasonalVisualsTest {

    @Test
    fun `foundry token resolves to forge palette`() {
        val visual = SeasonalVisuals.resolve(
            EventTheme("foundry", "FOUNDRY WEEK", "Foundry", "forge-orange"),
        )

        assertEquals("forge", visual.id)
        assertEquals("F", visual.badge)
        assertNotEquals(SeasonalVisuals.Default.accent, visual.accent)
    }

    @Test
    fun `approved alternative tokens resolve independently`() {
        assertEquals(
            "patina",
            SeasonalVisuals.resolve(EventTheme("event", "PATINA", "", "patina-teal")).id,
        )
        assertEquals(
            "brass",
            SeasonalVisuals.resolve(EventTheme("event", "BRASS", "", "brass")).id,
        )
        assertEquals(
            "steel",
            SeasonalVisuals.resolve(EventTheme("event", "STEEL", "", "steel-blue")).id,
        )
    }

    @Test
    fun `unknown token fails closed to default palette`() {
        val visual = SeasonalVisuals.resolve(
            EventTheme("remote-experiment", "UNKNOWN", "", "#ff00ff"),
        )

        assertEquals(SeasonalVisuals.Default, visual)
    }

    @Test
    fun `theme id can recover approved palette when token is missing`() {
        assertEquals(
            "forge",
            SeasonalVisuals.resolve(EventTheme("foundry", "FOUNDRY", "", "")).id,
        )
    }

    @Test
    fun `no event uses normal steamforge palette`() {
        assertEquals(SeasonalVisuals.Default, SeasonalVisuals.resolve(null))
    }
}
