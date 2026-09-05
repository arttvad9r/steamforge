package com.steamforge.game.progression

import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.MoveResult

/**
 * Client-side recorder for a deterministic Weekly Challenge attempt.
 *
 * Only moves accepted by [com.steamforge.game.core.GameEngine] are recorded. Rejected/no-op inputs do
 * not change the board or RNG stream, so omitting them keeps the canonical replay compact without
 * changing the resulting state. The submission carries the actual runtime result; the validator then
 * replays the encoded moves independently instead of trusting the client score.
 */
class WeeklyRunRecorder(
    private val challenge: WeeklyChallenge,
) {
    private val acceptedMoves = ArrayList<Move>()

    var overflowed: Boolean = false
        private set

    val acceptedMoveCount: Int
        get() = acceptedMoves.size

    val canSubmit: Boolean
        get() = !overflowed

    fun record(move: Move, result: MoveResult) {
        if (!result.moved || overflowed) return
        if (acceptedMoves.size >= WeeklyRunReplay.MAX_INPUT_MOVES) {
            overflowed = true
            return
        }
        acceptedMoves += move
    }

    fun submission(finalState: GameState): WeeklyRunSubmission? {
        if (overflowed) return null
        return WeeklyRunSubmission(
            protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION,
            challengeId = challenge.challengeId,
            seed = challenge.seed,
            moveSequence = WeeklyRunReplay.encodeMoves(acceptedMoves),
            finalScore = finalState.score,
            finalMaxTileLevel = finalState.maxLevel,
        )
    }
}
