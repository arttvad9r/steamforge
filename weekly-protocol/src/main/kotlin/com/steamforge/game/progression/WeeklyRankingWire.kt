package com.steamforge.game.progression

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_WEEKLY_WIRE_BYTES = 16 * 1024

private val weeklyWireJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
private data class WeeklyRunSubmissionPayload(
    val protocolVersion: Int,
    val challengeId: String,
    /** Decimal string: Weekly seeds use the full signed 64-bit range and must survive JS backends. */
    val seed: String,
    val moveSequence: String,
    val finalScore: Int,
    val finalMaxTileLevel: Int,
)

@Serializable
private enum class WeeklyRankingWireStatus {
    @SerialName("ranked")
    RANKED,

    @SerialName("rejected")
    REJECTED,

    @SerialName("unavailable")
    UNAVAILABLE,
}

@Serializable
private data class WeeklyRankingResponsePayload(
    val protocolVersion: Int,
    val status: WeeklyRankingWireStatus,
    val challengeId: String? = null,
    val score: Int? = null,
    val percentile: Double? = null,
    val rank: Int? = null,
    val participantCount: Int? = null,
)

/**
 * Stable backend-neutral JSON boundary for Stage 16.
 *
 * Domain models intentionally stay independent of JSON annotations. In particular, the deterministic
 * 64-bit seed is encoded as a decimal string so a JavaScript/JSON backend cannot lose precision by
 * parsing it as an IEEE-754 number.
 */
object WeeklyRankingWire {
    fun encodeSubmission(submission: WeeklyRunSubmission): String =
        weeklyWireJson.encodeToString(
            WeeklyRunSubmissionPayload(
                protocolVersion = submission.protocolVersion,
                challengeId = submission.challengeId,
                seed = submission.seed.toString(),
                moveSequence = submission.moveSequence,
                finalScore = submission.finalScore,
                finalMaxTileLevel = submission.finalMaxTileLevel,
            ),
        )

    /** Useful for a shared/backend implementation and protocol contract tests. */
    fun decodeSubmission(payload: String): WeeklyRunSubmission? {
        if (!fitsWireLimit(payload)) return null
        val decoded = runCatching {
            weeklyWireJson.decodeFromString<WeeklyRunSubmissionPayload>(payload)
        }.getOrNull() ?: return null
        if (decoded.protocolVersion != WeeklyRunReplay.PROTOCOL_VERSION) return null
        val seed = decoded.seed.toLongOrNull() ?: return null
        if (decoded.moveSequence.length > WeeklyRunReplay.MAX_INPUT_MOVES) return null

        return WeeklyRunSubmission(
            protocolVersion = decoded.protocolVersion,
            challengeId = decoded.challengeId,
            seed = seed,
            moveSequence = decoded.moveSequence,
            finalScore = decoded.finalScore,
            finalMaxTileLevel = decoded.finalMaxTileLevel,
        )
    }

    /**
     * Decodes ranking data defensively. Unknown protocol versions or invalid ranking invariants are
     * rejected before they can reach presentation code. Challenge/score ownership is still checked
     * with [WeeklyRankingResult.verifiedFor] against the exact submitted run.
     */
    fun decodeResult(payload: String): WeeklyRankingResult? {
        if (!fitsWireLimit(payload)) return null
        val decoded = runCatching {
            weeklyWireJson.decodeFromString<WeeklyRankingResponsePayload>(payload)
        }.getOrNull() ?: return null
        if (decoded.protocolVersion != WeeklyRunReplay.PROTOCOL_VERSION) return null

        return when (decoded.status) {
            WeeklyRankingWireStatus.RANKED -> {
                val challengeId = decoded.challengeId ?: return null
                val score = decoded.score ?: return null
                val percentile = decoded.percentile ?: return null
                runCatching {
                    WeeklyRankingResult.ranked(
                        WeeklyRankingSnapshot(
                            challengeId = challengeId,
                            score = score,
                            percentile = percentile,
                            rank = decoded.rank,
                            participantCount = decoded.participantCount,
                        ),
                    )
                }.getOrNull()
            }

            WeeklyRankingWireStatus.REJECTED -> WeeklyRankingResult.rejected()
            WeeklyRankingWireStatus.UNAVAILABLE -> WeeklyRankingResult.unavailable()
        }
    }

    private fun fitsWireLimit(payload: String): Boolean =
        payload.toByteArray(Charsets.UTF_8).size <= MAX_WEEKLY_WIRE_BYTES
}
