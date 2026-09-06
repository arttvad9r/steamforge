package com.steamforge.game.progression

/**
 * Backend-neutral Weekly ranking data. Percentile uses a stable 0..100 scale where larger is better.
 * It is only present when a real ranking provider produced one; the local fallback deliberately
 * reports UNAVAILABLE instead of inventing a value.
 */
data class WeeklyRankingSnapshot(
    val challengeId: String,
    val score: Int,
    val percentile: Double,
    val rank: Int? = null,
    val participantCount: Int? = null,
) {
    init {
        require(challengeId.isNotBlank()) { "challengeId must not be blank" }
        require(score >= 0) { "score must be non-negative" }
        require(percentile.isFinite() && percentile in 0.0..100.0) {
            "percentile must be finite and within 0..100"
        }
        require((rank == null) == (participantCount == null)) {
            "rank and participantCount must either both be present or both be absent"
        }
        if (rank != null && participantCount != null) {
            require(participantCount > 0) { "participantCount must be positive" }
            require(rank in 1..participantCount) { "rank must be within participantCount" }
        }
    }

    fun matches(submission: WeeklyRunSubmission): Boolean =
        challengeId == submission.challengeId && score == submission.finalScore
}

enum class WeeklyRankingStatus {
    RANKED,
    REJECTED,
    UNAVAILABLE,
}

data class WeeklyRankingResult(
    val status: WeeklyRankingStatus,
    val ranking: WeeklyRankingSnapshot? = null,
) {
    init {
        require((status == WeeklyRankingStatus.RANKED) == (ranking != null)) {
            "only RANKED results may carry ranking data"
        }
    }

    /** Fail closed if a remote response belongs to a different challenge or submitted score. */
    fun verifiedFor(submission: WeeklyRunSubmission): WeeklyRankingResult {
        if (status != WeeklyRankingStatus.RANKED) return this
        return if (ranking?.matches(submission) == true) this else rejected()
    }

    companion object {
        fun ranked(ranking: WeeklyRankingSnapshot): WeeklyRankingResult =
            WeeklyRankingResult(status = WeeklyRankingStatus.RANKED, ranking = ranking)

        fun rejected(): WeeklyRankingResult = WeeklyRankingResult(WeeklyRankingStatus.REJECTED)

        fun unavailable(): WeeklyRankingResult = WeeklyRankingResult(WeeklyRankingStatus.UNAVAILABLE)
    }
}

/** Ranking boundary for Stage 16. Runtime code never depends on a concrete backend SDK. */
interface WeeklyRankingProvider {
    suspend fun submit(submission: WeeklyRunSubmission): WeeklyRankingResult
}

/** Offline/default provider used until a real validated ranking service is configured. */
object UnavailableWeeklyRankingProvider : WeeklyRankingProvider {
    override suspend fun submit(submission: WeeklyRunSubmission): WeeklyRankingResult =
        WeeklyRankingResult.unavailable()
}
