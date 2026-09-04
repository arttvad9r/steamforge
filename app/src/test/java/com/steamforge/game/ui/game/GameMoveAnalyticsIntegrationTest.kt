package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.analytics.AnalyticsEvents
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameMoveAnalyticsIntegrationTest {
    private val dispatcher = StandardTestDispatcher()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `accepted merge emits merge and highest tile events from viewmodel`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialGame = mergeReadySave())
        val model = model(repo, analytics)
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertEquals(1, analytics.events.count { it.first == AnalyticsEvents.MERGE })
        assertEquals(1, analytics.events.count { it.first == AnalyticsEvents.HIGHEST_TILE_UNLOCKED })
        val merge = analytics.events.first { it.first == AnalyticsEvents.MERGE }.second
        assertEquals(2, merge["tile_level"])
        assertEquals(4, merge["tile_value"])
        assertEquals(false, merge["daily"])
    }

    @Test
    fun `undo and deterministic replay emit the real merge again`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialGame = mergeReadySave())
        val model = model(repo, analytics)
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()
        model.undo()
        advanceUntilIdle()
        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertEquals(2, analytics.events.count { it.first == AnalyticsEvents.MERGE })
        assertEquals(2, analytics.events.count { it.first == AnalyticsEvents.HIGHEST_TILE_UNLOCKED })
    }

    @Test
    fun `restoring an already persisted move does not replay move analytics`() = runTest(dispatcher) {
        val firstAnalytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialGame = mergeReadySave())
        val first = model(repo, firstAnalytics)
        advanceUntilIdle()
        first.onMove(Move.LEFT)
        advanceUntilIdle()
        assertTrue(firstAnalytics.events.any { it.first == AnalyticsEvents.MERGE })

        val persisted = repo.currentGame
        val restoredAnalytics = RecordingAnalytics()
        val restoredRepo = FakeDataRepo(initialGame = persisted)
        model(restoredRepo, restoredAnalytics)
        advanceUntilIdle()

        assertFalse(restoredAnalytics.events.any { it.first == AnalyticsEvents.MERGE })
        assertFalse(restoredAnalytics.events.any { it.first == AnalyticsEvents.HIGHEST_TILE_UNLOCKED })
    }

    private fun model(repo: FakeDataRepo, analytics: RecordingAnalytics): GameViewModel = GameViewModel(
        repo = repo,
        analytics = analytics,
        seedProvider = { 17L },
        savedGameProvider = { repo.currentGame },
        systemAnimationsEnabled = true,
    )

    private fun mergeReadySave(): SavedGame = SavedGame(
        state = GameState(
            size = 4,
            tiles = listOf(
                Tile(id = 1L, level = 1, row = 0, col = 0),
                Tile(id = 2L, level = 1, row = 0, col = 1),
            ),
            nextTileId = 3L,
        ),
        seed = 17L,
        pressure = 0,
        overdriveRemaining = 0,
        freeUndosLeft = 2,
        rngDraws = 0L,
    )
}
