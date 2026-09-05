package com.steamforge.game.analytics

import com.steamforge.game.GameRunMode
import com.steamforge.game.core.GameState
import com.steamforge.game.core.MergeEvent
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class GameMoveAnalyticsModeTest {
    @Test
    fun `weekly move analytics preserves explicit run mode`() {
        val result = MoveResult(
            state = GameState(score = 512, moves = 12),
            moved = true,
            scoreGained = 64,
            merges = listOf(
                MergeEvent(
                    consumedIds = listOf(1L, 2L),
                    tile = Tile(id = 3L, level = 6, row = 0, col = 0),
                ),
            ),
            spawned = Tile(id = 4L, level = 1, row = 3, col = 3),
        )

        val events = GameMoveAnalytics.eventsFor(
            result = result,
            previousMaxLevel = 5,
            mode = GameRunMode.WEEKLY,
        )

        assertEquals(2, events.size)
        events.forEach { event ->
            assertEquals("weekly", event.params["run_mode"])
            assertEquals(false, event.params["daily"])
        }
    }
}
