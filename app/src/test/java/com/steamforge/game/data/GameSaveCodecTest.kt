package com.steamforge.game.data

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameSaveCodecTest {

    @Test
    fun `v5 roundtrip preserves board rng counters and analytics run id`() {
        val original = SavedGame(
            state = GameState(
                tiles = listOf(Tile(1, 1, 0, 0), Tile(7, 5, 3, 2)),
                score = 1234,
                nextTileId = 8,
                won = true,
                moves = 42,
            ),
            seed = 987654321L,
            pressure = 73,
            overdriveRemaining = 3,
            freeUndosLeft = 1,
            rngDraws = 37L,
            mergesTotal = 29,
            maxMergesInOneMove = 3,
            overdrivesSession = 2,
            undosSession = 4,
            highMergesSession = 5,
            analyticsRunId = "normal-7a8e2b4f-6c20-47b7-9e82-44e2386d7c9a",
        )
        assertEquals(original, GameSaveCodec.decode(GameSaveCodec.encode(original)))
    }

    @Test
    fun `v4 remains readable without analytics run id`() {
        val raw = "v4|4|100|3|0|5|42|70|2|1|17|9|3|2|4|5|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(42L, decoded.seed)
        assertEquals(70, decoded.pressure)
        assertEquals(17L, decoded.rngDraws)
        assertEquals(9, decoded.mergesTotal)
        assertEquals(3, decoded.maxMergesInOneMove)
        assertEquals(2, decoded.overdrivesSession)
        assertEquals(4, decoded.undosSession)
        assertEquals(5, decoded.highMergesSession)
        assertNull(decoded.analyticsRunId)
    }

    @Test
    fun `v3 remains readable with zero session counters`() {
        val raw = "v3|4|100|3|0|5|42|70|2|1|17|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(42L, decoded.seed)
        assertEquals(70, decoded.pressure)
        assertEquals(2, decoded.overdriveRemaining)
        assertEquals(1, decoded.freeUndosLeft)
        assertEquals(17L, decoded.rngDraws)
        assertEquals(0, decoded.mergesTotal)
        assertEquals(0, decoded.maxMergesInOneMove)
        assertEquals(0, decoded.overdrivesSession)
        assertEquals(0, decoded.undosSession)
        assertEquals(0, decoded.highMergesSession)
        assertNull(decoded.analyticsRunId)
    }

    @Test
    fun `v2 remains readable with zero rng position`() {
        val raw = "v2|4|100|3|0|5|42|70|2|1|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(42L, decoded.seed)
        assertEquals(70, decoded.pressure)
        assertEquals(2, decoded.overdriveRemaining)
        assertEquals(1, decoded.freeUndosLeft)
        assertEquals(0L, decoded.rngDraws)
        assertNull(decoded.analyticsRunId)
    }

    @Test
    fun `v1 remains readable with safe defaults`() {
        val raw = "v1|4|100|3|0|5|1,1,0,0;2,2,1,1"
        val decoded = GameSaveCodec.decode(raw)!!
        assertEquals(null, decoded.seed)
        assertEquals(0, decoded.pressure)
        assertEquals(0, decoded.overdriveRemaining)
        assertEquals(0, decoded.freeUndosLeft)
        assertEquals(0L, decoded.rngDraws)
        assertNull(decoded.analyticsRunId)
    }

    @Test
    fun `broken or structurally invalid input returns null`() {
        assertNull(GameSaveCodec.decode("garbage"))
        assertNull(GameSaveCodec.decode("v5|bad"))
        assertNull(GameSaveCodec.decode("v4|bad"))
        assertNull(GameSaveCodec.decode("v3|bad"))
        assertNull(GameSaveCodec.decode("v3|4|0|1|0|0|1|0|0|0|0|bad,tile"))
        assertNull(GameSaveCodec.decode("v3|4|0|3|0|0|1|0|0|0|0|1,1,0,0;2,2,0,0"))
        assertNull(GameSaveCodec.decode("v3|4|0|2|0|0|1|0|0|0|0|2,1,0,0"))
        assertNull(GameSaveCodec.decode("v3|4|0|3|0|0|1|0|0|0|0|1,1,4,0;2,2,1,1"))
    }
}
