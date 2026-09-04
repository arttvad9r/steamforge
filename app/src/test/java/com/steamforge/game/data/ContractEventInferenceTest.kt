package com.steamforge.game.data

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Tile
import com.steamforge.game.progression.GameEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractEventInferenceTest {

    @Test
    fun `infers merge-created tile and excludes spawned tile`() {
        val previous = saved(
            moves = 5,
            merges = 2,
            nextId = 4,
            tiles = listOf(
                Tile(1, 6, 0, 0),
                Tile(2, 6, 0, 1),
                Tile(3, 2, 1, 0),
            ),
        )
        val current = saved(
            moves = 6,
            merges = 3,
            nextId = 6,
            tiles = listOf(
                Tile(4, 7, 0, 0),
                Tile(3, 2, 1, 0),
                Tile(5, 1, 3, 3),
            ),
        )

        assertEquals(listOf(GameEvent.TileCreated(level = 7, count = 1)), inferMergeCreatedTileEvents(previous, current))
    }

    @Test
    fun `groups multiple merge-created tiles by level`() {
        val previous = saved(
            moves = 8,
            merges = 10,
            nextId = 5,
            tiles = listOf(
                Tile(1, 5, 0, 0), Tile(2, 5, 0, 1),
                Tile(3, 5, 1, 0), Tile(4, 5, 1, 1),
            ),
        )
        val current = saved(
            moves = 9,
            merges = 12,
            nextId = 8,
            tiles = listOf(
                Tile(5, 6, 0, 0),
                Tile(6, 6, 1, 0),
                Tile(7, 1, 3, 3),
            ),
        )

        assertEquals(listOf(GameEvent.TileCreated(level = 6, count = 2)), inferMergeCreatedTileEvents(previous, current))
    }

    @Test
    fun `does not guess tile events across skipped autosave`() {
        val previous = saved(
            moves = 4,
            merges = 2,
            nextId = 3,
            tiles = listOf(Tile(1, 4, 0, 0), Tile(2, 4, 0, 1)),
        )
        val current = saved(
            moves = 6,
            merges = 3,
            nextId = 5,
            tiles = listOf(Tile(3, 5, 0, 0), Tile(4, 1, 3, 3)),
        )

        assertTrue(inferMergeCreatedTileEvents(previous, current).isEmpty())
    }

    @Test
    fun `does not infer events across different run seeds`() {
        val previous = saved(1, 0, 3, listOf(Tile(1, 2, 0, 0), Tile(2, 2, 0, 1)), seed = 1L)
        val current = saved(2, 1, 5, listOf(Tile(3, 3, 0, 0), Tile(4, 1, 3, 3)), seed = 2L)

        assertTrue(inferMergeCreatedTileEvents(previous, current).isEmpty())
    }

    @Test
    fun `contract tile-count codec is bounded and ignores malformed entries`() {
        val encoded = encodeContractTileCounts(
            mapOf(
                7 to 3,
                8 to Int.MAX_VALUE,
                0 to 9,
                31 to 4,
                9 to -1,
            ),
        )
        assertEquals(setOf("7:3", "8:10000000"), encoded)

        val decoded = decodeContractTileCounts(encoded + setOf("bad", "0:5", "7:-1", "31:2"))
        assertEquals(mapOf(7 to 3, 8 to 10_000_000), decoded)
    }

    private fun saved(
        moves: Int,
        merges: Int,
        nextId: Long,
        tiles: List<Tile>,
        seed: Long = 42L,
    ) = SavedGame(
        state = GameState(
            tiles = tiles,
            score = 100,
            nextTileId = nextId,
            moves = moves,
        ),
        seed = seed,
        pressure = 0,
        overdriveRemaining = 0,
        freeUndosLeft = 2,
        mergesTotal = merges,
    )
}
