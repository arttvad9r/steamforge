package com.steamforge.game.ui.game

import com.steamforge.game.GameRunMode
import com.steamforge.game.analytics.Analytics
import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.data.FakeDataRepo
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.progression.WeeklyRunReplay
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class WeeklyGameViewModelRecorderTest {
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

    private object SilentAnalytics : Analytics {
        override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    }

    private fun vm() = GameViewModel(
        repo = FakeDataRepo(),
        analytics = SilentAnalytics,
        runMode = GameRunMode.WEEKLY,
        weeklyProvider = { weekly },
        systemAnimationsEnabled = true,
    )

    @Test
    fun `weekly publishes only verified terminal submission`() = runTest(dispatcher) {
        val model = vm()
        advanceUntilIdle()

        assertNull(model.weeklySubmission.value)
        playToGameOver(model)

        val terminal = model.ui.value.state
        val submission = requireNotNull(model.weeklySubmission.value)
        val validation = WeeklyRunReplay.validate(weekly, submission)

        assertTrue(model.ui.value.finished)
        assertEquals(GameStatus.GAME_OVER, terminal.status)
        assertTrue(validation.valid)
        assertEquals(terminal, validation.replayedState)
        assertEquals(terminal.moves, submission.moveSequence.length)
        assertEquals(terminal.score, submission.finalScore)
        assertEquals(terminal.maxLevel, submission.finalMaxTileLevel)
    }

    @Test
    fun `weekly restart clears submission and starts a fresh recorder`() = runTest(dispatcher) {
        val model = vm()
        advanceUntilIdle()
        playToGameOver(model)

        assertTrue(model.weeklySubmission.value != null)
        model.restart()
        advanceUntilIdle()

        assertNull(model.weeklySubmission.value)
        assertFalse(model.ui.value.finished)
        assertEquals(WeeklyRunReplay.replay(weekly, emptyList()), model.ui.value.state)

        val moved = makeOneAcceptedMove(model)
        assertTrue(moved)
        assertNull(model.weeklySubmission.value)
        assertEquals(1, model.ui.value.state.moves)
    }

    private suspend fun TestScope.playToGameOver(model: GameViewModel) {
        var acceptedMoves = 0
        while (model.ui.value.state.status == GameStatus.PLAYING &&
            acceptedMoves < WeeklyRunReplay.MAX_INPUT_MOVES
        ) {
            val move = chooseAcceptedMove(model, acceptedMoves) ?: break
            model.onMove(move)
            acceptedMoves++
            advanceUntilIdle()
            if (!model.ui.value.finished) {
                assertNull(model.weeklySubmission.value)
            }
        }

        assertTrue("weekly run did not reach a terminal state", model.ui.value.finished)
        assertTrue(acceptedMoves < WeeklyRunReplay.MAX_INPUT_MOVES)
    }

    private suspend fun TestScope.makeOneAcceptedMove(model: GameViewModel): Boolean {
        val move = chooseAcceptedMove(model, 0) ?: return false
        model.onMove(move)
        advanceUntilIdle()
        return model.ui.value.state.moves == 1
    }

    private fun chooseAcceptedMove(model: GameViewModel, acceptedMoves: Int): Move? {
        val state = model.ui.value.state
        val probe = GameEngine()
        return MOVE_ORDERS[acceptedMoves % MOVE_ORDERS.size].firstOrNull { candidate ->
            probe.applyMove(state, candidate, Random(0)).moved
        }
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
