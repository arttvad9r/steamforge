package com.steamforge.game.ui.game

import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.Tile
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.PlayerProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonPassReadinessTelemetryTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()

        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    private fun model(repo: FakeDataRepo, analytics: RecordingAnalytics, seed: Long = 17L) = GameViewModel(
        repo = repo,
        analytics = analytics,
        seedProvider = { seed },
        savedGameProvider = { repo.currentGame },
    )

    private fun finishingSavedGame(seed: Long = 17L): SavedGame {
        val levels = listOf(
            1, 1, 3, 4,
            5, 6, 7, 8,
            9, 10, 1, 2,
            3, 4, 5, 6,
        )
        val tiles = levels.mapIndexed { index, level ->
            Tile(
                id = (index + 1).toLong(),
                level = level,
                row = index / 4,
                col = index % 4,
            )
        }
        return SavedGame(
            state = GameState(
                size = 4,
                tiles = tiles,
                score = 128,
                nextTileId = 17L,
                moves = 20,
            ),
            seed = seed,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 2,
            rngDraws = 0L,
        )
    }

    @Test
    fun `game finished includes actual progression reward and final gem balance`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(
            initialProgress = PlayerProgress(gems = 7),
            initialGame = finishingSavedGame(),
        )
        val model = model(repo, analytics)
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()

        val effects = model.ui.value.effects
        assertNotNull(effects)
        val finished = analytics.events.single { it.first == "game_finished" }.second
        assertEquals(effects!!.xpGained, finished["xp_gained"])
        assertEquals(effects.gemsGained, finished["gems_gained"])
        assertEquals(repo.currentProgress.gems, finished["gem_balance"])
    }

    @Test
    fun `wrench sink records cost and post spend balance`() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 50))
        val model = model(repo, analytics, seed = 7L)
        advanceUntilIdle()
        val target = model.ui.value.state.tiles.first { it.level <= 4 }

        model.toggleRemovingMode()
        assertTrue(model.ui.value.removingMode)
        model.removeTile(target)
        advanceUntilIdle()

        val spent = analytics.events.single { it.first == "powerup_used" }.second
        assertEquals("wrench", spent["type"])
        assertEquals(10, spent["cost_gems"])
        assertEquals(40, spent["gem_balance"])
        assertEquals(40, repo.currentProgress.gems)
    }
}
