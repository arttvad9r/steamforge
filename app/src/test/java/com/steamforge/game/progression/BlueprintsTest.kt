package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintsTest {

    @Test
    fun `steam engine contains six unique physical pieces`() {
        val set = Blueprints.steamEngine
        assertEquals(6, set.pieces.size)
        assertEquals(6, set.pieces.map { it.id }.toSet().size)
        assertFalse(Blueprints.isComplete(set, emptySet()))
    }

    @Test
    fun `next missing piece never returns an owned duplicate`() {
        val set = Blueprints.steamEngine
        var owned = emptySet<String>()

        repeat(set.pieces.size) { day ->
            val piece = Blueprints.nextMissingPiece(set, owned, seed = 10_000L + day)
            requireNotNull(piece)
            assertFalse(piece.id in owned)
            owned = owned + piece.id
        }

        assertEquals(set.pieces.size, owned.size)
        assertTrue(Blueprints.isComplete(set, owned))
        assertNull(Blueprints.nextMissingPiece(set, owned, seed = 99L))
    }

    @Test
    fun `completed collection unlocks Steam Engine workshop module`() {
        val allPieces = Blueprints.steamEngine.pieces.map { it.id }.toSet()
        val unlocks = Blueprints.workshopUnlocks(allPieces)
        assertTrue(Blueprints.STEAM_ENGINE_UNLOCK in unlocks)
    }
}
