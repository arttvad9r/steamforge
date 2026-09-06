package com.steamforge.game.weekly.server

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameStatus
import com.steamforge.game.core.Move
import com.steamforge.game.core.ReplayableRandom
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.progression.WeeklyRankingWire
import com.steamforge.game.progression.WeeklyReplayValidationStatus
import com.steamforge.game.progression.WeeklyRunReplay
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklySubmissionValidatorTest {
    private val challenge = WeeklyChallenges.forEpochDay(20_000L)
    private val validator = WeeklySubmissionValidator(
        WeeklyChallengeResolver { challengeId ->
            challenge.takeIf { it.challengeId == challengeId }
        },
    )

    @Test
    fun `terminal canonical payload is accepted from deterministic replay`() {
        val moves = playToGameOver()
        val submission = WeeklyRunReplay.submission(challenge, moves)
        val expectedState = WeeklyRunReplay.replay(challenge, moves)

        val result = validator.validate(WeeklyRankingWire.encodeSubmission(submission))

        assertTrue(result is WeeklySubmissionValidation.Accepted)
        val accepted = (result as WeeklySubmissionValidation.Accepted).run
        assertEquals(WeeklyRunReplay.PROTOCOL_VERSION, accepted.protocolVersion)
        assertEquals(challenge.challengeId, accepted.challengeId)
        assertEquals(challenge.seed, accepted.seed)
        assertEquals(expectedState.score, accepted.score)
        assertEquals(expectedState.maxLevel, accepted.maxTileLevel)
        assertEquals(GameStatus.GAME_OVER, expectedState.status)
    }

    @Test
    fun `forged client score is rejected instead of becoming accepted ranking score`() {
        val valid = WeeklyRunReplay.submission(challenge, playToGameOver())
        val forged = valid.copy(finalScore = valid.finalScore + 100_000)

        val result = validator.validate(WeeklyRankingWire.encodeSubmission(forged))

        assertRejected(
            result = result,
            reason = WeeklyServerRejectionReason.REPLAY_REJECTED,
            replayStatus = WeeklyReplayValidationStatus.SCORE_MISMATCH,
        )
    }

    @Test
    fun `canonical partial run is rejected because ranked attempts must be terminal`() {
        val partial = WeeklyRunReplay.submission(challenge, emptyList())

        val result = validator.validate(WeeklyRankingWire.encodeSubmission(partial))

        assertRejected(
            result = result,
            reason = WeeklyServerRejectionReason.REPLAY_REJECTED,
            replayStatus = WeeklyReplayValidationStatus.NOT_TERMINAL,
        )
    }

    @Test
    fun `resolver canonical seed wins over client supplied challenge data`() {
        val submission = WeeklyRunReplay.submission(challenge, playToGameOver())
        val validatorWithDifferentCanonical = WeeklySubmissionValidator(
            WeeklyChallengeResolver { challengeId ->
                challenge.copy(seed = challenge.seed + 1L).takeIf { it.challengeId == challengeId }
            },
        )

        val result = validatorWithDifferentCanonical.validate(
            WeeklyRankingWire.encodeSubmission(submission),
        )

        assertRejected(
            result = result,
            reason = WeeklyServerRejectionReason.REPLAY_REJECTED,
            replayStatus = WeeklyReplayValidationStatus.SEED_MISMATCH,
        )
    }

    @Test
    fun `malformed unsupported protocol and unknown challenge fail closed before acceptance`() {
        assertRejected(
            result = validator.validate("not-json"),
            reason = WeeklyServerRejectionReason.MALFORMED_PAYLOAD,
        )

        val valid = WeeklyRunReplay.submission(challenge, playToGameOver())
        val unsupportedProtocol = valid.copy(protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION + 1)
        assertRejected(
            result = validator.validate(WeeklyRankingWire.encodeSubmission(unsupportedProtocol)),
            reason = WeeklyServerRejectionReason.MALFORMED_PAYLOAD,
        )

        val unknownChallengeValidator = WeeklySubmissionValidator(WeeklyChallengeResolver { null })
        assertRejected(
            result = unknownChallengeValidator.validate(WeeklyRankingWire.encodeSubmission(valid)),
            reason = WeeklyServerRejectionReason.UNKNOWN_CHALLENGE,
        )
    }

    private fun assertRejected(
        result: WeeklySubmissionValidation,
        reason: WeeklyServerRejectionReason,
        replayStatus: WeeklyReplayValidationStatus? = null,
    ) {
        assertTrue(result is WeeklySubmissionValidation.Rejected)
        val rejected = result as WeeklySubmissionValidation.Rejected
        assertEquals(reason, rejected.reason)
        assertEquals(replayStatus, rejected.replayStatus)
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
