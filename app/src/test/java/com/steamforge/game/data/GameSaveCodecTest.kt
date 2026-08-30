package com.steamforge.game.data

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameSaveCodecTest {

    @Test
    fun `roundtrip preserves full session`() {
        val game = SavedGame(
            state = GameState(
                size = 4,
                tiles = listOf(Tile(1, 1, 0, 0), Tile(2, 3, 1, 2), Tile(5, 2, 3, 3)),
                score = 1234,
                nextTileId = 6,
                won = true,
                moves = 42,
            ),
            seed = 987654L,
            pressure = 77,
            overdriveRemaining = 3,
            freeUndosLeft = 1,
        )
        assertEquals(game, GameSaveCodec.decode(GameSaveCodec.encode(game)))
    }

    @Test
    fun `empty board roundtrip`() {
        val game = SavedGame(GameState(), seed = 7L, pressure = 0, overdriveRemaining = 0, freeUndosLeft = 2)
        assertEquals(game, GameSaveCodec.decode(GameSaveCodec.encode(game)))
    }

    @Test
    fun `v1 save without meta fields decodes with defaults and does not crash`() {
        // строка формата v1 из предыдущей версии приложения
        val v1 = "v1|4|1234|6|1|42|1,1,0,0;2,3,1,2"
        val decoded = GameSaveCodec.decode(v1)!!
        assertEquals(1234, decoded.state.score)
        assertEquals(42, decoded.state.moves)
        assertEquals(2, decoded.state.tiles.size)
        assertEquals(0, decoded.pressure)
        assertEquals(0, decoded.overdriveRemaining)
        assertEquals(0, decoded.freeUndosLeft)
        assertNull(decoded.seed)
    }

    @Test
    fun `garbage input returns null`() {
        assertNull(GameSaveCodec.decode(""))
        assertNull(GameSaveCodec.decode("garbage"))
        assertNull(GameSaveCodec.decode("v9|4|0|1|0|0|"))
        assertNull(GameSaveCodec.decode("v2|4|0|1|0|0|7|10|2"))
        assertNull(GameSaveCodec.decode("v2|4|x|1|0|0|7|10|2|2|"))
    }
}
