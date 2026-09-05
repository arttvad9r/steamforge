package com.steamforge.game.ui.game

import com.steamforge.game.GameRunMode
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameState
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.data.FinishedGameRecord
import com.steamforge.game.data.GameSaveCodec
import com.steamforge.game.data.SavedGame
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.PlayerProgress
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.progression.WeeklyRunReplay
import kotlin.random.Random
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyGameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val weekly = WeeklyChallenges.forEpochDay(20_000L)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }

    private fun vm(
        repo: FakeDataRepo = FakeDataRepo(),
        analytics: RecordingAnalytics = RecordingAnalytics(),
    ) = GameViewModel(
        repo = repo,
        analytics = analytics,
        runMode = GameRunMode.WEEKLY,
        weeklyProvider = { weekly },
        savedGameProvider = { repo.currentGame },
        systemAnimationsEnabled = true,
    )

    @Test
    fun `weekly runs use the challenge seed and typed analytics`() = runTest(dispatcher) {
        val analyticsA = RecordingAnalytics()
        val analyticsB = RecordingAnalytics()
        val a = vm(analytics = analyticsA)
        val b = vm(analytics = analyticsB)
        advanceUntilIdle()

        val canonicalStart = WeeklyRunReplay.replay(weekly, emptyList())
        assertEquals(canonicalStart, a.ui.value.state)
        assertEquals(a.ui.value.state, b.ui.value.state)
        val inputs = List(80) { index -> MOVE_ORDERS[index % MOVE_ORDERS.size][0] }
        inputs.forEach { move ->
            a.onMove(move)
            b.onMove(move)
            advanceUntilIdle()
            assertEquals(a.ui.value.state, b.ui.value.state)
        }

        val started = analyticsA.events.first { it.first == "game_started" }.second
        assertEquals("weekly", started["run_mode"])
        assertEquals(false, started["daily"])
        val moveEvents = analyticsA.events
            .filter { it.first == "merge" || it.first == "highest_tile_unlocked" }
        assertTrue(moveEvents.isNotEmpty())
        assertTrue(moveEvents.all { it.second["run_mode"] == "weekly" })
    }

    @Test
    fun `weekly ignores normal saved and finished state without deleting it`() = runTest(dispatcher) {
        val saved = SavedGame(
            state = GameState(score = 9_999, moves = 77),
            seed = 123L,
            pressure = 88,
            overdriveRemaining = 4,
            freeUndosLeft = 0,
            rngDraws = 42L,
        )
        val finished = FinishedGameRecord(
            id = "normal-finished",
            day = LocalDay.todayEpochDay(),
            daily = false,
            score = 9_999,
            maxTileLevel = 9,
            state = GameSaveCodec.encode(saved),
        )
        val repo = FakeDataRepo(initialGame = saved, initialFinished = finished)
        val model = vm(repo)
        advanceUntilIdle()

        assertEquals(2, model.ui.value.state.tiles.size)
        assertEquals(0, model.ui.value.state.score)
        assertEquals(saved, repo.currentGame)
        assertEquals(finished, repo.currentFinished)

        model.exit()
        advanceUntilIdle()
        assertEquals(saved, repo.currentGame)
        assertEquals(finished, repo.currentFinished)
    }

    @Test
    fun `weekly disables undo wrench and overdrive account advantages`() = runTest(dispatcher) {
        val repo = FakeDataRepo(initialProgress = PlayerProgress(gems = 500))
        val model = vm(repo)
        advanceUntilIdle()

        var accepted = false
        for (move in listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)) {
            val before = model.ui.value.state
            model.onMove(move)
            advanceUntilIdle()
            if (model.ui.value.state != before) {
                accepted = true
                break
            }
        }
        assertTrue(accepted)
        val afterMove = model.ui.value.state

        assertFalse(model.ui.value.canUndo)
        assertEquals(0, model.ui.value.freeUndosLeft)
        model.undo()
        advanceUntilIdle()
        assertEquals(afterMove, model.ui.value.state)

        model.toggleRemovingMode()
        assertFalse(model.ui.value.removingMode)
        model.ui.value.state.tiles.firstOrNull()?.let { tile ->
            assertFalse(model.canRemoveTile(tile))
            model.removeTile(tile)
        }
        advanceUntilIdle()

        assertEquals(500, repo.currentProgress.gems)
        assertEquals(0, model.ui.value.pressure)
        assertEquals(0, model.ui.value.overdriveRemaining)
        assertEquals(0, model.ui.value.overdrivesSession)
        assertNull(repo.currentGame)
    }

    @Test
    fun `weekly game over does not grant normal progression or overwrite finished record`() = runTest(dispatcher) {
        val initialProgress = PlayerProgress(gems = 123, workshopParts = 45, totalXp = 678)
        val sentinelFinishedState = SavedGame(
            state = GameState(score = 321),
            seed = 17L,
            pressure = 0,
            overdriveRemaining = 0,
            freeUndosLeft = 0,
        )
        val sentinelFinished = FinishedGameRecord(
            id = "sentinel-normal",
            day = LocalDay.todayEpochDay(),
            daily = false,
            score = 321,
            maxTileLevel = 6,
            state = GameSaveCodec.encode(sentinelFinishedState),
        )
        val repo = FakeDataRepo(
            initialProgress = initialProgress,
            initialFinished = sentinelFinished,
        )
        val analytics = RecordingAnalytics()
        val model = vm(repo, analytics)
        advanceUntilIdle()

        val probe = GameEngine()
        var acceptedMoves = 0
        while (model.ui.value.state.status == GameStatus.PLAYING && acceptedMoves < 5_000) {
            val state = model.ui.value.state
            val order = MOVE_ORDERS[acceptedMoves % MOVE_ORDERS.size]
            val move = order.firstOrNull { candidate ->
                probe.applyMove(state, candidate, Random(0)).moved
            } ?: break
            model.onMove(move)
            acceptedMoves++
            advanceUntilIdle()
        }

        assertTrue("weekly run did not reach a terminal state", model.ui.value.finished)
        assertTrue(acceptedMoves < 5_000)
        assertEquals(initialProgress, repo.currentProgress)
        assertEquals(sentinelFinished, repo.currentFinished)
        assertNull(repo.currentGame)
        assertNull(model.ui.value.effects)
        assertNull(model.ui.value.gameResultId)
        assertEquals(0, model.ui.value.pressure)
        assertEquals(0, model.ui.value.overdriveRemaining)

        val finishedEvent = analytics.events.last { it.first == "game_finished" }.second
        assertEquals("weekly", finishedEvent["run_mode"])
        assertEquals(false, finishedEvent["daily"])

        model.restart()
        advanceUntilIdle()
        assertEquals(sentinelFinished, repo.currentFinished)
        assertFalse(model.ui.value.finished)
    }

    private companion object {
        val MOVE_ORDERS = listOf(
            listOf(Move.DOWN, Move.LEFT, Move.RIGHT, Move.UP),
            listOf(Move.LEFT, Move.DOWN, Move.RIGHT, Move.UP),
            listOf(Move.DOWN, Move.RIGHT, Move.LEFT, Move.UP),
            listOf(Move.RIGHT, Move.DOWN, Move.LEFT, Move.UP),
        )
    }
}
