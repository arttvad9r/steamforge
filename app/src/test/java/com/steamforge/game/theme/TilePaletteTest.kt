package com.steamforge.game.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TilePaletteTest {
    @Test
    fun `low tiers stay non glowing`() {
        assertFalse(tileColors(1).glow)
        assertFalse(tileColors(8).glow)
        assertFalse(tileColors(10).glow)
    }

    @Test
    fun `2048 and rarer tiers use controlled glow`() {
        assertTrue(tileColors(11).glow)
        assertTrue(tileColors(12).glow)
        assertTrue(tileColors(13).glow)
    }

    @Test
    fun `2048 4096 and 8192 remain visually distinct`() {
        val core2048 = tileColors(11)
        val core4096 = tileColors(12)
        val core8192 = tileColors(13)

        assertNotEquals(core2048.background, core4096.background)
        assertNotEquals(core4096.background, core8192.background)
        assertNotEquals(core2048.background, core8192.background)
    }

    @Test
    fun `tiers above supported art range clamp to the rarest material`() {
        assertEquals(tileColors(13), tileColors(20))
    }
}
