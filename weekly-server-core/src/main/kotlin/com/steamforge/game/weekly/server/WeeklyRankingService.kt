package com.steamforge.game.weekly.server

import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRankingSnapshot
import kotlin.coroutines.cancellation.CancellationException

/**
 * Server-recognized identity established by the deployment authentication layer.
 *
 * This value must never be constructed directly from an identifier in the ranking request body. The
 * transport/auth adapter is responsible for authenticating a request first and only then constructing
 * this principal from the verified provider subject/session identity.
 */
data class WeeklyAuthenticatedPrincipal(
    val subject: String,
) {
    init {
        require(subject.isNotBlank()) { "authenticated subject must not be blank" }
        require(subject.length <= MAX_SUBJECT_LENGTH) { "authenticated subject is too long" }
    }

    private companion object {
        const val MAX_SUBJECT_LENGTH = 256
    }
}

/**
 * Persistence/population boundary for competitive Weekly ranking.
 *
 * Implementations must derive the returned snapshot only from server-accepted runs. The population
 * represented by percentile/rank must be deduplicated by authenticated participant for a challenge;
 * repeated requests from one authenticated subject must not inflate participantCount. The concrete
 * attempt policy (for example best accepted attempt vs another documented policy) belongs to the
 * deployment and is intentionally not selected here.
 *
 * The returned challengeId and score must describe [run] exactly. Storage adapters should perform any
 * write plus population measurement atomically enough that rank/participantCount describe one coherent
 * accepted-population snapshot.
 */
interface WeeklyRankingPopulationStore {
    suspend fun recordAndRank(
        principal: WeeklyAuthenticatedPrincipal,
        run: AcceptedWeeklyRun,
    ): WeeklyRankingSnapshot
}

/**
 * Server-side application service between authenticated transport and ranking persistence.
 *
 * Replay validation remains the security boundary for the score itself. Authentication supplies the
 * participant identity used by the population store. Persistence failures are reported as UNAVAILABLE;
 * malformed/forged submissions are REJECTED. Structured-concurrency cancellation always propagates.
 */
class WeeklyRankingService(
    private val validator: WeeklySubmissionValidator,
    private val populationStore: WeeklyRankingPopulationStore,
) {
    suspend fun submit(
        principal: WeeklyAuthenticatedPrincipal,
        payload: String,
    ): WeeklyRankingResult {
        return when (val validation = validator.validate(payload)) {
            is WeeklySubmissionValidation.Rejected -> WeeklyRankingResult.rejected()
            is WeeklySubmissionValidation.Accepted -> rankAccepted(principal, validation.run)
        }
    }

    private suspend fun rankAccepted(
        principal: WeeklyAuthenticatedPrincipal,
        run: AcceptedWeeklyRun,
    ): WeeklyRankingResult {
        val ranking = try {
            populationStore.recordAndRank(principal, run)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return WeeklyRankingResult.unavailable()
        }

        // A storage/adapter bug must never let ranking data for another run cross the trust boundary.
        if (ranking.challengeId != run.challengeId || ranking.score != run.score) {
            return WeeklyRankingResult.unavailable()
        }

        return WeeklyRankingResult.ranked(ranking)
    }
}
