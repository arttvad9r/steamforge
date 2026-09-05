package com.steamforge.game.progression

import com.steamforge.game.core.Move
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
}
