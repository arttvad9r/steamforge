package com.steamforge.game.analytics

import com.steamforge.game.core.GameState
import com.steamforge.game.core.MergeEvent
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMoveAnalyticsTest {

    @Test
    fun `multi merge emits one merge per core event and every newly crossed highest level`() {
        val result = result(
            moves = 7,
            score = 900,
            scoreGained = 96,
            merges = listOf(
                merge(id = 20, level = 5),
                merge(id = 21, level = 6),
            ),
        )

        val events = GameMoveAnalytics.eventsFor(result, previousMaxLevel = 4, daily = false)

        assertEquals(
            listOf(
                AnalyticsEvents.MERGE,
                AnalyticsEvents.MERGE,
                AnalyticsEvents.HIGHEST_TILE_UNLOCKED,
                AnalyticsEvents.HIGHEST_TILE_UNLOCKED,
            ),
            events.map { it.name },
        )
        assertEquals(5, events[0].params["tile_level"])
        assertEquals(32, events[0].params["tile_value"])
        assertEquals(2, events[0].params["merges_in_move"])
        assertEquals(96, events[0].params["score_gained"])
        assertEquals(7, events[0].params["move"])
        assertEquals(false, events[0].params["daily"])
        assertEquals(listOf(5, 6), events.drop(2).map { it.params["tile_level"] })
    }

    @Test
    fun `duplicate new merge levels emit one highest tile milestone`() {
        val result = result(
            moves = 3,
            score = 128,
            scoreGained = 64,
            merges = listOf(
                merge(id = 30, level = 5),
                merge(id = 31, level = 5),
            ),
        )

        val events = GameMoveAnalytics.eventsFor(result, previousMaxLevel = 4, daily = true)

        assertEquals(2, events.count { it.name == AnalyticsEvents.MERGE })
        val milestones = events.filter { it.name == AnalyticsEvents.HIGHEST_TILE_UNLOCKED }
        assertEquals(1, milestones.size)
        assertEquals(5, milestones.single().params["tile_level"])
        assertEquals(true, milestones.single().params["daily"])
    }

    @Test
    fun `existing highest level does not emit another milestone`() {
        val result = result(
            moves = 8,
            score = 1_000,
            scoreGained = 64,
            merges = listOf(merge(id = 40, level = 6)),
        )

        val events = GameMoveAnalytics.eventsFor(result, previousMaxLevel = 6, daily = false)

        assertEquals(1, events.size)
        assertEquals(AnalyticsEvents.MERGE, events.single().name)
    }

    @Test
    fun `move without merge emits no merge analytics`() {
        val result = MoveResult(
            state = GameState(score = 10, moves = 2),
            moved = true,
            scoreGained = 0,
            merges = emptyList(),
            spawned = Tile(id = 50, level = 1, row = 0, col = 0),
        )

        assertTrue(GameMoveAnalytics.eventsFor(result, previousMaxLevel = 3, daily = false).isEmpty())
    }

    private fun result(
        moves: Int,
        score: Int,
        scoreGained: Int,
        merges: List<MergeEvent>,
    ): MoveResult = MoveResult(
        state = GameState(score = score, moves = moves),
        moved = true,
        scoreGained = scoreGained,
        merges = merges,
        spawned = Tile(id = 99L, level = 1, row = 3, col = 3),
    )

    private fun merge(id: Long, level: Int): MergeEvent = MergeEvent(
        consumedIds = listOf(id - 2, id - 1),
        tile = Tile(id = id, level = level, row = 0, col = 0),
    )
}
