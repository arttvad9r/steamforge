package com.steamforge.game.progression

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.ReplayableRandom
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyReplayTest {
    private val challenge = WeeklyChallenges.forEpochDay(20_000L)
    private val moves = List(80) { index ->
        when (index % 4) {
            0 -> Move.LEFT
            1 -> Move.UP
            2 -> Move.RIGHT
            else -> Move.DOWN
        }
    }

    @Test
    fun `same weekly challenge and moves replay to the same state`() {
        val first = WeeklyRunReplay.replay(challenge, moves)
        val second = WeeklyRunReplay.replay(challenge, moves)

        assertEquals(first, second)
        assertTrue(first.tiles.isNotEmpty())
    }

    @Test
    fun `move sequence uses stable LURD wire format`() {
        val sample = listOf(Move.LEFT, Move.UP, Move.RIGHT, Move.DOWN)
        val encoded = WeeklyRunReplay.encodeMoves(sample)

        assertEquals("LURD", encoded)
        assertEquals(sample, WeeklyRunReplay.decodeMoves(encoded))
        assertEquals(null, WeeklyRunReplay.decodeMoves("LUXD"))
    }

    @Test
    fun `canonical submission validates after deterministic replay`() {
        val submission = WeeklyRunReplay.submission(challenge, moves)
        val validation = WeeklyRunReplay.validate(challenge, submission)

        assertTrue(WeeklyRunReplay.supports(challenge))
        assertTrue(validation.valid)
        assertEquals(80, submission.moveSequence.length)
        assertEquals(WeeklyReplayValidationStatus.VALID, validation.status)
        assertEquals(submission.finalScore, validation.replayedState?.score)
        assertEquals(submission.finalMaxTileLevel, validation.replayedState?.maxLevel)
    }

    @Test
    fun `terminal validation rejects canonical partial replay`() {
        val partial = WeeklyRunReplay.submission(challenge, emptyList())

        val portable = WeeklyRunReplay.validate(challenge, partial)
        val terminal = WeeklyRunReplay.validateTerminal(challenge, partial)

        assertTrue(portable.valid)
        assertFalse(terminal.valid)
        assertEquals(WeeklyReplayValidationStatus.NOT_TERMINAL, terminal.status)
        assertEquals(GameStatus.PLAYING, terminal.replayedState?.status)
    }

    @Test
    fun `terminal validation accepts completed deterministic run`() {
        val terminalMoves = playToGameOver()
        val submission = WeeklyRunReplay.submission(challenge, terminalMoves)

        val validation = WeeklyRunReplay.validateTerminal(challenge, submission)

        assertTrue(validation.valid)
        assertEquals(WeeklyReplayValidationStatus.VALID, validation.status)
        assertEquals(GameStatus.GAME_OVER, validation.replayedState?.status)
        assertEquals(terminalMoves.size, submission.moveSequence.length)
    }

    @Test
    fun `tampered score is rejected instead of trusting client value`() {
        val valid = WeeklyRunReplay.submission(challenge, moves)
        val tampered = valid.copy(finalScore = valid.finalScore + 10_000)

        val validation = WeeklyRunReplay.validate(challenge, tampered)

        assertFalse(validation.valid)
        assertEquals(WeeklyReplayValidationStatus.SCORE_MISMATCH, validation.status)
        assertNotEquals(tampered.finalScore, validation.replayedState?.score)
    }

    @Test
    fun `wrong challenge identity and seed are rejected before replay`() {
        val valid = WeeklyRunReplay.submission(challenge, moves)
        val wrongChallenge = valid.copy(challengeId = "weekly-tampered")
        val wrongSeed = valid.copy(seed = valid.seed + 1L)

        assertEquals(
            WeeklyReplayValidationStatus.CHALLENGE_MISMATCH,
            WeeklyRunReplay.validate(challenge, wrongChallenge).status,
        )
        assertEquals(
            WeeklyReplayValidationStatus.SEED_MISMATCH,
            WeeklyRunReplay.validate(challenge, wrongSeed).status,
        )
    }

    @Test
    fun `unsupported competitive rules fail closed before replay`() {
        val valid = WeeklyRunReplay.submission(challenge, moves)
        val unsupported = listOf(
            challenge.copy(rules = challenge.rules.copy(allowUndo = true)),
            challenge.copy(rules = challenge.rules.copy(allowWrench = true)),
            challenge.copy(rules = challenge.rules.copy(allowOverdrive = true)),
        )

        unsupported.forEach { candidate ->
            assertFalse(WeeklyRunReplay.supports(candidate))
            assertEquals(
                WeeklyReplayValidationStatus.UNSUPPORTED_RULES,
                WeeklyRunReplay.validate(candidate, valid).status,
            )
        }
    }

    @Test
    fun `protocol oversized and invalid move sequence are rejected`() {
        val valid = WeeklyRunReplay.submission(challenge, moves)
        val wrongProtocol = valid.copy(protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION + 1)
        val oversized = valid.copy(moveSequence = "L".repeat(WeeklyRunReplay.MAX_INPUT_MOVES + 1))
        val invalid = valid.copy(moveSequence = valid.moveSequence + "X")

        assertEquals(
            WeeklyReplayValidationStatus.PROTOCOL_MISMATCH,
            WeeklyRunReplay.validate(challenge, wrongProtocol).status,
        )
        assertEquals(
            WeeklyReplayValidationStatus.TOO_MANY_MOVES,
            WeeklyRunReplay.validate(challenge, oversized).status,
        )
        assertEquals(
            WeeklyReplayValidationStatus.INVALID_MOVE_SEQUENCE,
            WeeklyRunReplay.validate(challenge, invalid).status,
        )
    }

    private fun playToGameOver(): List<Move> {
        val engine = GameEngine()
        val rng = ReplayableRandom(challenge.seed)
        var state = engine.newGame(rng = rng)
        val accepted = ArrayList<Move>()

        while (state.status == GameStatus.PLAYING && accepted.size < WeeklyRunReplay.MAX_INPUT_MOVES) {
            val order = MOVE_ORDERS[accepted.size % MOVE_ORDERS.size]
            val move = order.firstOrNull { candidate ->
                engine.applyMove(state, candidate, Random(0)).moved
            } ?: break
            val result = engine.applyMove(state, move, rng)
            assertTrue(result.moved)
            accepted += move
            state = result.state
        }

        assertEquals(GameStatus.GAME_OVER, state.status)
        assertTrue(accepted.size < WeeklyRunReplay.MAX_INPUT_MOVES)
        return accepted
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
