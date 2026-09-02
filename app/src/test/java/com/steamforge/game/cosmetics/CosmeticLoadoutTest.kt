package com.steamforge.game.cosmetics

import org.junit.Assert.assertEquals
import org.junit.Test

class CosmeticLoadoutTest {
    @Test
    fun `paid choices fall back to classic without entitlement`() {
        val selected = CosmeticLoadout(
            tileSet = CosmeticCatalog.TILE_PATINA,
            workshopTheme = CosmeticCatalog.WORKSHOP_FOUNDRY,
        )

        val effective = selected.effective(tileSetOwned = false, workshopThemeOwned = false)

        assertEquals(CosmeticCatalog.TILE_CLASSIC, effective.tileSet)
        assertEquals(CosmeticCatalog.WORKSHOP_CLASSIC, effective.workshopTheme)
    }

    @Test
    fun `owned paid choices remain equipped`() {
        val selected = CosmeticLoadout(
            tileSet = CosmeticCatalog.TILE_PATINA,
            workshopTheme = CosmeticCatalog.WORKSHOP_FOUNDRY,
        )

        val effective = selected.effective(tileSetOwned = true, workshopThemeOwned = true)

        assertEquals(selected, effective)
    }
}
