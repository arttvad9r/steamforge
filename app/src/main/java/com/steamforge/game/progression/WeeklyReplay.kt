package com.steamforge.game.progression

import com.steamforge.game.core.GameEngine
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.ReplayableRandom

data class WeeklyRunSubmission(
    val protocolVersion: Int,
    val challengeId: String,
    val seed: Long,
    /** Stable cross-language wire format: one character per input move, using L/U/R/D. */
    val moveSequence: String,
    val finalScore: Int,
    val finalMaxTileLevel: Int,
)

enum class WeeklyReplayValidationStatus {
    VALID,
    PROTOCOL_MISMATCH,
    CHALLENGE_MISMATCH,
    SEED_MISMATCH,
    UNSUPPORTED_RULES,
    TOO_MANY_MOVES,
    INVALID_MOVE_SEQUENCE,
    SCORE_MISMATCH,
    MAX_TILE_MISMATCH,
}

data class WeeklyReplayValidation(
    val status: WeeklyReplayValidationStatus,
    val replayedState: GameState? = null,
) {
    val valid: Boolean
        get() = status == WeeklyReplayValidationStatus.VALID
}

/**
 * Canonical V1 replay path for Weekly Challenge submissions.
 *
 * The client score is never trusted by itself: challenge identity, seed and move sequence are replayed
 * through the real [GameEngine], then score/max tile are compared with the submitted result. This is
 * deliberately Android-free and uses a tiny stable move wire format so the same protocol can later be
 * implemented by a backend without Kotlin enum serialization assumptions.
 */
object WeeklyRunReplay {
    const val PROTOCOL_VERSION = 1
    /** More than 10x the current p90 run length while keeping future backend replay bounded. */
    const val MAX_INPUT_MOVES = 5_000

    fun replay(challenge: WeeklyChallenge, moves: List<Move>): GameState {
        require(supports(challenge)) { "weekly rules are not supported by replay protocol v$PROTOCOL_VERSION" }
        val rng = ReplayableRandom(challenge.seed)
        val engine = GameEngine()
        var state = engine.newGame(rng = rng)
        for (move in moves) {
            val result = engine.applyMove(state, move, rng)
            if (result.moved) state = result.state
        }
        return state
    }

    fun submission(challenge: WeeklyChallenge, moves: List<Move>): WeeklyRunSubmission {
        require(moves.size <= MAX_INPUT_MOVES) { "weekly move sequence too large" }
        val state = replay(challenge, moves)
        return WeeklyRunSubmission(
            protocolVersion = PROTOCOL_VERSION,
            challengeId = challenge.challengeId,
            seed = challenge.seed,
            moveSequence = encodeMoves(moves),
            finalScore = state.score,
            finalMaxTileLevel = state.maxLevel,
        )
    }

    fun validate(
        challenge: WeeklyChallenge,
        submission: WeeklyRunSubmission,
    ): WeeklyReplayValidation {
        if (submission.protocolVersion != PROTOCOL_VERSION) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.PROTOCOL_MISMATCH)
        }
        if (submission.challengeId != challenge.challengeId) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.CHALLENGE_MISMATCH)
        }
        if (submission.seed != challenge.seed) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.SEED_MISMATCH)
        }
        if (!supports(challenge)) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.UNSUPPORTED_RULES)
        }
        if (submission.moveSequence.length > MAX_INPUT_MOVES) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.TOO_MANY_MOVES)
        }
        val moves = decodeMoves(submission.moveSequence)
            ?: return WeeklyReplayValidation(WeeklyReplayValidationStatus.INVALID_MOVE_SEQUENCE)

        val state = replay(challenge, moves)
        if (submission.finalScore != state.score) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.SCORE_MISMATCH, state)
        }
        if (submission.finalMaxTileLevel != state.maxLevel) {
            return WeeklyReplayValidation(WeeklyReplayValidationStatus.MAX_TILE_MISMATCH, state)
        }
        return WeeklyReplayValidation(WeeklyReplayValidationStatus.VALID, state)
    }

    internal fun supports(challenge: WeeklyChallenge): Boolean =
        challenge.rules.type == WeeklyRuleType.STANDARD_SCORE_ATTACK &&
            !challenge.rules.allowUndo &&
            !challenge.rules.allowWrench &&
            !challenge.rules.allowOverdrive

    internal fun encodeMoves(moves: List<Move>): String = buildString(moves.size) {
        moves.forEach { move ->
            append(
                when (move) {
                    Move.LEFT -> 'L'
                    Move.UP -> 'U'
                    Move.RIGHT -> 'R'
                    Move.DOWN -> 'D'
                },
            )
        }
    }

    internal fun decodeMoves(sequence: String): List<Move>? {
        val result = ArrayList<Move>(sequence.length)
        for (code in sequence) {
            val move = when (code) {
                'L' -> Move.LEFT
                'U' -> Move.UP
                'R' -> Move.RIGHT
                'D' -> Move.DOWN
                else -> return null
            }
            result += move
        }
        return result
    }
}
