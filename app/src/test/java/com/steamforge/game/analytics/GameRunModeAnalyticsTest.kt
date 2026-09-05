package com.steamforge.game.analytics

import com.steamforge.game.GameRunMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GameRunModeAnalyticsTest {
    @Test
    fun `legacy daily overload keeps boolean and adds explicit mode`() {
        val normal = AnalyticsEvents.gameStarted(runId = "normal", daily = false)
        val daily = AnalyticsEvents.gameStarted(
            runId = "daily",
            daily = true,
            dailyType = "REACH_SCORE",
        )

        assertEquals(false, normal.params["daily"])
        assertEquals("normal", normal.params["run_mode"])
        assertFalse(normal.params.containsKey("daily_type"))

        assertEquals(true, daily.params["daily"])
        assertEquals("daily", daily.params["run_mode"])
        assertEquals("REACH_SCORE", daily.params["daily_type"])
    }

    @Test
    fun `weekly lifecycle events use weekly mode without pretending to be daily`() {
        val start = AnalyticsEvents.gameStarted(
            runId = "weekly-run",
            mode = GameRunMode.WEEKLY,
            dailyType = "SHOULD_NOT_LEAK",
        )
        val finish = AnalyticsEvents.gameFinished(
            runId = "weekly-run",
            score = 1234,
            maxTile = 256,
            moves = 321,
            mode = GameRunMode.WEEKLY,
        )
        val restart = AnalyticsEvents.gameRestarted(
            runId = "weekly-run",
            score = 500,
            maxTile = 128,
            moves = 100,
            mode = GameRunMode.WEEKLY,
        )

        listOf(start, finish, restart).forEach { event ->
            assertEquals(false, event.params["daily"])
            assertEquals("weekly", event.params["run_mode"])
        }
        assertFalse(start.params.containsKey("daily_type"))
    }

    @Test
    fun `weekly merge and highest tile events preserve explicit mode`() {
        val merge = AnalyticsEvents.merge(
            tileLevel = 8,
            tileValue = 256,
            moveNumber = 42,
            mergesInMove = 2,
            scoreGained = 512,
            mode = GameRunMode.WEEKLY,
        )
        val highest = AnalyticsEvents.highestTileUnlocked(
            tileLevel = 8,
            tileValue = 256,
            moveNumber = 42,
            mode = GameRunMode.WEEKLY,
        )

        assertEquals("weekly", merge.params["run_mode"])
        assertEquals(false, merge.params["daily"])
        assertEquals("weekly", highest.params["run_mode"])
        assertEquals(false, highest.params["daily"])
    }
}
