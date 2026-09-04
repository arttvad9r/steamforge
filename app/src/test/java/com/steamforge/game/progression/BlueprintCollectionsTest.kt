package com.steamforge.game.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintCollectionsTest {

    @Test
    fun `steam engine catalog has six stable unique parts`() {
        val pieces = BlueprintCollections.steamEngine.pieces

        assertEquals(6, pieces.size)
        assertEquals(6, pieces.map { it.id }.toSet().size)
        assertEquals(
            listOf(
                "steam_engine_boiler",
                "steam_engine_piston",
                "steam_engine_valve",
                "steam_engine_flywheel",
                "steam_engine_regulator",
                "steam_engine_pressure_gauge",
            ),
            pieces.map { it.id },
        )
    }

    @Test
    fun `steam engine completion requires all six pieces`() {
        val ids = BlueprintCollections.steamEngine.pieces.map { it.id }

        assertFalse(BlueprintCollections.isSteamEngineComplete(ids.take(5).toSet()))
        assertTrue(BlueprintCollections.isSteamEngineComplete(ids.toSet()))
    }

    @Test
    fun `next missing piece follows finite catalog order without duplicates`() {
        val pieces = BlueprintCollections.steamEngine.pieces
        val owned = setOf(pieces[0].id, pieces[1].id, pieces[3].id)

        assertEquals(pieces[2], BlueprintCollections.nextMissingPiece(BlueprintCollections.STEAM_ENGINE_ID, owned))
        assertNull(
            BlueprintCollections.nextMissingPiece(
                BlueprintCollections.STEAM_ENGINE_ID,
                pieces.map { it.id }.toSet(),
            ),
        )
    }

    @Test
    fun `unknown collection cannot create arbitrary blueprint id`() {
        assertNull(BlueprintCollections.nextMissingPiece("unknown", emptySet()))
    }
}
