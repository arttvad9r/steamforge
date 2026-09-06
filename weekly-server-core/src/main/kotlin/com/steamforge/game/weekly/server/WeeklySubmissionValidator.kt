package com.steamforge.game.weekly.server

import com.steamforge.game.progression.WeeklyChallenge
import com.steamforge.game.progression.WeeklyRankingWire
import com.steamforge.game.progression.WeeklyReplayValidationStatus
import com.steamforge.game.progression.WeeklyRunReplay

/** Resolves the canonical challenge definition that the server is willing to accept. */
fun interface WeeklyChallengeResolver {
    fun resolve(challengeId: String): WeeklyChallenge?
}

enum class WeeklyServerRejectionReason {
    MALFORMED_PAYLOAD,
    UNKNOWN_CHALLENGE,
    REPLAY_REJECTED,
}

data class AcceptedWeeklyRun(
    val protocolVersion: Int,
    val challengeId: String,
    val seed: Long,
    val score: Int,
    val maxTileLevel: Int,
)

sealed interface WeeklySubmissionValidation {
    data class Accepted(val run: AcceptedWeeklyRun) : WeeklySubmissionValidation

    data class Rejected(
        val reason: WeeklyServerRejectionReason,
        val replayStatus: WeeklyReplayValidationStatus? = null,
    ) : WeeklySubmissionValidation
}

/**
 * Pure server-side trust boundary for Stage 16.
 *
 * The accepted result is rebuilt from the canonical challenge plus deterministic terminal replay.
 * Client-supplied score/max-tile values are only consistency claims: if they do not match replay,
 * the submission is rejected and they are never used as the accepted competitive result.
 *
 * Identity, deduplication, rate limiting and active-window/late-grace policy intentionally live
 * outside this validator. [WeeklyChallengeResolver] should only resolve challenges the deployment
 * currently accepts, while this class remains deterministic and backend-vendor neutral.
 */
class WeeklySubmissionValidator(
    private val challengeResolver: WeeklyChallengeResolver,
) {
    fun validate(payload: String): WeeklySubmissionValidation {
        val submission = WeeklyRankingWire.decodeSubmission(payload)
            ?: return WeeklySubmissionValidation.Rejected(
                reason = WeeklyServerRejectionReason.MALFORMED_PAYLOAD,
            )

        val challenge = challengeResolver.resolve(submission.challengeId)
            ?: return WeeklySubmissionValidation.Rejected(
                reason = WeeklyServerRejectionReason.UNKNOWN_CHALLENGE,
            )

        val replay = WeeklyRunReplay.validateTerminal(challenge, submission)
        if (!replay.valid) {
            return WeeklySubmissionValidation.Rejected(
                reason = WeeklyServerRejectionReason.REPLAY_REJECTED,
                replayStatus = replay.status,
            )
        }

        val state = requireNotNull(replay.replayedState)
        return WeeklySubmissionValidation.Accepted(
            AcceptedWeeklyRun(
                protocolVersion = WeeklyRunReplay.PROTOCOL_VERSION,
                challengeId = challenge.challengeId,
                seed = challenge.seed,
                score = state.score,
                maxTileLevel = state.maxLevel,
            ),
        )
    }
}
