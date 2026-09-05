package com.steamforge.game.progression

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.ReplayableRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyRunRecorderTest {
    private val challenge = WeeklyChallenges.forEpochDay(20_000L)

    @Test
    fun `recorder keeps accepted moves and produces validator-compatible submission`() {
        val recorder = WeeklyRunRecorder(challenge)
        val engine = GameEngine()
        val rng = ReplayableRandom(challenge.seed)
        var state = engine.newGame(rng = rng)
        var accepted = 0
        val inputs = List(120) { index ->
            when (index % 4) {
                0 -> Move.LEFT
                1 -> Move.UP
                2 -> Move.RIGHT
                else -> Move.DOWN
            }
        }

        inputs.forEach { move ->
            val result = engine.applyMove(state, move, rng)
            recorder.record(move, result)
            if (result.moved) {
                accepted++
                state = result.state
            }
        }

        val submission = requireNotNull(recorder.submission(state))
        val validation = WeeklyRunReplay.validate(challenge, submission)

        assertEquals(accepted, recorder.acceptedMoveCount)
        assertEquals(accepted, submission.moveSequence.length)
        assertTrue(recorder.canSubmit)
        assertTrue(validation.valid)
        assertEquals(state, validation.replayedState)
    }

    @Test
    fun `no-op move is not recorded`() {
        val recorder = WeeklyRunRecorder(challenge)
        val noOp = MoveResult(
            state = GameState(score = 123),
            moved = false,
            scoreGained = 0,
            merges = emptyList(),
            spawned = null,
        )

        recorder.record(Move.LEFT, noOp)

        assertEquals(0, recorder.acceptedMoveCount)
        assertEquals("", requireNotNull(recorder.submission(noOp.state)).moveSequence)
    }

    @Test
    fun `submission preserves actual runtime result for independent verification`() {
        val recorder = WeeklyRunRecorder(challenge)
        val moved = MoveResult(
            state = GameState(score = 64),
            moved = true,
            scoreGained = 64,
            merges = emptyList(),
            spawned = null,
        )
        recorder.record(Move.UP, moved)
        val actual = GameState(score = 777)

        val submission = requireNotNull(recorder.submission(actual))

        assertEquals("U", submission.moveSequence)
        assertEquals(777, submission.finalScore)
        assertEquals(actual.maxLevel, submission.finalMaxTileLevel)
    }

    @Test
    fun `oversized attempt disables submission without crashing gameplay`() {
        val recorder = WeeklyRunRecorder(challenge)
        val moved = MoveResult(
            state = GameState(),
            moved = true,
            scoreGained = 0,
            merges = emptyList(),
            spawned = null,
        )

        repeat(WeeklyRunReplay.MAX_INPUT_MOVES + 1) {
            recorder.record(Move.LEFT, moved)
        }

        assertEquals(WeeklyRunReplay.MAX_INPUT_MOVES, recorder.acceptedMoveCount)
        assertTrue(recorder.overflowed)
        assertFalse(recorder.canSubmit)
        assertNull(recorder.submission(GameState(score = 999)))
    }
}
